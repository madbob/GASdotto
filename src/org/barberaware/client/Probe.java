/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class Probe extends FromServer {
	public Probe () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new StringFromObjectClosure () {
			public String retrive ( FromServer obj ) {
				return "Configurazione";
			}
		} );

		addAttribute ( "writable", FromServer.BOOLEAN );
		addAttribute ( "dbdrivers", FromServer.STRING );
		addAttribute ( "dbdriver", FromServer.STRING );
		addAttribute ( "dbuser", FromServer.STRING );
		addAttribute ( "dbpassword", FromServer.STRING );
		addAttribute ( "dbname", FromServer.STRING );
		addAttribute ( "dbhost", FromServer.STRING );
		addAttribute ( "gasname", FromServer.STRING );
		addAttribute ( "gasmail", FromServer.STRING );
		addAttribute ( "rootpassword", FromServer.STRING );

		setString ( "dbhost", "localhost" );
	}
}
