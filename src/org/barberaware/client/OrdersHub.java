/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.ui.*;

public class OrdersHub {
	private static ArrayList		widgets		= null;
	private static boolean			status		= false;

	public static void toggleShippedOrdersStatus ( boolean st, Date startdate, Date enddate, FromServer supplier ) {
		OrdersHubWidget wid;
		ObjectRequest params;

		status = st;

		if ( st == true ) {
			params = new ObjectRequest ( "Order" );
			params.add ( "status", "any" );
			params.add ( "startdate", Utils.encodeDate ( startdate ) );
			params.add ( "enddate", Utils.encodeDate ( enddate ) );

			if ( supplier != null )
				params.add ( "supplier", supplier.getLocalID () );

			Utils.getServer ().testObjectReceive ( params );
		}

		if ( widgets != null ) {
			for ( int i = 0; i < widgets.size (); i++ ) {
				wid = ( OrdersHubWidget ) widgets.get ( i );
				wid.engage ( false );
				wid.setContents ( st, startdate, enddate, supplier );
				wid.engage ( true );
			}
		}
	}

	public static boolean checkShippedOrdersStatus () {
		return status;
	}

	public static void syncCheckboxOnShippedOrders ( OrdersHubWidget widget ) {
		if ( widgets == null )
			widgets = new ArrayList ();

		widgets.add ( widget );
	}
}
