/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public class ServerHook {
	private class ServerMonitor {
		public String		type;
		public ArrayList	callbacks;
		public HashMap		objects;
		public JSONArray	comparingObjects;

		public ServerMonitor ( String t ) {
			type = t;
			callbacks = new ArrayList ();
			objects = new HashMap ();
			comparingObjects = new JSONArray ();
		}

		public void rebuildComparisons () {
			int num;
			Object [] collected;
			FromServer obj;

			comparingObjects = new JSONArray ();
			collected = objects.values ().toArray ();

			for ( int i = 0; i < collected.length; i++ ) {
				obj = ( FromServer ) collected [ i ];
				comparingObjects.set ( i, new JSONNumber ( obj.getLocalID () ) );
			}
		}
	}

	private int		CurrentRequests		= 0;
	private DialogBox	loadingDialog		= null;
	private HashMap		monitors		= null;
	private int		executingMonitor;
	private ArrayList	monitorSchedulingQueue;
	private RequestDesc	lastRequest		= null;

	/*
		Si forza il numero massimo di richieste concorrenti verso il server a 2, onde
		evitare strane sovrapposizioni. Da notare che limitando ad una sola (dunque
		trattando executingMonitor come un booleano) pare ci sia comunque un qualche
		problema, e le richieste schedulate non vengano eseguite: tenere presente questa
		limitazione qualora si volesse ritoccare questo parametro
	*/
	private static int	MAXIMUM_CONCURRENT_REQUESTS	= 1;

	private void initMonitors () {
		String type;
		ArrayList classes;

		monitors = new HashMap ();
		classes = FromServerFactory.getClasses ();

		for ( int i = 0; i < classes.size (); i++ ) {
			type = ( String ) classes.get ( i );
			monitors.put ( type, new ServerMonitor ( type ) );
		}
	}

	public ServerHook () {
		initMonitors ();
		executingMonitor = 0;
		monitorSchedulingQueue = new ArrayList ();
		lastRequest = new RequestDesc ();
	}

	/****************************************************************** raw */

	public String getDomain () {
		return GWT.getModuleBaseURL ();
	}

	public static String getURL () {
		return GWT.getModuleBaseURL () + "server/";
	}

	private void rawSendReq ( RequestBuilder.Method method, String script, String contents, RequestCallback callback ) {
		RequestBuilder builder;

		builder = new RequestBuilder ( method, getURL () + script );

		try {
			builder.setTimeoutMillis ( 20000 );
			builder.sendRequest ( contents, callback );
			engageLoadingBar ();
		}
		catch ( RequestException e ) {
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
		}
	}

	public void rawPost ( String script, String contents, RequestCallback callback ) {
		rawSendReq ( RequestBuilder.POST, script, contents, callback );
	}

	public void rawGet ( String script, RequestCallback callback ) {
		rawSendReq ( RequestBuilder.GET, script, "", callback );
	}

	public boolean serverGet ( ObjectRequest request, ServerResponse handler ) {
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

	public HTML fileLink ( String name, String group, String file ) {
		return new HTML ( "<a href=\"" + getURL () + "/" + group + "/" + file + "\" class=\"file-link\">" + name + "</a>" );
	}

	/****************************************************************** monitors */

	private boolean addObjectIntoMonitorCache ( ServerMonitor monitor, FromServer obj ) {
		String id;

		id = Integer.toString ( obj.getLocalID () );

		if ( monitor.objects.get ( id ) == null ) {
			monitor.objects.put ( id, obj );
			monitor.comparingObjects.set ( monitor.comparingObjects.size (), new JSONNumber ( obj.getLocalID () ) );
			return true;
		}
		else {
			return false;
		}
	}

	private void deleteObjectFromMonitorCache ( ServerMonitor monitor, FromServer obj ) {
		String id;

		id = Integer.toString ( obj.getLocalID () );
		if ( monitor.objects.remove ( id ) != null )
			monitor.rebuildComparisons ();
	}

	/*
		In linea di massima questa si comporta come

		deleteObjectFromMonitorCache(monitor, obj);
		addObjectIntoMonitorCache(monitor, obj);

		ma si salta il controllo dei duplicati in addObjectIntoMonitorCache() e si evita
		di ricostruire piu' volte l'elenco in monitor.comparingObjects
	*/
	private void updateObjectInMonitorCache ( ServerMonitor monitor, FromServer obj ) {
		String id;

		id = Integer.toString ( obj.getLocalID () );
		monitor.objects.remove ( id );
		monitor.objects.put ( id, obj );
	}

	private void nextObjectReceive () {
		ObjectRequest next;

		if ( monitorSchedulingQueue.size () != 0 ) {
			next = ( ObjectRequest ) monitorSchedulingQueue.remove ( 0 );
			testObjectReceiveImpl ( next );
		}
	}

	private void executeMonitor ( final ServerMonitor monitor, ObjectRequest params ) {
		params.put ( "has", monitor.comparingObjects );
		executingMonitor++;

		serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				JSONToObjects ( response );
				executingMonitor--;
				nextObjectReceive ();
			}
		} );
	}

	private void testObjectReceiveImpl ( ObjectRequest params ) {
		ServerMonitor tmp;

		if ( lastRequest.testAndSet ( params ) == false ) {
			nextObjectReceive ();
		}
		else {
			tmp = getMonitor ( params.getType () );
			executeMonitor ( tmp, params );
		}
	}

	private ServerMonitor getMonitor ( String type ) {
		ServerMonitor ret;

		ret = ( ServerMonitor ) monitors.get ( type );
		if ( ret == null )
			Window.alert ( "Pare che la classe " + type + " non sia stata gestita in FromServerFactory..." );

		return ret;
	}

	private void executeReceivingCallbacks ( ServerMonitor monitor, FromServer object ) {
		int num;
		ServerObjectReceive callback;

		num = monitor.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = ( ServerObjectReceive ) monitor.callbacks.get ( i );
			callback.onReceive ( object );
		}
	}

	public void JSONToObjects ( JSONValue response ) {
		int i;
		int num;
		int existing;
		JSONArray arr;
		FromServer tmp;

		arr = response.isArray ();

		if ( arr != null && arr.size () != 0 ) {
			i = 0;
			num = arr.size ();

			tmp = FromServer.instance ( arr.get ( i ).isObject () );
			triggerObjectBlockCreation ( tmp, true );
			triggerObjectCreation ( tmp );

			for ( i = 1; i < num; i++ ) {
				tmp = FromServer.instance ( arr.get ( i ).isObject () );
				triggerObjectCreation ( tmp );
			}

			triggerObjectBlockCreation ( tmp, false );
		}
	}

	public void onObjectEvent ( String type, ServerObjectReceive callback ) {
		int num;
		ServerMonitor monitor;
		Object [] collected;
		FromServer obj;

		monitor = getMonitor ( type );
		monitor.callbacks.add ( callback );

		collected = monitor.objects.values ().toArray ();

		for ( int i = 0; i < collected.length; i++ ) {
			obj = ( FromServer ) collected [ i ];
			callback.onReceive ( obj );
		}
	}

	/*
		In mancanza di una interfaccia "COMET" in GWT, quando serve forzo un controllo
		presso il server per recuperare la lista di oggetti che mi mancano
	*/
	public void testObjectReceive ( String type ) {
		ObjectRequest params;

		params = new ObjectRequest ( type );
		testObjectReceive ( params );
	}

	private void loadWithCachedObjects ( String type, ObjectRequest params ) {
		int num;
		String subtype;
		ArrayList subclasses;
		FromServer obj;
		ServerMonitor monitor;

		obj = FromServerFactory.dummyInstance ( type );
		subclasses = obj.getContainedObjectsClasses ();
		num = subclasses.size ();

		for ( int i = 0; i < num; i++ ) {
			subtype = ( String ) subclasses.get ( i );
			if ( params.containsKey ( "has_" + subtype ) )
				continue;

			monitor = getMonitor ( subtype );
			params.put ( "has_" + subtype, monitor.comparingObjects );
			loadWithCachedObjects ( subtype, params );
		}
	}

	public void testObjectReceive ( ObjectRequest params ) {
		loadWithCachedObjects ( params.getType (), params );

		if ( executingMonitor >= MAXIMUM_CONCURRENT_REQUESTS ) {
			monitorSchedulingQueue.add ( params );
		}
		else {
			testObjectReceiveImpl ( params );
		}
	}

	public void triggerObjectCreation ( FromServer object ) {
		ServerMonitor tmp;

		tmp = getMonitor ( object.getType () );
		if ( addObjectIntoMonitorCache ( tmp, object ) == true )
			executeReceivingCallbacks ( tmp, object );
	}

	public void triggerObjectBlockCreation ( FromServer object, boolean mode ) {
		int num;
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		num = tmp.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = ( ServerObjectReceive ) tmp.callbacks.get ( i );

			if ( mode == true )
				callback.onBlockBegin ();
			else
				callback.onBlockEnd ();
		}
	}

	public void triggerObjectModification ( FromServer object ) {
		int num;
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		updateObjectInMonitorCache (tmp, object);
		num = tmp.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = ( ServerObjectReceive ) tmp.callbacks.get ( i );
			callback.onModify ( object );
		}
	}

	public void triggerObjectDeletion ( FromServer object ) {
		int num;
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		deleteObjectFromMonitorCache ( tmp, object );

		num = tmp.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = ( ServerObjectReceive ) tmp.callbacks.get ( i );
			callback.onDestroy ( object );
		}
	}

	public void addToCache ( FromServer object ) {
		ServerMonitor monitor;

		monitor = getMonitor ( object.getType () );
		if ( addObjectIntoMonitorCache ( monitor, object ) == true )
			executeReceivingCallbacks ( monitor, object );
	}

	/*
		Attenzione: questa funzione funge solo per reperire gli oggetti nella cache
		locale, e non sincronizza la cache con il server: usare solo per accedere dati
		che si e' sicuri essere gia' stati scaricati da un monitor
	*/
	public FromServer getObjectFromCache ( String type, int id ) {
		ServerMonitor monitor;

		monitor = getMonitor ( type );
		return ( FromServer ) monitor.objects.get ( Integer.toString ( id ) );
	}

	public ArrayList getObjectsFromCache ( String type ) {
		ServerMonitor monitor;

		monitor = getMonitor ( type );
		return new ArrayList ( monitor.objects.values () );
	}

	public void invalidateCacheByCondition ( ObjectRequest req ) {
		int len;
		ArrayList objects;
		FromServer obj;

		objects = getObjectsFromCache ( req.getType () );
		len = objects.size ();

		for ( int i = 0; i < len; i++ ) {
			obj = ( FromServer ) objects.get ( i );
			if ( req.matches ( obj ) )
				triggerObjectDeletion ( obj );
		}

		testObjectReceive ( req );
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
