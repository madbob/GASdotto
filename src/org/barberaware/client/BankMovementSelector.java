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

public class BankMovementSelector extends FromServerRappresentation {
	private TextBox			main;

	private DialogBox		dialog;
	private float			defaultAmount;
	private Date			defaultDate;
	private FromServer		defaultTargetUser;
	private FromServer		defaultTargetSupplier;
	private boolean			defaultCro;
	private int			defaultType;
	private String			defaultNote;

	private boolean			justDate;
	private boolean			opened;
	private boolean			saved;
	private boolean			artificial;
	private FromServer		originalValue;

	public BankMovementSelector () {
		opened = false;
		saved = false;
		artificial = false;

		defaultDate = new Date ( System.currentTimeMillis () );
		defaultAmount = 0;
		defaultType = 0;
		justDate = false;

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
					originalValue = getValue ();

					pan = new VerticalPanel ();

					form = new BankMovementForm ();
					setWrap ( form );
					form.setDefaultDate ( defaultDate );
					form.setDefaultAmount ( defaultAmount );
					form.setDefaultTargetUser ( defaultTargetUser );
					form.setDefaultTargetSupplier ( defaultTargetSupplier );
					form.setDefaultNote ( defaultNote );
					form.showCro ( defaultCro );
					form.showJustDate ( justDate );
					pan.add ( form );

					buttons = new DialogButtons ();
					pan.add ( buttons );
					buttons.addCallback ( new SavingDialogCallback () {
						public void onSave ( SavingDialog sender ) {
							BankMovement movement;

							movement = ( BankMovement ) getValue ();

							if ( movement.testAmounts () == true ) {
								opened = false;
								saved = true;
								dialog.hide ();
								showName ();
							}
						}

						public void onCancel ( SavingDialog sender ) {
							opened = false;
							dialog.hide ();
							resetObject ();
							setValue ( originalValue );
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

	public void setDefaultDate ( Date date ) {
		defaultDate = date;
	}

	public void setDefaultAmount ( float amount ) {
		defaultAmount = amount;
	}

	public float getDefaultAmount () {
		return defaultAmount;
	}

	public void setDefaultTargetUser ( FromServer target ) {
		defaultTargetUser = target;
	}

	public void setDefaultTargetSupplier ( FromServer target ) {
		defaultTargetSupplier = target;
	}

	public void setDefaultType ( int type ) {
		defaultType = type;
	}

	public void setDefaultNote ( String note ) {
		defaultNote = note;
	}

	public void showCro ( boolean show ) {
		defaultCro = show;
	}

	public void showJustDate ( boolean just ) {
		justDate = just;
	}

	private void showName () {
		FromServer obj;

		obj = getValue ();

		if ( obj == null )
			main.setText ( "Mai" );
		else
			main.setText ( obj.getString ( "name" ) );
	}

	/****************************************************************** FromServerRappresentation */

	public void setValue ( FromServer obj ) {
		if ( obj == null ) {
			obj = new BankMovement ();
			artificial = true;
		}

		super.setValue ( obj );
		showName ();
	}

	public FromServer getValue () {
		FromServer ret;

		if ( artificial == true && saved == false ) {
			ret = null;
		}
		else {
			rebuildObject ();

			ret = super.getValue ();
			if ( ret != null )
				ret.setInt ( "movementtype", defaultType );
		}

		return ret;
	}
}

