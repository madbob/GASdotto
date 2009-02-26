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

public class ReferenceList extends FromServerArray {
	private TextBox			main;

	private DialogBox		dialog;
	private FlexTable		items;

	private ArrayList		selected;
	private boolean			opened;

	public ReferenceList () {
		opened = false;
		selected = new ArrayList ();

		dialog = new DialogBox ( false );
		dialog.setText ( "Seleziona Referenti" );
		dialog.setWidget ( doDialog () );

		main = new TextBox ();
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					syncToDialog ();
					dialog.center ();
					dialog.show ();
				}
			}

			public void onLostFocus ( Widget sender ) {
				/* dummy */
			}
		} );

		initWidget ( main );
		clean ();

		Utils.getServer ().onObjectEvent ( "User", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( object.getInt ( "privileges" ) >= User.USER_RESPONSABLE ) {
					doSelectableRow ( ( User ) object );
				}
			}

			public void onModify ( FromServer object ) {
				int a;
				Label name;

				a = retrieveUserIndex ( object );

				if ( object.getInt ( "privileges" ) >= User.USER_RESPONSABLE ) {
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

				a = retrieveUserIndex ( object );
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

		items = new FlexTable ();
		pan.add ( items );

		pan.add ( new HTML ( "<p>Per eleggere altri utenti al ruolo di responsabile modifica i loro privilegi dal pannello \"Gestione Utenti\"</p>" ) );

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

	public void clean () {
		main.setText ( "" );
	}

	private void doSelectableRow ( User user ) {
		int index;
		String str_id;
		Hidden iter;

		str_id = Integer.toString ( user.getLocalID () );
		index = items.getRowCount ();

		for ( int i = 0; i < index; i++ ) {
			iter = ( Hidden ) items.getWidget ( index, 0 );
			if ( iter.getName ().equals ( str_id ) )
				return;
		}

		items.insertRow ( index );

		items.setWidget ( index, 0, new Hidden ( str_id ) );
		items.setWidget ( index, 1, new CheckBox () );
		items.setWidget ( index, 2, new Label ( user.getString ( "name" ) ) );
	}

	private void retriveMainString () {
		int i;
		int num;
		String mainstring;
		User user;

		num = selected.size ();
		mainstring = "";

		if ( num != 0 ) {
			num--;

			for ( i = 0; i < num; i++ ) {
				user = ( User ) selected.get ( i );
				mainstring += user.getString ( "name" ) + ", ";
			}

			user = ( User ) selected.get ( i );
			mainstring += user.getString ( "name" );
		}

		main.setText ( mainstring );
	}

	private void syncToDialog () {
		int a;
		int sel_num;
		int avail_num;
		String tmp_id;
		CheckBox check;
		User tmp;

		sel_num = selected.size ();
		if ( sel_num == 0 )
			return;

		avail_num = items.getRowCount ();
		for ( int i = 0; i < avail_num; i++ ) {
			check = ( CheckBox ) items.getWidget ( i, 1 );
			check.setChecked ( false );
		}

		for ( int i = 0; i < sel_num; i++ ) {
			tmp = ( User ) selected.get ( i );

			a = retrieveUserIndex ( tmp );
			if ( a != -1 ) {
				check = ( CheckBox ) items.getWidget ( a, 1 );
				check.setChecked ( true );
			}
		}
	}

	private void syncFromDialog () {
		int avail_num;
		int selected_id;
		CheckBox check;
		Hidden hid;
		FromServer user;
		String final_output;

		selected.clear ();
		avail_num = items.getRowCount ();
		final_output = "";

		for ( int i = 0; i < avail_num; i++ ) {
			check = ( CheckBox ) items.getWidget ( i, 1 );

			if ( check.isChecked () ) {
				hid = ( Hidden ) items.getWidget ( i, 0 );
				selected_id = Integer.parseInt ( hid.getName () );
				user = Utils.getServer ().getObjectFromCache ( "User", selected_id );
				selected.add ( user );
			}
		}

		retriveMainString ();
	}

	private int retrieveUserIndex ( FromServer user ) {
		int avail_num;
		String id;
		Hidden hid;

		id = Integer.toString ( user.getLocalID () );
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
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		selected.clear ();

		if ( elements != null ) {
			for ( int i = 0; i < elements.size (); i++ )
				selected.add ( elements.get ( i ) );
		}

		retriveMainString ();
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		return selected;
	}
}
