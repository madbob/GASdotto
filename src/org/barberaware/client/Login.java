/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

public class Login extends Composite {
	private TextBox			username;
	private PasswordTextBox		password;
	private CheckBox		permanent;

	private DialogBox		recoveryDialog;
	private TextBox			recoveryMail;

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
			main.add ( new HTML ( "<p><a href=\"http://gasdotto.net\">Torna alla pagina principale</a></p>" ) );
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

			logo = new HTML ( "<p style=\"margin-top: 50px;\"><a href=\"http://gasdotto.net\"><img src=\"images/gasdotto_logo.png\" border=\"0\"></a></p>" );
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
			container.add ( new Label ( "Per contattare il gruppo invia una mail a " + mail ) );

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

		button = new Button ( "Prometto che cambierò password!", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
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

		if ( permanent.isChecked () )
			params.add ( "permanent", "true" );

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

	private DialogBox passwordRecoveryBox () {
		DialogBox dialog;
		VerticalPanel contents;
		HorizontalPanel bar;
		Button but;

		dialog = new DialogBox ();
		dialog.setText ( "Resetta Password" );

		contents = new VerticalPanel ();
		contents.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );

		contents.add ( new Label ( "Se hai dimenticato la tua password, da qui puoi resettarla e riceverne una nuova." ) );
		contents.add ( new Label ( "Immetti il tuo indirizzo mail e clicca 'Resetta Password': una nuova password verrà automaticamente generata, e ti sarà spedita." ) );

		bar = new HorizontalPanel ();
		contents.add ( bar );

		bar.add ( new Label ( "Il tuo indirizzo mail: " ) );

		recoveryMail = new TextBox ();
		recoveryMail.setVisibleLength ( 20 );
		bar.add ( recoveryMail );

		bar = new HorizontalPanel ();
		contents.add ( bar );

		but = new Button ( "Resetta Password" );
		but.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				String mail;
				ObjectRequest params;

				mail = recoveryMail.getText ();
				if ( mail == "" ) {
					Utils.showNotification ( "Non hai immesso l'indirizzo mail" );
					return;
				}

				params = new ObjectRequest ( "Reset" );
				params.add ( "mail", mail );

				Utils.getServer ().serverGet ( params, new ServerResponse () {
					public void onComplete ( JSONValue response ) {
						/*
							serverGet() non puo' avere una callback vuota, ma in fin dei
							conti qui non c'e' da fare nulla...
						*/
					}
				} );

				Utils.showNotification ( "Ti è stata spedita via mail la tua nuova password", Notification.INFO );
				recoveryDialog.hide ();
			}
		} );
		bar.add ( but );

		but = new Button ( "Annulla" );
		but.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				recoveryDialog.hide ();
			}
		} );
		bar.add ( but );

		dialog.setWidget ( contents );
		return dialog;
	}

	private Widget doCredentials () {
		FlexTable form;
		Button button;
		HorizontalPanel box;
		Hyperlink passwordrecovery;
		KeyUpHandler enter_key;

		form = new FlexTable ();

		username = new TextBox ();
		username.setVisibleLength ( 20 );

		password = new PasswordTextBox ();
		password.setVisibleLength ( 20 );

		form.setWidget ( 0, 0, new Label ( "Username" ) );
		form.setWidget ( 0, 1, username );
		form.setWidget ( 1, 0, new Label ( "Password" ) );
		form.setWidget ( 1, 1, password );

		box = new HorizontalPanel ();
		box.setSpacing ( 5 );
		box.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		form.setWidget ( 2, 1, box );

		button = new Button ( "Login", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				executeLogin ();
			}
		} );
		box.add ( button );

		permanent = new CheckBox ( "Resta Connesso" );
		permanent.addStyleName ( "small-text" );
		box.add ( permanent );

		if ( Session.getGAS ().getBool ( "use_mail" ) == true ) {
			passwordrecovery = new Hyperlink ();
			passwordrecovery.setText ( "Hai perso la password? Clicca qui!" );
			passwordrecovery.addStyleName ( "clickable" );
			passwordrecovery.addStyleName ( "small-text" );
			passwordrecovery.addStyleName ( "top-spaced" );

			passwordrecovery.addClickHandler ( new ClickHandler () {
				public void onClick ( ClickEvent event ) {
					recoveryDialog = passwordRecoveryBox ();
					recoveryDialog.center ();
					recoveryDialog.show ();
				}
			} );
			form.setWidget ( 3, 1, passwordrecovery );
		}

		username.setFocus ( true );

		/*
			Per permettere l'autenticazione con la pressione del tasto Enter
		*/

		enter_key = new KeyUpHandler () {
			public void onKeyUp ( KeyUpEvent event ) {
				int keycode;

				keycode = event.getNativeKeyCode();
				if ( keycode == KeyCodes.KEY_ENTER ) {
					executeLogin ();
				}
			}
		};

		username.addKeyUpHandler ( enter_key );
		password.addKeyUpHandler ( enter_key );

		return form;
	}
}
