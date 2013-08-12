/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class MultiGasEnabler extends Composite {
	private PasswordBox	password;
	private DialogBox	dialog;

	public MultiGasEnabler () {
		Button button;

		button = new Button ( "Abilita" );
		initWidget ( button );

		button.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.center ();
				dialog.show ();
			}
		} );

		dialog = new DialogBox ( false );
		dialog.setText ( "Abilita Multi-GAS" );
		dialog.setWidget ( doDialog () );
	}

	private Panel doDialog () {
		Grid layout;
		VerticalPanel pan;
		DialogButtons buttons;
		HTML message;

		pan = new VerticalPanel ();
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );

		message = new HTML ( "Per abilitare il Multi-GAS devi introdurre qui la password per l'utente \"master\", colui che potrà aggiungere e togliere GAS da questa istanza." );
		message.setStyleName ( "message" );
		pan.add ( message );

		layout = new Grid ( 2, 2 );
		pan.add ( layout );

		password = new PasswordBox ();
		layout.setWidget ( 0, 0, new Label ( "Password" ) );
		layout.setWidget ( 0, 1, password );

		buttons = new DialogButtons ();
		pan.add ( buttons );
		buttons.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog sender ) {
				String p;
				FromServer master;

				p = password.getValue ();

				if ( p == "" ) {
					Utils.showNotification ( "Non hai definito nessuna password" );
				}
				else {
					master = new User ();
					master.setString ( "login", "master" );
					master.setString ( "firstname", "master" );
					master.setString ( "surname", "master" );
					master.setString ( "password", p );
					master.setInt ( "privileges", User.USER_MASTER );

					master.save ( new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							GAS master;

							master = new GAS ();
							master.setString ( "name", "Multi GAS" );
							master.setBool ( "is_master", true );

							master.save ( new ServerResponse () {
								public void onComplete ( JSONValue response ) {
									finishEnable ();
								}
							} );
						}
					} );

					dialog.hide ();
				}
			}

			public void onCancel ( SavingDialog sender ) {
				dialog.hide ();
			}
		} );

		return pan;
	}

	private void finishEnable () {
		DialogBox box;
		VerticalPanel contents;
		HTML message;
		HorizontalPanel buttons;
		Button button;

		box = new DialogBox ();
		box.setText ( "Ricorda!" );

		contents = new VerticalPanel ();

		message = new HTML ( "username: master" );
		message.setStyleName ( "message" );
		contents.add ( message );

		message = new HTML ( "password: quella che hai appena immesso" );
		message.setStyleName ( "message" );
		contents.add ( message );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		contents.add ( buttons );

		button = new Button ( "Me lo son scritto!", new ClickListener () {
			public void onClick ( Widget sender ) {
				Utils.performLogout ();
			}
		} );
		buttons.add ( button );

		box.setWidget ( contents );
		box.center ();
		box.show ();
	}
}

