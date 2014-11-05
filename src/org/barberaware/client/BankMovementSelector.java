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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankMovementSelector extends BankMovementComponent {
	private TextBox			main;

	private DialogBox		dialog;
	private boolean			opened;

	public BankMovementSelector () {
		opened = false;

		defaultDate = new Date ( System.currentTimeMillis () );
		defaultAmount = 0;
		defaultType = 0;
		justDate = false;
		editable = true;

		main = new TextBox ();
		main.setStyleName ( "bankmovement-selector" );
		main.setVisibleLength ( 40 );
		main.addFocusHandler ( new FocusHandler () {
			public void onFocus ( FocusEvent event ) {
				if ( opened == false ) {
					VerticalPanel pan;
					DialogButtons buttons;
					BankMovementForm form;

					opened = true;
					saveOriginal ();

					pan = new VerticalPanel ();

					form = new BankMovementForm ();
					setWrap ( form );
					transferBankAttributesTo ( form );
					pan.add ( form );

					buttons = new DialogButtons ();
					pan.add ( buttons );
					buttons.addCallback ( new SavingDialogCallback () {
						public void onSave ( SavingDialog sender ) {
							doSave ();
						}

						public void onCancel ( SavingDialog sender ) {
							opened = false;
							dialog.hide ();
							resetObject ();
							unwrap ();
							restoreOriginal ();
						}
					} );

					dialog = new DialogBox ( false );
					dialog.setText ( "Definisci Evento" );
					dialog.setWidget ( pan );

					dialog.center ();
					dialog.show ();
				}
			}

			public void onLostFocus ( Widget sender ) {
				/* dummy */
			}
		} );

		initWidget ( main );
		clean ();
	}

	public void clean () {
		main.setText ( "" );
	}

	private void doSave () {
		BankMovement movement;

		rebuildObject ();
		movement = ( BankMovement ) super.getValue ();

		if ( movement.getFloat ( "amount" ) == 0 ) {
			Utils.showNotification ( "Importo a 0 non valido" );
			return;
		}

		if ( movement.testAmounts () == true ) {
			opened = false;
			dialog.hide ();
			showName ();

			newOriginal ();
			unwrap ();
			setValue ( originalValue );
		}
	}

	private void showName () {
		FromServer obj;

		obj = getValue ();

		if ( obj == null ) {
			main.setText ( "Mai" );
		}
		else {
			main.addStyleName ( "bankmovement-selector-" + obj.getInt ( "method" ) );
			main.setText ( obj.getString ( "name" ) );
		}
	}

	/****************************************************************** FromServerRappresentation */

	public void setValue ( FromServer obj ) {
		super.setValue ( obj );
		showName ();
	}

	public FromServer getValue () {
		FromServer ret;

		rebuildObject ();

		ret = super.getValue ();
		if ( ret != null )
			ret.setInt ( "movementtype", defaultType );

		return ret;
	}
}

