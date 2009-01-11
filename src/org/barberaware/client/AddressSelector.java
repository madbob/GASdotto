/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class AddressSelector extends Composite {
	private TextBox			main;
	private Address			currentValue;

	private DialogBox		dialog;
	private TextBox			street;
	private TextBox			cap;
	private TextBox			city;

	private boolean			opened;

	public AddressSelector () {
		opened = false;

		dialog = new DialogBox ( false );
		dialog.setWidget ( doDialog () );

		main = new TextBox ();
		main.setStyleName ( "address-selector" );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					syncToDialog ();
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

	public Address getValue () {
		return currentValue;
	}

	public void setValue ( Address addr ) {
		currentValue = addr;
	}

	public void clean () {
		main.setText ( "" );
		currentValue = null;
	}

	private Panel doDialog () {
		FlexTable pan;
		Button but;

		pan = new FlexTable ();

		pan.setWidget ( 0, 0, new Label ( "Indirizzo" ) );
		street = new TextBox ();
		pan.setWidget ( 0, 1, street );

		pan.setWidget ( 1, 0, new Label ( "CAP" ) );
		cap = new TextBox ();
		pan.setWidget ( 1, 1, cap );

		pan.setWidget ( 2, 0, new Label ( "Città" ) );
		city = new TextBox ();
		pan.setWidget ( 2, 1, city );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				syncFromDialog ();
				opened = false;
				dialog.hide ();
			}
		} );
		pan.setWidget ( 3, 0, but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				dialog.hide ();
			}
		} );
		pan.setWidget ( 3, 1, but );

		return pan;
	}

	private void syncToDialog () {
		if ( currentValue == null )
			return;

		street.setText ( currentValue.getStreet () );
		cap.setText ( currentValue.getCap () );
		city.setText ( currentValue.getCity () );
	}

	private void syncFromDialog () {
		if ( currentValue == null )
			currentValue = new Address ();

		currentValue.setStreet ( street.getText () );
		currentValue.setCap ( cap.getText () );
		currentValue.setCity ( city.getText () );
	}
}
