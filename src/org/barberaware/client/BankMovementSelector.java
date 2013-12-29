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

	private boolean			editable;
	private boolean			justDate;
	private boolean			opened;

	private FromServer		originalValue;
	private Date			originalDate;
	private float			originalAmount;

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
					originalValue = getValue ();
					originalDate = originalValue.getDate ( "date" );
					originalAmount = originalValue.getFloat ( "amount" );

					pan = new VerticalPanel ();

					form = new BankMovementForm ();
					form.setEditable ( editable );
					form.showJustDate ( justDate );
					setWrap ( form );
					form.setDefaultDate ( defaultDate );
					form.setDefaultAmount ( defaultAmount );
					form.setDefaultTargetUser ( defaultTargetUser );
					form.setDefaultTargetSupplier ( defaultTargetSupplier );
					form.setDefaultNote ( defaultNote );
					form.showCro ( defaultCro );
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

							originalValue.setDate ( "date", originalDate );
							originalValue.setFloat ( "amount", originalAmount );
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

	public void setEditable ( boolean edit ) {
		editable = edit;
	}

	private void doSave () {
		BankMovement movement;

		rebuildObject ();
		movement = ( BankMovement ) super.getValue ();

		if ( movement.testAmounts () == true ) {
			opened = false;
			dialog.hide ();
			showName ();

			originalValue = getValue ();
			originalValue.forceMod ( true );
			unwrap ();
			setValue ( originalValue );
		}
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
