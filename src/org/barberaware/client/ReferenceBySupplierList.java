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

public class ReferenceBySupplierList extends ObjectWidget {
	private ListBox				main;
	private Supplier			supplier;

	public ReferenceBySupplierList ( Supplier supp ) {
		main = new ListBox ();
		initWidget ( main );

		supplier = supp;
		refreshList ( supp );

		Utils.getServer ().onObjectEvent ( "Supplier", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				/*dummy  */
			}

			public void onModify ( FromServer object ) {
				if ( supplier != null && supplier.equals ( object ) ) {
					supplier = ( Supplier ) object;
					refreshList ( supplier );
				}
			}

			public void onDestroy ( FromServer object ) {
				/*dummy  */
			}
		} );

		/**
			TODO	Aggiungere refresh su Users
		*/
	}

	public void setSupplier ( Supplier supp ) {
		supplier = supp;
		refreshList ( supp );
	}

	private void refreshList ( Supplier supp ) {
		int selected_index;
		ArrayList references;
		FromServer selected;
		User u;

		selected = getValue ();
		selected_index = 0;

		main.clear ();
		main.addItem ( "Nessuno", "-1" );

		if ( supp == null )
			return;

		references = supp.getArray ( "references" );

		for ( int i = 0; i < references.size (); i++ ) {
			u = ( User ) references.get ( i );
			main.addItem ( u.getString ( "name" ), Integer.toString ( u.getLocalID () ) );

			if ( selected != null && selected.equals ( u ) )
				selected_index = i;
		}

		main.setItemSelected ( selected_index, true );
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
		return Utils.getServer ().getObjectFromCache ( "Supplier", selected );
	}
}
