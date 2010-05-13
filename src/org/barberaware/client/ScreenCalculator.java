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

public class ScreenCalculator extends DialogBox {
	private ArrayList		boxes;
	private FloatBox		completeSum;
	private FloatBox		finalTarget;

	public ScreenCalculator ( int pieces ) {
		boxes = new ArrayList ();
		setWidget ( doDialog ( pieces ) );
	}

	public void setTarget ( FloatBox target ) {
		finalTarget = target;
	}

	public void show () {
		FloatBox box;

		super.show ();
		box = ( FloatBox ) boxes.get ( 0 );
		box.setFocus ( true );
	}

	public void hide () {
		finalTarget.setFocus ( true );
		super.hide ();
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

	private Panel doDialog ( int pieces ) {
		VerticalPanel pan;
		FlexTable table;
		FloatBox box;
		HorizontalPanel buttons;
		Button but;

		pan = new VerticalPanel ();
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.setWidth ( "100%" );

		table = new FlexTable ();
		pan.add ( table );

		for ( int i = 0; i < pieces; i++ ) {
			box = new FloatBox ();
			boxes.add ( box );

			table.setWidget ( i, 1, box );

			if ( i == pieces - 1 )
				table.setWidget ( i, 2, new Label ( "=" ) );
			else
				table.setWidget ( i, 2, new Label ( "+" ) );

			box.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
					/*
						dummy
					*/
				}

				public void onLostFocus ( Widget sender ) {
					updateSum ();
				}
			} );
		}

		completeSum = new FloatBox ();
		table.setWidget ( pieces, 0, new Label ( "Totale" ) );
		table.setWidget ( pieces, 1, completeSum );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				saveOnTarget ();
				hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				hide ();
			}
		} );
		buttons.add ( but );

		return pan;
	}
}
