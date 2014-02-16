/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public abstract class WritebackInOutClosure extends ValueFromObjectClosure {
	private void iterateSet ( FromServerAggregate parent, String name, Object value ) {
		ArrayList children;
		FromServer child;

		children = parent.getObjects ();

		if ( children != null ) {
			for ( int i = 0; i < children.size (); i++ ) {
				child = ( FromServer ) children.get ( i );
				child.setAttributeValue ( name, value );
			}
		}
	}

	/*
		Di default, il comportamento e' di assegnare lo stesso valore a
		tutti i "figli" inclusi nell'aggregato. Sovrascrivere questa
		funzione all'occorrenza
	*/
	public void setAttribute ( FromServerAggregate parent, String name, Object value ) {
		iterateSet ( parent, name, value );
	}
}

