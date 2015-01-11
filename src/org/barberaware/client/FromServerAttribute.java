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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class FromServerAttribute {
	public String			name;
	public int			type;
	public Class			objectType	= null;

	private ValueFromObjectClosure	fakeClosure	= null;
	private String			string		= "";
	private int			integer		= 0;
	private float			floating	= 0;
	private Date			date		= null;
	private boolean			bool		= false;
	private Address			addr		= null;

	/*
		Questi due sono sempre condizionati da objectType
	*/
	private ArrayList<FromServer>	array		= null;
	private int			objectId	= -1;
	private FromServer		realObject	= null;

	private boolean			alwaysSend	= false;

	private void buildCommon ( String name, int type ) {
		this.name = name;
		this.type = type;
	}

	public FromServerAttribute ( String name, int type, Class reference ) {
		buildCommon ( name, type );
		this.objectType = reference;
	}

	public FromServerAttribute ( String name, int type, ValueFromObjectClosure closure ) {
		buildCommon ( name, type );
		this.fakeClosure = closure;
	}

	public FromServerAttribute ( String name, int type, Class reference, ValueFromObjectClosure closure ) {
		buildCommon ( name, type );
		this.objectType = reference;
		this.fakeClosure = closure;
	}

	public boolean isFake () {
		return ( fakeClosure != null );
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

	public void setArray ( ArrayList<FromServer> value ) {
		array = value;
	}

	public void setObject ( FromServer value ) {
		if ( value != null ) {
			/*
				Modificare questo, per tenere sempre una copia
				locale dell'oggetto, implica spaccare ogni cosa:
				gli oggetti verrebbero molto rapidamente
				duplicati in giro e salterebbero tutte le
				referenze
			*/
			if ( value.getLocalID () == -1 ) {
				realObject = value;
				objectId = -1;
			}
			else {
				realObject = null;
				objectId = value.getLocalID ();
			}
		}
		else {
			realObject = null;
			objectId = -1;
		}
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

	public void setDefault () {
		/*
			Negli altri casi, il default viene gia' settato dal costruttore
		*/
		if ( type == FromServer.ARRAY )
			setArray ( new ArrayList<FromServer> () );
		else if ( type == FromServer.OBJECT )
			setObject ( FromServerFactory.create ( attr.objectType.getName () ) );
		else if ( type == FromServer.ADDRESS )
			setAddress ( new Address () );
	}

	public void setValue ( FromServerAttribute cpy ) {
		if ( type == FromServer.STRING || type == FromServer.LONGSTRING || type == FromServer.PERCENTAGE ) {
			string = cpy.string;
		}
		else if ( type == FromServer.INTEGER ) {
			integer = cpy.integer;
		}
		else if ( type == FromServer.FLOAT || type == FromServer.PRICE ) {
			floating = cpy.floating;
		}
		else if ( type == FromServer.ARRAY ) {
			array = Utils.duplicateFromServerArray ( cpy.array );
		}
		else if ( type == FromServer.OBJECT ) {
			realObject = cpy.realObject;
			objectId = cpy.objectId;
		}
		else if ( type == FromServer.DATE ) {
			date = cpy.date;
		}
		else if ( type == FromServer.BOOLEAN ) {
			bool = cpy.bool;
		}
		else if ( type == FromServer.ADDRESS ) {
			if ( cpy.addr != null )
				addr = ( Address ) cpy.addr.clone ();
		}
	}

	public String getString ( FromServer obj ) {
		if ( fakeClosure != null )
			return fakeClosure.retriveString ( obj );
		else
			return string;
	}

	public int getInt ( FromServer obj ) {
		if ( fakeClosure != null )
			return fakeClosure.retriveInteger ( obj );
		else
			return integer;
	}

	public float getFloat () {
		return floating;
	}

	public ArrayList<FromServer> getArray ( FromServer obj ) {
		ArrayList<FromServer> ret;

		ret = null;

		if ( fakeClosure != null )
			ret = fakeClosure.retriveArray ( obj );

		if ( ret == null )
			return array;
		else
			return ret;
	}

	public FromServer getObject ( FromServer obj ) {
		FromServer ret;

		ret = null;

		if ( fakeClosure != null )
			ret = fakeClosure.retriveObject ( obj );

		if ( ret == null ) {
			if ( realObject != null )
				ret = realObject;
			else if ( objectId != -1 )
				ret = Utils.getServer ().getObjectFromCache ( getClassName (), objectId );
		}

		return ret;
	}

	public Date getDate ( FromServer obj ) {
		if ( fakeClosure != null )
			return fakeClosure.retriveDate ( obj );
		else
			return date;
	}

	public boolean getBool () {
		return bool;
	}

	public Address getAddress () {
		return addr;
	}

	public String getClassName () {
		return Utils.classFinalName ( objectType.getName () );
	}

	public JSONValue getJSON () {
		if ( fakeClosure != null )
			return null;

		if ( type == FromServer.STRING || type == FromServer.LONGSTRING || type == FromServer.PERCENTAGE ) {
			if ( string != null )
				return new JSONString ( string );
			else
				return null;
		}

		else if ( type == FromServer.INTEGER )
			return new JSONString ( Integer.toString ( integer ) );

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE )
			return new JSONString ( Float.toString ( floating ) );

		else if ( type == FromServer.ARRAY ) {
			JSONArray arr;
			JSONValue o;
			FromServer tmp;

			arr = new JSONArray ();

			if ( array != null ) {
				for ( int i = 0; i < array.size (); i++ ) {
					tmp = ( FromServer ) array.get ( i );

					/*
						Attenzione, ci sono casi in cui questo puo' andare in ricorsione
						infinita.

						e.g. il Supplier contiene un elenco di User (i referenti), i quali
						contengono una lista di Supplier (per le notifiche) tra cui il
						Supplier stesso
					*/
					if ( tmp.getLocalID () == -1 || alwaysSend == true )
						o = tmp.toJSONObject ();
					else
						o = new JSONString ( Integer.toString ( tmp.getLocalID () ) );

					arr.set ( i, o );
				}
			}

			return arr;
		}

		else if ( type == FromServer.OBJECT ) {
			JSONValue ret;
			FromServer real_object;

			real_object = this.getObject ( null );

			if ( real_object == null )
				ret = null;
			else if ( real_object.getLocalID () == -1 || alwaysSend == true )
				ret = real_object.toJSONObject ();
			else
				ret = new JSONString ( Integer.toString ( real_object.getLocalID () ) );

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

	public void setAlwaysSend ( boolean send ) {
		alwaysSend = send;
	}
}
