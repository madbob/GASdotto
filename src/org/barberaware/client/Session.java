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

import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class Session {
	private static boolean		isInstalled	= false;
	private static GAS		currentGAS	= null;
	private static User		currentUser	= null;
	private static SystemConf	currentSystem	= null;

	public static void initSession ( final ServerResponse on_finish ) {
		ObjectRequest params;

		params = new ObjectRequest ( "Session" );

		Utils.getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				JSONObject obj;
				JSONString error;

				error = response.isString ();

				if ( error != null ) {
					if ( error.stringValue () == "no_db" )
						isInstalled = false;
				}
				else {
					isInstalled = true;

					obj = response.isObject ();

					currentUser = new User ();
					currentUser.fromJSONObject ( obj.get ( "user" ).isObject () );

					currentGAS = new GAS ();
					currentGAS.fromJSONObject ( obj.get ( "gas" ).isObject () );

					currentSystem = new SystemConf ();
					currentSystem.fromJSONObject ( obj.get ( "system" ).isObject () );
				}

				on_finish.onComplete ( null );
			}
			public void onError () {
				Window.Location.reload ();
			}
		} );
	}

	public static boolean platformCheck () {
		return isInstalled;
	}

	public static boolean isLoggedIn () {
		return currentUser.isValid ();
	}

	public static User getUser () {
		return ( User ) Utils.getServer ().getObjectFromCache ( "User", currentUser.getLocalID () );
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
