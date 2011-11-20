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

import com.allen_sauer.gwt.log.client.Log;

public class OrderAggregate extends FromServerAggregate implements OrderInterface {
	public OrderAggregate () {
		super ( "orders" );

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				String name;
				String date;
				Date tmp_ship;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null || orders.size () == 0 )
					return "Nuovo Aggregato";

				order = ( FromServer ) orders.get ( 0 );
				name = order.getObject ( "supplier" ).getString ( "name" );

				date = "dal " + Utils.printableDate ( order.getDate ( "startdate" ) ) + " al " + Utils.printableDate ( order.getDate ( "enddate" ) );
				tmp_ship = order.getDate ( "shippingdate" );
				if ( tmp_ship != null )
					date += ", in consegna il " + Utils.printableDate ( tmp_ship );

				for ( int i = 1; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					name += " / " + order.getObject ( "supplier" ).getString ( "name" );

					date += " - dal " + Utils.printableDate ( order.getDate ( "startdate" ) ) + " al " + Utils.printableDate ( order.getDate ( "enddate" ) );
					tmp_ship = order.getDate ( "shippingdate" );
					if ( tmp_ship != null )
						date += ", in consegna il " + Utils.printableDate ( tmp_ship );
				}

				return name + "\n" + date;
			}
		} );

		addFakeAttribute ( "status", FromServer.INTEGER, new ValueFromObjectClosure () {
			public int retriveInteger ( FromServer obj ) {
				int ret;
				int check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null || orders.size () == 0 )
					return Order.OPENED;

				ret = 1000;

				for ( int i = 0; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getInt ( "status" );

					if ( check < ret )
						ret = check;
				}

				return ret;
			}
		} );

		addFakeAttribute ( "startdate", FromServer.DATE, new ValueFromObjectClosure () {
			public Date retriveDate ( FromServer obj ) {
				Date ret;
				Date check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				order = ( FromServer ) orders.get ( 0 );
				ret = order.getDate ( "startdate" );

				for ( int i = 1; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "startdate" );
					if ( check.before ( ret ) )
						ret = check;
				}

				return ret;
			}
		} );

		addFakeAttribute ( "enddate", FromServer.DATE, new ValueFromObjectClosure () {
			public Date retriveDate ( FromServer obj ) {
				Date ret;
				Date check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				order = ( FromServer ) orders.get ( 0 );
				ret = order.getDate ( "enddate" );

				for ( int i = 1; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "enddate" );
					if ( check.after ( ret ) )
						ret = check;
				}

				return ret;
			}
		} );

		addFakeAttribute ( "shippingdate", FromServer.DATE, new ValueFromObjectClosure () {
			public Date retriveDate ( FromServer obj ) {
				int i;
				Date ret;
				Date check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				i = 0;
				ret = null;

				do {
					order = ( FromServer ) orders.get ( i );
					ret = order.getDate ( "shippingdate" );
					i++;
				} while ( ret == null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "shippingdate" );
					if ( check != null && check.after ( ret ) )
						ret = check;
				}

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "mail_summary_sent", FromServer.DATE, new ValueFromObjectClosure () {
			public Date retriveDate ( FromServer obj ) {
				int i;
				Date ret;
				Date check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				i = 0;

				do {
					order = ( FromServer ) orders.get ( i );
					ret = order.getDate ( "mail_summary_sent" );
					i++;
				} while ( ret != null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "mail_summary_sent" );
					if ( check != null && check.after ( ret ) )
						ret = check;
				}

				return ret;
			}
		} );

		addAttribute ( "orders", FromServer.ARRAY, Order.class );

		isSharable ( true );
	}

	public boolean hasSupplier ( FromServer target ) {
		ArrayList orders;
		FromServer order;

		orders = getArray ( "orders" );

		for ( int i = 0; i < orders.size (); i++ ) {
			order = ( FromServer ) orders.get ( i );
			if ( order.getObject ( "supplier" ).equals ( target ) )
				return true;
		}

		return false;
	}

	public static OrderAggregate retrieveAggregate ( FromServer order ) {
		ArrayList aggregates;
		OrderAggregate aggregate;

		aggregates = Utils.getServer ().getObjectsFromCache ( "OrderAggregate" );

		for ( int i = 0; i < aggregates.size (); i++ ) {
			aggregate = ( OrderAggregate ) aggregates.get ( i );
			if ( aggregate.hasObject ( order ) )
				return aggregate;
		}

		return null;
	}

	/****************************************************************** OrderInterface */

	public void asyncLoadUsersOrders () {
		ArrayList orders;
		Order ord;

		orders = getObjects ();

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( Order ) orders.get ( i );
			ord.asyncLoadUsersOrders ();
		}
	}

	public boolean iAmReference () {
		ArrayList orders;
		FromServer ord;
		Supplier supplier;

		orders = getArray ( "orders" );

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( FromServer ) orders.get ( i );
			supplier = ( Supplier ) ord.getObject ( "supplier" );
			if ( supplier.iAmReference () == true )
				return true;
		}

		return false;
	}
}
