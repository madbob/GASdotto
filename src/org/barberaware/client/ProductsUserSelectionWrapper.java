/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

/*
	Questo serve a wrappare una ProductsUserSelection in un elemento che sia compatibile con
	FromServerRappresentation, di fatto rappresenta un OrderUser
*/

public class ProductsUserSelectionWrapper extends FromServerRappresentation {
	private boolean				inited;
	private VerticalPanel			main;
	private ProductsUserSelection		selection;

	public ProductsUserSelectionWrapper ( FromServer order, boolean edit, boolean freeedit ) {
		main = new VerticalPanel ();
		main.setWidth ( "100%" );
		initWidget ( main );

		selection = new ProductsUserSelection ( order.getArray ( "products" ), edit, freeedit, false );
		inited = false;
	}

	public void setEditable ( boolean editable ) {
		selection.setEditable ( editable );
	}

	public float getTotalPrice () {
		return selection.getTotalPrice ();
	}

	public void upgradeProductsList ( ArrayList products ) {
		selection.upgradeProductsList ( products );
	}

	/****************************************************************** FromServerRappresentation */

	public void setValue ( FromServer value ) {
		super.setValue ( value );

		if ( inited == false ) {
			main.add ( getPersonalizedWidget ( "products", selection ) );
			inited = true;
		}
	}

	public FromServer getValue () {
		FromServer ret;
		super.rebuildObject ();
		ret = super.getValue ();
		return ret;
	}
}
