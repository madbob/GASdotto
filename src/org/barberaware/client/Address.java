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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;

public class Address implements Comparator {
	private String		street		= "";
	private String		cap		= "";
	private String		city		= "";

	public Address () {
	}

	public void setStreet ( String s ) {
		street = s;
	}

	public String getStreet () {
		return street;
	}

	public void setCap ( String c ) {
		cap = c;
	}

	public String getCap () {
		return cap;
	}

	public void setCity ( String c ) {
		city = c;
	}

	public String getCity () {
		return city;
	}

	public JSONObject toJSON () {
		JSONObject obj;

		obj = new JSONObject ();
		obj.put ( "street", new JSONString ( street ) );
		obj.put ( "cap", new JSONString ( cap ) );
		obj.put ( "city", new JSONString ( city ) );
		return obj;
	}

	public void fromJSON ( JSONObject obj ) {
		JSONValue value;
		JSONString tmp;

		if ( obj == null )
			return;

		street = "";
		cap = "";
		city = "";

		value = obj.get ( "street" );
		if ( value != null ) {
			tmp = value.isString ();
			if ( tmp != null )
				street = tmp.stringValue ();
		}

		value = obj.get ( "cap" );
		if ( value != null ) {
			tmp = value.isString ();
			if ( tmp != null )
				cap = tmp.stringValue ();
		}

		value = obj.get ( "city" );
		if ( value != null ) {
			tmp = value.isString ();
			if ( tmp != null )
				city = tmp.stringValue ();
		}
	}

	/****************************************************************** Object */

	public Object clone () {
		Address dup;

		dup = new Address ();
		dup.street = street;
		dup.cap = cap;
		dup.city = city;
		return dup;
	}

	/****************************************************************** Comparator */

	public int compare ( Object first, Object second ) {
		Address f;
		Address s;
		int test;

		f = ( Address ) first;
		s = ( Address ) second;

		test = f.street.compareTo ( s.street );
		if ( test != 0 )
			return test;

		test = f.cap.compareTo ( s.cap );
		if ( test != 0 )
			return test;

		test = f.city.compareTo ( s.city );
		if ( test != 0 )
			return test;

		return 0;
	}

	public boolean equals ( Object second ) {
		Address other;

		if ( second == null )
			return false;

		other = ( Address ) second;
		return ( street.equals ( other.street ) && cap.equals ( other.cap ) && city.equals ( other.city ) );
	}
}
