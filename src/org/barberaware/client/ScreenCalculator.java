/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public class ScreenCalculator extends DialogBox implements SavingDialog {
	private ArrayList		boxes;
	private FlexTable		table;
	private FloatBox		completeSum;
	private FloatBox		finalTarget;

	private boolean			running;
	private boolean			wasUsed;

	private ArrayList		savingCallbacks;

	public ScreenCalculator () {
		boxes = new ArrayList ();

		running = false;
		wasUsed = false;

		setWidget ( doDialog () );
	}

	public void setTarget ( FloatBox target ) {
		finalTarget = target;
	}

	public void show () {
		FloatBox box;

		if ( running == true )
			return;

		running = true;

		super.show ();
		box = ( FloatBox ) boxes.get ( 0 );
		box.setFocus ( true );
	}

	public void hide () {
		finalTarget.setFocus ( true );
		super.hide ();
		finalTarget.setFocus ( false );

		running = false;
	}

	public void addCells ( int pieces ) {
		int row;
		FloatBox box;

		row = table.getRowCount () - 1;

		for ( int i = 0; i < pieces; i++, row++ ) {
			box = new FloatBox ();
			boxes.add ( box );

			table.insertRow ( row );
			table.setWidget ( row, 1, box );
			table.setWidget ( row, 2, new Label ( "+" ) );

			box.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
					/* dummy */
				}

				public void onLostFocus ( Widget sender ) {
					updateSum ();
				}
			} );
		}
	}

	public void setValue ( float value ) {
		FloatBox box;

		for ( int i = 0; i < boxes.size (); i++ ) {
			box = ( FloatBox ) boxes.get ( i );
			box.setVal ( value );
		}

		wasUsed = true;
	}

	public float [] getValues () {
		float [] ret;
		FloatBox box;

		ret = new float [ boxes.size () ];

		for ( int i = 0; i < boxes.size (); i++ ) {
			box = ( FloatBox ) boxes.get ( i );
			ret [ i ] = box.getVal ();
		}

		return ret;
	}

	public void clear () {
		FloatBox box;

		for ( int i = 0; i < boxes.size (); i++ ) {
			box = ( FloatBox ) boxes.get ( i );
			box.setVal ( 0 );
		}
	}

	public boolean hasBeenUsed () {
		return wasUsed;
	}

	private void saveOnTarget () {
		finalTarget.setVal ( completeSum.getVal () );
	}

	private void updateSum () {
		float sum;
		FloatBox box;

		sum = 0;

		for ( int i = 0; i < boxes.size (); i++ ) {
			box = ( FloatBox ) boxes.get ( i );
			sum += box.getVal ();
		}

		completeSum.setVal ( sum );
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

	private Panel doDialog () {
		VerticalPanel pan;
		HorizontalPanel buttons;
		Button but;

		pan = new VerticalPanel ();
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.setWidth ( "100%" );

		table = new FlexTable ();
		pan.add ( table );

		completeSum = new FloatBox ();
		table.setWidget ( 0, 0, new Label ( "Totale" ) );
		table.setWidget ( 0, 1, completeSum );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				saveOnTarget ();
				wasUsed = true;
				closeCallbacks ( 1 );
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				closeCallbacks ( 0 );
			}
		} );
		buttons.add ( but );

		return pan;
	}

	/****************************************************************** SavingDialog */

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
