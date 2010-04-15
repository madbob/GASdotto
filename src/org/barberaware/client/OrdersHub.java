/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	private static ArrayList		checkboxes		= null;
	private static ArrayList		callbacks		= null;
	private static boolean			status			= false;

	public static void toggleShippedOrdersStatus ( boolean st ) {
		CheckBox check;
		ClickListener callback;
		ObjectRequest params;

		if ( st != status ) {
			status = st;

			if ( st == true ) {
				params = new ObjectRequest ( "Order" );
				params.add ( "status", Order.SHIPPED );
				Utils.getServer ().testObjectReceive ( params );
			}

			if ( checkboxes != null ) {
				for ( int i = 0; i < checkboxes.size (); i++ ) {
					check = ( CheckBox ) checkboxes.get ( i );
					check.setChecked ( st );

					callback = ( ClickListener ) callbacks.get ( i );
					callback.onClick ( check );
				}
			}
		}
	}

	public static boolean checkShippedOrdersStatus () {
		return status;
	}

	public static void syncCheckboxOnShippedOrders ( CheckBox check, ClickListener callback ) {
		if ( checkboxes == null ) {
			checkboxes = new ArrayList ();
			callbacks = new ArrayList ();
		}

		checkboxes.add ( check );
		callbacks.add ( callback );

		check.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				CheckBox check;

				check = ( CheckBox ) sender;
				toggleShippedOrdersStatus ( check.isChecked () );
			}
		} );
	}
}
