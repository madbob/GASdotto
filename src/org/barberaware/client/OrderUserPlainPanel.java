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

public class OrderUserPlainPanel extends Composite implements OrderUserManagerMode, ObjectWidget {
	private ProductsUserSelection	selection;
	private FromServer		baseOrder;

	private boolean			editable;
	private FromServer		currentValue;

	public OrderUserPlainPanel ( FromServer order, boolean edit, boolean freedit ) {
		VerticalPanel main;

		main = new VerticalPanel ();
		initWidget ( main );

		editable = edit;
		baseOrder = order;

		selection = new ProductsUserSelection ( baseOrder.getArray ( "products" ), edit, freedit );
		main.add ( selection );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		ArrayList friends;

		currentValue = element;
		selection.setElements ( currentValue.getArray ( "products" ) );

		friends = element.getArray ( "friends" );
		if ( friends == null || friends.size () == 0 )
			selection.setEditable ( true );
		else
			selection.setEditable ( false );
	}

	public FromServer getValue () {
		if ( editable == true )
			currentValue.setArray ( "products", selection.getElements () );

		return currentValue;
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeProductsList ( ArrayList products ) {
		selection.upgradeProductsList ( products );
	}
}
