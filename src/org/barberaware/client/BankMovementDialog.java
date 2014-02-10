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

public class BankMovementDialog extends BankMovementComponent implements SavingDialog {
	private BankMovementForm		form;
	private DialogBox			dialog;

	private ArrayList<SavingDialogCallback>	savingCallbacks;

	public BankMovementDialog () {
		VerticalPanel pan;
		DialogButtons buttons;

		pan = new VerticalPanel ();

		form = new BankMovementForm ();
		pan.add ( form );

		buttons = new DialogButtons ();
		pan.add ( buttons );
		buttons.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog sender ) {
				doSave ();
			}

			public void onCancel ( SavingDialog sender ) {
				dialog.hide ();
				resetObject ();
				unwrap ();
				restoreOriginal ();
				closeCallbacks ( 1 );
			}
		} );

		dialog = new DialogBox ( false );
		dialog.setText ( "Definisci Evento" );
		dialog.setWidget ( pan );

		/*
			Super hack: BankMovementComponent e' necessariamente un
			Composite, dunque necessita di un widget iniziale, ma il
			DialogBox non puo' essere usato e mettendoci "pan"
			saltano i riferimenti della gerarchia del DOM.
			Pertanto qui ci metto un elemento a caso, che non verra'
			mai aggiunto da nessuna parte
		*/
		initWidget ( new Button () );
	}

	public void show () {
		saveOriginal ();
		setWrap ( form );
		transferBankAttributesTo ( form );

		dialog.center ();
		dialog.show ();
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
			dialog.hide ();

			newOriginal ();
			unwrap ();
			setValue ( originalValue );

			closeCallbacks ( 0 );
		}
	}

	private void closeCallbacks ( int mode ) {
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

	/****************************************************************** FromServerRappresentation */

	public void setValue ( FromServer obj ) {
		super.setValue ( obj );
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

