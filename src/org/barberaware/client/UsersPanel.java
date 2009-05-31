/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;

public class UsersPanel extends GenericPanel {
	private FormCluster		main;

	public UsersPanel () {
		super ();

		FormClusterFilter filter;

		/**
			TODO	La costruzione di tutti i form per tutti gli utenti rallenta
				indicibilmente l'applicazione la prima volta che essi vengono
				richiamati: provvedere una qualche policy percui l'operazione di
				realizzazione del form avvenga solo quando espressamente
				richiesto, magari all'ingresso del pannello stesso
		*/

		main = new FormCluster ( "User", "images/new_user.png" ) {
				protected FromServerForm doEditableRow ( FromServer u ) {
					final FromServerForm ver;
					boolean handle_payments;
					FlexTable fields;
					User user;
					CyclicToggle privileges;

					handle_payments = ( Session.getGAS ().getBool ( "payments" ) == true );

					user = ( User ) u;
					ver = new FromServerForm ( user );

					fields = new FlexTable ();
					ver.add ( fields );

					/* prima colonna */

					fields.setWidget ( 0, 0, new Label ( "Login Accesso" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "login" ) );
					ver.setValidation ( "login", checkLoginNameCallback () );

					fields.setWidget ( 1, 0, new Label ( "Nome" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "firstname" ) );

					fields.setWidget ( 2, 0, new Label ( "Cognome" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "surname" ) );

					fields.setWidget ( 3, 0, new Label ( "Telefono" ) );
					fields.setWidget ( 3, 1, ver.getWidget ( "phone" ) );
					ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

					fields.setWidget ( 4, 0, new Label ( "Cellulare" ) );
					fields.setWidget ( 4, 1, ver.getWidget ( "mobile" ) );
					ver.setValidation ( "mobile", FromServerValidateCallback.defaultPhoneValidationCallback () );

					fields.setWidget ( 5, 0, new Label ( "Mail" ) );
					fields.setWidget ( 5, 1, ver.getWidget ( "mail" ) );
					ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

					fields.setWidget ( 6, 0, new Label ( "Mail 2" ) );
					fields.setWidget ( 6, 1, ver.getWidget ( "mail2" ) );
					ver.setValidation ( "mail2", FromServerValidateCallback.defaultMailValidationCallback () );

					fields.setWidget ( 7, 0, new Label ( "Indirizzo" ) );
					fields.setWidget ( 7, 1, ver.getWidget ( "address" ) );

					/* seconda colonna */

					fields.setWidget ( 0, 2, new Label ( "Iscritto da" ) );
					fields.setWidget ( 0, 3, ver.getWidget ( "join_date" ) );

					fields.setWidget ( 1, 2, new Label ( "Numero Tessera" ) );
					fields.setWidget ( 1, 3, ver.getWidget ( "card_number" ) );
					ver.setValidation ( "card_number", FromServerValidateCallback.defaultUniqueStringValidationCallback () );

					/*
						Se il settaggio sul pagamento delle quote viene modificato
						l'applicazione viene riavviata, pertanto non c'e' bisogno di
						correggere questo pannello ma semplicemente attendere che sia
						ricaricato
					*/
					if ( handle_payments == true ) {
						fields.setWidget ( 2, 2, new Label ( "Quota pagata" ) );
						fields.setWidget ( 2, 3, ver.getWidget ( "paying" ) );
					}
					else
						user.setDate ( "paying", Utils.decodeDate ( "2000-01-01" ) );

					fields.setWidget ( 3, 2, new Label ( "Ruolo" ) );
					privileges = new CyclicToggle ();
					privileges.addState ( "images/user_role_standard.png" );
					privileges.addState ( "images/user_role_reference.png" );
					privileges.addState ( "images/user_role_admin.png" );
					fields.setWidget ( 3, 3, ver.getPersonalizedWidget ( "privileges", privileges ) );

					fields.setWidget ( 4, 2, new Label ( "Password" ) );
					fields.setWidget ( 4, 3, ver.getPersonalizedWidget ( "password", new PasswordBox () ) );

					/*
						Se si sta definendo un nuovo utente si controlla
						che la password sia effettivamente settata
					*/
					if ( u.isValid () == false ) {
						ver.setValidation ( "password", new FromServerValidateCallback () {
							public boolean check ( FromServer object, String attribute, Widget widget ) {
								String text;

								text = ( ( StringWidget ) widget ).getValue ();
								if ( text.equals ( "" ) ) {
									Utils.showNotification ( "La password non è stata definita" );
									return false;
								}

								/*
									Se il controllo e' andato a buon fine,
									elimino questa callback di validazione.
									L'oggetto viene salvato, ed il form
									resettato, ed il campo password torna ad
									essere vuoto pur essendo stata la password
									settata
								*/
								ver.setValidation ( "password", null );
								return true;
							}
						} );
					}

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					return doEditableRow ( new User () );
				}

				protected int sorting ( FromServer first, FromServer second ) {
					if ( first == null )
						return 1;
					else if ( second == null )
						return -1;

					return -1 * ( first.getString ( "name" ).compareTo ( second.getString ( "name" ) ) );
				}
		};

		/*
			Poiche' addTop() mette il widget specificato in cima alla pagina,
			inserisco la lista di form e la barra di ricerca in ordine inverso in
			modo che il secondo sia alla fine sopra al primo
		*/
		addTop ( main );

		filter = new FormClusterFilter ( main, new FilterCallback () {
			public boolean check ( FromServer obj, String text ) {
				int len;
				String start;

				len = text.length ();

				start = obj.getString ( "firstname" ).substring ( 0, len );
				if ( start.compareToIgnoreCase ( text ) == 0 )
					return true;

				start = obj.getString ( "surname" ).substring ( 0, len );
				if ( start.compareToIgnoreCase ( text ) == 0 )
					return true;

				return false;
			}
		} );
		addTop ( filter );
	}

	private FromServerValidateCallback checkLoginNameCallback () {
		return 
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					String text;
					boolean ret;
					FromServer iter;
					ArrayList list;

					text = ( ( StringWidget ) widget ).getValue ();
					if ( text.equals ( "" ) ) {
						Utils.showNotification ( "Il nome account non è stato definito" );
						return false;
					}

					/*
						Attenzione: il valore viene confrontato solo con
						quelli gia' presenti in cache
					*/

					list = Utils.getServer ().getObjectsFromCache ( "User" );
					ret = true;

					for ( int i = 0; i < list.size (); i++ ) {
						iter = ( FromServer ) list.get ( i );

						if ( ( iter.equals ( object ) == false ) &&
								( iter.getString ( "login" ).equals ( text ) ) ) {

							Utils.showNotification ( "Nome account non univoco" );
							ret = false;
							break;
						}
					}

					return ret;
				}
			};
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Utenti";
	}

	public String getSystemID () {
		return "users";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_users.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "User" );
	}
}
