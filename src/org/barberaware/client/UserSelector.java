/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class UserSelector extends ObjectWidget {
	private ListBox				main;

	public UserSelector () {
		main = new ListBox ();
		initWidget ( main );

		main.addItem ( "Seleziona Utente", "none" );

		Utils.getServer ().onObjectEvent ( "User", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				main.addItem ( object.getString ( "name" ), Integer.toString ( object.getLocalID () ) );
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveUserIndex ( object );
				if ( index != -1 )
					main.setItemText ( index, object.getString ( "name" ) );
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveUserIndex ( object );
				if ( index != -1 )
					main.removeItem ( index );
			}
		} );
	}

	private int retrieveUserIndex ( FromServer user ) {
		int num;
		String sel;

		num = main.getItemCount ();
		sel = Integer.toString ( user.getLocalID () );

		for ( int i = 0; i < num; i++ ) {
			if ( sel.equals ( main.getValue ( i ) ) )
				return i;
		}

		return -1;
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		int num;
		String sel;

		if ( selected == null )
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
		return Utils.getServer ().getObjectFromCache ( "User", selected );
	}
}
