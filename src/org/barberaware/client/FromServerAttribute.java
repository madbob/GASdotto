/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class FromServerAttribute {
	public String			name;
	public int			type;
	public Class			objectType	= null;

	private StringFromObjectClosure	fakeString	= null;
	private String			string		= "";
	private int			integer		= 0;
	private float			floating	= 0;
	private Date			date		= null;
	private boolean			bool		= false;
	private Address			addr		= null;

	/*
		Questi due sono sempre condizionati da objectType
	*/
	private ArrayList		array		= null;
	private int			objectId	= -1;

	private void buildCommon ( String name, int type ) {
		this.name = name;
		this.type = type;
	}

	public FromServerAttribute ( String name, int type, Class reference ) {
		buildCommon ( name, type );
		this.objectType = reference;
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
		objectId = value.getLocalID ();
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

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE )
			floating = cpy.floating;

		else if ( type == FromServer.ARRAY )
			array = Utils.dupliacateFromServerArray ( cpy.array );

		else if ( type == FromServer.OBJECT )
			objectId = cpy.objectId;

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
		return Utils.getServer ().getObjectFromCache ( getClassName (), objectId );
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
		return Utils.classFinalName ( objectType.getName () );
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

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE )
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
			FromServer real_object;

			if ( objectId == -1 )
				real_object = FromServerFactory.create ( objectType.getName () );
			else
				real_object = this.getObject ();

			if ( real_object.getLocalID () == -1 )
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
}
