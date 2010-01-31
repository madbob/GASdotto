/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class PastOrdersList extends OrdersList {
	public PastOrdersList ( FromServer supplier, FromServerForm reference ) {
		buildMe ( supplier, reference );
	}

	protected String getEmptyNotification () {
		return "Non sono mai stati eseguiti ordini per questo fornitore";
	}

	protected String getMainIcon () {
		return "images/notifications/supplier_having_past_orders.png";
	}

	protected void checkExistingOrders ( FromServer supplier ) {
		ArrayList list;
		FromServer ord;
		Order base_ord;
		int supp_id;

		list = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		supp_id = supplier.getLocalID ();

		for ( int i = 0; i < list.size (); i++ ) {
			ord = ( FromServer ) list.get ( i );
			base_ord = ( Order ) ord.getObject ( "baseorder" );

			if ( base_ord.getInt ( "status" ) == Order.CLOSED &&
					base_ord.getObject ( "supplier" ).getLocalID () == supp_id )
				addOrder ( base_ord );
		}
	}
}
