/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class FromServerSelector extends ListBox {
	private String				type;
	private FromServerValidateCallback	filterCallback;

	public FromServerSelector ( String t ) {
		type = t;
		filterCallback = null;

		addItem ( "Nessuno", "0" );

		Utils.getServer ().onObjectReceive ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( filterCallback != null ) {
					if ( filterCallback.checkObject ( object ) == false )
						return;
				}

				addItem ( object.getString ( "name" ), Integer.toString ( object.getLocalID () ) );
			}
		} );

		Utils.getServer ().testObjectReceive ( type );
	}

	public void addFilter ( FromServerValidateCallback filter ) {
		int id;
		FromServer tmp;

		filterCallback = filter;

		for ( int i = 0; i < getItemCount (); ) {
			id = Integer.parseInt ( getValue ( i ) );
			tmp = Utils.getServer ().getObjectFromCache ( type, id );

			if ( filterCallback.checkObject ( tmp ) == false )
				removeItem ( i );
			else
				i++;
		}
	}

	public void setSelected ( FromServer selected ) {
		int num;
		String sel;

		if ( selected == null )
			setItemSelected ( 0, true );

		else {
			num = getItemCount ();
			sel = Integer.toString ( selected.getLocalID () );

			for ( int i = 0; i < num; i++ ) {
				if ( sel.equals ( getValue ( i ) ) ) {
					setItemSelected ( i, true );
					break;
				}
			}
		}
	}

	public FromServer getSelected () {
		int selected;
		int index;

		index = getSelectedIndex ();
		if ( index == 0 )
			return null;

		selected = Integer.parseInt ( getValue ( index ) );
		return Utils.getServer ().getObjectFromCache ( type, selected );
	}
}
