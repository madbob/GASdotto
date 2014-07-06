/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
	private class CashCountTotal extends Composite {
		private VerticalPanel	main;
		private Label		totalLabel;
		private Label		totalBankLabel;
		private Label		totalCashLabel;

		public CashCountTotal () {
			main = new VerticalPanel ();
			main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
			initWidget ( main );
			clean ();
		}

		public void clean () {
			HorizontalPanel row;

			main.clear ();

			totalLabel = new Label ( Utils.priceToString ( 0 ) );
			totalLabel.setStyleName ( "bigger-text" );
			main.add ( totalLabel );

			if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
				row = new HorizontalPanel ();
				row.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
				main.add ( row );
				main.setCellHorizontalAlignment ( row, HasHorizontalAlignment.ALIGN_LEFT );
				row.add ( new Image ( "images/by_bank.png" ) );
				totalBankLabel = new Label ();
				row.add ( totalBankLabel );

				row = new HorizontalPanel ();
				row.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
				main.add ( row );
				main.setCellHorizontalAlignment ( row, HasHorizontalAlignment.ALIGN_LEFT );
				row.add ( new Image ( "images/by_cash.png" ) );
				totalCashLabel = new Label ();
				row.add ( totalCashLabel );
			}
		}

		public void addOrderDetails ( OrderUser order ) {
			float tot;
			Label target;

			if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
				tot = order.getDeliveredPriceWithFriends ( true );

				if ( order.getObject ( "payment_event" ).getInt ( "method" ) == BankMovement.BY_CASH )
					target = totalCashLabel;
				else
					target = totalBankLabel;

				target.setText ( Utils.priceToString ( Utils.stringToPrice ( target.getText () ) + tot ) );
			}
		}

		public void updateTotal ( float total ) {
			totalLabel.setText ( Utils.priceToString ( total ) );
		}
	}

	private FlexTable	main;
	private int		columns;
	private FromServerList	currentOrders;
	private Date		now;

	public CashCount () {
		Label text;

		main = new FlexTable ();
		initWidget ( main );
		main.setStyleName ( "top-spaced" );

		now = new Date ( System.currentTimeMillis () );
		now = new Date ( now.getYear (), now.getMonth (), now.getDate () );

		clean ();
		currentOrders = new FromServerList ();
	}

	public void addOrder ( OrderUserInterface uorder ) {
		ArrayList orders;
		FromServer uord;
		OrderUser ord;

		uord = ( FromServer ) uorder;
		if ( checkEligibility ( uord ) == false )
			return;

		if ( uord instanceof OrderUserAggregate ) {
			orders = uord.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( OrderUser ) orders.get ( i );
				doOrder ( ord );
			}
		}
		else {
			ord = ( OrderUser ) uord;
			doOrder ( ord );
		}
	}

	public void modOrder ( OrderUserInterface uorder ) {
		delOrder ( uorder );
		addOrder ( uorder );
	}

	public void delOrder ( OrderUserInterface uorder ) {
		ArrayList orders;
		OrderUser uord;
		OrderUser u;

		clean ();

		if ( uorder instanceof OrderUserAggregate ) {
			orders = ( ( OrderUserAggregate ) uorder ).getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				uord = ( OrderUser ) orders.get ( i );
				currentOrders.remove ( uord );
			}
		}
		else {
			uord = ( OrderUser ) uorder;
			currentOrders.remove ( uord );
		}

		for ( int i = 0; i < currentOrders.size (); i++ ) {
			u = ( OrderUser ) currentOrders.get ( i );
			doOrder ( u );
		}
	}

	public void clean () {
		Label text;
		CashCountTotal totals;

		while ( main.getRowCount () > 0 )
			main.removeRow ( 0 );

		totals = new CashCountTotal ();
		main.setWidget ( 1, 1, totals );

		text = new Label ( "" );
		main.setWidget ( 0, 1, text );

		columns = 1;
	}

	private void doOrder ( OrderUser ord ) {
		int target_column;
		int row_index;
		float tot;
		boolean found;
		String supplier_name;
		String iter;
		Label lab;
		FlexTable.FlexCellFormatter format;
		CashCountTotal totals;

		target_column = columnByPlace ( ord );
		supplier_name = ord.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" );
		tot = ord.getDeliveredPriceWithFriends ( true );

		row_index = 1;
		found = false;

		for ( int a = 1; a < main.getRowCount () - 1; a++ ) {
			lab = ( Label ) main.getWidget ( a, 0 );
			iter = lab.getText ();

			if ( iter.equals ( supplier_name ) ) {
				found = true;
				row_index = a;
				break;
			}
		}

		if ( found == false ) {
			row_index = main.getRowCount () - 1;
			main.insertRow ( row_index );

			format = main.getFlexCellFormatter ();

			lab = new Label ( supplier_name );
			main.setWidget ( row_index, 0, lab );

			for ( int e = 1; e < columns + 1; e++ ) {
				lab = new Label ( "" );
				main.setWidget ( row_index, e, lab );
				format.setHorizontalAlignment ( row_index, e, HasHorizontalAlignment.ALIGN_CENTER );
			}
		}

		lab = ( Label ) main.getWidget ( row_index, target_column );
		lab.setText ( Utils.priceToString ( Utils.stringToPrice ( lab.getText () ) + tot ) );

		if ( currentOrders.has ( ord ) == false )
			currentOrders.add ( ord );

		totals = ( CashCountTotal ) main.getWidget ( main.getRowCount () - 1, target_column );
		totals.addOrderDetails ( ord );

		tot = 0;

		for ( int i = 1; i < main.getRowCount () - 1; i++ ) {
			lab = ( Label ) main.getWidget ( i, target_column );
			tot += Utils.stringToPrice ( lab.getText () );
		}

		totals.updateTotal ( tot );
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

	private int columnByPlace ( FromServer ord ) {
		int i;
		boolean found;
		String place;
		String iter;
		Label lab;
		OrderInterface parent;

		parent = ( OrderInterface ) ord.getObject ( "baseorder" );
		if ( parent.hasShippingPlaces () == false )
			return 1;

		found = false;
		place = ord.getObject ( "baseuser" ).getObject ( "shipping" ).getString ( "name" );

		for ( i = 1; i < columns + 1; i++ ) {
			lab = ( Label ) main.getWidget ( 0, i );

			iter = lab.getText ();

			if ( iter == "" ) {
				lab.setText ( place );
				found = true;
				break;
			}

			if ( iter.equals ( place ) ) {
				found = true;
				break;
			}
		}

		if ( found == false ) {
			lab = new Label ( place );
			main.setWidget ( 0, i, lab );
			columns++;

			for ( int a = 1; a < main.getRowCount () - 1; a++ ) {
				lab = new Label ( "" );
				main.setWidget ( a, i, lab );
			}

			main.setWidget ( main.getRowCount () - 1, i, new CashCountTotal () );
		}

		return i;
	}
}
