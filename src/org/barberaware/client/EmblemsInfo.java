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

public class EmblemsInfo {
	private class Emblem {
		public int index	= 0;
		public String name	= null;
		public String path	= null;
		public ArrayList paths	= null;

		public Emblem ( int i, String n, String p ) {
			index = i;
			name = n;
			path = p;
		}

		public Emblem ( int i, String n, ArrayList p ) {
			index = i;
			name = n;
			paths = p;
		}
	}

	private HashMap		symbols;

	public EmblemsInfo () {
		symbols = new HashMap ();
	}

	public void addSymbol ( String name, String path ) {
		Emblem em;

		em = new Emblem ( symbols.size (), name, path );
		symbols.put ( name, em );
	}

	public void addSymbol ( String name, ArrayList paths ) {
		Emblem em;

		em = new Emblem ( symbols.size (), name, paths );
		symbols.put ( name, em );
	}

	public int numSymbols () {
		return symbols.size ();
	}

	public String getSymbol ( String name, int index ) {
		Emblem m;

		m = ( Emblem ) symbols.get ( name );

		if ( m == null ) {
			Window.alert ( "Emblema " + name + " non definito" );
			return null;
		}
		else {
			if ( m.path != null && index == 0 )
				return m.path;
			else
				return ( String ) m.paths.get ( index );
		}
	}

	public int getIndex ( String name ) {
		Emblem m;

		m = ( Emblem ) symbols.get ( name );

		if ( m == null ) {
			Window.alert ( "Emblema " + name + " non definito" );
			return -1;
		}
		else {
			return m.index;
		}
	}
}
