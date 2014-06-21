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

public abstract class PasswordValidateDialog extends DialogBox implements SavingDialog {
	private VerticalPanel			main;
	private PasswordTextBox			password;
	private DialogButtons			buttons;
	private ArrayList<SavingDialogCallback>	savingCallbacks;

	public PasswordValidateDialog () {
		main = new VerticalPanel ();
		this.setWidget ( main );

		main.add ( new HTML ( "Digita la tua password per confermare." ) );

		password = new PasswordTextBox ();
		main.add ( password );

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
								confirmedExecution ();
								executeCallbacks ( 0 );
								hide ();
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

		main.add ( buttons );
	}

	public void add ( Widget widget ) {
		main.insert ( widget, main.getWidgetCount () - 3 );
	}

	public void customSaveLabel ( String label ) {
		buttons.customSaveLabel ( label );
	}

	private void executeCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
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

	/****************************************************************** PasswordValidateDialog */

	protected abstract void confirmedExecution ();
}

