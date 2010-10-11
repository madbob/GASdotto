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

public class OpenedOrdersList extends OrdersList {
	public OpenedOrdersList ( FromServer supplier ) {
		buildMe ( supplier );
	}

	protected String getEmptyNotification () {
		return "Non sono mai stati aperti ordini per questo fornitore";
	}

	protected void checkExistingOrders ( FromServer supplier ) {
		ArrayList list;
		FromServer ord;
		int supp_id;

		list = Utils.getServer ().getObjectsFromCache ( "Order" );
		supp_id = supplier.getLocalID ();

		for ( int i = 0; i < list.size (); i++ ) {
			ord = ( FromServer ) list.get ( i );

			if ( ord.getObject ( "supplier" ).getLocalID () == supp_id )
				addOrder ( ( Order ) ord );
		}
	}

	protected FromServerForm doEditableRow ( FromServer obj ) {
		FromServerForm ver;

		ver = new FromServerForm ( obj, FromServerForm.NOT_EDITABLE );
		ver.addStyleName ( "subform" );
		ver.add ( ver.getPersonalizedWidget ( "products", new ProductsPlainList () ) );
		return ver;
	}

	protected int sorting ( FromServer first, FromServer second ) {
		if ( first == null )
			return 1;
		else if ( second == null )
			return -1;

		if ( first.getDate ( "enddate" ).before ( second.getDate ( "enddate" ) ) )
			return -1;
		else
			return 1;
	}
}
