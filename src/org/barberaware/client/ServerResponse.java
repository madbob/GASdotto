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

import java.util.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

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

		try {
			/**
				TODO	Se GASdotto viene installato su una piattaforma web che forza suoi proprio
					contenuti HTML all'interno della pagina, i dati JSON in transito sono
					sporcati ed il parser fallisce nel suo lavoro. Occorrerebbe revisionare le
					stringhe che arrivano per isolare il pezzo di JSON e buttare tutto il resto.

					Da correggere qui e in tutti gli altri posti in cui viene usato
					JSONParser.parse
			*/

			jsonObject = JSONParser.parse ( str );
			ret = jsonObject.isString ();
		}
		catch ( com.google.gwt.json.client.JSONException e ) {
			ret = new JSONString ( "risposta dal server non valida" );
			jsonObject = null;
		}

		if ( ret != null && ret.stringValue ().startsWith ( "Errore: " ) ) {
			Utils.showNotification ( ret.stringValue () );
			onError ();
		}
		else {
			onComplete ( jsonObject );
		}

		Utils.getServer ().dataArrived ();
	}

	protected void onError () {
		/* Sovrascrivere quando necessario */
	}

	protected abstract void onComplete ( JSONValue response );
}
