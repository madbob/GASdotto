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
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class MonthsSelector extends Composite implements StringWidget {
	private Label		main;
	private DialogBox	selector;
	private ArrayList	months;

	public MonthsSelector ( boolean editable ) {
		main = new Label ( "Nessuno in particolare" );
		main.addStyleName ( "static-value" );
		initWidget ( main );

		/*
			Questa funzione la invoco anche se l'elemento non e' editabile, perche'
			dall'array di CheckBoxes dipendono poi le funzioni di assegnamento ed
			aggregazione dei valori
		*/
		doDialog ();

		if ( editable == true ) {
			main.addStyleName ( "clickable" );

			main.addClickListener ( new ClickListener () {
				public void onClick ( Widget sender ) {
					selector.center ();
					selector.show ();
				}
			} );
		}
	}

	private void doDialog () {
		VerticalPanel container;
		FlexTable chooses;
		CheckBox check;

		months = new ArrayList ();

		selector = new DialogBox ();
		selector.setText ( "Seleziona Mesi" );

		container = new VerticalPanel ();
		selector.setWidget ( container );

		chooses = new FlexTable ();
		container.add ( chooses );

		for ( int i = 0; i < Utils.months.length; i++ ) {
			check = new CheckBox ();
			months.add ( check );
			chooses.setWidget ( i, 0, check );
			chooses.setWidget ( i, 1, new Label ( Utils.months [ i ] ) );
		}

		container.add ( doButtons () );
	}

	private void monthsSelected () {
		String result;
		CheckBox check;

		result = "";

		for ( int i = 0; i < months.size (); i++ ) {
			check = ( CheckBox ) months.get ( i );
			if ( check.isChecked () )
				result = result + " " + Utils.months [ i ];
		}

		if ( result == "" )
			main.setText ( "Nessuno in particolare" );
		else
			main.setText ( result );
	}

	private Widget doButtons () {
		Button but;
		HorizontalPanel buttons;

		buttons = new HorizontalPanel ();
		buttons.setWidth ( "100%" );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				monthsSelected ();
				selector.hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				selector.hide ();
			}
		} );
		buttons.add ( but );

		return buttons;
	}

	public void reset () {
		CheckBox check;

		for ( int i = 0; i < months.size (); i++ ) {
			check = ( CheckBox ) months.get ( i );
			check.setChecked ( false );
		}
	}

	/****************************************************************** StringWidget */

	public void setValue ( String value ) {
		char [] valid;
		CheckBox check;

		reset ();

		if ( value != null ) {
			valid = value.toCharArray ();

			for ( int i = 0; i < valid.length && i < months.size (); i++ ) {
				check = ( CheckBox ) months.get ( i );

				if ( valid [ i ] == 'X' )
					check.setChecked ( true );
				else
					check.setChecked ( false );
			}
		}

		monthsSelected ();
	}

	public String getValue () {
		String result;
		CheckBox check;

		result = "";

		for ( int i = 0; i < months.size (); i++ ) {
			check = ( CheckBox ) months.get ( i );
			if ( check.isChecked () )
				result += 'X';
			else
				result += ' ';
		}

		return result;
	}
}
