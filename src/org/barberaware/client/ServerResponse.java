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

import java.util.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

public abstract class ServerResponse implements RequestCallback {
	public void onError ( Request request, Throwable exception ) {
		if ( exception instanceof RequestTimeoutException )
			Utils.showNotification ( "Timeout sulla connessione: accertarsi che il server sia raggiungibile" );
		else
			Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );

		Utils.getServer ().dataArrived ();
	}

	public void onResponseReceived ( Request request, Response response ) {
		JSONValue jsonObject;
		String str;
		JSONString ret;

		str = response.getText ();

		jsonObject = JSONParser.parse ( str );
		ret = jsonObject.isString ();

		if ( ret != null && ret.stringValue ().startsWith ( "Errore: " ) ) {
			Utils.showNotification ( str );
			onError ();
		}
		else
			onComplete ( jsonObject );

		Utils.getServer ().dataArrived ();
	}

	protected void onError () {
		/* Sovrascrivere quando necessario */
	}

	protected abstract void onComplete ( JSONValue response );
}
