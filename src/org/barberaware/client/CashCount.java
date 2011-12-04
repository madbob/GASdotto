/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
 *
 *  This is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.barberaware.client;

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class CashCount extends Composite {
	private HorizontalPanel		main;
	private VerticalPanel		totalColumn;
	private VerticalPanel		suppliersColumn;
	private Date			now;

	public CashCount () {
		Label text;

		main = new HorizontalPanel ();
		initWidget ( main );
		main.setStyleName ( "top-spaced" );
		main.setSpacing ( 10 );

		now = new Date ( System.currentTimeMillis () );
		now = new Date ( now.getYear (), now.getMonth (), now.getDate () );

		totalColumn = new VerticalPanel ();
		main.add ( totalColumn );

		text = new Label ( "Consegne di Oggi" );
		totalColumn.add ( text );
		totalColumn.setCellHorizontalAlignment ( text, HasHorizontalAlignment.ALIGN_CENTER );

		text = new Label ();
		text.setStyleName ( "bigger-text" );
		text.setText ( Utils.priceToString ( 0 ) );
		totalColumn.add ( text );
		totalColumn.setCellHorizontalAlignment ( text, HasHorizontalAlignment.ALIGN_CENTER );

		suppliersColumn = null;
	}

	public void addOrder ( OrderUserInterface uorder ) {
		boolean found;
		float tot;
		int row_index;
		ArrayList orders;
		String supplier_name;
		VerticalPanel pan;
		VerticalPanel target_column;
		Label lab;
		FromServer uord;
		OrderUser ord;

		uord = ( FromServer ) uorder;
		if ( checkEligibility ( uord ) == false )
			return;

		target_column = columnByPlace ( uord );
		if ( target_column == null )
			return;

		if ( uord instanceof OrderUserAggregate ) {
			if ( hasSuppliersColumn () == false ) {
				suppliersColumn = new VerticalPanel ();
				suppliersColumn.setSpacing ( 10 );
				main.insert ( suppliersColumn, 0 );

				/*
					Una casella in cima al posto dell'intestazione, una casella al fondo in
					prossimita' dei totali per luogo
				*/

				lab = new Label ( "" );
				suppliersColumn.add ( lab );
				lab.addStyleName ( "high-as-label" );

				lab = new Label ( "" );
				suppliersColumn.add ( lab );
				lab.addStyleName ( "high-as-label" );

				lab = ( Label ) totalColumn.getWidget ( 0 );
				lab.setText ( "" );
				lab.addStyleName ( "high-as-label" );
				totalColumn.setSpacing ( 10 );
			}

			orders = uord.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( OrderUser ) orders.get ( i );
				supplier_name = ord.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" );
				tot = ord.getDeliveredPriceWithFriends ();

				row_index = 1;
				found = false;

				/*
					Cerco tra le caselle della colonna dei
					fornitori, se ne trovo una con lo stesso
					nome sommo nella relativa casella della
					colonna del luogo
				*/

				for ( int a = 1; a < suppliersColumn.getWidgetCount (); a++ ) {
					lab = ( Label ) suppliersColumn.getWidget ( a );

					if ( lab.getText ().equals ( supplier_name ) ) {
						found = true;
						lab = ( Label ) target_column.getWidget ( a );
						lab.removeStyleName ( "bigger-text" );
						lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );
						row_index = a;
						break;
					}
				}

				/*
					Se non trovo il fornitore, creo una
					nuova casella nella prima colonna ed
					itero tutte le altre per metterci la
					nuova cella vuota
				*/

				if ( found == false ) {
					row_index = suppliersColumn.getWidgetCount () - 1;
					suppliersColumn.insert ( new Label ( supplier_name ), row_index );

					for ( int e = 1; e < main.getWidgetCount (); e++ ) {
						pan = ( VerticalPanel ) main.getWidget ( e );

						while ( pan.getWidgetCount () < suppliersColumn.getWidgetCount () ) {
							lab = new Label ( Utils.priceToString ( 0 ) );
							pan.insert ( lab, row_index );
							pan.setCellHorizontalAlignment ( lab, HasHorizontalAlignment.ALIGN_CENTER );
						}

						lab = ( Label ) pan.getWidget ( row_index );

						if ( pan == target_column )
							lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );
					}
				}
			}

			computeAllTotals ( target_column );
		}
		else {
			tot = uorder.getDeliveredPriceWithFriends ();
			lab = ( Label ) target_column.getWidget ( 1 );
			lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );
			computeTotals ();
		}
	}

	public void modOrder ( OrderUserInterface uorder ) {
		FromServer baseorder;
		FromServer uord;
		FromServer past_user_order;
		ArrayList past_orders;

		uord = ( FromServer ) uorder;

		uord = ( FromServer ) uorder;
		if ( checkEligibility ( uord ) == false )
			return;

		baseorder = uord.getObject ( "baseorder" );

		clean ();

		if ( baseorder instanceof OrderAggregate ) {
			past_orders = Utils.getServer ().getObjectsFromCache ( "OrderUserAggregate" );
		}
		else if ( baseorder.getBool ( "parent_aggregate" ) == true ) {
			baseorder = OrderAggregate.retrieveAggregate ( baseorder );
			past_orders = Utils.getServer ().getObjectsFromCache ( "OrderUserAggregate" );
		}
		else {
			past_orders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		}

		for ( int i = 0; i < past_orders.size (); i++ ) {
			past_user_order = ( FromServer ) past_orders.get ( i );

			if ( past_user_order.getObject ( "baseorder" ).equals ( baseorder ) )
				addOrder ( ( OrderUserInterface ) past_user_order );
		}

		/*
			Qui non occorre invocare nuovamente addOrder() sull'ordine in input, in quanto esso gia'
			esiste nella cache dunque viene comunque gestito nel ciclo qui sopra
		*/
	}

	public void delOrder ( OrderUserInterface uorder ) {
		float tot;
		ArrayList orders;
		String supplier_name;
		Label lab;
		VerticalPanel target_column;
		FromServer uord;
		OrderUser ord;

		uord = ( FromServer ) uorder;
		if ( checkEligibility ( uord ) == false )
			return;

		target_column = columnByPlace ( uord );

		if ( hasSuppliersColumn () == true ) {
			if ( uorder instanceof OrderUserAggregate == false )
				return;

			orders = uord.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( OrderUser ) orders.get ( i );
				supplier_name = ord.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" );
				tot = ord.getDeliveredPriceWithFriends ();

				for ( int a = 1; a < suppliersColumn.getWidgetCount (); a++ ) {
					lab = ( Label ) suppliersColumn.getWidget ( a );

					if ( lab.getText ().equals ( supplier_name ) ) {
						lab = ( Label ) target_column.getWidget ( a );
						lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) - tot ) );
						break;
					}
				}
			}

			computeAllTotals ( target_column );
		}
		else {
			tot = uorder.getDeliveredPriceWithFriends ();
			lab = ( Label ) target_column.getWidget ( 1 );
			lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) - tot ) );
			computeTotals ();
		}
	}

	public void clean () {
		Label lab;

		while ( main.getWidgetCount () != 1 )
			main.remove ( 0 );

		while ( totalColumn.getWidgetCount () != 2 )
			totalColumn.remove ( 1 );

		lab = ( Label ) totalColumn.getWidget ( 1 );
		lab.setText ( Utils.priceToString ( 0 ) );

		suppliersColumn = null;
	}

	/*
		Questa viene invocata quando c'e' un solo fornitore, e comunque
		fa qualcosa di utile solo se sono contemplati luoghi di consegna
	*/
	private void computeTotals () {
		float tot;
		float maintot;
		Label lab;
		VerticalPanel pan;

		if ( main.getWidgetCount () > 1 ) {
			tot = 0;

			for ( int e = 0; e < main.getWidgetCount () - 1; e++ ) {
				pan = ( VerticalPanel ) main.getWidget ( e );
				lab = ( Label ) pan.getWidget ( 1 );
				tot += Utils.stringToPrice ( lab.getText () );
			}

			lab = ( Label ) totalColumn.getWidget ( 1 );
			lab.setText ( Utils.priceToString ( tot ) );
		}
	}

	/*
		Questa viene invocata in presenza della tabella completa
		fornitori x luoghi di consegna
	*/
	private void computeAllTotals ( VerticalPanel target_column ) {
		float tot;
		float maintot;
		Label lab;
		VerticalPanel pan;

		if ( target_column != totalColumn ) {
			maintot = 0;

			for ( int i = 1; i < totalColumn.getWidgetCount () - 1; i++ ) {
				tot = 0;

				for ( int e = 1; e < main.getWidgetCount () - 1; e++ ) {
					pan = ( VerticalPanel ) main.getWidget ( e );
					lab = ( Label ) pan.getWidget ( i );
					tot += Utils.stringToPrice ( lab.getText () );
				}

				lab = ( Label ) totalColumn.getWidget ( i );
				lab.setText ( Utils.priceToString ( tot ) );

				maintot += tot;
			}

			lab = ( Label ) totalColumn.getWidget ( target_column.getWidgetCount () - 1 );
			lab.setText ( Utils.priceToString ( maintot ) );
		}

		tot = 0;

		for ( int e = 1; e < target_column.getWidgetCount () - 1; e++ ) {
			lab = ( Label ) target_column.getWidget ( e );
			tot += Utils.stringToPrice ( lab.getText () );
		}

		lab = ( Label ) target_column.getWidget ( target_column.getWidgetCount () - 1 );
		lab.setText ( Utils.priceToString ( tot ) );
	}

	private boolean checkEligibility ( FromServer order ) {
		int status;
		Date d;

		d = order.getDate ( "deliverydate" );
		if ( d == null )
			return false;

		status = order.getInt ( "status" );
		if ( status != OrderUser.PARTIAL_DELIVERY && status != OrderUser.COMPLETE_DELIVERY )
			return false;

		d = new Date ( d.getYear (), d.getMonth (), d.getDate () );
		return now.equals ( d );
	}

	private VerticalPanel columnByPlace ( FromServer ord ) {
		boolean found;
		int rows;
		String place;
		Label lab;
		VerticalPanel ret;
		OrderInterface parent;

		parent = ( OrderInterface ) ord.getObject ( "baseorder" );
		if ( parent.hasShippingPlaces () == false )
			return totalColumn;

		ret = null;
		found = false;
		place = ord.getObject ( "baseuser" ).getObject ( "shipping" ).getString ( "name" );

		for ( int i = ( hasSuppliersColumn () ? 1 : 0 ); i < main.getWidgetCount () - 1; i++ ) {
			ret = ( VerticalPanel ) main.getWidget ( i );
			lab = ( Label ) ret.getWidget ( 0 );

			if ( lab.getText ().equals ( place ) ) {
				found = true;
				break;
			}
		}

		if ( found == false ) {
			ret = ( VerticalPanel ) main.getWidget ( 0 );
			rows = ret.getWidgetCount ();

			ret = new VerticalPanel ();
			ret.setSpacing ( 10 );
			lab = new Label ( place );
			ret.add ( lab );
			ret.setCellHorizontalAlignment ( lab, HasHorizontalAlignment.ALIGN_CENTER );

			for ( int a = 1; a < rows; a++ ) {
				lab = new Label ( "" );
				ret.add ( lab );
				ret.setCellHorizontalAlignment ( lab, HasHorizontalAlignment.ALIGN_CENTER );
			}

			main.insert ( ret, main.getWidgetCount () - 1 );
		}

		return ret;
	}

	private boolean hasSuppliersColumn () {
		return ( suppliersColumn != null );
	}
}
