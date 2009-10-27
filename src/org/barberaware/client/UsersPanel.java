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

		main = new FormCluster ( "User", "Nuovo Utente", true, true ) {
				protected FromServerForm doEditableRow ( FromServer u ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					boolean handle_payments;
					User user;
					CyclicToggle privileges;

					handle_payments = ( Session.getGAS ().getBool ( "payments" ) == true );

					user = ( User ) u;
					ver = new FromServerForm ( user );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Anagrafica" );
					hor.add ( frame );

					frame.addPair ( "Login Accesso", ver.getWidget ( "login" ) );
					ver.setValidation ( "login", checkLoginNameCallback () );

					frame.addPair ( "Nome", ver.getWidget ( "firstname" ) );
					frame.addPair ( "Cognome", ver.getWidget ( "surname" ) );

					frame.addPair ( "Telefono", ver.getWidget ( "phone" ) );
					ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

					frame.addPair ( "Cellulare", ver.getWidget ( "mobile" ) );
					ver.setValidation ( "mobile", FromServerValidateCallback.defaultPhoneValidationCallback () );

					frame.addPair ( "Mail", ver.getWidget ( "mail" ) );
					ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

					frame.addPair ( "Mail 2", ver.getWidget ( "mail2" ) );
					ver.setValidation ( "mail2", FromServerValidateCallback.defaultMailValidationCallback () );

					frame.addPair ( "Indirizzo", ver.getWidget ( "address" ) );

					/* seconda colonna */

					frame = new CustomCaptionPanel ( "Nel GAS" );
					hor.add ( frame );

					frame.addPair ( "Iscritto da", ver.getWidget ( "join_date" ) );

					frame.addPair ( "Numero Tessera", ver.getWidget ( "card_number" ) );
					ver.setValidation ( "card_number", FromServerValidateCallback.defaultUniqueStringValidationCallback () );

					/*
						Se il settaggio sul pagamento delle quote viene modificato
						l'applicazione viene riavviata, pertanto non c'e' bisogno di
						correggere questo pannello ma semplicemente attendere che sia
						ricaricato
					*/
					if ( handle_payments == true ) {
						frame.addPair ( "Quota pagata", ver.getWidget ( "paying" ) );
						user.checkUserPaying ( ver );
					}
					else {
						/*
							Se la gestione delle quote non e' attivata, e non e' stata
							definita alcuna data di pagamento, viene settata una data
							volutamente nel passato per far si che il giorno in cui la
							funzione viene abilitata risulti non pagante
						*/
						if ( user.getDate ( "paying" ) == null )
							user.setDate ( "paying", Utils.decodeDate ( "2000-01-01" ) );
					}

					privileges = new CyclicToggle ();
					privileges.addState ( "images/user_role_standard.png" );
					privileges.addState ( "images/user_role_reference.png" );
					privileges.addState ( "images/user_role_admin.png" );
					frame.addPair ( "Ruolo", ver.getPersonalizedWidget ( "privileges", privileges ) );

					frame.addPair ( "Password", ver.getPersonalizedWidget ( "password", new PasswordBox () ) );

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

					frame.addPair ( "Ultimo Login", ver.getPersonalizedWidget ( "lastlogin", new DateViewer () ) );

					setRoleIcon ( ver, user );
					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					return doEditableRow ( new User () );
				}

				protected void customModify ( FromServerForm form ) {
					User user;

					user = ( User ) form.getObject ();

					if ( Session.getGAS ().getBool ( "payments" ) == true )
						user.checkUserPaying ( form );

					setRoleIcon ( form, user );
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

	private void setRoleIcon ( FromServerForm form, User user ) {
		int privileges;
		IconsBar bar;

		privileges = user.getInt ( "privileges" );
		bar = form.getIconsBar ();

		bar.delImage ( "images/notifications/user_responsable.png" );
		bar.delImage ( "images/notifications/user_admin.png" );

		if ( privileges == User.USER_RESPONSABLE ) {
			bar.addImage ( "images/notifications/user_responsable.png" );
		}
		else if ( privileges == User.USER_ADMIN ) {
			bar.addImage ( "images/notifications/user_admin.png" );
		}
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
		main.unlock ();
		Utils.getServer ().testObjectReceive ( "User" );
	}
}
