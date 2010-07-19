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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class MultiSelector extends Composite implements FromServerArray {
	private VerticalPanel		main;

	private FilterCallback		filterCallback;

	private SelectionDialog		dialog;
	private String			objectType;

	/*
		Il parametro "mode" si riferisce ai valori in SelectionDialog
	*/
	public MultiSelector ( String type, int mode, FilterCallback filter ) {
		Button mod_button;

		objectType = type;
		filterCallback = filter;

		dialog = new SelectionDialog ( mode );
		dialog.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				rebuildMainList ();
			}
		} );

		main = new VerticalPanel ();
		main.setStyleName ( "multi-selector" );
		initWidget ( main );

		mod_button = new Button ( "Modifica Lista" );
		mod_button.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.center ();
				dialog.show ();
			}
		} );
		main.add ( mod_button );

		if ( type != null ) {
			Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
				public void onReceive ( FromServer object ) {
					if ( checkValidity ( object ) == true )
						dialog.addElementInList ( object );
				}

				public void onModify ( FromServer object ) {
					if ( checkValidity ( object ) == true )
						dialog.refreshElement ( object );
					else
						dialog.removeElementInList ( object );
				}

				public void onDestroy ( FromServer object ) {
					dialog.removeElementInList ( object );
				}

				protected String debugName () {
					return "MultiSelector su " + objectType;
				}
			} );

			Utils.getServer ().testObjectReceive ( type );
		}
	}

	public void clean () {
		int num;

		num = main.getWidgetCount () - 1;
		for ( int i = 0; i < num; i++ )
			main.remove ( 1 );
	}

	public void forceInitialElements ( ArrayList elements ) {
		int num;
		FromServer obj;

		num = elements.size ();

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) elements.get ( i );
			if ( checkValidity ( obj ) == true )
				dialog.addElementInList ( obj );
		}
	}

	public void addSelectionCallbacks ( String name, FilterCallback callback ) {
		dialog.addSelectionCallbacks ( name, callback );
	}

	private boolean checkValidity ( FromServer object ) {
		if ( filterCallback != null )
			return filterCallback.check ( object, null );
		else
			return true;
	}

	private void rebuildMainList () {
		int num;
		FromServer iter;
		ArrayList objects;

		clean ();
		objects = dialog.getElements ();
		num = objects.size ();

		for ( int i = 0; i < num; i++ ) {
			iter = ( FromServer ) objects.get ( i );
			main.add ( new Label ( iter.getString ( "name" ) ) );
		}
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		dialog.addElement ( element );
		rebuildMainList ();
	}

	public void setElements ( ArrayList elements ) {
		dialog.setElements ( elements );
		rebuildMainList ();
	}

	public void removeElement ( FromServer element ) {
		dialog.removeElement ( element );
		rebuildMainList ();
	}

	public ArrayList getElements () {
		return dialog.getElements ();
	}

	public void refreshElement ( FromServer element ) {
		dialog.refreshElement ( element );
	}
}
