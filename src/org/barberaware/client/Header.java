/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class Header extends Composite {
	private MainStack	stack;
	private DialogBox	logout;
	private DialogBox	permalinkDialog;
	private TextBox		permalink;

	private void doPermalink () {
		Button but;
		VerticalPanel contents;

		permalinkDialog = new DialogBox ();
		permalinkDialog.setText ( "Permalink" );

		contents = new VerticalPanel ();
		contents.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		contents.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );

		permalink = new TextBox ();
		permalink.setReadOnly ( true );
		permalink.setVisibleLength ( 50 );
		contents.add ( permalink );

		but = new Button ( "Annulla" );
		but.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				permalinkDialog.hide ();
			}
		} );
		contents.add ( but );

		permalinkDialog.setWidget ( contents );
	}

	private void doLogout () {
		Button but;
		HorizontalPanel contents;

		logout = new DialogBox ();
		logout.setText ( "Logout" );

		contents = new HorizontalPanel ();
		contents.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );

		but = new Button ( "Logout" );
		but.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				ServerRequest params;

				params = new ServerRequest ( "Logout" );

				Utils.getServer ().serverGet ( params, new ServerResponse () {
					public void onComplete ( JSONValue response ) {
						logout.hide ();
						Window.Location.reload ();
					}
				} );
			}
		} );
		contents.add ( but );

		but = new Button ( "Annulla" );
		but.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				logout.hide ();
			}
		} );
		contents.add ( but );

		logout.setWidget ( contents );
	}

	private Widget doPermalinkButton () {
		FocusPanel main;
		HorizontalPanel container;

		main = new FocusPanel ();

		container = new HorizontalPanel ();
		container.setStyleName ( "main-menu-item" );
		container.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		container.add ( new Label ( "Permalink" ) );
		container.add ( new Image ( "images/permalink.png" ) );

		main.add ( container );

		main.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				permalink.setText ( stack.getCurrentBookmark () );
				permalinkDialog.center ();
				permalinkDialog.show ();
			}
		} );

		return main;
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

		main.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
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
		text = user.getString ( "firstname" ) + " " + user.getString ( "surname" ) + " @ " + gas.getString ( "name" );
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

		/*
			Per motivi di semplicita' dell'interfaccia il tasto per il permalink
			viene visualizzato solo per referenti ed amministratori, che in fondo
			sono gli unici che se ne fanno qualcosa
		*/
		if ( Session.getUser ().getInt ( "privileges" ) >= User.USER_RESPONSABLE )
			doPermalink ();

		item = doGreetings ();
		main.add ( item );
		main.setCellHorizontalAlignment ( item, HasHorizontalAlignment.ALIGN_LEFT );

		/**
			TODO	Eventualmente rimuovere definitivamente il permalink
		*/
		/*
		item = doPermalinkButton ();
		main.add ( item );
		main.setCellHorizontalAlignment ( item, HasHorizontalAlignment.ALIGN_RIGHT );
		*/

		item = doLogoutButton ();
		main.add ( item );
		main.setCellHorizontalAlignment ( item, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	public void wireMainStack ( MainStack s ) {
		stack = s;
	}
}
