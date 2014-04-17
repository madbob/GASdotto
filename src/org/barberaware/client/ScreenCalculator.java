/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class ScreenCalculator extends DialogBox implements SavingDialog {
	private ArrayList<FloatBox>		boxes;
	private FlexTable			table;
	private FloatBox			completeSum;
	private FloatBox			finalTarget;

	private boolean				running;
	private boolean				wasUsed;

	private ArrayList<SavingDialogCallback>	savingCallbacks;

	public ScreenCalculator () {
		boxes = new ArrayList<FloatBox> ();

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
		box = boxes.get ( 0 );
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

			box.addBlurHandler ( new BlurHandler () {
				public void onBlur ( BlurEvent event ) {
					updateSum ();
				}
			} );
		}
	}

	public void setValue ( float value ) {
		for ( FloatBox box : boxes )
			box.setVal ( value );

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
		for ( FloatBox box : boxes )
			box.setVal ( 0 );
	}

	public boolean hasBeenUsed () {
		return wasUsed;
	}

	private void saveOnTarget () {
		finalTarget.setVal ( completeSum.getVal () );
	}

	private void updateSum () {
		float sum;

		sum = 0;

		for ( FloatBox box : boxes )
			sum += box.getVal ();

		completeSum.setVal ( sum );
	}

	private void closeCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
		hide ();
	}

	private Panel doDialog () {
		VerticalPanel pan;
		DialogButtons buttons;

		pan = new VerticalPanel ();
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.setWidth ( "100%" );

		table = new FlexTable ();
		pan.add ( table );

		completeSum = new FloatBox ();
		table.setWidget ( 0, 0, new Label ( "Totale" ) );
		table.setWidget ( 0, 1, completeSum );

		buttons = new DialogButtons ();
		pan.add ( buttons );
		buttons.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				saveOnTarget ();
				wasUsed = true;
				closeCallbacks ( 0 );
			}

			public void onCancel ( SavingDialog dialog ) {
				closeCallbacks ( 1 );
			}
		} );

		return pan;
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
