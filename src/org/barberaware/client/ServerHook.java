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
import com.google.gwt.core.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class ServerHook {
	private class ServerMonitor {
		public String				type;
		public ArrayList<ServerObjectReceive>	callbacks;
		public HashMap<String, FromServer>	objects;
		public JSONArray			comparingObjects;

		public ServerMonitor ( String t ) {
			type = t;
			callbacks = new ArrayList<ServerObjectReceive> ();
			objects = new HashMap<String, FromServer> ();
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

	private int					CurrentRequests		= 0;
	private DialogBox				loadingDialog		= null;
	private HashMap<String, ServerMonitor>		monitors		= null;
	private int					executingMonitor;
	private ArrayList<ObjectRequest>		monitorSchedulingQueue;
	private RequestDesc				lastRequest		= null;
	private HashMap<String, ArrayList<FromServer>>	recursionStack;

	/*
		Si forza il numero massimo di richieste concorrenti verso il server a 2, onde
		evitare strane sovrapposizioni. Da notare che limitando ad una sola (dunque
		trattando executingMonitor come un booleano) pare ci sia comunque un qualche
		problema, e le richieste schedulate non vengano eseguite: tenere presente questa
		limitazione qualora si volesse ritoccare questo parametro
	*/
	private static int	MAXIMUM_CONCURRENT_REQUESTS	= 2;

	private void initMonitors () {
		String type;
		ArrayList classes;

		monitors = new HashMap<String, ServerMonitor> ();
		classes = FromServerFactory.getClasses ();

		for ( int i = 0; i < classes.size (); i++ ) {
			type = ( String ) classes.get ( i );
			monitors.put ( type, new ServerMonitor ( type ) );
		}
	}

	public ServerHook () {
		initMonitors ();
		executingMonitor = 0;
		monitorSchedulingQueue = new ArrayList<ObjectRequest> ();
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

		if ( request.getUseCache () == true )
			loadWithCachedObjects ( request.getType (), request );

		builder = new RequestBuilder ( RequestBuilder.POST, getURL () + "server.php?action=get" );

		try {
			response = builder.sendRequest ( request.toString (), handler );
			engageLoadingBar ();
			return true;
		}
		catch ( RequestException e ) {
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
			return false;
		}
	}

	public static HTML fileLink ( String name, String group, String file ) {
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

	public void updateObjectInMonitorCache ( FromServer obj ) {
		String id;
		FromServer old;
		ServerMonitor monitor;

		if ( obj == null )
			return;

		monitor = getMonitor ( obj.getType () );
		id = Integer.toString ( obj.getLocalID () );

		old = monitor.objects.get ( id );
		if ( old != null )
			old.transferRelatedInfo ( obj );

		monitor.objects.remove ( id );
		monitor.objects.put ( id, obj );
	}

	private void nextObjectReceive () {
		ObjectRequest next;

		if ( monitorSchedulingQueue.size () != 0 ) {
			next = monitorSchedulingQueue.remove ( 0 );
			testObjectReceiveImpl ( next );
		}
	}

	private void executeMonitor ( final ServerMonitor monitor, ObjectRequest params ) {
		if ( params.getUseCache () == true )
			params.put ( "has", monitor.comparingObjects );

		executingMonitor++;

		serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				responseToObjects ( response );
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

		ret = monitors.get ( type );
		if ( ret == null )
			Window.alert ( "Pare che la classe " + type + " non sia stata gestita in FromServerFactory..." );

		return ret;
	}

	private void executeReceivingCallbacks ( ServerMonitor monitor, FromServer object ) {
		int num;
		ServerObjectReceive callback;

		num = monitor.callbacks.size ();

		/*
			Le callback onReceivePreemptive() e onReceive() devono essere invocate separatamente, *non*
			unire questi due cicli
		*/

		for ( int i = 0; i < num; i++ ) {
			callback = monitor.callbacks.get ( i );
			callback.onReceivePreemptive ( object );
		}

		for ( int i = 0; i < num; i++ ) {
			callback = monitor.callbacks.get ( i );
			callback.onReceive ( object );
		}
	}

	public ArrayList<FromServer> responseToObjects ( JSONValue response, String defaultType ) {
		int num;
		int existing;
		boolean first_round;
		ArrayList<FromServer> ret;
		JSONArray arr;
		JSONObject obj;
		FromServer tmp;
		FromServer test;
		FromServer mod;

		initRecursionStack ();
		ret = new ArrayList<FromServer> ();
		arr = response.isArray ();

		if ( arr != null && arr.size () != 0 ) {
			num = arr.size ();
			mod = null;
			first_round = true;

			for ( int i = 0; i < num; i++ ) {
				obj = arr.get ( i ).isObject ();

				/*
					In questo caso nell'array mi trovo solo l'ID di un oggetto
					gia' gestito da qualche parte in precedenza, posso evitare
					di attivare le callback di ricezione
				*/
				if ( obj == null ) {
					if ( defaultType != null ) {
						tmp = getObjectFromCache ( defaultType, arr.get ( i ).isString ().stringValue () );
						if ( tmp != null )
							ret.add ( tmp );
					}

					continue;
				}

				test = getObjectFromCache ( obj.get ( "type" ).isString ().stringValue (), obj.get ( "id" ).isString ().stringValue () );
				tmp = FromServer.instance ( obj );

				if ( test != null ) {
					test.transferRelatedInfo ( tmp );
					triggerObjectModification ( tmp );
				}
				else {
					if ( first_round == true ) {
						mod = tmp;
						triggerObjectBlockCreation ( tmp, true );
						first_round = false;
					}

					triggerObjectCreation ( tmp );
				}

				/*
					Le funzioni di trigger provvedono gia' ad
					immettere il nuovo oggetto (o aggiornare
					l'oggetto esistente) nelle cache
				*/

				ret.add ( tmp );
			}

			if ( first_round == false )
				triggerObjectBlockCreation ( mod, false );
		}

		closeRecursionStack ();
		return ret;
	}

	public void responseToObjects ( JSONValue response ) {
		responseToObjects ( response, null );
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

	public void removeObjectEvent ( String type, String identifier ) {
		ServerMonitor monitor;
		ServerObjectReceive callback;

		monitor = getMonitor ( type );

		for ( int i = 0; i < monitor.callbacks.size (); i++ ) {
			callback = monitor.callbacks.get ( i );

			if ( callback.handleId () == identifier ) {
				monitor.callbacks.remove ( callback );
				break;
			}
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

		if ( FromServerFactory.classExists ( type ) == false )
			return;

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
		if ( params.getUseCache () == true )
			loadWithCachedObjects ( params.getType (), params );

		if ( executingMonitor >= MAXIMUM_CONCURRENT_REQUESTS ) {
			monitorSchedulingQueue.add ( params );
		}
		else {
			testObjectReceiveImpl ( params );
		}
	}

	private void testIterationBehaviour ( FromServer child ) {
		FromServer test;

		test = getObjectFromCache ( child.getType (), child.getLocalID () );
		if ( test != null )
			triggerObjectModification ( child );
		else
			triggerObjectCreation ( child );
	}

	private void iterateObjectNotification ( FromServer object ) {
		int type;
		ArrayList<String> subnames;
		FromServer child;
		ArrayList<FromServer> children;

		subnames = object.getContainedObjectsName ();
		for ( String attribute : subnames ) {
			type = object.getAttributeType ( attribute );

			if ( type == FromServer.OBJECT ) {
				child = object.getObject ( attribute );
				if ( child != null && child.isValid () )
					testIterationBehaviour ( child );
			}
			else if ( type == FromServer.ARRAY ) {
				children = object.getArray ( attribute );
				if ( children != null )
					for ( FromServer iter : children )
						testIterationBehaviour ( iter );
			}
		}
	}

	public void triggerObjectCreation ( FromServer object ) {
		ServerMonitor tmp;

		if ( testRecursionStack ( object ) == false )
			return;
		addToRecursionStack ( object );

		tmp = getMonitor ( object.getType () );
		if ( addObjectIntoMonitorCache ( tmp, object ) == true )
			executeReceivingCallbacks ( tmp, object );

		iterateObjectNotification ( object );
	}

	/*
		Questa esegue sempre le callback di ricezione, senza
		controllare se l'oggetto e' gia' in cache o meno. Usare con
		cautela
	*/
	public void triggerObjectCreated ( FromServer object ) {
		ServerMonitor tmp;

		tmp = getMonitor ( object.getType () );
		executeReceivingCallbacks ( tmp, object );
	}

	public void triggerObjectBlockCreation ( FromServer object, boolean mode ) {
		int num;
		ServerMonitor tmp;
		ServerObjectReceive callback;

		if ( object == null )
			return;

		tmp = getMonitor ( object.getType () );
		num = tmp.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = tmp.callbacks.get ( i );

			if ( mode == true )
				callback.onBlockBegin ();
			else
				callback.onBlockEnd ();
		}
	}

	public void triggerObjectModification ( FromServer object, boolean force_propagation ) {
		int num;
		ServerMonitor tmp;
		ServerObjectReceive callback;

		if ( testRecursionStack ( object ) == false )
			return;
		addToRecursionStack ( object );

		tmp = getMonitor ( object.getType () );
		updateObjectInMonitorCache ( object );
		num = tmp.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = tmp.callbacks.get ( i );
			callback.onModify ( object );
		}

		if ( object.alwaysReload () || force_propagation )
			iterateObjectNotification ( object );

		object.executeLocalObjectEvent ( FromServerResponse.ACTION_MODIFY );
	}

	public void triggerObjectModification ( FromServer object ) {
		triggerObjectModification ( object, false );
	}

	public void triggerObjectDeletion ( FromServer object ) {
		int num;
		ServerMonitor tmp;
		ServerObjectReceive callback;

		tmp = getMonitor ( object.getType () );
		deleteObjectFromMonitorCache ( tmp, object );

		num = tmp.callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			callback = tmp.callbacks.get ( i );
			callback.onDestroy ( object );
		}

		object.executeLocalObjectEvent ( FromServerResponse.ACTION_DELETE );
	}

	public void forceObjectReload ( String type, int id ) {
		ObjectRequest params;

		params = new ObjectRequest ( type );
		params.add ( "id", id );
		params.setUseCache ( false );
		testObjectReceive ( params );
	}

	public void forceObjectReload ( FromServer obj ) {
		forceObjectReload ( obj.getType (), obj.getLocalID () );
	}

	/****************************************************************** recursion stack */

	public void initRecursionStack () {
		recursionStack = new HashMap<String, ArrayList<FromServer>> ();
	}

	/*
		Ritorna
		- false se l'oggetto e' nello stack di ricorsione
		- true se non c'e'
	*/
	public boolean testRecursionStack ( FromServer object ) {
		ArrayList<FromServer> elements;

		/*
			Un OrderUserAggregate viene creato leggendo gli ordini arrivati in blocco, dunque viene messo
			in stack di ricorsione subito. Questo ovviamente impedisce di intervenire nuovamente sullo
			stesso elemento, cosa invece voluta.
			In generale, e' difficile che un FromServerAggregateVirtual sia coinvolto in una ricorsione
			ciclica non essendo mai contenuto in altri elementi
		*/
		if ( object instanceof FromServerAggregateVirtual )
			return true;

		if ( recursionStack != null ) {
			elements = recursionStack.get ( object.getType () );
			if ( elements == null )
				return true;

			for ( FromServer a : elements ) {
				if ( a.getLocalID () == object.getLocalID () )
					return false;
			}
		}

		return true;
	}

	public void addToRecursionStack ( FromServer object ) {
		String type;
		ArrayList<FromServer> elements;

		if ( recursionStack != null ) {
			type = object.getType ();

			elements = recursionStack.get ( type );
			if ( elements == null ) {
				elements = new ArrayList<FromServer> ();
				recursionStack.put ( type, elements );
			}

			elements.add ( object );
		}
	}

	public void closeRecursionStack () {
		recursionStack = null;
	}

	/****************************************************************** cache */

	public boolean addToCache ( FromServer object ) {
		ServerMonitor monitor;

		monitor = getMonitor ( object.getType () );
		return addObjectIntoMonitorCache ( monitor, object );
	}

	/*
		Attenzione: questa funzione funge solo per reperire gli oggetti nella cache
		locale, e non sincronizza la cache con il server: usare solo per accedere dati
		che si e' sicuri essere gia' stati scaricati da un monitor
	*/
	public FromServer getObjectFromCache ( String type, int id ) {
		return getObjectFromCache ( type, Integer.toString ( id ) );
	}

	public FromServer getObjectFromCache ( String type, String id ) {
		FromServer ret;
		ServerMonitor monitor;

		monitor = getMonitor ( type );
		ret = monitor.objects.get ( id );
		return ret;
	}

	public ArrayList<FromServer> getObjectsFromCache ( String type ) {
		ServerMonitor monitor;

		monitor = getMonitor ( type );
		return new ArrayList<FromServer> ( monitor.objects.values () );
	}

	public ArrayList<FromServer> getObjectsFromCache ( String type, int [] ids ) {
		ArrayList<FromServer> ret;
		ServerMonitor monitor;

		monitor = getMonitor ( type );
		ret = new ArrayList<FromServer> ();

		for ( int i = 0; i < ids.length; i++ )
			ret.add ( monitor.objects.get ( ids [ i ] ) );

		return ret;
	}

	public void invalidateCacheByCondition ( ObjectRequest req ) {
		int len;
		ArrayList<FromServer> objects;
		FromServer obj;

		objects = getObjectsFromCache ( req.getType () );
		len = objects.size ();

		for ( int i = 0; i < len; i++ ) {
			obj = objects.get ( i );
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
		else {
			loadingDialog.hide ();
		}
	}

	public void engageLoadingBar () {
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
