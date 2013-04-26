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
import com.google.gwt.user.client.ui.*;

public class FromServerSavingDialog extends FromServerRappresentationFull implements SavingDialog {
	private FromServer	reference;

	private DialogBox	dialog;
	private VerticalPanel	main;

	private ArrayList	savingCallbacks;

	public FromServerSavingDialog ( FromServer ref, String title ) {
		HorizontalPanel buttons;
		VerticalPanel fake;
		Button but;

		fake = new VerticalPanel ();
		initWidget ( fake );

		setValue ( ref );

		dialog = new DialogBox ();
		dialog.setText ( title );

		main = new VerticalPanel ();
		dialog.setWidget ( main );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				savingObject ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				resetObject ();
				closeCallbacks ( 0 );
			}
		} );
		buttons.add ( but );
	}

	public void setWidget ( Widget wid ) {
		while ( main.getWidgetCount () > 1 )
			main.remove ( 0 );
		main.insert ( wid, 0 );
	}

	public void show () {
		openCallbacks ();
		dialog.show ();
		dialog.center ();
	}

	public void hide () {
		closingCallbacks ();
		dialog.hide ();
		closeCallbacks ();
	}

	private void closeCallbacks ( int mode ) {
		SavingDialogCallback call;

		if ( savingCallbacks != null ) {
			for ( int i = 0; i < savingCallbacks.size (); i++ ) {
				call = ( SavingDialogCallback ) savingCallbacks.get ( i );

				if ( mode == 1 )
					call.onSave ( this );
				else
					call.onCancel ( this );
			}
		}

		hide ();
	}

	/****************************************************************** FromServerRappresentationFull */

	protected void beforeSave () {
		dialog.hide ();
	}

	protected void afterSave () {
		closeCallbacks ( 1 );
	}

	/****************************************************************** SavingDialog */

	/*
		In verita' l'implementazione dell'interfaccia SavingDialog e'
		assolutamente ridondante, in quanto gia' le funzioni delle
		callbacks FromServerFormCallback prevedono queste funzioni, ma
		la aggiungo per consistenza
	*/

	public void addCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			savingCallbacks = new ArrayList ();
		savingCallbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks != null )
			savingCallbacks.remove ( callback );
	}
}
