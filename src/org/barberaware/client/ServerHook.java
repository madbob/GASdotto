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

import java.util.*;
import com.google.gwt.core.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

public class ServerHook {
	private class ServerMonitor {
		public String		type;
		public ArrayList	callbacks;
		public ArrayList	objects;
		public JSONArray	comparingObjects;

		public ServerMonitor ( String t ) {
			type = t;
			callbacks = new ArrayList ();
			objects = new ArrayList ();
			comparingObjects = new JSONArray ();
		}

		public void rebuildComparisons () {
			FromServer obj;

			comparingObjects = new JSONArray ();

			for ( int i = 0; i < objects.size (); i++ ) {
				obj = ( FromServer ) objects.get ( i );
				comparingObjects.set ( i, new JSONNumber ( obj.getLocalID () ) );
			}
		}
	}

	private int		CurrentRequests		= 0;
	private DialogBox	loadingDialog		= null;
	private ArrayList	monitors;
	private int		executingMonitor;
	private ArrayList	monitorSchedulingQueue;

	/*
		Si forza il numero massimo di richieste concorrenti verso il server a 2, onde
		evitare strane sovrapposizioni. Da notare che limitando ad una sola (dunque
		trattando executingMonitor come un booleano) pare ci sia comunque un qualche
		problema, e le richieste schedulate non vengano eseguite: tenere presente questa
		limitazione qualora si volesse ritoccare questo parametro
	*/
	private static int	MAXIMUM_CONCURRENT_REQUESTS	= 2;

	public ServerHook () {
		monitors = new ArrayList ();

		executingMonitor = 0;
		monitorSchedulingQueue = new ArrayList ();
	}

	/****************************************************************** raw */

	public String getDomain () {
		return GWT.getModuleBaseURL ();
	}

	public String getURL () {
		return GWT.getModuleBaseURL () + "server/";
	}

	public void rawPost ( String script, String contents, RequestCallback callback ) {
		RequestBuilder builder;

		builder = new RequestBuilder ( RequestBuilder.POST, getURL () + script );

		try {
			builder.setTimeoutMillis ( 15000 );
			builder.sendRequest ( contents, callback );
		}
		catch ( RequestException e ) {
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
		}
	}

	public boolean serverGet ( ServerRequest request, ServerResponse handler ) {
		RequestBuilder builder;
		Request response;

		builder = new RequestBuilder ( RequestBuilder.POST, getURL () + "server.php?action=get" );

		try {
			builder.setTimeoutMillis ( 15000 );
			response = builder.sendRequest ( request.toString (), handler );
			engageLoadingBar ();
			return true;
		}
		catch ( RequestException e ) {
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
			return false;
		}
	}

	public Widget fileLink ( String name, String group, String file ) {
		return new HTML ( "<a href=\"" + getURL () + "/" + group + "/" + file + "\" class=\"file-link\">" + name + "</a>" );
	}

	/****************************************************************** monitors */

	private void addObjectIntoMonitorCache ( ServerMonitor monitor, FromServer obj ) {
		monitor.objects.add ( obj );
		monitor.comparingObjects.set ( monitor.comparingObjects.size (), new JSONNumber ( obj.getLocalID () ) );
	}

	private void deleteObjectFromMonitorCache ( ServerMonitor monitor, FromServer obj ) {
		int i;
		FromServer iter;

		for ( i = 0; i < monitor.objects.size (); i++ ) {
			iter = ( FromServer ) monitor.objects.get ( i );
			if ( iter.equals ( obj ) ) {
				monitor.objects.remove ( i );
				break;
			}
		}

		monitor.rebuildComparisons ();
	}

	private void executeMonitor ( final ServerMonitor monitor, ServerRequest params ) {
		params.put ( "has", monitor.comparingObjects );
		executingMonitor++;

		serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				int existing;
				JSONArray arr;
				FromServer tmp;
				ServerObjectReceive callback;

				arr = response.isArray ();

				if ( arr != null && arr.size () != 0 ) {
					for ( int i = 0; i < arr.size (); i++ ) {
						tmp = FromServer.instance ( arr.get ( i ).isObject () );
						triggerObjectCreation ( tmp );
					}
				}

				executingMonitor--;

				if ( monitorSchedulingQueue.size () != 0 ) {
					ServerRequest next;
					next = ( ServerRequest ) monitorSchedulingQueue.remove ( 0 );
					testObjectReceiveImpl ( next );
				}
			}
		} );
	}

	private void testObjectReceiveImpl ( ServerRequest params ) {
		ServerMonitor tmp;

		tmp = getMonitor ( params.getType () );
		if ( tmp != null )
			executeMonitor ( tmp, params );
	}

	private ServerMonitor getMonitor ( String type ) {
		ServerMonitor tmp;

		for ( int i = 0; i < monitors.size (); i++ ) {
			tmp = ( ServerMonitor ) monitors.get ( i );

			if ( tmp.type.equals ( type ) )
				return tmp;
		}

		return null;
	}

	public void onObjectEvent ( String type, ServerObjectReceive callback ) {
		ServerMonitor tmp;
		FromServer obj;

		tmp = getMonitor ( type );

		if ( tmp != null ) {
			tmp.callbacks.add ( callback );

			for ( int i = 0; i < tmp.objects.size (); i++ ) {
				obj = ( FromServer ) tmp.objects.get ( i );
				callback.onReceive ( obj );
			}
		}
		else {
			tmp = new ServerMonitor ( type );
			tmp.callbacks.add ( callback );
			monitors.add ( tmp );
		}
	}

	/*
		In mancanza di una interfaccia "COMET" in GWT, quando serve forzo un controllo
		presso il server per recuperare la lista di oggetti che mi mancano
	*/
	public void testObjectReceive ( String type ) {
		ServerRequest params;
		params = new ServerRequest ( type );
		testObjectReceive ( params );
	}

	public void testObjectReceive ( ServerRequest params ) {
		if ( executingMonitor > MAXIMUM_CONCURRENT_REQUESTS )
			monitorSchedulingQueue.add ( params );
		else
			testObjectReceiveImpl ( params );
	}

	public void triggerObjectCreation ( FromServer object ) {
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		if ( tmp != null ) {
			addObjectIntoMonitorCache ( tmp, object );

			for ( int i = 0; i < tmp.callbacks.size (); i++ ) {
				callback = ( ServerObjectReceive ) tmp.callbacks.get ( i );
				callback.onReceive ( object );
			}
		}
	}

	public void triggerObjectModification ( FromServer object ) {
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		if ( tmp != null ) {
			for ( int i = 0; i < tmp.callbacks.size (); i++ ) {
				callback = ( ServerObjectReceive ) tmp.callbacks.get ( i );
				callback.onModify ( object );
			}
		}
	}

	public void triggerObjectDeletion ( FromServer object ) {
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		if ( tmp != null ) {
			for ( int i = 0; i < tmp.callbacks.size (); i++ ) {
				callback = ( ServerObjectReceive ) tmp.callbacks.get ( i );
				callback.onDestroy ( object );
			}

			deleteObjectFromMonitorCache ( tmp, object );
		}
	}

	/*
		Attenzione: questa funzione funge solo per reperire gli oggetti nella cache
		locale, e non sincronizza la cache con il server: usare solo per accedere dati
		che si e' sicuri essere gia' stati scaricati da un monitor
	*/
	public FromServer getObjectFromCache ( String type, int id ) {
		int num;
		ServerMonitor tmp;
		FromServer candidate;

		tmp = getMonitor ( type );
		num = tmp.objects.size ();

		for ( int i = 0; i < num; i++ ) {
			candidate = ( FromServer ) tmp.objects.get ( i );
			if ( candidate.getLocalID () == id )
				return candidate;
		}

		return null;
	}

	public ArrayList getObjectsFromCache ( String type ) {
		int num;
		ServerMonitor tmp;
		ArrayList ret;

		tmp = getMonitor ( type );
		if ( tmp == null )
			return null;

		num = tmp.objects.size ();
		ret = new ArrayList ();

		for ( int i = 0; i < num; i++ )
			ret.add ( tmp.objects.get ( i ) );

		return ret;
	}

	/****************************************************************** loading */

	private void createLoadingNotification () {
		loadingDialog = new DialogBox ();
		loadingDialog.setStyleName ( "loading" );
		loadingDialog.setWidget ( new Image ( "images/loading.gif" ) );
	}

	public void loadingAlert ( boolean activate ) {
		if ( loadingDialog == null )
			createLoadingNotification ();

		if ( activate ) {
			loadingDialog.center ();
			loadingDialog.show ();
		}
		else
			loadingDialog.hide ();
	}

	private void engageLoadingBar () {
		if ( CurrentRequests == 0 )
			loadingAlert ( true );

		CurrentRequests++;
	}

	/*
		Reso pubblico solo per essere accessibile dall'interno delle varie callback
		asincrone, sconsigliato usarlo altrove
	*/
	public void dataArrived () {
		CurrentRequests--;

		if ( CurrentRequests <= 0 )
			loadingAlert ( false );
	}
}
