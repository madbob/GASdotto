/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public abstract class GenericPanel extends VerticalPanel {
	private MainStack		mainParent;

	public GenericPanel () {
		setStyleName ( "genericpanel" );
		setSize ( "100%", "100%" );
	}

	protected void addTop ( Widget to_add ) {
		insert ( to_add, 0 );
	}

	protected void addBottom ( Widget to_add ) {
		insert ( to_add, getWidgetCount () - 1 );
	}

	protected void goTo ( String address ) {
		mainParent.goTo ( address );
	}

	public void setParent ( MainStack p ) {
		mainParent = p;
	}

	public void openBookmark ( String address ) {
		/*
			By default questa funzione non viene usata, reimplementarla in caso di
			bisogno. Attenzione che "address" contempla il path completo, ivi
			compreso l'identificativo in getSystemID()
		*/
	}

	public String getCurrentInternalReference () {
		/*
			By default questa funzione non viene usata, reimplementarla in caso di
			bisogno. Dovrebbe tornare un identificativo interno in grado di essere
			elaborato da openBookmark()
		*/

		return "";
	}

	public abstract String getName ();
	public abstract String getSystemID ();
	public abstract Image getIcon ();
	public abstract void initView ();
}
