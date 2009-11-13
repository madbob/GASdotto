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

public abstract class ServerObjectReceive implements RequestCallback {
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

		str = response.getText ();

		jsonObject = JSONParser.parse ( str );

		if ( jsonObject.isString () != null ) {
			Utils.showNotification ( str );
			onError ();
		}
		else
			onReceive ( FromServer.instance ( jsonObject.isObject () ) );

		Utils.getServer ().dataArrived ();
	}

	protected void onError () {
		/* dummy */
	}

	/*
		Richiamata ad ogni nuovo oggetto del tipo definito in arrivo, ricevuto dal
		server. All'atto della registrazione della callback questa viene eseguita anche
		sugli elementi gia' presenti in cache
	*/
	protected abstract void onReceive ( FromServer object );

	/*
		Simile a onReceive, ma viene invocata una volta sola per ogni blocco di oggetti
		in arrivo dal server. Ideale per trattare i dati in ingresso all'avvio
		dell'applicazione, quando tutte le informazioni vengono caricate in un colpo
		solo. Non riceve nessun parametro, i dati sono reperibili nella cache di
		ServerHook: volutamente non viene specificato quali sono i dati arrivati tutti
		insieme nel blocco, per forzare a gestire i dati in modo omogeneo come se fossero
		sempre tutti nuovi (ed evitarsi tanti grattacapi per discriminare oggetti vecchi
		e nuovi).
		Da sovrascrivere solo quando necessario.
	*/
	protected void onBlockBegin () {
		/* dummy */
	}

	/*
		Indica che il blocco di dati, iniziato con onBlockBegin(), e' terminato.
		Da sovrascrivere solo quando necessario.
	*/
	protected void onBlockEnd () {
		/* dummy */
	}

	/*
		Richiamata ogni volta che un oggetto viene modificato, localmente o su
		indicazione del server
	*/
	protected abstract void onModify ( FromServer object );

	/*
		Richiamata ogni volta che un oggetto viene eliminato, localmente o su indicazione
		del server
	*/
	protected abstract void onDestroy ( FromServer object );

	/*
		Da sovrascrivere con una stringa a piacere, univoca per ogni istanza della
		classe. Usata a sole finalita' di debug
	*/
	protected String debugName () {
		return "undefined";
	}
}
