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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankRemoveDialog extends DialogBox implements SavingDialog, ObjectWidget {
	private FromServer			object;
	private BankMovementViewer		info;
	private PasswordTextBox			password;
	private ArrayList<SavingDialogCallback>	savingCallbacks;

	public BankRemoveDialog () {
		final VerticalPanel pan;
		DialogButtons buttons;

		this.setText ( "Rimuovi Movimento" );

		pan = new VerticalPanel ();
		this.setWidget ( pan );

		pan.add ( new HTML ( "Sicuro di voler rimuovere questo movimento? Se si, digita la tua password al fondo della finestra." ) );

		info = new BankMovementViewer ();
		pan.add ( info );

		password = new PasswordTextBox ();
		pan.add ( password );

		buttons = new DialogButtons ();
		buttons.addCallback (
			new SavingDialogCallback () {
				public void onSave ( SavingDialog d ) {
					String pass;
					ObjectRequest params;

					pass = password.getText ();
					if ( pass == "" ) {
						Utils.showNotification ( "Password non specificata" );
						return;
					}

					params = new ObjectRequest ( "User" );
					params.add ( "username", Session.getUser ().getString ( "login" ) );
					params.add ( "password", pass );

					Utils.getServer ().serverGet ( params, new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							if ( response.isString () != null && response.isString ().stringValue () == "OK" ) {
								object.destroy ( new ServerResponse () {
									public void onComplete ( JSONValue response ) {
										int type;
										ServerHook hook;

										type = object.getInt ( "movementtype" );
										hook = Utils.getServer ();

										if ( type == BankMovement.ANNUAL_PAYMENT || type == BankMovement.ORDER_USER_PAYMENT || type == BankMovement.USER_CREDIT )
											hook.forceObjectReload ( "User", object.getInt ( "payuser" ) );
										if ( type == BankMovement.ORDER_USER_PAYMENT || type == BankMovement.ORDER_PAYMENT )
											hook.forceObjectReload ( "Supplier", object.getInt ( "paysupplier" ) );

										hook.forceObjectReload ( "GAS", Session.getGAS ().getLocalID () );

										executeCallbacks ( 0 );
										hide ();
									}
								} );
							}
							else {
								Utils.showNotification ( "Password errata. Riprova" );
							}
						}
					} );
				}

				public void onCancel ( SavingDialog d ) {
					executeCallbacks ( 1 );
					hide ();
				}
			}
		);

		pan.add ( buttons );
	}

	private void executeCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		if ( selected == null )
			return;

		object = selected;
		info.setValue ( selected );
	}

	public FromServer getValue () {
		return info.getValue ();
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			savingCallbacks = new ArrayList<SavingDialogCallback> ();
		savingCallbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks != null )
			savingCallbacks.remove ( callback );
	}
}

