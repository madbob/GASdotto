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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class PasswordBox extends Composite implements StringWidget {
	private PasswordTextBox		main;

	private DialogBox		dialog;
	private PasswordTextBox		password;
	private PasswordTextBox		passwordCheck;

	private boolean			opened;

	public PasswordBox () {
		opened = false;

		dialog = new DialogBox ( false );
		dialog.setText ( "Definisci nuova password" );
		dialog.setWidget ( doDialog () );

		/**
			TODO	Aggiungere icona
		*/

		main = new PasswordTextBox ();
		main.setStyleName ( "password-selector" );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;

					password.setText ( "" );
					passwordCheck.setText ( "" );

					dialog.center ();
					dialog.show ();
				}
			}

			public void onLostFocus ( Widget sender ) {
				/* dummy */
			}
		} );

		initWidget ( main );
		main.setText ( "" );
	}

	private Panel doDialog () {
		Grid layout;
		VerticalPanel pan;
		HorizontalPanel buttons;
		Button but;

		pan = new VerticalPanel ();

		layout = new Grid ( 2, 2 );
		pan.add ( layout );

		password = new PasswordTextBox ();
		passwordCheck = new PasswordTextBox ();
		layout.setWidget ( 0, 0, new Label ( "Password" ) );
		layout.setWidget ( 0, 1, password );
		layout.setWidget ( 1, 0, new Label ( "Conferma Password" ) );
		layout.setWidget ( 1, 1, passwordCheck );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "bottom-buttons" );
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				String first;
				String second;

				first = password.getText ();
				second = passwordCheck.getText ();

				if ( first.equals ( second ) ) {
					main.setText ( password.getText () );
					opened = false;
					dialog.hide ();
				}
				else
					Utils.showNotification ( "Le password immesse non combaciano" );
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		return pan;
	}

	public void setValue ( String value ) {
		/*
			Il campo della password viene lasciato volutamente vuoto:

			1) se la password non viene modificata al server deve essere passata una
				stringa vuota affinche' non faccia nulla
			2) in questo modo appare chiaro (campo pieno / campo vuoto) se il
				contenuto e' stato editato o meno
		*/
	}

	public String getValue () {
		return main.getText ();
	}
}
