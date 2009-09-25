/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class DeliverySummary extends Composite {
	private VerticalPanel		main;
	private int			numOrders;

	public DeliverySummary () {
		main = new VerticalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		Utils.getServer ().onObjectEvent ( "User", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				/* dummy */
			}

			public void onModify ( FromServer object ) {
				int index;
				FromServerForm form;
				User user;

				if ( Session.getGAS ().getBool ( "payments" ) == true ) {
					user = ( User ) object;
					index = retrieveUser ( user );

					if ( index != -1 ) {
						form = ( FromServerForm ) main.getWidget ( index );
						user.checkUserPaying ( form );
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;
				User user;

				user = ( User ) object;
				index = retrieveUser ( user );
				if ( index != -1 )
					main.remove ( index );
			}
		} );

		numOrders = 0;
		cleanUp ();
	}

	public void addOrder ( OrderUser uorder ) {
		int index;
		FromServerForm row;
		User user;
		ProductsDeliveryTable products;

		if ( numOrders == 0 )
			main.remove ( 0 );

		index = retrieveOrder ( uorder );
		if ( index != -1 )
			return;

		row = new FromServerForm ( uorder, FromServerForm.EDITABLE_UNDELETABLE );

		products = new ProductsDeliveryTable ();
		row.add ( row.getPersonalizedWidget ( "products", products ) );

		row.setCallback ( new FromServerFormCallbacks () {
			public String getName ( FromServerForm form ) {
				return form.getObject ().getObject ( "baseuser" ).getString ( "name" );
			}
		} );

		user = ( User ) uorder.getObject ( "baseuser" );

		if ( Session.getGAS ().getBool ( "payments" ) == true )
			user.checkUserPaying ( row );

		main.insert ( row, getSortedIndex ( user ) );
		numOrders += 1;
	}

	public void modOrder ( OrderUser uorder ) {
		int index;

		index = retrieveOrder ( uorder );
		if ( index != -1 ) {
			/**
				TODO
			*/
		}
	}

	public void delOrder ( OrderUser uorder ) {
		int index;

		index = retrieveOrder ( uorder );
		if ( index != -1 ) {
			main.remove ( index );
			numOrders -= 1;
			cleanUp ();
		}
	}

	private int retrieveOrder ( OrderUser uorder ) {
		int index;
		FromServerForm row;

		index = uorder.getLocalID ();

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );
			if ( row.getObject ().getLocalID () == index )
				return i;
		}

		return -1;
	}

	private int retrieveUser ( User user ) {
		int index;
		FromServerForm row;

		index = user.getLocalID ();

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );
			if ( row.getObject ().getObject ( "baseuser" ).getLocalID () == index )
				return i;
		}

		return -1;
	}

	private int getSortedIndex ( User to_place ) {
		int i;
		FromServerForm row;
		User u_iter;
		String name_iter;
		String name_to_place;

		name_to_place = to_place.getString ( "name" );

		for ( i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );
			u_iter = ( User ) row.getObject ().getObject ( "baseuser" );
			name_iter = u_iter.getString ( "name" );

			if ( name_iter.compareTo ( name_to_place ) > 0 )
				return i;
		}

		return i;
	}

	private void cleanUp () {
		if ( numOrders == 0 )
			main.add ( new Label ( "Non sono stati avanzati ordini" ) );
	}
}
