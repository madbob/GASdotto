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

import com.allen_sauer.gwt.log.client.Log;

public class FromServerTable extends Composite implements FromServerArray {
	public static int		TABLE_REMOVE		= 0;
	public static int		TABLE_EDIT		= 1;

	private class FromServerTableColumn {
		public String			head;
		public String			attr;
		public boolean			edit;
		public WidgetFactoryCallback	customWid;
		public int			action;
		public String			extraStyle;

		public FromServerTableColumn ( String header, String attribute, boolean editable, WidgetFactoryCallback custom, int act ) {
			head = header;
			attr = attribute;
			edit = editable;
			customWid = custom;
			action = act;
			extraStyle = "";
		}

		public String getUneditable ( FromServer obj ) {
			int type;
			FromServer child;

			if ( obj != null ) {
				type = obj.getAttributeType ( attr );

				if ( type == FromServer.OBJECT ) {
					child = obj.getObject ( attr );
					if ( child != null )
						return child.getString ( "name" );
				}
				else {
					return obj.getString ( attr );
				}
			}

			return "";
		}
	}

	private FlexTable				main;
	private ArrayList<FromServerTableColumn>	columns;
	private ArrayList				rows;
	private String					emptyWarning;
	private FromServerSortCallback			sorting;

	public FromServerTable () {
		columns = new ArrayList<FromServerTableColumn> ();
		rows = new ArrayList ();
		emptyWarning = null;

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		main.setCellPadding ( 2 );
		main.setWidth ( "100%" );
		initWidget ( main );

		sorting = new FromServerSortCallback () {
			public int compare ( FromServer first, FromServer second ) {
				return first.getString ( "name" ).compareTo ( second.getString ( "name" ) );
			}
		};

		main.addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				FromServer obj;
				FromServerTableColumn col;
				SavingDialog dialog;
				DialogBox true_dialog;

				col = columns.get ( cell );

				if ( col.action != -1 ) {
					obj = ( FromServer ) rows.get ( row - 1 );

					if ( col.action == TABLE_REMOVE ) {
						if ( col.customWid == null ) {
							removeElement ( obj );
						}
						else {
							dialog = ( SavingDialog ) col.customWid.create ();
							( ( ObjectWidget ) dialog ).setValue ( obj );

							dialog.addCallback ( new SavingDialogCallback () {
								public void onSave ( SavingDialog dialog ) {
									ObjectWidget myself;

									myself = ( ObjectWidget ) dialog;
									removeElement ( myself.getValue () );
								}
							} );

							true_dialog = ( DialogBox ) dialog;
							true_dialog.center ();
							true_dialog.show ();
						}
					}
					else if ( col.action == TABLE_EDIT ) {
						dialog = ( SavingDialog ) col.customWid.create ();
						( ( ObjectWidget ) dialog ).setValue ( obj );

						dialog.addCallback ( new SavingDialogCallback () {
							public void onSave ( SavingDialog dialog ) {
								ObjectWidget myself;

								myself = ( ObjectWidget ) dialog;
								refreshElement ( myself.getValue () );
							}
						} );

						true_dialog = ( DialogBox ) dialog;
						true_dialog.center ();
						true_dialog.show ();
					}
				}
			}
		} );

		clean ( true );
	}

	public int addColumn ( String header, String attribute, boolean editable ) {
		columns.add ( new FromServerTableColumn ( header, attribute, editable, null, -1 ) );
		return columns.size () - 1;
	}

	public int addColumn ( String header, String attribute, WidgetFactoryCallback custom ) {
		columns.add ( new FromServerTableColumn ( header, attribute, true, custom, -1 ) );
		return columns.size () - 1;
	}

	/*
		Se action == TABLE_REMOVE la callback custom puo' essere null (e dunque viene
			ignorata) oppure deve ritornare sempre un SavingDialog che implementi
			ObjectWidget e che al salvataggio conferma la rimozione dell'elemento
		Se action == TABLE_EDIT la callback deve ritornare sempre un SavingDialog che
			implementi ObjectWidget
	*/
	public int addColumn ( String header, int action, WidgetFactoryCallback custom ) {
		columns.add ( new FromServerTableColumn ( header, null, false, custom, action ) );
		return columns.size () - 1;
	}

	public void setColumnStyle ( int index, String style ) {
		FromServerTableColumn col;

		if ( index >= columns.size () )
			return;

		col = columns.get ( index );
		col.extraStyle = style;
	}

	public void setEmptyWarning ( String warning ) {
		emptyWarning = warning;
	}

	public void clean ( boolean empty ) {
		int num_rows;

		num_rows = main.getRowCount ();

		for ( int i = num_rows - 1; i > 0; i-- )
			main.removeRow ( i );

		rows.clear ();

		if ( empty == true ) {
			if ( num_rows > 0 )
				main.removeRow ( 0 );

			if ( emptyWarning != null )
				main.setWidget ( 0, 0, new Label ( emptyWarning ) );
		}
	}

	public void setSorting ( FromServerSortCallback callback ) {
		this.sorting = callback;
	}

	private void doHeader () {
		int cols;
		FromServerTableColumn c;

		cols = columns.size ();

		for ( int i = 0; i < cols; i++ ) {
			c = columns.get ( i );
			main.setWidget ( 0, i, new Label ( c.head ) );
		}

		main.getRowFormatter ().setStyleName ( 0, "table-header" );
	}

	private ArrayList syncRowsContents () {
		int num;
		int cols;
		boolean to_add;
		ArrayList changed;
		FromServer obj;
		FromServerTableColumn c;
		FromServerWidget wid;

		changed = new ArrayList ();
		num = rows.size ();
		cols = columns.size ();

		for ( int i = 0; i < num; i++ ) {
			to_add = false;
			obj = ( FromServer ) rows.get ( i );

			for ( int a = 0; a < cols; a++ ) {
				c = columns.get ( a );

				if ( c.edit == true ) {
					wid = ( FromServerWidget ) main.getWidget ( i + 1, a );

					if ( wid.compare ( obj ) == false ) {
						wid.assign ( obj );
						to_add = true;
					}
				}
			}

			if ( to_add == true )
				changed.add ( obj );
		}

		return changed;
	}

	public void saveChanges () {
		int num;
		ArrayList to_save;
		FromServer obj;

		to_save = syncRowsContents ();
		num = to_save.size ();

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) to_save.get ( i );
			obj.save ( null );
		}
	}

	public void revertChanges () {
		int num;
		int cols;
		FromServer obj;
		FromServerTableColumn c;
		FromServerWidget wid;

		num = rows.size ();
		cols = columns.size ();

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) rows.get ( i );

			for ( int a = 0; a < cols; a++ ) {
				c = columns.get ( a );

				if ( c.edit == true ) {
					wid = ( FromServerWidget ) main.getWidget ( i + 1, a );
					wid.set ( obj );
					Utils.graphicPulseWidget ( wid );
				}
			}
		}
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		int row;
		int cols;
		String element_name;
		Widget wid;
		FromServer cmp;
		FromServerTableColumn c;

		if ( element == null )
			return;

		if ( retrieveElementRow ( element ) != -1 ) {
			refreshElement ( element );
			return;
		}

		row = main.getRowCount ();
		cols = columns.size ();

		if ( row < 2 ) {
			doHeader ();
			row = 1;
		}
		else {
			for ( int i = 0; i < rows.size (); i++ ) {
				cmp = ( FromServer ) rows.get ( i );

				if ( this.sorting.compare ( cmp, element ) > 0 ) {
					main.insertRow ( i + 1 );
					row = i + 1;
					break;
				}
			}
		}

		for ( int i = 0; i < cols; i++ ) {
			c = columns.get ( i );

			if ( c.action != -1 ) {
				if ( c.action == TABLE_REMOVE )
					wid = new Image ( "images/mini_delete.png" );
				else if ( c.action == TABLE_EDIT )
					wid = new Image ( "images/mini_edit.png" );
				else
					wid = null;
			}
			else if ( c.customWid != null ) {
				wid = new FromServerWidget ( element, c.attr, c.customWid.create () );
			}
			else if ( c.edit == true ) {
				wid = new FromServerWidget ( element, c.attr );
			}
			else {
				wid = new Label ( c.getUneditable ( element ) );
			}

			if ( c.extraStyle != "" )
				wid.addStyleName ( c.extraStyle );

			main.setWidget ( row, i, wid );
		}

		rows.add ( row - 1, element );
	}

	public void setElements ( ArrayList elements ) {
		int num;
		ArrayList sorted_elements;
		FromServer obj;

		if ( elements == null ) {
			clean ( true );
			return;
		}

		sorted_elements = Utils.sortArrayByName ( elements );
		num = sorted_elements.size ();

		if ( num == 0 ) {
			clean ( true );
			return;
		}

		clean ( false );

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) sorted_elements.get ( i );
			if ( obj != null )
				addElement ( obj );
		}
	}

	public void removeElement ( FromServer element ) {
		int i;

		i = retrieveElementRow ( element );
		if ( i == -1 )
			return;

		main.removeRow ( i );
		rows.remove ( i - 1 );

		if ( rows.size () == 0 )
			clean ( true );
	}

	public void refreshElement ( FromServer element ) {
		int cols;
		int row;
		Label lab;
		FromServerTableColumn c;
		FromServerWidget wid;

		cols = columns.size ();
		row = retrieveElementRow ( element );
		if ( row == -1 )
			return;

		for ( int i = 0; i < cols; i++ ) {
			c = columns.get ( i );

			if ( c.action != -1 ) {
				continue;
			}
			else if ( c.edit == true ) {
				wid = ( FromServerWidget ) main.getWidget ( row, i );
				wid.set ( element );
			}
			else {
				lab = ( Label ) main.getWidget ( row, i );
				lab.setText ( c.getUneditable ( element ) );
			}
		}
	}

	public ArrayList getElements () {
		syncRowsContents ();
		return Utils.duplicateFromServerArray ( rows );
	}

	private int retrieveElementRow ( FromServer element ) {
		int num;
		int element_id;
		FromServer obj;

		num = rows.size ();
		element_id = element.getLocalID ();

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) rows.get ( i );

			if ( obj.getLocalID () == element_id )
				return i + 1;
		}

		return -1;
	}
}
