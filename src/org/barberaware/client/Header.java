/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class Header extends Composite {
	private MainStack	stack;
	private DialogBox	logout;

	private void doLogout () {
		Button but;
		HorizontalPanel contents;

		logout = new DialogBox ();
		logout.setText ( "Logout" );

		contents = new HorizontalPanel ();
		contents.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );

		but = new Button ( "Logout" );
		but.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				Utils.performLogout ();
			}
		} );
		contents.add ( but );

		but = new Button ( "Annulla" );
		but.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				logout.hide ();
			}
		} );
		contents.add ( but );

		logout.setWidget ( contents );
	}

	private Widget doLogoutButton () {
		FocusPanel main;
		HorizontalPanel container;

		main = new FocusPanel ();

		container = new HorizontalPanel ();
		container.setStyleName ( "main-menu-item" );
		container.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		container.add ( new Label ( "Logout" ) );
		container.add ( new Image ( "images/logout.png" ) );

		main.add ( container );

		main.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				logout.center ();
				logout.show ();
			}
		} );

		return main;
	}

	public Widget doGreetings () {
		String text;
		User user;
		GAS gas;
		Label label;

		user = Session.getUser ();
		gas = Session.getGAS ();
		text = user.getString ( "surname" ) + " " + user.getString ( "firstname" ) + " @ " + gas.getString ( "name" );
		label = new Label ( text );
		return label;
	}

	public Header () {
		HorizontalPanel main;
		Widget item;

		main = new HorizontalPanel ();
		main.setStyleName ( "main-header" );
		initWidget ( main );

		doLogout ();

		item = doGreetings ();
		main.add ( item );
		main.setCellHorizontalAlignment ( item, HasHorizontalAlignment.ALIGN_LEFT );

		item = doLogoutButton ();
		main.add ( item );
		main.setCellHorizontalAlignment ( item, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	public void wireMainStack ( MainStack s ) {
		stack = s;
	}
}
