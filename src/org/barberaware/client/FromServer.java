/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	public static int	STRING		= 0;
	public static int	INTEGER		= 1;
	public static int	FLOAT		= 2;
	public static int	ARRAY		= 3;
	public static int	OBJECT		= 4;
	public static int	DATE		= 5;
	public static int	BOOLEAN		= 6;
	public static int	LONGSTRING	= 7;
	public static int	FAKESTRING	= 8;
	public static int	PERCENTAGE	= 9;
	public static int	ADDRESS		= 10;
	public static int	PRICE		= 11;

	private int		localID;
	private String		type;
	private HashMap		attributes;
	private HashMap		relatedInfo;

	/*
		Internamente questo attributo, settato in fase di costruzione di una delle classi
		che implementano questa, serve a poco, ma permette di forzare il ricaricamento
		dei dati dal server e di non fidarsi della cache. Cio' vale per i casi in cui il
		server apporta delle trasformazioni sui dati salvati, e dunque l'oggetto che si
		ha gia' in locale potrebbe non avere valori completamente validi
	*/
	private boolean		forceReloadFromServer;

	/*
		Flag speciale, settato a true quando si esegue il salvataggio e a false quando
		viene settato l'ID dell'oggetto. Viene valutato per sapere se l'oggetto e' valido
		(isValid()): se savingOperation == true viene dato per buono anche se con
		ID == -1, in quanto e' in fase di trasferimento sul server e si assume dunque che
		un ID gli venga assegnato nel giro di poco
	*/
	private boolean		savingOperation;

	/****************************************************************** init */

	public FromServer () {
		localID = -1;
		attributes = new HashMap ();
		type = Utils.classFinalName ( this.getClass ().getName () );
		forceReloadFromServer = false;
		savingOperation = false;
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

	protected void addAttribute ( String name, int type ) {
		addAttribute ( name, type, null );
	}

	protected void addAttribute ( String name, int type, Class object ) {
		attributes.put ( name, new FromServerAttribute ( name, type, object ) );
	}

	protected void addFakeAttribute ( String name, int type, ValueFromObjectClosure value ) {
		attributes.put ( name, new FromServerAttribute ( name, type, value ) );
	}

	private FromServerAttribute getInternalAttribute ( String name ) {
		FromServerAttribute ret;

		ret = ( FromServerAttribute ) attributes.get ( name );
		if ( ret == null )
			Utils.showNotification ( "Errore interno: impossibile reperire parametro '" + name + "' in oggetto '" + getType () + "'" );

		return ret;
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

		obj = FromServerFactory.create ( type );
		obj.localID = localID;
		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			my_attr = ( FromServerAttribute ) attributes.get ( k );
			cpy_attr = ( FromServerAttribute ) obj.attributes.get ( k );
			cpy_attr.setValue ( my_attr );
		}

		return obj;
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
		getInternalAttribute ( name ).setString ( value );;
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

	/****************************************************************** get */

	public int getAttributeType ( String type ) {
		FromServerAttribute attr;

		attr = getInternalAttribute ( type );
		if ( attr != null )
			return attr.type;
		else
			return -1;
	}

	public String getString ( String name ) {
		return getInternalAttribute ( name ).getString ( this );
	}

	public int getInt ( String name ) {
		return getInternalAttribute ( name ).getInt ();
	}

	public float getFloat ( String name ) {
		return getInternalAttribute ( name ).getFloat ();
	}

	public ArrayList getArray ( String name ) {
		return getInternalAttribute ( name ).getArray ( this );
	}

	public FromServer getObject ( String name ) {
		return getInternalAttribute ( name ).getObject ();
	}

	public Date getDate ( String name ) {
		return getInternalAttribute ( name ).getDate ();
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

		if ( isValid () == false )
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
		ArrayList ret;
		FromServerAttribute attr;

		ret = new ArrayList ();
		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			attr = ( FromServerAttribute ) attributes.get ( k );

			if ( ( attr.type == OBJECT || attr.type == ARRAY ) && attr.objectType != null )
				ret.add ( attr.getClassName () );
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
			attr = ( FromServerAttribute ) attributes.get ( k );

			value = attr.getJSON ();
			if ( value != null )
				obj.put ( k, value );
		}

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

	public void fromJSONObject ( JSONObject obj ) {
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

		keys = attributes.keySet ().toArray ();

		for ( int i = 0; i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			attr = ( FromServerAttribute ) attributes.get ( k );
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

				else if ( attr.type == FromServer.FLOAT || attr.type == FromServer.PRICE )
					attr.setFloat ( Float.parseFloat ( value.isString ().stringValue () ) );

				else if ( attr.type == FromServer.ARRAY ) {
					ArrayList arr;
					JSONArray array;

					arr = new ArrayList ();
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

		Utils.getServer ().addToCache ( this );
	}

	public void addRelatedInfo ( String identifier, Object object ) {
		if ( relatedInfo == null )
			relatedInfo = new HashMap ();
		relatedInfo.put ( identifier, object );
	}

	public Object getRelatedInfo ( String identifier ) {
		if ( relatedInfo == null )
			return null;
		else
			return relatedInfo.get ( identifier );
	}

	public void delRelatedInfo ( String identifier ) {
		relatedInfo.remove ( identifier );
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

		if ( second == null )
			return false;

		other = ( FromServer ) second;
		// return ( ( getLocalID () == other.getLocalID () ) && ( getType () == other.getType () ) );
		return ( this.compare ( this, second ) == 0 );
	}
}
