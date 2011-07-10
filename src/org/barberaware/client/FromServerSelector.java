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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

/*
	Il FromServerSelector puo' essere costruito solo per tipi di oggetto che hanno
	l'attributo "name" in forma di stringa
*/

public class FromServerSelector extends ListBox implements ObjectWidget, Lockable {
	private String					type;
	private boolean					noVoidArticat;
	private boolean					sortItems;
	private boolean					locked;
	private int					scheduledSelectionID;
	private FromServerValidateCallback		filterCallback;

	public FromServerSelector ( String t, boolean hide_void, boolean sort, boolean lock ) {
		type = t;
		filterCallback = null;
		noVoidArticat = hide_void;
		scheduledSelectionID = -1;
		sortItems = sort;

		if ( hide_void == false )
			addItem ( "Nessuno", "0" );

		locked = lock;
		if ( locked == false )
			registerCallbacks ();
	}

	public void addAllSelector () {
		addItem ( "Tutti", "-1" );
	}

	public void addFilter ( FromServerValidateCallback filter ) {
		int id;
		FromServer tmp;

		filterCallback = filter;

		for ( int i = 0; i < getItemCount (); ) {
			id = Integer.parseInt ( getValue ( i ) );

			/*
				Questo e' per saltare l'eventuale elemento
				"Tutti" aggiunto con addAllSelector()
			*/
			if ( id == -1 ) {
				i++;
				continue;
			}

			tmp = Utils.getServer ().getObjectFromCache ( type, id );

			if ( filterCallback.checkObject ( tmp ) == false )
				removeItem ( i );
			else
				i++;
		}
	}

	public String getType () {
		return type;
	}

	public int getNumValues () {
		return getItemCount ();
	}

	private void registerCallbacks () {
		Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int id;
				String name;

				if ( filterCallback != null ) {
					if ( filterCallback.checkObject ( object ) == false )
						return;
				}

				id = object.getLocalID ();

				/*
					Caldamente sconsigliato procedere qui con un controllo
					sull'esistenza dell'elemento che si sta aggiungendo:
					quando tratto grosse quantita' di dati (gli utenti
					registrati) e' facile far andare in palla il browser
					ciclando e ri-ciclando su moli di elementi costantemente
					crescenti ad ogni iterazione
				*/

				name = object.getString ( "name" );

				if ( sortItems == true ) {
					int index;

					index = findPositionForName ( name );
					insertItem ( name, Integer.toString ( id ), index );
				}
				else
					addItem ( name, Integer.toString ( id ) );

				if ( scheduledSelectionID == id )
					setValue ( object );
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveObjectIndex ( object );
				if ( index != -1 )
					setItemText ( index, object.getString ( "name" ) );
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveObjectIndex ( object );
				if ( index != -1 )
					removeItem ( index );
			}

			public String debugName () {
				return "FromServerSelector su " + type;
			}
		} );
	}

	private int findPositionForName ( String search ) {
		int low;
		int high;
		int mid;
		int comp;
		String name;

		/*
			Comune implementazione di BinarySearch
		*/

		low = 0;
		high = getItemCount () - 1;

		while ( low <= high ) {
			mid = ( low + high ) / 2;
			name = getItemText ( mid );
			comp = name.compareTo ( search );

			if ( comp < 0 )
				low = mid + 1;
			else if ( comp > 0 )
				high = mid - 1;
			else
				return mid;
		}

		return low;
	}

	private int retrieveObjectIndex ( FromServer object ) {
		int search_id;
		int id;
		int num_items;
		String search_name;

		num_items = getItemCount ();

		/*
			Se gli elementi sono ordinati per nome faccio la ricerca sulla stringa
			degli items
		*/

		if ( sortItems == true ) {
			search_name = object.getString ( "name" );

			/**
				TODO	Usare una ricerca binaria?
			*/

			for ( int i = 0; i < num_items; i++ ) {
				switch ( search_name.compareTo ( getItemText ( i ) ) ) {
					case 0:
						return i;

					case -1:
						return -1;

					default:
						break;
				}
			}
		}
		else {
			search_id = object.getLocalID ();

			for ( int i = 0; i < num_items; i++ ) {
				id = Integer.parseInt ( getValue ( i ) );
				if ( search_id == id )
					return i;
			}
		}

		return -1;
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		int num;
		String sel;

		if ( selected == null ) {
			if ( getItemCount () != 0 )
				setItemSelected ( 0, true );
		}
		else {
			num = getItemCount ();

			if ( num == 0 )
				scheduledSelectionID = selected.getLocalID ();

			else {
				sel = Integer.toString ( selected.getLocalID () );

				for ( int i = 0; i < num; i++ ) {
					if ( sel.equals ( getValue ( i ) ) ) {
						setItemSelected ( i, true );
						break;
					}
				}
			}
		}
	}

	public FromServer getValue () {
		int selected;
		int index;

		if ( getItemCount () == 0 )
			return null;

		index = getSelectedIndex ();

		/*
			Se noVoidArticat == false, in posizione 0 si trova la voce appositamente
			iniettata nel costruttore che rappresenta il valore nullo
		*/
		if ( noVoidArticat == false && index == 0 )
			return null;

		selected = Integer.parseInt ( getValue ( index ) );
		if ( selected == 0 || selected == -1 )
			return null;

		return Utils.getServer ().getObjectFromCache ( type, selected );
	}

	/****************************************************************** Lockable */

	public void unlock () {
		if ( locked == true )
			registerCallbacks ();

		locked = false;
	}
}
