/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class GASPanel extends GenericPanel {
	private FormCluster		main;

	private FromServerForm		masterForm;
	private MailConfigurator	masterMailConf;

	private FromServer		tmp_root;
	private FromServerForm		tmp_form;

	public GASPanel () {
		super ();

		CaptionPanel frame;

		frame = new CaptionPanel ( "Lista dei GAS" );
		addTop ( frame );

		Utils.getServer ().onObjectEvent ( "ACL", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( object.getString ( "target_type" ) == tmp_root.getType () &&
						object.getInt ( "target_id" ) == tmp_root.getLocalID () &&
						object.getObject ( "gas" ).equals ( Session.getGAS () ) ) {

					object.destroy ( null );
				}
			}

			public void onModify ( FromServer object ) {
				/* dummy */
			}

			public void onDestroy ( FromServer object ) {
				/* dummy */
			}
		} );

		main = new FormCluster ( "GAS", "Nuovo GAS" ) {
			protected FromServerForm doEditableRow ( FromServer gas ) {
				FromServerForm form;
				CustomCaptionPanel frame;

				if ( gas.getBool ( "is_master" ) == true ) {
					masterForm.setValue ( gas );
					return null;
				}

				form = new FromServerForm ( gas );

				frame = new CustomCaptionPanel ( "Attributi" );
				form.add ( frame );
				frame.addPair ( "Nome", form.getWidget ( "name" ) );

				return form;
			}

			protected FromServerForm doNewEditableRow () {
				FromServerForm form;
				CustomCaptionPanel frame;
				UserNameCell username;
				PasswordBox password;

				form = new FromServerForm ( new GAS () );

				form.setCallback ( new FromServerFormCallbacks () {
					public boolean onSave ( FromServerRappresentationFull form ) {
						String u;
						String p;
						DummyTextBox username;
						PasswordBox password;

						username = ( DummyTextBox ) form.retriveInternalWidget ( "username" );
						password = ( PasswordBox ) form.retriveInternalWidget ( "password" );

						u = username.getValue ();
						if ( u.equals ( "" ) ) {
							Utils.showNotification ( "Il nome account non è stato definito" );
							return false;
						}

						p = password.getValue ();
						if ( p.equals ( "" ) ) {
							Utils.showNotification ( "La password non è stata definita" );
							return false;
						}

						tmp_form = ( FromServerForm ) form;

						tmp_root = new User ();
						tmp_root.setString ( "login", u );
						tmp_root.setString ( "firstname", "Amministratore" );
						tmp_root.setString ( "password", p );
						tmp_root.setInt ( "privileges", User.USER_ADMIN );
						tmp_root.save ( new ServerResponse () {
							public void onComplete ( JSONValue response ) {
								int root_id;
								String root_type;
								ACL request;
								ObjectRequest params;

								root_id = tmp_root.getLocalID ();
								root_type = tmp_root.getType ();

								/*
									Dopo aver salvato l'utente, devo prelevarne le ACL in modo da
									eliminarlo dalla lista dei dati accessibili dal GAS corrente (in
									cui viene immesso di default).
									E' un metodo un po' macchinoso, ma l'alternativa sarebbe
									introdurre un ennesimo parametro in fase di salvataggio per
									ignorare l'assegnazione automatica dei permessi e non mi sembra
									il caso
								*/
								params = new ObjectRequest ( "ACL" );
								params.add ( "target_type", root_type );
								params.add ( "target_id", root_id );
								Utils.getServer ().testObjectReceive ( params );

								request = new ACL ();
								request.setObject ( "gas", tmp_form.getValue () );
								request.setString ( "target_type", root_type );
								request.setInt ( "target_id", root_id );
								request.setInt ( "privileges", ACL.ACL_OWNER );
								request.save ( null );
							}
						} );

						return true;
					}
				} );

				frame = new CustomCaptionPanel ( "Attributi" );
				form.add ( frame );
				frame.addPair ( "Nome", form.getWidget ( "name" ) );

				frame = new CustomCaptionPanel ( "Amministratore" );
				form.add ( frame );

				username = new UserNameCell ( null );
				form.setExtraWidget ( "username", username );
				frame.addPair ( "Nome", username );

				password = new PasswordBox ();
				form.setExtraWidget ( "password", password );
				frame.addPair ( "Password", password );

				return form;
			}
		};

		frame.add ( main );

		frame = new CaptionPanel ( "Attributi del Gruppo" );
		addTop ( frame );
		frame.add ( doMasterForm () );

		Utils.getServer ().testObjectReceive ( "GAS" );
	}

	private Widget doMasterForm () {
		CustomCaptionPanel frame;
		BooleanSelector mail;

		masterForm = new FromServerForm ( new GAS () );

		frame = new CustomCaptionPanel ( "Attributi" );
		masterForm.add ( frame );

		frame.addPair ( "Nome", masterForm.getWidget ( "name" ) );

		if ( Session.getSystemConf ().getBool ( "has_file" ) == true )
			frame.addPair ( "Logo Homepage", masterForm.getPersonalizedWidget ( "image", new FileUploadDialog () ) );

		mail = new BooleanSelector ();
		mail.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				BooleanSelector myself;

				myself = ( BooleanSelector ) sender;
				masterMailConf.setEnabled ( myself.getValue () );
			}
		} );
		frame.addPair ( "Abilita Mail", masterForm.getPersonalizedWidget ( "use_mail", mail ) );

		masterMailConf = new MailConfigurator ();
		frame.addPair ( "Configurazione Mail", masterForm.getPersonalizedWidget ( "mail_conf", masterMailConf ) );
		masterMailConf.setEnabled ( Session.getGAS ().getBool ( "use_mail" ) );

		return masterForm;
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione GAS";
	}

	public String getSystemID () {
		return "gas";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_gas.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "GAS" );
	}
}
