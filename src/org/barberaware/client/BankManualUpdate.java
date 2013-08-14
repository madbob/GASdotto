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

public class BankManualUpdate extends DialogBox implements SavingDialog, ObjectWidget {
	private ListBox			reason;
	private FromServerSelector	user;
	private BankMovementForm	info;
	private ArrayList		savingCallbacks;

	public BankManualUpdate () {
		final VerticalPanel pan;
		DialogButtons buttons;

		this.setText ( "Aggiornamento Manuale" );

		pan = new VerticalPanel ();
		this.setWidget ( pan );

		pan.add ( new HTML ( "Per movimenti come il pagamento di o per un ordine o il pagamento<br />" +
					"della quota di iscrizione da parte di un utente, fare riferimento alle<br />" +
					"apposite caselle nei relativi pannelli.<br />" ) );

		/*
			Reminder: aggiungendo e rimuovendo voci da qui, deve
			essere aggiornata l'assegnazione della "direzione" del
			movimento nella callback sotto
		*/
		reason = new ListBox ();
		reason.addItem ( "Trasferimento Conto / Cassa" );
		reason.addItem ( "Trasferimento Cassa / Conto" );
		reason.addItem ( "Acquisto del GAS con Bonifico" );
		reason.addItem ( "Acquisto del GAS in Contanti" );
		pan.add ( reason );

		user = new FromServerSelector ( "User", false, true, false );
		pan.add ( user );

		reason.addChangeHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				int selected;

				selected = reason.getSelectedIndex ();

				if ( selected != 0 && selected != 1 )
					user.setVisible ( false );
				else
					user.setVisible ( true );
			}
		} );

		info = new BankMovementForm ();
		pan.add ( info );
		info.setValue ( null );

		buttons = new DialogButtons ();

		buttons.addCallback (
			new SavingDialogCallback () {
				public void onSave ( SavingDialog d ) {
					FromServer movement;

					movement = getValue ();
					movement.save ( null );
					executeCallbacks ( 0 );
					hide ();
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

		reason.setVisible ( false );
		user.setValue ( selected.getObject ( "payuser" ) );
		info.setValue ( selected );
	}

	public FromServer getValue () {
		FromServer movement;

		movement = info.getValue ();

		if ( user.isVisible () )
			movement.setObject ( "payuser", user.getValue () );

		switch ( reason.getSelectedIndex () ) {
			case 0:
				movement.setInt ( "movementtype", BankMovement.INTERNAL_TRANSFER );
				movement.setInt ( "method", BankMovement.BY_BANK );
				break;
			case 1:
				movement.setInt ( "movementtype", BankMovement.INTERNAL_TRANSFER );
				movement.setInt ( "method", BankMovement.BY_CASH );
				break;
			case 2:
				movement.setInt ( "movementtype", BankMovement.GAS_BUYING );
				movement.setInt ( "method", BankMovement.BY_BANK );
				break;
			case 3:
				movement.setInt ( "movementtype", BankMovement.GAS_BUYING );
				movement.setInt ( "method", BankMovement.BY_CASH );
				break;
		}

		if ( movement.getString ( "notes" ) == "" )
			movement.setString ( "notes", reason.getItemText ( reason.getSelectedIndex () ) );

		return movement;
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			savingCallbacks = new ArrayList ();
		savingCallbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			return;
		savingCallbacks.remove ( callback );
	}
}

