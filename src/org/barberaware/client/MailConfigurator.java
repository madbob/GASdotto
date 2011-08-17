/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class MailConfigurator extends Composite implements StringWidget {
	private DeckPanel		container;
	private TextBox			address;
	private TextBox			username;
	private PasswordTextBox		password;
	private TextBox			server;
	private NumberBox		port;
	private BooleanSelector		ssl;

	public MailConfigurator () {
		HTML message;
		FlexTable main;

		container = new DeckPanel ();
		initWidget ( container );

		message = new HTML ( "Se abiliti i messaggi di posta elettronica, qui dovrai immettere i dati dell'account<br />" +
					"mail da usare per l'invio. Puoi usare un account esistente oppure crearne uno apposito,<br />" +
					"purché il tuo provider permetta di spedire messaggi via SMTP." );

		message.setStyleName ( "smaller-text" );
		container.add ( message );

		main = new FlexTable ();
		container.add ( main );

		address = new TextBox ();
		address.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				Utils.getServer ().rawGet ( "mail_conf.php?address=" + address.getText (), new RequestCallback () {
					public void onError ( Request request, Throwable exception ) {
						if ( exception instanceof RequestTimeoutException )
							Utils.showNotification ( "Timeout sulla connessione: accertarsi che il server sia raggiungibile" );
						else
							Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );

						Utils.getServer ().dataArrived ();
					}

					public void onResponseReceived ( Request request, Response response ) {
						JSONValue jsonObject;
						JSONObject obj;

						if ( response.getText () != "" ) {
							try {
								jsonObject = JSONParser.parse ( response.getText () );
								obj = jsonObject.isObject ().get ( "smtp" ).isObject ();

								username.setText ( obj.get ( "username" ).isString ().stringValue () );
								server.setText ( obj.get ( "server" ).isString ().stringValue () );
								port.setVal ( Integer.parseInt ( obj.get ( "port" ).isString ().stringValue () ) );
								ssl.setValue ( Boolean.parseBoolean ( obj.get ( "ssl" ).isString ().stringValue () ) );
							}
							catch ( com.google.gwt.json.client.JSONException e ) {
								Utils.showNotification ( "Ricevuti dati invalidi dal server" );
							}
						}

						Utils.getServer ().dataArrived ();
					}
				} );
			}
		} );

		main.setWidget ( 0, 0, new Label ( "Indirizzo Mail" ) );
		main.setWidget ( 0, 1, address );

		username = new TextBox ();
		main.setWidget ( 1, 0, new Label ( "Username" ) );
		main.setWidget ( 1, 1, username );

		password = new PasswordTextBox ();
		main.setWidget ( 1, 2, new Label ( "Password" ) );
		main.setWidget ( 1, 3, password );

		server = new TextBox ();
		main.setWidget ( 2, 0, new Label ( "Server SMTP" ) );
		main.setWidget ( 2, 1, server );

		port = new NumberBox ();
		main.setWidget ( 2, 2, new Label ( "Porta" ) );
		main.setWidget ( 2, 3, port );

		ssl = new BooleanSelector ();
		main.setWidget ( 3, 0, new Label ( "Usa SSL" ) );
		main.setWidget ( 3, 1, ssl );

		container.showWidget ( 0 );
	}

	public void setEnabled ( boolean enabled ) {
		container.showWidget ( enabled == true ? 1 : 0 );
	}

	/****************************************************************** StringWidget */

	public void setValue ( String value ) {
		String [] tokens;

		if ( value == null || value == "" ) {
			setEnabled ( false );
		}
		else {
			setEnabled ( true );
			tokens = value.split ( "::" );
			address.setText ( tokens [ 0 ] );
			username.setText ( tokens [ 1 ] );
			password.setText ( tokens [ 2 ] );
			server.setText ( tokens [ 3 ] );
			port.setVal ( Integer.parseInt ( tokens [ 4 ] ) );
			ssl.setValue ( Boolean.parseBoolean ( tokens [ 5 ] ) );
		}
	}

	public String getValue () {
		if ( container.getVisibleWidget () == 0 ) {
			return "";
		}
		else {
			return address.getText () + "::" + username.getText () + "::" + password.getText () + "::" +
				server.getText () + "::" + port.getVal () + "::" + Boolean.toString ( ssl.getValue () );
		}
	}
}
