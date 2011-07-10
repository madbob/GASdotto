/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class OrderUserAggregate extends FromServerAggregate {
	public OrderUserAggregate () {
		super ( "orders" );

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				FromServer order;

				order = obj.getObject ( "baseorder" );
				if ( order == null )
					return "Nuovo Aggregato";
				else
					return order.getString ( "name" );
			}
		} );

		addFakeAttribute ( "baseuser", FromServer.OBJECT, User.class, new ValueFromObjectClosure () {
			public FromServer retriveObject ( FromServer obj ) {
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				order = ( FromServer ) orders.get ( 0 );
				return order.getObject ( "baseuser" );
			}
		} );

		addWritebackFakeAttribute ( "status", FromServer.INTEGER, new ValueFromObjectClosure () {
			public int retriveInteger ( FromServer obj ) {
				int ret;
				int check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return OrderUser.TO_DELIVER;

				ret = 1000;

				for ( int i = 1; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getInt ( "status" );
					if ( check < ret )
						ret = check;
				}

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "deliverydate", FromServer.DATE, new ValueFromObjectClosure () {
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
					ret = order.getDate ( "deliverydate" );
					i++;
				} while ( ret != null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "deliverydate" );
					if ( check != null && check.after ( ret ) )
						ret = check;
				}

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "deliveryperson", FromServer.OBJECT, User.class, new ValueFromObjectClosure () {
			public FromServer retriveObject ( FromServer obj ) {
				int i;
				Date greater;
				Date check;
				ArrayList orders;
				FromServer order;
				FromServer ret;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				i = 0;

				do {
					order = ( FromServer ) orders.get ( i );
					ret = order.getObject ( "deliveryperson" );
					greater = order.getDate ( "deliverydate" );
					i++;
				} while ( greater != null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "deliverydate" );
					if ( check != null && check.after ( greater ) ) {
						greater = check;
						ret = order.getObject ( "deliveryperson" );
					}
				}

				return ret;
			}
		} );

		addAttribute ( "baseorder", FromServer.OBJECT, OrderAggregate.class );
		addAttribute ( "orders", FromServer.ARRAY, OrderUser.class );
	}

	/*
		Trattandosi questo di un oggetto totalmente virtuale, senza un corrispettivo sul
		database e costruito localmente alla bisogna, viene considerato sempre valido
	*/
	public boolean isValid () {
		return true;
	}

	public void save ( ServerResponse callback ) {
		ArrayList orders;
		FromServer order;

		orders = getArray ( "orders" );

		for ( int i = 0; i < orders.size (); i++ ) {
			order = ( FromServer ) orders.get ( i );
			order.save ( callback );
		}
	}

	public void addOrder ( FromServer order ) {
		ArrayList orders;

		orders = getArray ( "orders" );
		if ( orders == null )
			orders = new ArrayList ();

		orders.add ( order );
		setArray ( "orders", orders );
	}
}