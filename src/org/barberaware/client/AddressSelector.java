/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class AddressSelector extends Composite implements AddressWidget {
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
		dialog.setText ( "Definisci Indirizzo" );
		dialog.setWidget ( doDialog () );

		main = new TextBox ();
		main.setStyleName ( "address-selector" );
		main.setVisibleLength ( 40 );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					syncToDialog ();
					dialog.center ();
					street.setFocus ( true );
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
		currentValue = null;
	}

	private Panel doDialog () {
		VerticalPanel container;
		FlexTable pan;
		HorizontalPanel buttons;
		Button but;

		container = new VerticalPanel ();

		pan = new FlexTable ();
		container.add ( pan );

		pan.setWidget ( 0, 0, new Label ( "Indirizzo" ) );
		street = new TextBox ();
		pan.setWidget ( 0, 1, street );

		pan.setWidget ( 1, 0, new Label ( "CAP" ) );
		cap = new TextBox ();
		pan.setWidget ( 1, 1, cap );

		pan.setWidget ( 2, 0, new Label ( "Città" ) );
		city = new TextBox ();
		pan.setWidget ( 2, 1, city );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		container.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( syncFromDialog () == true ) {
					opened = false;
					dialog.hide ();
				}
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		return container;
	}

	private void syncToDialog () {
		if ( currentValue == null )
			return;

		street.setText ( currentValue.getStreet () );
		cap.setText ( currentValue.getCap () );
		city.setText ( currentValue.getCity () );
	}

	private boolean syncFromDialog () {
		String postalcode;
		char c;

		postalcode = cap.getText ();

		/*
			Probabilmente invece di fare il controllo esplicito sulla natura del CAP
			(che deve essere interalmente numerico) si poteva sostituire la TextBox
			con una NumberBox, ma per evitare futuri possibili impicci (dopotutto il
			CAP e' un codice, tanto vale rappresentarlo come stringa) si adotta
			questa soluzione
		*/

		for ( int i = 0; i < postalcode.length (); i++ ) {
			c = postalcode.charAt ( i );

			if ( Character.isDigit ( c ) == false ) {
				Utils.showNotification ( "CAP non valido" );
				return false;
			}
		}

		if ( currentValue == null )
			currentValue = new Address ();

		currentValue.setStreet ( street.getText () );
		currentValue.setCap ( postalcode );
		currentValue.setCity ( city.getText () );

		showAddr ();
		return true;
	}

	private void showAddr () {
		main.setText ( currentValue.getStreet () + " " + currentValue.getCap () + " " + currentValue.getCity () );
	}

	/****************************************************************** AddressWidget */

	public void setValue ( Address addr ) {
		if ( addr == null )
			currentValue = new Address ();
		else
			currentValue = ( Address ) addr.clone ();

		showAddr ();
	}

	public Address getValue () {
		return currentValue;
	}
}
