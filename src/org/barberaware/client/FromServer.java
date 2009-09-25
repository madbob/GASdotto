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

import java.lang.*;
import java.util.*;
import com.google.gwt.core.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;

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

	private class FromServerAttribute {
		public String			name;
		public int			type;
		public Class			object_type	= null;

		private StringFromObjectClosure	fakeString	= null;
		private String			string		= "";
		private int			integer		= 0;
		private float			floating	= 0;
		private Date			date		= null;
		private boolean			bool		= false;
		private Address			addr		= null;

		/*
			Questi due sono sempre condizionati da object_type
		*/
		private ArrayList		array		= null;
		private FromServer		object		= null;

		private void buildCommon ( String name, int type ) {
			this.name = name;
			this.type = type;
		}

		public FromServerAttribute ( String name, int type, Class reference ) {
			buildCommon ( name, type );
			this.object_type = reference;
		}

		public FromServerAttribute ( String name, int type, StringFromObjectClosure reference ) {
			buildCommon ( name, type );
			this.fakeString = reference;
		}

		public void setString ( String value ) {
			string = value;
		}

		public void setInt ( int value ) {
			integer = value;
		}

		public void setFloat ( float value ) {
			floating = value;
		}

		public void setArray ( ArrayList value ) {
			array = value;
		}

		public void setObject ( FromServer value ) {
			object = value;
		}

		public void setDate ( Date value ) {
			date = value;
		}

		public void setBool ( boolean value ) {
			bool = value;
		}

		public void setAddress ( Address value ) {
			addr = value;
		}

		public void setValue ( FromServerAttribute cpy ) {
			if ( type == FromServer.STRING || type == FromServer.LONGSTRING || type == FromServer.PERCENTAGE )
				string = cpy.string;

			else if ( type == FromServer.INTEGER )
				integer = cpy.integer;

			else if ( type == FromServer.FLOAT )
				floating = cpy.floating;

			else if ( type == FromServer.ARRAY )
				array = Utils.dupliacateFromServerArray ( cpy.array );

			else if ( type == FromServer.OBJECT )
				object = cpy.object.duplicate ();

			else if ( type == FromServer.DATE )
				date = cpy.date;

			else if ( type == FromServer.BOOLEAN )
				bool = cpy.bool;

			else if ( type == FromServer.ADDRESS )
				addr = ( Address ) cpy.addr.clone ();
		}

		public String getString ( FromServer obj ) {
			if ( fakeString != null )
				return fakeString.retrive ( obj );
			else
				return string;
		}

		public int getInt () {
			return integer;
		}

		public float getFloat () {
			return floating;
		}

		public ArrayList getArray () {
			return array;
		}

		public FromServer getObject () {
			return object;
		}

		public Date getDate () {
			return date;
		}

		public boolean getBool () {
			return bool;
		}

		public Address getAddress () {
			return addr;
		}

		public String getClassName () {
			return Utils.classFinalName ( object_type.getName () );
		}

		public JSONValue getJSON () {
			if ( type == FromServer.STRING || type == FromServer.LONGSTRING || type == FromServer.PERCENTAGE ) {
				if ( string != null && string != "" )
					return new JSONString ( string );
				else
					return null;
			}

			else if ( type == FromServer.INTEGER )
				return new JSONString ( Integer.toString ( integer ) );

			else if ( type == FromServer.FLOAT )
				return new JSONString ( Float.toString ( floating ) );

			else if ( type == FromServer.ARRAY ) {
				JSONArray arr;
				FromServer tmp;

				arr = new JSONArray ();

				if ( array != null ) {
					for ( int i = 0; i < array.size (); i++ ) {
						tmp = ( FromServer ) array.get ( i );
						arr.set ( i, tmp.toJSONObject () );
					}
				}

				return arr;
			}

			else if ( type == FromServer.OBJECT ) {
				JSONValue ret;

				if ( object == null )
					object = FromServerFactory.create ( object_type.getName () );

				/**
					TODO	Ottimizzazione: qui si potrebbe usare solo l'ID
						dell'oggetto anziche' il blocco completo, per
						risparmiare sia il tempo di conversione in JSON
						che di trasmissione del dato. Devo pero' essere
						sicuro che l'oggetto stesso non sia variato nel
						tempo: eventualmente si puo' agganciare un
						qualche flag nelle funzioni setNNN()
				*/

				ret = object.toJSONObject ();
				return ret;
			}

			else if ( type == FromServer.DATE ) {
				if ( date != null )
					return new JSONString ( Utils.encodeDate ( date ) );
				else
					return new JSONString ( "" );
			}

			else if ( type == FromServer.BOOLEAN )
				return new JSONString ( Boolean.toString ( bool ) );

			else if ( type == FromServer.ADDRESS ) {
				if ( addr == null )
					addr = new Address ();

				return addr.toJSON ();
			}

			else
				return null;
		}
	}

	private int		localID;
	private String		type;

	/**
		TODO	Sostituire l'ArrayList con una HashMap
	*/
	private ArrayList	attributes;

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
		attributes = new ArrayList ();
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
		attributes.add ( new FromServerAttribute ( name, type, object ) );
	}

	protected void addFakeAttribute ( String name, int type, StringFromObjectClosure value ) {
		attributes.add ( new FromServerAttribute ( name, type, value ) );
	}

	private FromServerAttribute getInternalAttribute ( String name ) {
		int size;
		FromServerAttribute attr;

		size = attributes.size ();

		for ( int i = 0; i < size; i++ ) {
			attr = ( FromServerAttribute ) attributes.get ( i );

			if ( attr.name.equals ( name ) )
				return attr;
		}

		return null;
	}

	/*
		Questo crea una esatta copia dell'oggetto. Da usare con estrema cautela, in
		quanto se i valori di una copia vengono modificati e l'oggetto viene spedito al
		server quelli vengono prese per le informazioni buone e la cache e' invalida
	*/
	public FromServer duplicate () {
		FromServer obj;
		FromServerAttribute my_attr;
		FromServerAttribute cpy_attr;

		obj = FromServerFactory.create ( type );
		obj.localID = localID;

		for ( int i = 0; i < attributes.size (); i++ ) {
			my_attr = ( FromServerAttribute ) attributes.get ( i );
			cpy_attr = ( FromServerAttribute ) obj.attributes.get ( i );
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
		return attr.type;
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
		return getInternalAttribute ( name ).getArray ();
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

	public JSONObject toJSONObject () {
		int attrs_num;
		FromServerAttribute attr;
		JSONObject obj;
		JSONValue value;

		obj = new JSONObject ();
		obj.put ( "id", new JSONString ( Integer.toString ( localID ) ) );
		obj.put ( "type", new JSONString ( type ) );

		attrs_num = attributes.size ();

		for ( int i = 0; i < attrs_num; i++ ) {
			attr = ( FromServerAttribute ) attributes.get ( i );

			value = attr.getJSON ();
			if ( value != null )
				obj.put ( attr.name, value );
		}

		return obj;
	}

	public void fromJSONObject ( JSONObject obj ) {
		int attrs_num;
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

		attrs_num = attributes.size ();

		for ( int i = 0; i < attrs_num; i++ ) {
			attr = ( FromServerAttribute ) attributes.get ( i );

			value = obj.get ( attr.name );

			if ( value == null ) {
				if ( attr.type == FromServer.INTEGER )
					attr.setInt ( 0 );

				else if ( attr.type == FromServer.FLOAT )
					attr.setFloat ( 0 );

				else if ( attr.type == FromServer.ARRAY )
					attr.setArray ( new ArrayList () );

				else if ( attr.type == FromServer.OBJECT )
					attr.setObject ( FromServerFactory.create ( attr.object_type.getName () ) );

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

				else if ( attr.type == FromServer.FLOAT )
					attr.setFloat ( Float.parseFloat ( value.isString ().stringValue () ) );

				else if ( attr.type == FromServer.ARRAY ) {
					ArrayList arr;
					JSONArray array;

					arr = new ArrayList ();
					array = value.isArray ();

					for ( int a = 0; a < array.size (); a++ ) {
						tmp = FromServerFactory.create ( attr.object_type.getName () );
						tmp.fromJSONObject ( array.get ( a ).isObject () );
						arr.add ( tmp );
					}

					attr.setArray ( arr );
				}

				else if ( attr.type == FromServer.OBJECT ) {
					tmp = FromServerFactory.create ( attr.object_type.getName () );
					tmp.fromJSONObject ( value.isObject () );
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
		return getLocalID () == other.getLocalID ();
	}
}
