/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class ObjectRequest extends JSONObject {
	private String			type;
	private ArrayList		attributes;

	public ObjectRequest ( String t ) {
		type = t;
		attributes = new ArrayList ();
		put ( "type", new JSONString ( type ) );
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

		put ( key, new JSONString ( value ) );
	}

	public void add ( String key, int value ) {
		FromServerAttribute attr;
		Class useless;

		useless = null;
		attr = new FromServerAttribute ( key, FromServer.INTEGER, useless );
		attr.setInt ( value );
		attributes.add ( attr );

		put ( key, new JSONNumber ( value ) );
	}

	public void add ( String key, FromServer value, Class classref ) {
		FromServerAttribute attr;
		Class useless;

		useless = null;
		attr = new FromServerAttribute ( key, FromServer.OBJECT, classref );
		attr.setObject ( value );
		attributes.add ( attr );

		put ( key, new JSONNumber ( value.getLocalID () ) );
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

			if ( type == FromServer.STRING || type == FromServer.LONGSTRING || type == FromServer.PERCENTAGE ) {
				good = ( compare.getString ( attr.name ).equals ( attr.getString ( null ) ) );
			}

			else if ( type == FromServer.INTEGER ) {
				/*
					Questo e' per trattare anche il parametro speciale "id" come se fosse un
					comune intero, sebbene negli oggetti FromServer sia gestito in modo
					particolare e sia accessibile non in forma di attributo ma con l'apposita
					funzione
				*/
				if ( attr.name == "id" )
					good = ( compare.getLocalID () == attr.getInt ( null ) );
				else
					good = ( compare.getInt ( attr.name ) == attr.getInt ( null ) );
			}

			else if ( type == FromServer.OBJECT ) {
				FromServer mine;
				FromServer his;

				/*
					Attenzione: questo non puo' funzionare se una callback
					"fasulla" viene usata per mappare l'oggetto restituito da
					getObject, semplicemente perche' qui non c'e' alcun
					FromServer reale da cui pescarlo
				*/
				mine = attr.getObject ( null );
				his = compare.getObject ( attr.name );
				good = ( his.equals ( mine ) );
			}
		}

		return good;
	}
}
