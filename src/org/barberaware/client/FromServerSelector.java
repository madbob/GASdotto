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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

/*
	Il FromServerSelector puo' essere costruito solo per tipi di oggetto che hanno
	l'attributo "name" in forma di stringa
*/

public class FromServerSelector extends ObjectWidget {
	private ListBox					main;
	private String					type;
	private FromServerValidateCallback		filterCallback;
	private DelegatingChangeListenerCollection	changeListeners;

	public FromServerSelector ( String t, boolean hide_void ) {
		main = new ListBox ();
		initWidget ( main );

		type = t;
		filterCallback = null;

		if ( hide_void == false )
			main.addItem ( "Nessuno", "0" );

		Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( filterCallback != null ) {
					if ( filterCallback.checkObject ( object ) == false )
						return;
				}

				main.addItem ( object.getString ( "name" ), Integer.toString ( object.getLocalID () ) );
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveObjectIndex ( object );
				if ( index != -1 )
					main.setItemText ( index, object.getString ( "name" ) );
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveObjectIndex ( object );
				if ( index != -1 )
					main.removeItem ( index );
			}
		} );

		Utils.getServer ().testObjectReceive ( type );
	}

	public void addFilter ( FromServerValidateCallback filter ) {
		int id;
		FromServer tmp;

		filterCallback = filter;

		for ( int i = 0; i < main.getItemCount (); ) {
			id = Integer.parseInt ( main.getValue ( i ) );
			tmp = Utils.getServer ().getObjectFromCache ( type, id );

			if ( filterCallback.checkObject ( tmp ) == false )
				main.removeItem ( i );
			else
				i++;
		}
	}

	private int retrieveObjectIndex ( FromServer object ) {
		int search_id;
		int id;

		search_id = object.getLocalID ();

		for ( int i = 0; i < main.getItemCount (); ) {
			id = Integer.parseInt ( main.getValue ( i ) );
			if ( search_id == id )
				return i;
		}

		return -1;
	}

	protected ListBox getListWidget () {
		return main;
	}

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeListeners == null )
			changeListeners = new DelegatingChangeListenerCollection ( this, main );
		changeListeners.add ( listener );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		int num;
		String sel;

		if ( selected == null )
			if ( main.getItemCount () != 0 )
				main.setItemSelected ( 0, true );

		else {
			num = main.getItemCount ();
			sel = Integer.toString ( selected.getLocalID () );

			for ( int i = 0; i < num; i++ ) {
				if ( sel.equals ( main.getValue ( i ) ) ) {
					main.setItemSelected ( i, true );
					break;
				}
			}
		}
	}

	public FromServer getValue () {
		int selected;
		int index;

		if ( main.getItemCount () == 0 )
			return null;

		index = main.getSelectedIndex ();
		if ( index == 0 )
			return null;

		selected = Integer.parseInt ( main.getValue ( index ) );
		return Utils.getServer ().getObjectFromCache ( type, selected );
	}
}
