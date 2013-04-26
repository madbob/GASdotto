/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class GASdotto implements EntryPoint {
	/*
		TODO	Usare GWT.UncaughtExceptionHandler() al posto di questo
	*/
	private static native void catchErrors () /*-{
		window.onerror = function(msg, url, line) {
			@org.barberaware.client.Utils::bigError(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(msg, url, line);
		}
	}-*/;

	public void onModuleLoad () {
		catchErrors ();
		Utils.initEnvironment ();

		Session.initSession ( new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				Widget main;

				if ( Session.platformCheck () == false )
					main = new InstallForm ();
				else if ( Session.isLoggedIn () )
					main = new MainApp ();
				else
					main = new Login ();

				RootPanel.get ().add ( main );
			}
		} );
	}
}
