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

import com.allen_sauer.gwt.log.client.Log;

public class PastOrdersList extends OrdersList {
	public PastOrdersList ( FromServer supplier ) {
		buildMe ( supplier );
	}

	protected String getEmptyNotification () {
		return "Non sono mai stati eseguiti ordini per questo fornitore";
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

	protected FromServerForm doEditableRow ( FromServer obj ) {
		FromServerForm ver;
		OrderUserManager products;

		ver = new FromServerForm ( obj, FromServerForm.NOT_EDITABLE );
		ver.addStyleName ( "subform" );

		products = new OrderUserManager ( obj.getObject ( "baseorder" ), false );
		products.setValue ( obj );
		ver.add ( products );

		return ver;
	}

	protected int sorting ( FromServer first, FromServer second ) {
		if ( first == null )
			return 1;
		else if ( second == null )
			return -1;

		if ( first.getObject ( "baseorder" ).getDate ( "enddate" ).before ( second.getObject ( "baseorder" ).getDate ( "enddate" ) ) )
			return -1;
		else
			return 1;
	}
}
