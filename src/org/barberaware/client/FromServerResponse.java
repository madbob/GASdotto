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

public class FromServerResponse extends ServerResponse {
	public static int		ACTION_CREATE	= 0;
	public static int		ACTION_MODIFY	= 1;
	public static int		ACTION_DELETE	= 2;

	private int			type;
	private FromServer		reference;
	private ServerResponse		callback;

	public FromServerResponse ( int t, FromServer ref, ServerResponse resp ) {
		type = t;
		reference = ref;
		callback = resp;
	}

	public void onComplete ( JSONValue response ) {
		int localID;

		localID = Integer.parseInt ( response.isString ().stringValue () );

		if ( localID < 0 ) {
			/*
				Apparentemente inutile risettare il localid se il salvataggio e'
				fallito, ma questo permette quantomeno di attivare qualche
				eventuale trigger che ci si aspetta sia eseguito alla ricezione
				di una risposta.
				Cfr. FromServer::savingOperation
			*/
			reference.setLocalID ( reference.getLocalID () );

			Utils.showNotification ( "Errore nel salvataggio sul database" );

			if ( callback != null )
				callback.onError ();
		}

		else {
			ServerHook server;

			reference.setLocalID ( localID );
			server = Utils.getServer ();

			if ( type == ACTION_CREATE ) {
				/*
					Se l'oggetto ha la proprieta' "alwaysReload" si provvede
					a forzare il ricaricamento dello stesso dal server
					(chiedendo il reperimento di tutti gli oggetti dello
					stesso tipo che gia' non stanno in cache, dunque anche
					questo). Altrimenti si provvede localmente a metterlo in
					cache e a triggerare le callbacks.
					Questo per gestire correttamente quegli oggetti che in
					fase di salvataggio vengono rimaneggiati pesantemente dal
					server (ad esempio: gli Ordini, i cui prodotti sono
					assegnati in funzione dei prodotti correntemente
					ordinabili), e per caricarne una versione aggiornata
				*/
				if ( reference.alwaysReload () == true )
					server.forceObjectReload ( reference.getType (), localID );
				else
					server.triggerObjectCreation ( reference );
			}
			else if ( type == ACTION_MODIFY ) {
				if ( reference.alwaysReload () == true )
					server.forceObjectReload ( reference.getType (), localID );
				else
					server.triggerObjectModification ( reference );
			}
			else if ( type == ACTION_DELETE ) {
				server.triggerObjectDeletion ( reference );
			}

			if ( callback != null )
				callback.onComplete ( response );
		}
	}
}
