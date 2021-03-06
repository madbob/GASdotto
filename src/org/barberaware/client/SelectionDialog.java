/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class SelectionDialog extends DialogBox implements FromServerArray, SavingDialog {
	public static int			SELECTION_MODE_SINGLE	= 0;
	public static int			SELECTION_MODE_MULTI	= 1;
	public static int			SELECTION_MODE_ALL	= 2;

	private ArrayList<SavingDialogCallback>	savingCallbacks;
	private ArrayList			extraCallbacks;

	private int				selectionMode;
	private ArrayList			loadedObjects;
	private ArrayList			selectedObjects;

	private VerticalPanel			main;
	private HorizontalPanel			selectionButtons;
	private FlexTable			extraOptions;
	private FlexTable			itemsTable;

	public SelectionDialog ( int mode ) {
		savingCallbacks = null;
		extraCallbacks = null;

		selectionMode = mode;
		loadedObjects = new ArrayList ();
		selectedObjects = new ArrayList ();

		setText ( "Seleziona" );

		main = doDialog ();
		setWidget ( main );
	}

	/*
		Attenzione: non fa controllo di duplicati!
	*/
	public void addElementInList ( FromServer object ) {
		int i;
		int num;
		int cmp;
		String str_id;
		String str_name;
		Hidden iter;
		Label label;
		CheckBox check;

		str_id = Integer.toString ( object.getLocalID () );
		str_name = object.getString ( "name" );
		num = itemsTable.getRowCount ();

		for ( i = 0; i < num; i++ ) {
			label = ( Label ) itemsTable.getWidget ( i, 2 );
			cmp = label.getText ().compareTo ( str_name );
			if ( cmp > 0 )
				break;
		}

		itemsTable.insertRow ( i );

		check = new CheckBox ();

		if ( selectionMode == SELECTION_MODE_SINGLE ) {
			check.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
					CheckBox c;

					c = ( CheckBox ) event.getSource ();

					if ( c.isChecked () )
						uncheckAllBut ( c );
				}
			} );
		}

		itemsTable.setWidget ( i, 0, new Hidden ( str_id ) );
		itemsTable.setWidget ( i, 1, check );
		itemsTable.setWidget ( i, 2, new Label ( str_name ) );

		loadedObjects.add ( i, object );
	}

	public void removeElementInList ( FromServer object ) {
		int a;

		a = retrieveObjIndex ( object );
		if ( a != -1 )
			itemsTable.removeRow ( a );

		a = retrieveObjectIndexById ( object.getLocalID () );
		if ( a != -1 )
			loadedObjects.remove ( a );
	}

	public void show () {
		syncToDialog ();
		super.show ();
	}

	public void addSelectionCallbacks ( String name, FilterCallback callback ) {
		Button toggle;

		if ( extraCallbacks == null )
			extraCallbacks = new ArrayList ();

		extraCallbacks.add ( callback );

		toggle = new Button ( name );

		toggle.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				int pos;
				int num;
				Widget sender;
				CheckBox checkbox;
				FromServer tmp;
				FilterCallback callback;

				sender = ( Widget ) event.getSource ();

				/*
					Se il pannello gia' comprende i tasti "seleziona tutto" e "deseleziona
					tutto", per pescare l'indice corretto nell'array delle callbacks devo appunto
					togliere queste due posizioni
				*/
				pos = ( ( HorizontalPanel ) sender.getParent () ).getWidgetIndex ( sender );
				if ( selectionMode == SELECTION_MODE_ALL )
					pos = pos - 2;

				callback = ( FilterCallback ) extraCallbacks.get ( pos );
				num = loadedObjects.size ();

				for ( int a = 0; a < num; a++ ) {
					tmp = ( FromServer ) loadedObjects.get ( a );
					checkbox = ( CheckBox ) itemsTable.getWidget ( a, 1 );
					checkbox.setChecked ( callback.check ( tmp, null ) );
				}
			}
		} );

		if ( selectionButtons == null ) {
			selectionButtons = new HorizontalPanel ();
			main.insert ( selectionButtons, main.getWidgetIndex ( itemsTable ) );
		}

		selectionButtons.add ( toggle );
	}

	public void addExtraElement ( String text ) {
		int row;

		row = extraOptions.getRowCount ();
		extraOptions.setWidget ( row, 0, new Hidden () );
		extraOptions.setWidget ( row, 1, new CheckBox () );
		extraOptions.setWidget ( row, 2, new Label ( text ) );
	}

	public boolean getExtraElement ( String text ) {
		int num;
		Label lab;
		CheckBox check;

		num = extraOptions.getRowCount ();

		for ( int i = 0; i < num; i++ ) {
			lab = ( Label ) extraOptions.getWidget ( i, 2 );

			if ( lab.getText () == text ) {
				check = ( CheckBox ) extraOptions.getWidget ( i, 1 );
				return check.isChecked ();
			}
		}

		return false;
	}

	private void uncheckAllBut ( CheckBox not_this ) {
		int num;
		CheckBox tmp;

		num = itemsTable.getRowCount ();

		for ( int i = 0; i < num; i++ ) {
			tmp = ( CheckBox ) itemsTable.getWidget ( i, 1 );
			if ( not_this == null || ( not_this != tmp && tmp.isChecked () ) )
				tmp.setChecked ( false );
		}
	}

	private VerticalPanel doDialog () {
		VerticalPanel pan;
		DialogButtons buttons;
		ScrollPanel scroll;

		pan = new VerticalPanel ();

		if ( selectionMode == SELECTION_MODE_ALL ) {
			selectionButtons = doSelectDeselectAll ();
			pan.add ( selectionButtons );
		}
		else {
			selectionButtons = null;
		}

		extraOptions = new FlexTable ();
		pan.add ( extraOptions );

		scroll = new ScrollPanel ();
		scroll.setHeight ( ( Window.getClientHeight () - 250 ) + "px" );
		pan.add ( scroll );

		itemsTable = new FlexTable ();
		scroll.add ( itemsTable );

		buttons = new DialogButtons ();
		pan.add ( buttons );
		buttons.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				syncFromDialog ();
				hide ();
				executeCallbacks ( 0 );
			}

			public void onCancel ( SavingDialog dialog ) {
				hide ();
				executeCallbacks ( 1 );
			}
		} );

		return pan;
	}

	private HorizontalPanel doSelectDeselectAll () {
		Button toggle;
		HorizontalPanel buttons;

		buttons = new HorizontalPanel ();

		toggle = new Button ( "Seleziona Tutti" );
		toggle.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				CheckBox iter;

				for ( int i = 0; i < itemsTable.getRowCount (); i++ ) {
					iter = ( CheckBox ) itemsTable.getWidget ( i, 1 );
					iter.setChecked ( true );
				}
			}
		} );
		buttons.add ( toggle );

		toggle = new Button ( "Deseleziona Tutti" );
		toggle.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				CheckBox iter;

				for ( int i = 0; i < itemsTable.getRowCount (); i++ ) {
					iter = ( CheckBox ) itemsTable.getWidget ( i, 1 );
					iter.setChecked ( false );
				}
			}
		} );
		buttons.add ( toggle );

		return buttons;
	}

	private void executeCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
	}

	/*
		Questa e' da usare solo quando si vuole accedere agli elementi nell'array locale
		loadedObjects, altrimenti e' consigliato l'uso di retrieveObjectById()
	*/
	private int retrieveObjectIndexById ( int id ) {
		int num;
		int ret;
		FromServer tmp;

		ret = -1;
		num = loadedObjects.size ();

		for ( int i = 0; i < num; i++ ) {
			tmp = ( FromServer ) loadedObjects.get ( i );
			if ( tmp.getLocalID () == id ) {
				ret = i;
				break;
			}
		}

		return ret;
	}

	private FromServer retrieveObjectById ( int id ) {
		int index;

		index = retrieveObjectIndexById ( id );
		if ( index != -1 )
			return ( FromServer ) loadedObjects.get ( index );
		else
			return null;
	}

	private int retrieveObjIndex ( FromServer obj ) {
		int avail_num;
		String id;
		Hidden hid;

		id = Integer.toString ( obj.getLocalID () );
		avail_num = itemsTable.getRowCount ();

		for ( int i = 0; i < avail_num; i++ ) {
			hid = ( Hidden ) itemsTable.getWidget ( i, 0 );
			if ( id.equals ( hid.getName () ) )
				return i;
		}

		return -1;
	}

	private void syncFromDialog () {
		int avail_num;
		CheckBox check;
		String final_output;

		selectedObjects.clear ();
		avail_num = itemsTable.getRowCount ();

		for ( int i = 0; i < avail_num; i++ ) {
			check = ( CheckBox ) itemsTable.getWidget ( i, 1 );

			if ( check.isChecked () ) {
				int selected_id;
				Hidden hid;

				hid = ( Hidden ) itemsTable.getWidget ( i, 0 );
				selected_id = Integer.parseInt ( hid.getName () );
				selectedObjects.add ( retrieveObjectById ( selected_id ) );
			}
		}
	}

	private void syncToDialog () {
		int a;
		int sel_num;
		int avail_num;
		String tmp_id;
		CheckBox check;
		FromServer tmp;

		sel_num = selectedObjects.size ();

		avail_num = itemsTable.getRowCount ();
		for ( int i = 0; i < avail_num; i++ ) {
			check = ( CheckBox ) itemsTable.getWidget ( i, 1 );
			check.setChecked ( false );
		}

		if ( sel_num == 0 ) {
			for ( int i = 0; i < sel_num; i++ ) {
				check = ( CheckBox ) itemsTable.getWidget ( i, 1 );
				check.setChecked ( true );
			}
		}
		else {
			for ( int i = 0; i < sel_num; i++ ) {
				tmp = ( FromServer ) selectedObjects.get ( i );

				a = retrieveObjIndex ( tmp );
				if ( a != -1 ) {
					check = ( CheckBox ) itemsTable.getWidget ( a, 1 );
					check.setChecked ( true );
				}
			}
		}
	}

	/****************************************************************** FromServerArray */

	/*
		Da tenere presente che l'interfaccia FromServerArray si riferisce agli elementi
		selezionati e non selezionati nella lista; quelli fissi tra cui scegliere sono
		settati con addElementInList() e removeElementInList()
	*/

	public void addElement ( FromServer element ) {
		boolean found;
		FromServer iter;

		if ( element != null ) {
			found = false;

			for ( int i = 0; i < selectedObjects.size (); i++ ) {
				iter = ( FromServer ) selectedObjects.get ( i );
				if ( iter.equals ( element ) == true ) {
					found = true;
					break;
				}
			}

			if ( found == false )
				selectedObjects.add ( element );
		}
	}

	public void setElements ( ArrayList elements ) {
		selectedObjects.clear ();

		if ( elements != null ) {
			for ( int i = 0; i < elements.size (); i++ )
				selectedObjects.add ( elements.get ( i ) );
		}
	}

	public void removeElement ( FromServer element ) {
		/**
			TODO?	Questa funzione non dovrebbe mai essere usata, ma per completezza
				sarebbe da implementare
		*/
	}

	public ArrayList getElements () {
		int num;
		ArrayList ret;

		ret = new ArrayList ();
		num = selectedObjects.size ();

		for ( int i = 0; i < num; i++ )
			ret.add ( selectedObjects.get ( i ) );

		return ret;
	}

	/*
		Questa funzione cerca l'oggetto nella lista, se lo trova ne aggiorna il nome
		altrimenti lo aggiunge
	*/
	public void refreshElement ( FromServer object ) {
		int a;
		Label name;

		a = retrieveObjIndex ( object );
		if ( a != -1 ) {
			name = ( Label ) itemsTable.getWidget ( a, 2 );
			name.setText ( object.getString ( "name" ) );

			a = retrieveObjectIndexById ( object.getLocalID () );
			if ( a != -1 )
				loadedObjects.remove ( a );
			loadedObjects.add ( object );
		}
		else
			addElementInList ( object );
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			savingCallbacks = new ArrayList<SavingDialogCallback> ();
		savingCallbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks != null )
			savingCallbacks.remove ( callback );
	}
}
