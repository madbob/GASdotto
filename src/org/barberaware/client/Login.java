/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		VerticalPanel container;
		Widget login;
		HTML logo;
		GAS gas;

		main = new VerticalPanel ();
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setSize ( "100%", "100%" );
		initWidget ( main );

		gas = Session.getGAS ();

		if ( gas == null ) {
			main.add ( new HTML ( "<p>Non è stato selezionato alcun Gruppo di Acquisto Solidale.</p>" ) );
			main.add ( new HTML ( "<p><a href=\"http://gasdotto.barberaware.org\">Torna alla pagina principale</a></p>" ) );
		}

		else {
			main.add ( Utils.getNotificationsArea () );

			container = new VerticalPanel ();

			/**
				TODO	Offrire possibilita' di rendere pubblici nella pagina di
					login gli ordini aperti, per permettere a tutti di
					constatare l'attivita' del gruppo anche senza accedere
			*/

			container.add ( doPresentationHeader ( gas ) );

			login = doCredentials ();
			container.add ( login );
			container.setCellHorizontalAlignment ( login, HasHorizontalAlignment.ALIGN_CENTER );

			container.add ( doPresentationFooter ( gas ) );

			logo = new HTML ( "<p style=\"margin-top: 50px;\"><a href=\"http://gasdotto.barberaware.org\"><img src=\"images/gasdotto_logo.png\" border=\"0\"></a></p>" );
			container.add ( logo );
			container.setCellHorizontalAlignment ( logo, HasHorizontalAlignment.ALIGN_RIGHT );
			container.setCellVerticalAlignment ( logo, HasVerticalAlignment.ALIGN_BOTTOM );

			main.add ( container );
		}
	}

	private Widget doPresentationHeader ( GAS gas ) {
		String desc;
		VerticalPanel container;
		Label label;
		Image image;

		container = new VerticalPanel ();

		desc = gas.getString ( "image" );
		if ( desc.equals ( "" ) == false ) {
			image = new Image ( Utils.getServer ().getDomain () + desc );
			image.setStyleName ( "genericpanel-header" );
			container.add ( image );
		}
		else {
			desc = gas.getString ( "name" );
			if ( desc.equals ( "" ) == false ) {
				label = new Label ( desc );
				label.setStyleName ( "genericpanel-header" );
				container.add ( label );
			}
		}

		desc = gas.getString ( "description" );
		if ( desc.equals ( "" ) == false )
			container.add ( new Label ( desc ) );

		container.add ( new HTML ( "<hr>" ) );

		return container;
	}

	private Widget doPresentationFooter ( GAS gas ) {
		String mail;
		VerticalPanel container;

		container = new VerticalPanel ();
		container.add ( new HTML ( "<hr>" ) );

		mail = gas.getString ( "mail" );
		if ( mail.equals ( "" ) == false )
			container.add ( new Label ( "Per contattatare il gruppo, invia una mail a " + mail ) );

		return container;
	}

	private void notifySameUserPwd () {
		DialogBox box;
		VerticalPanel contents;
		HTML message;
		HorizontalPanel buttons;
		Button button;

		box = new DialogBox ();
		box.setText ( "Attenzione!" );

		contents = new VerticalPanel ();

		message = new HTML ( "<p>La tua password è uguale allo username,<br />è fortemente consigliato cambiarla!</p>" );
		message.setStyleName ( "message" );
		contents.add ( message );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		contents.add ( buttons );

		button = new Button ( "Prometto che cambierò password!", new ClickListener () {
			public void onClick ( Widget sender ) {
				Window.Location.reload ();
			}
		} );
		buttons.add ( button );

		box.setWidget ( contents );
		box.center ();
		box.show ();
	}

	private void executeLogin () {
		final String user;
		final String pwd;
		ObjectRequest params;

		user = username.getText ();
		if ( user.equals ( "" ) ) {
			Utils.showNotification ( "Non hai immesso alcun username" );
			username.setFocus ( true );
			return;
		}

		pwd = password.getText ();

		params = new ObjectRequest ( "Login" );
		params.add ( "username", user );
		params.add ( "password", pwd );

		Utils.getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				User utente;

				utente = new User ();
				utente.fromJSONObject ( response.isObject () );

				if ( utente.isValid () ) {
					if ( user == pwd )
						notifySameUserPwd ();
					else
						Window.Location.reload ();
				}
				else {
					Utils.showNotification ( "Autenticazione fallita. Riprova" );
					username.setText ( "" );
					password.setText ( "" );
					username.setFocus ( true );
				}

				utente = null;
			}
		} );

		params = null;
	}

	private Widget doCredentials () {
		FlexTable form;
		Button button;
		KeyboardListenerAdapter enter_key;

		form = new FlexTable ();

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
				executeLogin ();
			}
		} );

		/**
			TODO	Aggiungere opzione "Hai perso la password?"
		*/

		form.setWidget ( 2, 1, button );
		form.getCellFormatter ().setHorizontalAlignment ( 3, 1, HasHorizontalAlignment.ALIGN_RIGHT );

		username.setFocus ( true );

		/*
			Per permettere l'autenticazione con la pressione del tasto Enter
		*/

		enter_key = new KeyboardListenerAdapter () {
			public void onKeyPress ( Widget sender, char keyCode, int modifiers ) {
				if ( keyCode == KeyboardListener.KEY_ENTER ) {
					executeLogin ();
				}
			}
		};

		username.addKeyboardListener ( enter_key );
		password.addKeyboardListener ( enter_key );

		return form;
	}
}
