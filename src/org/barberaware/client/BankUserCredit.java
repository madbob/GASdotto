/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankUserCredit extends Composite implements FloatWidget {
	private PriceViewer		credit;
	private DialogBox		dialog;
	private BankMovementForm	info;
	private FromServer		currentUser;

	public BankUserCredit () {
		HorizontalPanel main;
		PushButton button;

		main = new HorizontalPanel ();
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_LEFT );
		main.setStyleName ( "bank-user-credit" );
		initWidget ( main );

		credit = new PriceViewer ();
		main.add ( credit );

		button = new PushButton ( new Image ( "images/mini_edit.png" ), new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				doDialog ();
			}
		} );
		main.add ( button );
	}

	private void doDialog () {
		VerticalPanel pan;
		DialogButtons buttons;

		dialog = new DialogBox ();
		dialog.setText ( "Aggiungi Credito" );

		pan = new VerticalPanel ();
		dialog.setWidget ( pan );

		info = new BankMovementForm ();
		info.setValue ( new BankMovement () );
		info.setDefaultDate ( new Date ( System.currentTimeMillis () ) );
		info.showCro ( false );
		info.setDefaultMethod ( BankMovement.BY_CASH );
		info.setDefaultTargetUser ( currentUser );
		info.setDefaultType ( BankMovement.USER_CREDIT );
		pan.add ( info );

		buttons = new DialogButtons ();

		buttons.addCallback (
			new SavingDialogCallback () {
				public void onSave ( SavingDialog d ) {
					FromServer movement;

					movement = info.getValue ();

					if ( movement.getFloat ( "amount" ) == 0 ) {
						Utils.showNotification ( "Importo a 0 non valido" );
						return;
					}

					movement.save ( new ServerResponse () {
						protected void onComplete ( JSONValue response ) {
							Utils.getServer ().forceObjectReload ( currentUser );
						}
					} );

					dialog.hide ();
				}

				public void onCancel ( SavingDialog d ) {
					dialog.hide ();
				}
			}
		);

		pan.add ( buttons );

		dialog.center ();
		dialog.show ();
	}

	public void setUser ( FromServer user ) {
		currentUser = user;
	}

	/****************************************************************** FloatWidget */

	public void setVal ( float value ) {
		credit.setVal ( value );
	}

	public float getVal () {
		return credit.getVal ();
	}
}
