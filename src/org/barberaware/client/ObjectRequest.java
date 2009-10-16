/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import java.lang.*;
import java.util.*;
import com.google.gwt.json.client.*;

public class ObjectRequest {
	private String			type;
	private ArrayList		attributes;

	public ObjectRequest ( String t ) {
		type = t;
		attributes = new ArrayList ();
	}

	public String getType () {
		return type;
	}

	public void add ( String key, String value ) {
		FromServerAttribute attr;
		Class useless;

		useless = null;
		attr = new FromServerAttribute ( key, FromServer.STRING, useless );
		attr.setString ( value );
		attributes.add ( attr );
	}

	public void add ( String key, int value ) {
		FromServerAttribute attr;
		Class useless;

		useless = null;
		attr = new FromServerAttribute ( key, FromServer.INTEGER, useless );
		attr.setInt ( value );
		attributes.add ( attr );
	}

	public void add ( String key, FromServer value ) {
		FromServerAttribute attr;
		Class useless;

		useless = null;
		attr = new FromServerAttribute ( key, FromServer.OBJECT, useless );
		attr.setObject ( value );
		attributes.add ( attr );
	}

	public boolean matches ( FromServer compare ) {
		int len;
		int type;
		boolean good;
		FromServerAttribute attr;

		len = attributes.size ();
		good = true;

		for ( int i = 0; i < len && good == true; i++ ) {
			attr = ( FromServerAttribute ) attributes.get ( i );
			type = attr.type;

			if ( type == FromServer.STRING || type == FromServer.LONGSTRING || type == FromServer.PERCENTAGE )
				good = ( compare.getString ( attr.name ).equals ( attr.getString ( null ) ) );

			else if ( type == FromServer.INTEGER )
				good = ( compare.getInt ( attr.name ) == attr.getInt () );

			else if ( type == FromServer.OBJECT )
				good = ( compare.getObject ( attr.name ).equals ( attr.getObject () ) );
		}

		return good;
	}
}