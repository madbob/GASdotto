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
import java.lang.*;
import com.google.gwt.user.client.ui.*;

/*
	Costruisce una combo box partendo da un array di stringhe formattate come
	nome/valore
	Rapido hack per i casi di emergenza...
*/

public class PrimitiveStringSelect extends ListBox implements StringWidget {
	public PrimitiveStringSelect ( String [] strings ) {
		String [] tokens;

		for ( int i = 0; i < strings.length; i++ ) {
			tokens = ( strings [ i ] ).split ( "/" );
			addItem ( tokens [ 0 ], tokens [ 1 ] );
		}

		setSelectedIndex ( 0 );
	}

	public void setValue ( String value ) {
		for ( int i = 0; i < getItemCount (); i++ ) {
			if ( getItemText ( i ) == value ) {
				setItemSelected ( i, true );
				break;
			}
		}
	}

	public String getValue () {
		return getValue ( getSelectedIndex () );
	}
}
