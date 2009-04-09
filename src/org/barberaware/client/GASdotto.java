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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class GASdotto implements EntryPoint {
	public void onModuleLoad () {
		/**
			TODO	Provvedere ad un qualche meccanismo di fallback qualora il cookie
				non fosse coerente con la sessione salvata sul server
		*/

		Utils.initEnvironment ();

		Session.initSession ( new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				Widget main;

				if ( Session.isLoggedIn () )
					main = new MainApp ();
				else
					main = new Login ();

				RootPanel.get ().add ( main );
			}

			public void onError () {
				Cookies.removeCookie ( "gasdotto" );
				Window.Location.reload ();
			}
		} );
	}
}
