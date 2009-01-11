/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.json.client.*;

public class ServerRequest extends JSONObject {
	public ServerRequest ( String type ) {
		put ( "type", new JSONString ( type ) );
	}

	public void add ( String key, String value ) {
		put ( key, new JSONString ( value ) );
	}

	public void add ( String key, int value ) {
		put ( key, new JSONNumber ( value ) );
	}

	public void add ( String key, FromServer value ) {
		put ( key, value.toJSONObject () );
	}

	public String getType () {
		return get ( "type" ).isString ().stringValue ();
	}
}
