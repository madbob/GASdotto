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

public class NamesLabelsWidget extends Label implements FromServerArray {
	private ArrayList	currentElements		= null;

	public NamesLabelsWidget () {
		reset ();
	}

	private void reset () {
		setText ( "Non ci sono opzioni settate" );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		if ( currentElements == null )
			currentElements = new ArrayList ();

		currentElements.add ( element );
		currentElements = Utils.sortArrayByName ( currentElements );
		setElements ( currentElements );
	}

	public void setElements ( ArrayList elements ) {
		int tot;
		String text;
		FromServer obj;

		if ( elements == null ) {
			reset ();
			return;
		}

		tot = elements.size ();
		if ( tot == 0 ) {
			reset ();
			return;
		}

		obj = ( FromServer ) elements.get ( 0 );
		text = obj.getString ( "name" );

		for ( int i = 1; i < tot; i++ ) {
			obj = ( FromServer ) elements.get ( i );
			text = text + ", " + obj.getString ( "name" );
		}

		setText ( text );
		currentElements = elements;
	}

	public void removeElement ( FromServer element ) {
		if ( currentElements == null )
			return;

		currentElements.remove ( element );
		setElements ( currentElements );
	}

	public ArrayList getElements () {
		if ( currentElements == null )
			return new ArrayList ();
		else
			return Utils.dupliacateFromServerArray ( currentElements );
	}

	public void refreshElement ( FromServer element ) {
		FromServer obj;

		if ( currentElements == null )
			return;

		for ( int i = 0; i < currentElements.size (); i++ ) {
			obj = ( FromServer ) currentElements.get ( i );

			if ( obj.equals ( element ) ) {
				currentElements.set ( i, element.duplicate () );
				setElements ( currentElements );
				break;
			}
		}
	}
}
