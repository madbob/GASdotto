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

		numOrders = 0;
		cleanUp ();
	}

	public void addOrder ( OrderUser uorder ) {
		int index;
		FromServerForm row;
		ProductsDeliveryTable products;

		if ( numOrders == 0 )
			main.remove ( 0 );

		index = retrieveOrder ( uorder );
		if ( index != -1 )
			return;

		/**
			TODO	Ordinare per nome utente
		*/

		row = new FromServerForm ( uorder, FromServerForm.EDITABLE_UNDELETABLE );

		products = new ProductsDeliveryTable ();
		row.add ( row.getPersonalizedWidget ( "products", products ) );

		row.setCallback ( new FromServerFormCallbacks () {
			public String getName ( FromServerForm form ) {
				return form.getObject ().getObject ( "baseuser" ).getString ( "name" );
			}
		} );

		main.add ( row );
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

	private void cleanUp () {
		if ( numOrders == 0 )
			main.add ( new Label ( "Non sono stati avanzati ordini" ) );
	}
}
