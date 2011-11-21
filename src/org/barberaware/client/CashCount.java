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
	private ArrayList		dates;
	private Date			now;

	public CashCount () {
		Label text;
		VerticalPanel today;

		main = new HorizontalPanel ();
		initWidget ( main );
		main.setStyleName ( "top-spaced" );
		main.setSpacing ( 10 );

		dates = new ArrayList ();
		now = new Date ( System.currentTimeMillis () );
		now = new Date ( now.getYear (), now.getMonth (), now.getDate () );
		dates.add ( now );

		today = new VerticalPanel ();
		main.add ( today );

		text = new Label ( "Consegne di Oggi" );
		today.add ( text );
		today.setCellHorizontalAlignment ( text, HasHorizontalAlignment.ALIGN_CENTER );

		text = new Label ();
		text.setStyleName ( "bigger-text" );
		text.setText ( Utils.priceToString ( 0 ) );
		today.add ( text );
		today.setCellHorizontalAlignment ( text, HasHorizontalAlignment.ALIGN_CENTER );
	}

	public void addOrder ( OrderUserInterface uorder ) {
		boolean found;
		float tot;
		int row_index;
		Date d;
		ArrayList orders;
		String supplier_name;
		VerticalPanel col;
		VerticalPanel pan;
		VerticalPanel target_column;
		Label lab;
		FromServer uord;
		OrderUser ord;

		uord = ( FromServer ) uorder;

		d = checkEligibility ( uord );
		if ( d == null )
			return;

		target_column = columnByDate ( d );
		if ( target_column == null )
			return;

		if ( uord instanceof OrderUserAggregate ) {
			if ( hasSuppliersColumn () == false ) {
				col = new VerticalPanel ();
				main.insert ( col, 0 );

				lab = new Label ( "" );
				col.add ( lab );
				lab.addStyleName ( "high-as-label" );
			}
			else {
				col = ( VerticalPanel ) main.getWidget ( 0 );
			}

			orders = uord.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( OrderUser ) orders.get ( i );
				supplier_name = ord.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" );
				tot = ord.getDeliveredPriceWithFriends ();
				found = false;

				for ( int a = 1; a < col.getWidgetCount (); a++ ) {
					lab = ( Label ) col.getWidget ( a );

					if ( lab.getText ().equals ( supplier_name ) ) {
						found = true;
						lab = ( Label ) target_column.getWidget ( a );
						lab.removeStyleName ( "bigger-text" );
						lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );
						break;
					}
				}

				if ( found == false ) {
					col.add ( new Label ( supplier_name ) );
					row_index = col.getWidgetCount () - 1;

					for ( int e = 1; e < main.getWidgetCount (); e++ ) {
						pan = ( VerticalPanel ) main.getWidget ( e );

						if ( pan.getWidgetCount () < col.getWidgetCount () ) {
							lab = new Label ( "" );
							pan.add ( lab );
							pan.setCellHorizontalAlignment ( lab, HasHorizontalAlignment.ALIGN_CENTER );
						}
						else {
							lab = ( Label ) pan.getWidget ( row_index );
						}

						if ( pan == target_column )
							lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );
					}
				}
			}
		}
		else {
			tot = uorder.getDeliveredPriceWithFriends ();
			lab = ( Label ) target_column.getWidget ( 1 );
			lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );
		}
	}

	public void modOrder ( OrderUserInterface uorder ) {
		FromServer baseorder;
		FromServer uord;
		FromServer past_user_order;
		ArrayList past_orders;

		uord = ( FromServer ) uorder;
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
		Date d;
		ArrayList orders;
		String supplier_name;
		Label lab;
		VerticalPanel target_column;
		VerticalPanel col;
		FromServer uord;
		OrderUser ord;

		uord = ( FromServer ) uorder;

		d = checkEligibility ( uord );
		if ( d == null )
			return;

		target_column = columnByDate ( d );

		if ( hasSuppliersColumn () == true ) {
			if ( uorder instanceof OrderUserAggregate == false )
				return;

			col = ( VerticalPanel ) main.getWidget ( 0 );
			orders = uord.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( OrderUser ) orders.get ( i );
				supplier_name = ord.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" );
				tot = ord.getDeliveredPriceWithFriends ();

				for ( int a = 1; a < col.getWidgetCount (); a++ ) {
					lab = ( Label ) col.getWidget ( a );

					if ( lab.getText ().equals ( supplier_name ) ) {
						lab = ( Label ) target_column.getWidget ( a );
						lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) - tot ) );
						break;
					}
				}
			}
		}
		else {
			tot = uorder.getDeliveredPriceWithFriends ();
			lab = ( Label ) target_column.getWidget ( 1 );
			lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) - tot ) );
		}
	}

	public void clean () {
		Label lab;
		VerticalPanel col;

		while ( main.getWidgetCount () != 1 )
			main.remove ( 0 );

		col = ( VerticalPanel ) main.getWidget ( 0 );
		while ( col.getWidgetCount () != 2 )
			col.remove ( 1 );

		dates.clear ();
		dates.add ( now );

		lab = ( Label ) col.getWidget ( 1 );
		lab.setText ( Utils.priceToString ( 0 ) );
	}

	private Date checkEligibility ( FromServer order ) {
		int status;
		Date d;

		d = order.getDate ( "deliverydate" );
		if ( d == null )
			return null;

		status = order.getInt ( "status" );
		if ( status != OrderUser.PARTIAL_DELIVERY && status != OrderUser.COMPLETE_DELIVERY )
			return null;

		return d;
	}

	private VerticalPanel columnByDate ( Date d ) {
		boolean has_suppliers_columns;
		int rows;
		Label lab;
		VerticalPanel ret;
		Date ed;

		ret = null;
		has_suppliers_columns = hasSuppliersColumn ();
		d = new Date ( d.getYear (), d.getMonth (), d.getDate () );

		for ( int i = 0; i < dates.size (); i++ ) {
			ed = ( Date ) dates.get ( i );

			if ( ed.equals ( d ) ) {
				if ( has_suppliers_columns == true )
					i++;

				ret = ( VerticalPanel ) main.getWidget ( i );
				break;
			}
			else if ( ed.after ( d ) ) {
				ret = ( VerticalPanel ) main.getWidget ( i );
				rows = ret.getWidgetCount ();

				ret = new VerticalPanel ();
				lab = new Label ( d.getDate () + "/" + d.getMonth () );
				ret.add ( lab );
				ret.setCellHorizontalAlignment ( lab, HasHorizontalAlignment.ALIGN_CENTER );

				if ( has_suppliers_columns == true )
					i++;

				for ( int a = 1; a < rows; a++ ) {
					lab = new Label ( "" );
					ret.add ( lab );
					ret.setCellHorizontalAlignment ( lab, HasHorizontalAlignment.ALIGN_CENTER );
				}

				main.insert ( ret, i );
				dates.add ( i, d );
				break;
			}
		}

		return ret;
	}

	private boolean hasSuppliersColumn () {
		VerticalPanel col;
		Label lab;

		col = ( VerticalPanel ) main.getWidget ( 0 );
		lab = ( Label ) col.getWidget ( 0 );
		return ( lab.getText () == "" );
	}
}
