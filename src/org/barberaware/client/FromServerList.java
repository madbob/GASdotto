/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class FromServerList implements Iterable {
	private ArrayList<FromServer>	contents;

	public FromServerList () {
		contents = new ArrayList<FromServer> ();
	}

	public void add ( FromServer obj ) {
		if ( has ( obj ) == false )
			contents.add ( obj );
	}

	public void remove ( FromServer obj ) {
		FromServer a;

		for ( int i = 0; i < contents.size (); i++ ) {
			a = contents.get ( i );

			if ( a.equals ( obj ) ) {
				contents.remove ( i );
				break;
			}
		}
	}

	public FromServer get ( int i ) {
		return contents.get ( i );
	}

	public int size () {
		return contents.size ();
	}

	public boolean has ( FromServer obj ) {
		FromServer a;

		for ( int i = 0; i < contents.size (); i++ ) {
			a = contents.get ( i );

			if ( a.equals ( obj ) )
				return true;
		}

		return false;
	}

	public Iterator<FromServer> iterator () {
		return contents.iterator ();
	}
}

