/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class OrderUserPlainPanel extends OrderUserManagerMode {
	private VerticalPanel		main;
	private ProductsUserSelection	selection;
	private FromServer		baseOrder;

	private boolean			editable;
	private boolean			freeEditable;

	public OrderUserPlainPanel ( FromServer order, boolean edit, boolean freedit ) {
		main = new VerticalPanel ();
		initWidget ( main );

		editable = edit;
		freeEditable = freedit;
		baseOrder = order;
		selection = null;
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		ArrayList friends;

		if ( element == null ) {
			element = new OrderUser ();
			element.setObject ( "baseorder", baseOrder );
			element.setObject ( "baseuser", Session.getUser () );
		}

		super.setValue ( element );

		if ( selection == null ) {
			selection = new ProductsUserSelection ( baseOrder.getArray ( "products" ), editable, freeEditable );
			main.add ( getPersonalizedWidget ( "products", selection ) );
			main.add ( getPersonalizedWidget ( "notes", new DummyTextArea () ) );
		}

		friends = element.getArray ( "friends" );
		if ( friends == null || friends.size () == 0 )
			selection.setEditable ( true );
		else
			selection.setEditable ( false );
	}

	public FromServer getValue () {
		if ( editable == true )
			rebuildObject ();

		return super.getValue ();
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeOrder ( FromServer order ) {
		if ( selection == null )
			setValue ( null );

		selection.upgradeProductsList ( order.getArray ( "products" ) );
	}

	public void unlock () {
		/* dummy */
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		selection.addChangeListener ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		selection.removeChangeListener ( listener );
	}
}
