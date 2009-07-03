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

public class MultiSelector extends FromServerArray {
	private VerticalPanel		main;

	private FilterCallback		filterCallback;

	private DialogBox		dialog;
	private FlexTable		items;

	private String			objectType;
	private boolean			manageAll;
	private ArrayList		selected;
	private boolean			opened;

	public MultiSelector ( String type, boolean selectall, FilterCallback filter ) {
		Button mod_button;

		opened = false;
		objectType = type;
		manageAll = selectall;
		selected = new ArrayList ();
		filterCallback = filter;

		dialog = new DialogBox ( false );
		dialog.setText ( "Seleziona" );
		dialog.setWidget ( doDialog () );

		main = new VerticalPanel ();
		main.setStyleName ( "multi-selector" );
		initWidget ( main );

		mod_button = new Button ( "Modifica Lista" );
		mod_button.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					syncToDialog ();
					dialog.center ();
					dialog.show ();
				}
			}
		} );
		main.add ( mod_button );

		clean ();

		Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( filterCallback != null )
					if ( filterCallback.check ( object, null ) == false )
						return;

				doSelectableRow ( object );
			}

			public void onModify ( FromServer object ) {
				int a;
				boolean do_it;
				Label name;

				a = retrieveObjIndex ( object );

				if ( filterCallback != null )
					do_it = filterCallback.check ( object, null );
				else
					do_it = true;

				if ( do_it == true ) {
					if ( a != -1 ) {
						name = ( Label ) items.getWidget ( a, 2 );
						name.setText ( object.getString ( "name" ) );
					}
					else
						onReceive ( object );
				}
				else
					if ( a != -1 )
						onDestroy ( object );
			}

			public void onDestroy ( FromServer object ) {
				int a;

				a = retrieveObjIndex ( object );
				if ( a != -1 )
					items.removeRow ( a );
			}
		} );
	}

	private Panel doDialog () {
		VerticalPanel pan;
		HorizontalPanel buttons;
		Button but;

		pan = new VerticalPanel ();

		if ( manageAll == true )
			pan.add ( doSelectDeselectAll () );

		items = new FlexTable ();
		pan.add ( items );

		buttons = new HorizontalPanel ();
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				syncFromDialog ();
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		return pan;
	}

	private HorizontalPanel doSelectDeselectAll () {
		Button toggle;
		HorizontalPanel buttons;

		buttons = new HorizontalPanel ();

		toggle = new Button ( "Seleziona Tutti" );
		toggle.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				CheckBox iter;

				for ( int i = 0; i < items.getRowCount (); i++ ) {
					iter = ( CheckBox ) items.getWidget ( i, 1 );
					iter.setChecked ( true );
				}
			}
		} );
		buttons.add ( toggle );

		toggle = new Button ( "Deseleziona Tutti" );
		toggle.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				CheckBox iter;

				for ( int i = 0; i < items.getRowCount (); i++ ) {
					iter = ( CheckBox ) items.getWidget ( i, 1 );
					iter.setChecked ( false );
				}
			}
		} );
		buttons.add ( toggle );

		return buttons;
	}

	public void clean () {
		int num;

		num = main.getWidgetCount () - 1;
		for ( int i = 0; i < num; i++ )
			main.remove ( 1 );
	}

	private void doSelectableRow ( FromServer obj ) {
		int index;
		String str_id;
		Hidden iter;
		CheckBox check;

		str_id = Integer.toString ( obj.getLocalID () );
		index = items.getRowCount ();

		for ( int i = 0; i < index; i++ ) {
			iter = ( Hidden ) items.getWidget ( i, 0 );
			if ( iter.getName ().equals ( str_id ) )
				return;
		}

		items.insertRow ( index );

		items.setWidget ( index, 0, new Hidden ( str_id ) );
		items.setWidget ( index, 1, new CheckBox () );
		items.setWidget ( index, 2, new Label ( obj.getString ( "name" ) ) );
	}

	private void rebuildMainList () {
		int i;
		int num;
		FromServer iter;

		clean ();
		num = selected.size ();

		for ( i = 0; i < num; i++ ) {
			iter = ( FromServer ) selected.get ( i );
			main.add ( new Label ( iter.getString ( "name" ) ) );
		}
	}

	private void syncToDialog () {
		int a;
		int sel_num;
		int avail_num;
		String tmp_id;
		CheckBox check;
		FromServer tmp;

		sel_num = selected.size ();

		avail_num = items.getRowCount ();
		for ( int i = 0; i < avail_num; i++ ) {
			check = ( CheckBox ) items.getWidget ( i, 1 );
			check.setChecked ( false );
		}

		if ( sel_num == 0 ) {
			for ( int i = 0; i < sel_num; i++ ) {
				check = ( CheckBox ) items.getWidget ( i, 1 );
				check.setChecked ( true );
			}
		}
		else {
			for ( int i = 0; i < sel_num; i++ ) {
				tmp = ( FromServer ) selected.get ( i );

				a = retrieveObjIndex ( tmp );
				if ( a != -1 ) {
					check = ( CheckBox ) items.getWidget ( a, 1 );
					check.setChecked ( true );
				}
			}
		}
	}

	private void syncFromDialog () {
		int avail_num;
		CheckBox check;
		String final_output;

		selected.clear ();
		avail_num = items.getRowCount ();
		final_output = "";

		for ( int i = 0; i < avail_num; i++ ) {
			check = ( CheckBox ) items.getWidget ( i, 1 );

			if ( check.isChecked () ) {
				int selected_id;
				Hidden hid;
				FromServer obj;

				hid = ( Hidden ) items.getWidget ( i, 0 );
				selected_id = Integer.parseInt ( hid.getName () );
				obj = Utils.getServer ().getObjectFromCache ( objectType, selected_id );
				selected.add ( obj );
			}
		}

		rebuildMainList ();
	}

	private int retrieveObjIndex ( FromServer obj ) {
		int avail_num;
		String id;
		Hidden hid;

		id = Integer.toString ( obj.getLocalID () );
		avail_num = items.getRowCount ();

		for ( int i = 0; i < avail_num; i++ ) {
			hid = ( Hidden ) items.getWidget ( i, 0 );
			if ( id.equals ( hid.getName () ) )
				return i;
		}

		return -1;
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		boolean found;
		FromServer iter;

		if ( element != null ) {
			found = false;

			for ( int i = 0; i < selected.size (); i++ ) {
				iter = ( FromServer ) selected.get ( i );
				if ( iter.equals ( element ) == true ) {
					found = true;
					break;
				}
			}

			if ( found == false )
				selected.add ( element );

			rebuildMainList ();
		}
	}

	public void setElements ( ArrayList elements ) {
		selected.clear ();

		if ( elements != null ) {
			for ( int i = 0; i < elements.size (); i++ )
				selected.add ( elements.get ( i ) );
		}

		rebuildMainList ();
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		int num;
		ArrayList ret;

		ret = new ArrayList ();
		num = selected.size ();

		for ( int i = 0; i < num; i++ )
			ret.add ( selected.get ( i ) );

		return ret;
	}
}
