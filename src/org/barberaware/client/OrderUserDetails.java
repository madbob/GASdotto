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

public class OrderUserDetails extends FromServerRappresentation implements SourcesChangeEvents {
	private VerticalPanel		main;
	private ProductsDeliveryTable	products;
	private ArrayList		changeCallbacks		= null;

	public OrderUserDetails () {
		main = new VerticalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		products = null;
	}

	public float getTotal () {
		if ( products != null )
			return products.getTotal ();
		else
			return 0;
	}

	public void setValue ( FromServer object ) {
		super.setValue ( object );

		main.clear ();

		products = new ProductsDeliveryTable ();
		main.add ( getPersonalizedWidget ( "allproducts", products ) );

		products.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				triggerChange ();
			}
		} );
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( changeCallbacks, this );
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks == null )
			changeCallbacks = new ArrayList ();
		changeCallbacks.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks != null )
			changeCallbacks.remove ( listener );
	}
}
