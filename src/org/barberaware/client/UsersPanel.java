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

import java.util.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class UsersPanel extends GenericPanel {
	private FormCluster		main;
	private boolean			handlePayments;
	private CheckBox		toggleLeavedView;

	public UsersPanel () {
		super ();

		/*
			Da notare che se il settaggio sulla gestione dei pagamenti viene cambiato
			l'applicazione si riavvia, dunque non c'e' bisogno di operare un
			controllo a realtime sulla sua correzione
		*/
		handlePayments = ( Session.getGAS ().getBool ( "payments" ) == true );

		main = new FormCluster ( "User", "Nuovo Utente", true, true ) {
				protected FromServerForm doEditableRow ( FromServer u ) {
					final FromServerForm ver;

					if ( u.getInt ( "privileges" ) == User.USER_LEAVED && toggleLeavedView.isChecked () == false )
						return null;

					ver = new FromServerForm ( u );
					ver.emblemsAttach ( Utils.getEmblemsCache ( "users" ) );

					/*
						Il form vero e proprio viene popolate in asyncLoad(), all'atto
						dell'apertura dell'utente selezionato. Questo per evitare di eseguire
						la procedura sempre per tutti gli utenti in arrivo dal server, con il
						risultato di bloccare completamente l'applicazione se gli utenti sono
						numerosi
					*/

					ver.setCallback ( new FromServerFormCallbacks () {
						public void onSave ( FromServerForm form ) {
							CyclicToggle role;
							DateSelector leave;
							Date leavedate;

							role = ( CyclicToggle ) form.retriveInternalWidget ( "privileges" );
							leave = ( DateSelector ) form.retriveInternalWidget ( "leaving_date" );
							leavedate = leave.getValue ();

							/*
								Questo e' per accertarsi che la data di cessazione
								della partecipazione del membro sia settata
							*/
							if ( role.getVal () == User.USER_LEAVED ) {
								if ( leavedate == null )
									leave.setValue ( new Date ( System.currentTimeMillis () ) );
							}

							/*
								Se l'utente viene riabilitato viene annullata la data
								di cessazione
							*/
							else if ( leavedate != null )
								leave.setValue ( null );
						}
					} );

					if ( handlePayments == true ) {
						User user;
						user = ( User ) u;
						user.checkUserPaying ( ver );
					}
					else {
						/*
							Se la gestione delle quote non e' attivata, e non e' stata
							definita alcuna data di pagamento, viene settata una data
							volutamente nel passato per far si che il giorno in cui la
							funzione viene abilitata risulti non pagante
						*/
						if ( u.getDate ( "paying" ) == null )
							u.setDate ( "paying", Utils.decodeDate ( "2000-01-01" ) );
					}

					setRoleIcon ( ver, u );
					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					final FromServerForm ret;

					ret = doEditableRow ( new User () );
					asyncLoad ( ret );
					return ret;
				}

				protected void asyncLoad ( FromServerForm form ) {
					HorizontalPanel hor;
					FromServer user;
					CustomCaptionPanel frame;
					CyclicToggle privileges;
					DateSelector birth;

					/*
						Questa funzione viene invocata ogni volta che un form viene aperto,
						dunque devo accertarmi che non sia gia' stato popolato
					*/
					if ( form.retriveInternalWidget ( "login" ) != null )
						return;

					user = form.getObject ();

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					form.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Anagrafica" );
					hor.add ( frame );

					frame.addPair ( "Login Accesso", form.getWidget ( "login" ) );
					form.setValidation ( "login", checkLoginNameCallback () );

					frame.addPair ( "Nome", form.getWidget ( "firstname" ) );
					frame.addPair ( "Cognome", form.getWidget ( "surname" ) );

					frame.addPair ( "Telefono", form.getWidget ( "phone" ) );
					form.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

					frame.addPair ( "Cellulare", form.getWidget ( "mobile" ) );
					form.setValidation ( "mobile", FromServerValidateCallback.defaultPhoneValidationCallback () );

					frame.addPair ( "Mail", form.getWidget ( "mail" ) );
					form.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

					frame.addPair ( "Mail 2", form.getWidget ( "mail2" ) );
					form.setValidation ( "mail2", FromServerValidateCallback.defaultMailValidationCallback () );

					frame.addPair ( "Indirizzo", form.getWidget ( "address" ) );

					birth = new DateSelector ();
					birth.yearSelectable ( true );
					frame.addPair ( "Data di Nascita", form.getPersonalizedWidget ( "birthday", birth ) );

					frame.addPair ( "Persone in Famiglia", form.getWidget ( "family" ) );

					/*
						La foto personale puo' essere personalizzato solo
						se e' concesso il caricamento di files sul server
					*/
					if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
						String path;
						FileUploadDialog photo;
						final Image image;

						photo = new FileUploadDialog ();
						image = new Image ();

						path = user.getString ( "photo" );
						if ( path == null || path == "" )
							image.setVisible ( false );
						else
							image.setUrl ( path );

						frame.addPair ( "Foto", form.getPersonalizedWidget ( "photo", photo ) );
						frame.addRight ( image );

						photo.setDestination ( "upload_image.php" );

						photo.addChangeListener ( new ChangeListener () {
							public void onChange ( Widget sender ) {
								FileUploadDialog photo;

								photo = ( FileUploadDialog ) sender;
								image.setVisible ( true );
								image.setUrl ( Utils.getServer ().getDomain () + photo.getValue () );
							}
						} );
					}

					/* seconda colonna */

					frame = new CustomCaptionPanel ( "Nel GAS" );
					hor.add ( frame );

					frame.addPair ( "Iscritto da", form.getWidget ( "join_date" ) );
					frame.addPair ( "Data di Cessazione", form.getWidget ( "leaving_date" ) );

					frame.addPair ( "Numero Tessera", form.getWidget ( "card_number" ) );
					form.setValidation ( "card_number", FromServerValidateCallback.defaultUniqueStringValidationCallback () );

					/*
						Se il settaggio sul pagamento delle quote viene modificato
						l'applicazione viene riavviata, pertanto non c'e' bisogno di
						correggere questo pannello ma semplicemente attendere che sia
						ricaricato
					*/
					if ( handlePayments == true )
						frame.addPair ( "Quota pagata", form.getWidget ( "paying" ) );

					privileges = new CyclicToggle ();
					privileges.addState ( "images/user_role_standard.png" );
					privileges.addState ( "images/user_role_reference.png" );
					privileges.addState ( "images/user_role_admin.png" );
					privileges.addState ( "images/user_role_leaved.png" );
					frame.addPair ( "Ruolo", form.getPersonalizedWidget ( "privileges", privileges ) );

					frame.addPair ( "Password", form.getPersonalizedWidget ( "password", new PasswordBox () ) );
					form.setValidation ( "password", FromServerValidateCallback.defaultPasswordValidationCallback () );
					frame.addPair ( "Ultimo Login", form.getPersonalizedWidget ( "lastlogin", new DateViewer () ) );
				}

				protected void customModify ( FromServerForm form ) {
					FromServer u;

					u = form.getObject ();

					if ( handlePayments == true ) {
						User user;
						user = ( User ) u;
						user.checkUserPaying ( form );
					}

					setRoleIcon ( form, u );
				}
		};

		addTop ( Utils.getEmblemsCache ( "users" ).getLegend () );

		/*
			Poiche' addTop() mette il widget specificato in cima alla pagina,
			inserisco la lista di form e la barra di ricerca in ordine inverso in
			modo che il secondo sia alla fine sopra al primo
		*/
		addTop ( main );

		doFilterOptions ();
	}

	private void doFilterOptions () {
		HorizontalPanel pan;
		FormClusterFilter filter;

		pan = new HorizontalPanel ();
		pan.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_LEFT );
		pan.setStyleName ( "panel-up" );
		addTop ( pan );

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
		pan.add ( filter );

		toggleLeavedView = new CheckBox ( "Mostra Utenti Cessati" );
		toggleLeavedView.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				boolean show;
				ArrayList forms;
				CheckBox myself;
				FromServerForm form;

				myself = ( CheckBox ) sender;
				forms = main.collectForms ();
				show = myself.isChecked ();

				if ( show == true ) {
					ObjectRequest params;
					params = new ObjectRequest ( "User" );
					params.add ( "privileges", User.USER_LEAVED );
					Utils.getServer ().testObjectReceive ( params );
				}

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					if ( form.getObject ().getInt ( "privileges" ) == User.USER_LEAVED )
						form.setVisible ( show );
				}
			}
		} );
		pan.add ( toggleLeavedView );
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

	private void setRoleIcon ( FromServerForm form, FromServer user ) {
		int priv;
		EmblemsBar bar;

		priv = user.getInt ( "privileges" );
		bar = form.emblems ();
		bar.activate ( "privileges", priv );

		if ( priv == User.USER_LEAVED && toggleLeavedView.isChecked () == false )
			form.setVisible ( false );
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
