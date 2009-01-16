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

		Utils.getServer ().onObjectReceive ( "User", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				doSelectableRow ( ( User ) object );
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

		index = items.getRowCount ();
		items.insertRow ( index );

		items.setWidget ( index, 0, new Hidden ( Integer.toString ( user.getLocalID () ) ) );
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
		int sel_num;
		int avail_num;
		String tmp_id;
		CheckBox check;
		Hidden hid;
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
			tmp_id = Integer.toString ( tmp.getLocalID () );

			for ( int a = 0; a < avail_num; a++ ) {
				hid = ( Hidden ) items.getWidget ( i, 0 );

				if ( tmp_id.equals ( hid.getName () ) ) {
					check = ( CheckBox ) items.getWidget ( i, 1 );
					check.setChecked ( true );
					break;
				}
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

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		if ( elements == null )
			selected.clear ();
		else
			selected = elements;

		retriveMainString ();
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		return selected;
	}
}
