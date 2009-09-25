/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class Session {
	private static GAS		currentGAS	= null;
	private static User		currentUser	= null;
	private static SystemConf	currentSystem	= null;

	public static void initSession ( final ServerResponse on_finish ) {
		ServerRequest params;

		params = new ServerRequest ( "Session" );

		Utils.getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				JSONObject obj;

				obj = response.isObject ();

				currentUser = new User ();
				currentUser.fromJSONObject ( obj.get ( "user" ).isObject () );

				currentGAS = new GAS ();
				currentGAS.fromJSONObject ( obj.get ( "gas" ).isObject () );

				currentSystem = new SystemConf ();
				currentSystem.fromJSONObject ( obj.get ( "system" ).isObject () );

				on_finish.onComplete ( null );
			}
			public void onError () {
				Window.Location.reload ();
			}
		} );
	}

	public static boolean isLoggedIn () {
		return currentUser.isValid ();
	}

	public static User getUser () {
		return currentUser;
	}

	public static int getPrivileges () {
		return currentUser.getInt ( "privileges" );
	}

	public static GAS getGAS () {
		return currentGAS;
	}

	public static SystemConf getSystemConf () {
		return currentSystem;
	}
}
