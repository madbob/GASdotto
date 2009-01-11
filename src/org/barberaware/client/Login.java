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

import java.lang.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class Login extends Composite {
	private TextBox			username;
	private PasswordTextBox		password;

	public Login () {
		VerticalPanel main;
		HTML text;
		Grid form;
		Button button;
		GAS gas;

		main = new VerticalPanel ();
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setSize ( "100%", "100%" );
		initWidget ( main );

		gas = Session.getGAS ();

		if ( gas == null ) {
			text = new HTML ( "<p>Non è stato selezionato alcun Gruppo di Acquisto Solidale.</p>" );
			main.add ( text );

			main.add ( new HTML ( "<p><a href=\"http://gasdotto.barberaware.org\">Torna alla pagina principale</a></p>" ) );
		}

		else {
			main.add ( Utils.getNotificationsArea () );

			form = new Grid ( 3, 2 );

			username = new TextBox ();
			username.setVisibleLength ( 20 );

			password = new PasswordTextBox ();
			password.setVisibleLength ( 20 );

			form.setWidget ( 0, 0, new Label ( "Username" ) );
			form.setWidget ( 0, 1, username );
			form.setWidget ( 1, 0, new Label ( "Password" ) );
			form.setWidget ( 1, 1, password );

			button = new Button ( "Login", new ClickListener () {
				public void onClick ( Widget sender ) {
					String user;
					String pwd;
					ServerRequest params;

					user = username.getText ();
					if ( user.equals ( "" ) ) {
						Utils.showNotification ( "Non hai immesso alcun username" );
						return;
					}

					pwd = password.getText ();

					params = new ServerRequest ( "Login" );
					params.add ( "username", user );
					params.add ( "password", pwd );

					Utils.getServer ().serverGet ( params, new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							User utente;

							utente = new User ();
							utente.fromJSONObject ( response.isObject () );

							if ( utente.isValid () )
								Window.Location.reload ();

							else {
								Utils.showNotification ( "Autenticazione fallita. Riprova" );
								username.setText ( "" );
								password.setText ( "" );
							}

							utente = null;
						}
					} );

					params = null;
				}
			} );

			form.setWidget ( 2, 1, button );
			form.getCellFormatter ().setHorizontalAlignment ( 2, 1, HasHorizontalAlignment.ALIGN_RIGHT );

			username.setFocus ( true );
			main.add ( form );
		}
	}
}
