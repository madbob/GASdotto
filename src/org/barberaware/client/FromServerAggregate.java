/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public abstract class FromServerAggregate extends FromServer {
	private String					indexAttribute;
	private HashMap<String, WritebackInOutClosure>	writeBacks;

	public FromServerAggregate ( String attribute ) {
		super ();
		indexAttribute = attribute;
		writeBacks = new HashMap<String, WritebackInOutClosure> ();
	}

	public void setObjects ( ArrayList objects ) {
		setArray ( indexAttribute, objects );
	}

	public ArrayList getObjects () {
		ArrayList ret;

		ret = getArray ( indexAttribute );
		if ( ret == null )
			ret = new ArrayList ();

		return ret;
	}

	public boolean hasObject ( FromServer obj ) {
		return hasObject ( obj.getLocalID () );
	}

	public boolean hasObject ( int id ) {
		ArrayList objects;
		FromServer tmp;

		objects = getObjects ();

		for ( int i = 0; i < objects.size (); i++ ) {
			tmp = ( FromServer ) objects.get ( i );
			if ( tmp.getLocalID () == id )
				return true;
		}

		return false;
	}

	protected void addWritebackFakeAttribute ( String name, int type, WritebackInOutClosure value ) {
		writeBacks.put ( name, value );
		super.addFakeAttribute ( name, type, value );
	}

	protected void addWritebackFakeAttribute ( String name, int type, Class object, WritebackInOutClosure value ) {
		writeBacks.put ( name, value );
		super.addFakeAttribute ( name, type, object, value );
	}

	private void testFake ( String name, Object value ) {
		WritebackInOutClosure writeback;

		if ( isAttributeFake ( name ) == true ) {
			writeback = writeBacks.get ( name );
			if ( writeback != null )
				writeback.setAttribute ( this, name, value );
		}
	}

	/*
		Nelle funzioni set*() setto comunque sempre il valore anche a questo oggetto, che ci sia o non ci sia
		writeback, altrimenti quando l'aggregatore non ha sotto-elementi l'informazione andrebbe persa e non
		si potrebbe piu' recuperare
	*/

	public void setString ( String name, String value ) {
		testFake ( name, value );
		super.setString ( name, value );
	}

	public void setInt ( String name, int value ) {
		testFake ( name, new Integer ( value ) );
		super.setInt ( name, value );
	}

	public void setFloat ( String name, float value ) {
		testFake ( name, new Float ( value ) );
		super.setFloat ( name, value );
	}

	public void setArray ( String name, ArrayList value ) {
		testFake ( name, value );
		super.setArray ( name, value );
	}

	public void addObject ( FromServer value ) {
		ArrayList array;

		array = getObjects ();
		array.add ( value );
		setObjects ( array );
	}

	public void setObject ( String name, FromServer value ) {
		testFake ( name, value );
		super.setObject ( name, value );
	}

	public void setDate ( String name, Date value ) {
		testFake ( name, value );
		super.setDate ( name, value );
	}

	public void setBool ( String name, boolean value ) {
		testFake ( name, new Boolean ( value ) );
		super.setBool ( name, value );
	}

	public void setAddress ( String name, Address value ) {
		testFake ( name, value );
		super.setAddress ( name, value );
	}

	/*
		Questa funzione va sovrascritta se si vuole avere controllo sui "children" che vengono ricostruiti
		automaticamente: se torna false, il nuovo "child" non viene incluso nella lista di sotto-oggetti
		dell'aggregato.
		Ad esempio: un OrderUser non ha prodotti al suo interno
	*/
	public boolean validateNewChild ( FromServer child ) {
		return true;
	}
}
