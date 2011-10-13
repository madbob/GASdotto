/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class FromServerRappresentationFull extends FromServerRappresentation {
	private ArrayList		callbacks;

	public FromServerRappresentationFull () {
		callbacks = new ArrayList ();
		noExplicitCallbacks ();
	}

	protected void resetObject () {
		super.resetObject ();
		resetCallbacks ();
	}

	public void setCallback ( FromServerFormCallbacks routine ) {
		if ( routine == null )
			noExplicitCallbacks ();
		else
			callbacks.add ( routine );
	}

	public void removeCallback ( String id ) {
		FromServerFormCallbacks call;

		for ( int i = 0; i < callbacks.size (); i++ ) {
			call = ( FromServerFormCallbacks ) callbacks.get ( i );
			if ( call.getID () == id ) {
				callbacks.remove ( call );
				break;
			}
		}
	}

	public boolean savingObject () {
		boolean confirm;
		FromServer obj;

		confirm = true;

		for ( int i = 0; i < callbacks.size (); i++ )
			confirm = ( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onSave ( this ) || confirm;

		if ( confirm == false || rebuildObject () == false )
			return false;

		beforeSave ();
		obj = getValue ();

		obj.save ( new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				savedCallbacks ();
				afterSave ();
			}

			public void onError () {
				errorCallbacks ();
			}
		} );

		return true;
	}

	protected void savedCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onSaved ( this );
	}

	protected void resetCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onReset ( this );
	}

	protected boolean deleteCallbacks () {
		boolean ret;
		FromServerFormCallbacks callback;

		ret = true;

		for ( int i = 0; i < callbacks.size (); i++ ) {
			callback = ( FromServerFormCallbacks ) callbacks.get ( i );
			/*
				In questo modo eseguo tutte le callback di
				onDelete, ma se anche una sola torna false la
				funzione esce
			*/
			ret = ( ret && callback.onDelete ( this ) );
		}

		return ret;
	}

	protected void openCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onOpen ( this );
	}

	protected void closingCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onClosing ( this );
	}

	protected void closeCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onClose ( this );
	}

	protected void errorCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onError ( this );
	}

	protected String retrieveNameInCallbacks () {
		String name;

		/*
			FromServerFormCallbacks.getName() di default torna il "name" dell'oggetto, anche se tale
			funzione non viene esplicitata. Cio' vuol dire che se non si presta attenzione viene saltata
			la propria getName().
			Qui itero l'array di callbacks partendo dal fondo, ovvero dall'ultima FromServerFormCallbacks
			assegnata, ma occorre ricordarsi di far tornare esplicitamente null alle funzioni immesse in
			un punto qualunque dell'esecuzione altrimenti forzeranno sempre il "name" dell'oggetto
		*/
		for ( int i = callbacks.size () - 1; i > -1; i-- ) {
			name = ( ( FromServerFormCallbacks ) callbacks.get ( i ) ).getName ( this );
			if ( name != null && name.equals ( "" ) == false )
				return name;
		}

		return "";
	}

	protected void noExplicitCallbacks () {
		callbacks.clear ();

		/*
			Viene comunque sempre settata una classe di callback di
			default, per garantire che almeno il nome del form sia
			sempre gestito
		*/
		callbacks.add ( new FromServerFormCallbacks () );
	}

	/*
		beforeSave() e afterSave() potrebbero essere implementate come
		callbacks FromServerFormCallbacks, ma vengono definite qui per
		gestire le funzioni che comunque sono sempre invocate dalle
		classi che ereditano da questa
	*/
	protected abstract void beforeSave ();
	protected abstract void afterSave ();
}

