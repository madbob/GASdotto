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

import java.lang.*;
import java.util.*;
import com.google.gwt.core.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class FromServer implements Comparator {
	public static int			STRING		= 0;
	public static int			INTEGER		= 1;
	public static int			FLOAT		= 2;
	public static int			ARRAY		= 3;
	public static int			OBJECT		= 4;
	public static int			DATE		= 5;
	public static int			BOOLEAN		= 6;
	public static int			LONGSTRING	= 7;
	public static int			PERCENTAGE	= 8;
	public static int			ADDRESS		= 9;
	public static int			PRICE		= 10;

	private int					localID;
	private String					type;
	private HashMap<String, FromServerAttribute>	attributes;
	private HashMap<String, Object>			relatedInfo;

	private boolean				sharable;
	private int				sharingPrivileges;

	/*
		Internamente questo attributo, settato in fase di costruzione di una delle classi
		che implementano questa, serve a poco, ma permette di forzare il ricaricamento
		dei dati dal server e di non fidarsi della cache. Cio' vale per i casi in cui il
		server apporta delle trasformazioni sui dati salvati, e dunque l'oggetto che si
		ha gia' in locale potrebbe non avere valori completamente validi
	*/
	private boolean				forceReloadFromServer;

	/*
		Flag speciale, settato a true quando si esegue il salvataggio e a false quando
		viene settato l'ID dell'oggetto. Viene valutato per sapere se l'oggetto e' valido
		(isValid()): se savingOperation == true viene dato per buono anche se con
		ID == -1, in quanto e' in fase di trasferimento sul server e si assume dunque che
		un ID gli venga assegnato nel giro di poco
	*/
	private boolean				savingOperation;

	/*
		Array di eventuali callbacks locali, che vengono invocate sulla modifica e
		rimozione del singolo oggetto
	*/
	private ArrayList<ServerObjectReceive>	localCallbacks;

	private boolean				forceModified;

	/****************************************************************** init */

	public FromServer () {
		localID = -1;
		attributes = new HashMap<String, FromServerAttribute> ();
		type = Utils.classFinalName ( this.getClass ().getName () );
		sharable = false;
		sharingPrivileges = ACL.ACL_OWNER;
		forceReloadFromServer = false;
		savingOperation = false;
		localCallbacks = null;
		forceModified = false;

		addAttribute ( "unique_identifier", FromServer.STRING );
		setString ( "unique_identifier", Utils.randomString () );
	}

	public int getLocalID () {
		return localID;
	}

	/*
		E' sconsigliato usare questa funzione direttamente al di fuori delle classi
		piu' interne (tra cui FromServerResponse)
	*/
	public void setLocalID ( int lid ) {
		localID = lid;
		savingOperation = false;
	}

	public String getType () {
		return type;
	}

	public boolean alwaysReload () {
		return forceReloadFromServer;
	}

	protected void alwaysReload ( boolean reload ) {
		forceReloadFromServer = reload;
	}

	public boolean isSharable () {
		return sharable;
	}

	protected void isSharable ( boolean share ) {
		sharable = share;
	}

	public int sharingStatus () {
		return sharingPrivileges;
	}

	protected void addAttribute ( String name, int type ) {
		addAttribute ( name, type, null );
	}

	protected void addAttribute ( String name, int type, Class object ) {
		attributes.put ( name, new FromServerAttribute ( name, type, object ) );
	}

	protected void addFakeAttribute ( String name, int type, ValueFromObjectClosure value ) {
		attributes.put ( name, new FromServerAttribute ( name, type, value ) );
	}

	protected void addFakeAttribute ( String name, int type, Class object, ValueFromObjectClosure value ) {
		attributes.put ( name, new FromServerAttribute ( name, type, object, value ) );
	}

	private FromServerAttribute getInternalAttribute ( String name ) {
		FromServerAttribute ret;

		if ( this == null )
			Log.debug ( "Oggetto nullo! - getInternalAttribute (" + name + ")" );

		ret = ( FromServerAttribute ) attributes.get ( name );
		if ( ret == null ) {
			Utils.showNotification ( "Errore interno: impossibile reperire parametro '" + name + "' in oggetto '" + getType () + "'" );
			Log.debug ( "Errore interno: impossibile reperire parametro '" + name + "' in oggetto '" + getType () + "'" );
		}

		return ret;
	}

	/*
		Questo e' per forzare l'invio completo di un sotto-oggetto, anziche' solo il suo
		ID, ogni volta che si salva. Da usare nei casi in cui tale sotto-oggetto puo'
		essere modificato editando quello che lo contiene
	*/
	protected void alwaysSendObject ( String name, boolean send ) {
		FromServerAttribute attr;

		attr = ( FromServerAttribute ) attributes.get ( name );
		attr.setAlwaysSend ( send );
	}

	/*
		Questo crea una esatta copia dell'oggetto. Da usare con estrema cautela, in
		quanto se i valori di una copia vengono modificati e l'oggetto viene spedito al
		server quelli vengono prese per le informazioni buone e la cache e' invalida
	*/
	public FromServer duplicate () {
		String k;
		Object [] keys;
		FromServer obj;
		FromServerAttribute my_attr;
		FromServerAttribute cpy_attr;

		if ( this == null )
			Log.debug ( "Oggetto nullo!" );

		obj = FromServerFactory.create ( type );
		obj.localID = localID;
		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			my_attr = ( FromServerAttribute ) attributes.get ( k );
			cpy_attr = ( FromServerAttribute ) obj.attributes.get ( k );
			cpy_attr.setValue ( my_attr );
		}

		obj.setString ( "unique_identifier", Utils.randomString () );
		return obj;
	}

	public void forceMod ( boolean mod ) {
		forceModified = mod;
	}

	public boolean isMod () {
		return forceModified;
	}

	/*
		A futura memoria: avevo pensato di aggiungere una funzione mayBeNull() che di
		default tornasse true e potesse essere reimplementata dai singoli oggetti, ma
		questo genere di informazione non puo' essere gestito globalmente ed
		universalmente ma nel singolo contesto. Dunque, i singoli widget che
		rappresentano dei FromServer permettono di avere o non avere l'opzione che
		indichi "null"
	*/

	/****************************************************************** set */

	public void setString ( String name, String value ) {
		getInternalAttribute ( name ).setString ( value );
	}

	public void setInt ( String name, int value ) {
		getInternalAttribute ( name ).setInt ( value );
	}

	public void setFloat ( String name, float value ) {
		getInternalAttribute ( name ).setFloat ( value );
	}

	public void setArray ( String name, ArrayList value ) {
		getInternalAttribute ( name ).setArray ( value );
	}

	public void setObject ( String name, FromServer value ) {
		getInternalAttribute ( name ).setObject ( value );
	}

	public void setDate ( String name, Date value ) {
		getInternalAttribute ( name ).setDate ( value );
	}

	public void setBool ( String name, boolean value ) {
		getInternalAttribute ( name ).setBool ( value );
	}

	public void setAddress ( String name, Address value ) {
		getInternalAttribute ( name ).setAddress ( value );
	}

	public void setAttributeValue ( String name, String value ) {
		setString ( name, value );
	}

	public void setAttributeValue ( String name, int value ) {
		setInt ( name, value );
	}

	public void setAttributeValue ( String name, Integer value ) {
		setInt ( name, value.intValue () );
	}

	public void setAttributeValue ( String name, float value ) {
		setFloat ( name, value );
	}

	public void setAttributeValue ( String name, Float value ) {
		setFloat ( name, value.floatValue () );
	}

	public void setAttributeValue ( String name, ArrayList value ) {
		setArray ( name, value );
	}

	public void setAttributeValue ( String name, FromServer value ) {
		setObject ( name, value );
	}

	public void setAttributeValue ( String name, Date value ) {
		setDate ( name, value );
	}

	public void setAttributeValue ( String name, boolean value ) {
		setBool ( name, value );
	}

	public void setAttributeValue ( String name, Boolean value ) {
		setBool ( name, value.booleanValue () );
	}

	public void setAttributeValue ( String name, Address value ) {
		setAddress ( name, value );
	}

	public void setAttributeValue ( String name, Object value ) {
		if ( value instanceof String )
			setString ( name, ( String ) value );
		else if ( value instanceof Integer )
			setInt ( name, ( ( Integer ) value ).intValue () );
		else if ( value instanceof Float )
			setFloat ( name, ( ( Float ) value ).floatValue () );
		else if ( value instanceof ArrayList )
			setArray ( name, ( ArrayList ) value );
		else if ( value instanceof FromServer )
			setObject ( name, ( FromServer ) value );
		else if ( value instanceof Date )
			setDate ( name, ( Date ) value );
		else if ( value instanceof Boolean )
			setBool ( name, ( ( Boolean ) value ).booleanValue () );
		else if ( value instanceof Address )
			setAddress ( name, ( Address ) value );
	}

	/****************************************************************** get */

	public int getAttributeType ( String type ) {
		FromServerAttribute attr;

		attr = getInternalAttribute ( type );
		if ( attr != null )
			return attr.type;
		else
			return -1;
	}

	public boolean isAttributeFake ( String type ) {
		FromServerAttribute attr;

		attr = getInternalAttribute ( type );
		if ( attr != null )
			return attr.isFake ();
		else
			return false;
	}

	public boolean hasAttribute ( String type ) {
		return ( attributes.get ( type ) != null );
	}

	public String getString ( String name ) {
		return getInternalAttribute ( name ).getString ( this );
	}

	public int getInt ( String name ) {
		return getInternalAttribute ( name ).getInt ( this );
	}

	public float getFloat ( String name ) {
		return getInternalAttribute ( name ).getFloat ();
	}

	public ArrayList getArray ( String name ) {
		return getInternalAttribute ( name ).getArray ( this );
	}

	public FromServer getObject ( String name ) {
		return getInternalAttribute ( name ).getObject ( this );
	}

	public Date getDate ( String name ) {
		return getInternalAttribute ( name ).getDate ( this );
	}

	public boolean getBool ( String name ) {
		return getInternalAttribute ( name ).getBool ();
	}

	public Address getAddress ( String name ) {
		return getInternalAttribute ( name ).getAddress ();
	}

	public String getClassName ( String name ) {
		return getInternalAttribute ( name ).getClassName ();
	}

	/****************************************************************** utility sugli array */

	public void addToArray ( String array_name, FromServer to_add ) {
		ArrayList array;
		FromServer tmp;

		array = getArray ( array_name );

		for ( int a = 0; a < array.size (); a++ ) {
			tmp = ( FromServer ) array.get ( a );
			if ( tmp.equals ( to_add ) )
				return;
		}

		array.add ( to_add );
		setArray ( array_name, array );
	}

	public boolean removeFromArray ( String array_name, FromServer to_remove ) {
		ArrayList array;
		FromServer tmp;

		array = getArray ( array_name );

		for ( int a = 0; a < array.size (); a++ ) {
			tmp = ( FromServer ) array.get ( a );
			if ( tmp.equals ( to_remove ) ) {
				array.remove ( tmp );
				setArray ( array_name, array );
				return true;
			}
		}

		return false;
	}

	/****************************************************************** server interface */

	public boolean isValid () {
		return ( localID != -1 || savingOperation == true );
	}

	public static FromServer instance ( JSONObject obj ) {
		FromServer tmp;

		tmp = FromServerFactory.create ( obj.get ( "type" ).isString ().stringValue () );
		tmp.fromJSONObject ( obj );
		return tmp;
	}

	public void save ( ServerResponse callback ) {
		int type;
		JSONObject obj;
		FromServerResponse true_callback;

		obj = this.toJSONObject ();
		savingOperation = true;

		type = ( ( localID == -1 ) ? FromServerResponse.ACTION_CREATE : FromServerResponse.ACTION_MODIFY );
		true_callback = new FromServerResponse ( type, this, callback );

		if ( obj != null ) {
			RequestBuilder builder;

			builder = new RequestBuilder ( RequestBuilder.POST,
							Utils.getServer ().getURL () + "server.php?action=save" );

			try {
				builder.sendRequest ( obj.toString (), true_callback );
				builder.setTimeoutMillis ( 15000 );
				Utils.getServer ().engageLoadingBar ();
			}
			catch ( RequestException e ) {
				Utils.showNotification ( "Impossibile salvare oggetto" );
			}
		}
	}

	public void destroy ( ServerResponse callback ) {
		JSONObject obj;
		FromServerResponse true_callback;

		if ( isValid () == false && getType () != "ACL" )
			return;

		obj = this.toJSONObject ();
		true_callback = new FromServerResponse ( FromServerResponse.ACTION_DELETE, this, callback );

		if ( obj != null ) {
			RequestBuilder builder;

			builder = new RequestBuilder ( RequestBuilder.POST,
							Utils.getServer ().getURL () + "server.php?action=destroy" );

			try {
				builder.sendRequest ( obj.toString (), true_callback );
				builder.setTimeoutMillis ( 15000 );
			}
			catch ( RequestException e ) {
				Utils.showNotification ( "Impossibile eliminare oggetto" );
			}
		}
	}

	public ArrayList getContainedObjectsClasses () {
		String k;
		Object [] keys;
		ArrayList<String> ret;
		FromServerAttribute attr;

		ret = new ArrayList<String> ();
		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			attr = ( FromServerAttribute ) attributes.get ( k );

			if ( ( attr.type == OBJECT || attr.type == ARRAY ) && attr.objectType != null )
				ret.add ( attr.getClassName () );
		}

		return ret;
	}

	public ArrayList<String> getContainedObjectsName () {
		String k;
		Object [] keys;
		ArrayList<String> ret;
		FromServerAttribute attr;

		ret = new ArrayList<String> ();
		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			attr = ( FromServerAttribute ) attributes.get ( k );

			if ( ( attr.type == OBJECT || attr.type == ARRAY ) && attr.objectType != null )
				ret.add ( k );
		}

		return ret;
	}

	public JSONObject toJSONObject () {
		String k;
		Object [] keys;
		FromServerAttribute attr;
		JSONObject obj;
		JSONValue value;

		obj = new JSONObject ();
		obj.put ( "id", new JSONString ( Integer.toString ( localID ) ) );
		obj.put ( "type", new JSONString ( type ) );

		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];

			/*
				L'identificativo univoco ha senso solo all'interno
				dell'applicazione client, non lo trasmetto in giro
			*/
			if ( k == "unique_identifier" )
				continue;

			attr = ( FromServerAttribute ) attributes.get ( k );

			value = attr.getJSON ();
			if ( value != null )
				obj.put ( k, value );
		}

		forceMod ( false );
		return obj;
	}

	private FromServer JSONValueToObject ( FromServerAttribute attr, JSONValue value ) {
		JSONObject child;
		JSONString child_id;
		FromServer ret;
		ServerHook server;

		ret = null;
		server = Utils.getServer ();

		child = value.isObject ();

		if ( child != null ) {
			ret = FromServerFactory.create ( attr.objectType.getName () );
			ret.fromJSONObject ( child );
		}
		else {
			child_id = value.isString ();

			if ( child_id != null ) {
				ret = server.getObjectFromCache ( attr.getClassName (),
								Integer.parseInt ( child_id.stringValue () ) );
			}
		}

		return ret;
	}

	public void fromJSONObject ( JSONObject obj, boolean skip_cache ) {
		boolean do_callbacks;
		String k;
		Object [] keys;
		JSONValue value;
		FromServer tmp;
		FromServerAttribute attr;

		if ( obj == null )
			return;

		value = obj.get ( "id" );
		if ( value != null ) {
			String str;

			str = value.isString ().stringValue ();
			if ( str.length () != 0 )
				localID = Integer.parseInt ( str );
			else
				return;
		}

		do_callbacks = false;

		/*
			Metto un riferimento all'oggetto subito in cache, onde averlo a
			disposizione in caso di riferimenti ricorsivi convertendo sotto-oggetti
		*/
		if ( skip_cache == false )
			do_callbacks = Utils.getServer ().addToCache ( this );

		value = obj.get ( "sharing_privileges" );
		if ( value != null )
			sharingPrivileges = ( int ) value.isNumber ().doubleValue ();

		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];

			attr = ( FromServerAttribute ) attributes.get ( k );
			if ( attr.isFake () )
				continue;

			value = obj.get ( k );

			if ( value == null ) {
				if ( attr.type == FromServer.INTEGER )
					attr.setInt ( 0 );

				else if ( attr.type == FromServer.FLOAT || attr.type == FromServer.PRICE )
					attr.setFloat ( 0 );

				else if ( attr.type == FromServer.ARRAY )
					attr.setArray ( new ArrayList () );

				else if ( attr.type == FromServer.OBJECT )
					attr.setObject ( FromServerFactory.create ( attr.objectType.getName () ) );

				else if ( attr.type == FromServer.ADDRESS )
					attr.setAddress ( new Address () );
			}
			else {
				if ( attr.type == FromServer.STRING ||
						attr.type == FromServer.LONGSTRING ||
						attr.type == FromServer.PERCENTAGE )
					attr.setString ( value.isString ().stringValue () );

				else if ( attr.type == FromServer.INTEGER ) {
					String str;

					str = value.isString ().stringValue ();
					if ( str.length () != 0 )
						attr.setInt ( Integer.parseInt ( str ) );
					else
						attr.setInt ( 0 );
				}

				else if ( attr.type == FromServer.FLOAT || attr.type == FromServer.PRICE ) {
					attr.setFloat ( Float.parseFloat ( value.isString ().stringValue () ) );
				}

				else if ( attr.type == FromServer.ARRAY ) {
					ArrayList<FromServer> arr;
					JSONArray array;

					arr = new ArrayList<FromServer> ();
					array = value.isArray ();

					for ( int a = 0; a < array.size (); a++ ) {
						tmp = JSONValueToObject ( attr, array.get ( a ) );
						arr.add ( tmp );
					}

					attr.setArray ( arr );
				}

				else if ( attr.type == FromServer.OBJECT ) {
					tmp = JSONValueToObject ( attr, value );
					attr.setObject ( tmp );
				}

				else if ( attr.type == FromServer.DATE )
					attr.setDate ( Utils.decodeDate ( value.isString ().stringValue () ) );

				else if ( attr.type == FromServer.BOOLEAN )
					attr.setBool ( Boolean.valueOf ( value.isString ().stringValue () ) );

				else if ( attr.type == FromServer.ADDRESS ) {
					Address addr;
					addr = new Address ();
					addr.fromJSON ( value.isObject () );
					attr.setAddress ( addr );
				}
			}
		}

		if ( do_callbacks == true )
			Utils.getServer ().triggerObjectCreated ( this );
	}

	public void fromJSONObject ( JSONObject obj ) {
		fromJSONObject ( obj, false );
	}

	public void addLocalObjectEvent ( ServerObjectReceive callback ) {
		if ( localCallbacks == null )
			localCallbacks = new ArrayList<ServerObjectReceive> ();

		localCallbacks.add ( callback );
	}

	public void executeLocalObjectEvent ( int type ) {
		if ( localCallbacks == null )
			return;

		for ( ServerObjectReceive c : localCallbacks ) {
			/*
				FromServerResponse.ACTION_CREATE non viene volutamente gestito:
				difficile che ci siano gia' delle callback su un elemento appena
				creato / ricevuto
			*/
			if ( type == FromServerResponse.ACTION_MODIFY )
				c.onModify ( this );
			else if ( type == FromServerResponse.ACTION_DELETE )
				c.onDestroy ( this );
		}
	}

	public void addRelatedInfo ( String identifier, Object object ) {
		if ( relatedInfo == null )
			relatedInfo = new HashMap<String, Object> ();
		relatedInfo.put ( identifier, object );
	}

	public Object getRelatedInfo ( String identifier ) {
		if ( relatedInfo == null )
			return null;
		else
			return relatedInfo.get ( identifier );
	}

	public void delRelatedInfo ( String identifier ) {
		if ( relatedInfo != null )
			relatedInfo.remove ( identifier );
	}

	public void delRelatedInfo ( Object related ) {
		String k;
		Object [] keys;
		Object rel;

		if ( relatedInfo != null ) {
			keys = relatedInfo.keySet ().toArray ();

			for ( int i = 0; i < keys.length; i++ ) {
				k = ( String ) keys [ i ];
				rel = relatedInfo.get ( k );

				if ( rel == related ) {
					delRelatedInfo ( k );
					break;
				}
			}
		}
	}

	public void printRelatedInfo () {
		String k;
		Object [] keys;

		keys = relatedInfo.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			Log.debug ( k );
		}
	}

	public void transferRelatedInfo ( FromServer to ) {
		to.relatedInfo = this.relatedInfo;
		to.localCallbacks = this.localCallbacks;
	}

	/****************************************************************** Comparator */

	public int compare ( Object first, Object second ) {
		FromServer f;
		FromServer s;
		int fid;
		int sid;

		f = ( FromServer ) first;
		fid = f.getLocalID ();
		s = ( FromServer ) second;
		sid = s.getLocalID ();

		if ( fid < sid )
			return -1;
		else if ( fid > sid )
			return 1;

		return 0;
	}

	public boolean equals ( Object second ) {
		FromServer other;

		if ( this == null )
			Log.debug ( "Oggetto nullo!" );

		if ( second == null )
			return false;

		other = ( FromServer ) second;

		if ( getType () != other.getType () )
			return false;
		else
			return ( this.compare ( this, second ) == 0 );
	}
}
