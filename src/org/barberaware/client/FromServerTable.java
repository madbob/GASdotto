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

public class FromServerTable extends FromServerArray {
	private class FromServerTableColumn {
		public String		head;
		public String		attr;
		public boolean		edit;

		public FromServerTableColumn ( String header, String attribute, boolean editable ) {
			head = header;
			attr = attribute;
			edit = editable;
		}
	}

	private FlexTable		main;
	private ArrayList		columns;
	private ArrayList		rows;

	public FromServerTable () {
		columns = new ArrayList ();
		rows = new ArrayList ();

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		initWidget ( main );
	}

	public void addColumn ( String header, String attribute, boolean editable ) {
		columns.add ( new FromServerTableColumn ( header, attribute, editable ) );
	}

	public void clean () {
		for ( int i = main.getRowCount () - 1; i > 0; i-- )
			main.removeRow ( i );

		rows.clear ();
	}

	private void doHeader () {
		int cols;
		FromServerTableColumn c;

		cols = columns.size ();

		for ( int i = 0; i < cols; i++ ) {
			c = ( FromServerTableColumn ) columns.get ( i );
			main.setWidget ( 0, i, new Label ( c.head ) );
		}

		main.getRowFormatter ().addStyleName ( 0, "header" );
	}

	private void syncRowsContents () {
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
				c = ( FromServerTableColumn ) columns.get ( a );

				if ( c.edit == true ) {
					wid = ( FromServerWidget ) main.getWidget ( i + 1, a );
					wid.assign ( obj );
				}
			}
		}
	}

	public void saveChanges () {
		int num;
		FromServer obj;

		syncRowsContents ();
		num = rows.size ();

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) rows.get ( i );
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
				c = ( FromServerTableColumn ) columns.get ( a );

				if ( c.edit == true ) {
					wid = ( FromServerWidget ) main.getWidget ( i + 1, a );
					wid.set ( obj );
				}
			}
		}
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		int row;
		int cols;
		Widget wid;
		FromServerTableColumn c;

		row = main.getRowCount ();
		cols = columns.size ();

		if ( row == 0 ) {
			doHeader ();
			row = 1;
		}

		for ( int i = 0; i < cols; i++ ) {
			c = ( FromServerTableColumn ) columns.get ( i );

			if ( c.edit == true )
				wid = new FromServerWidget ( element, c.attr );
			else
				wid = new Label ( element.getString ( c.attr ) );

			main.setWidget ( row, i, wid );
		}

		rows.add ( element );
	}

	public void setElements ( ArrayList elements ) {
		int num;
		FromServer obj;

		clean ();
		num = elements.size ();

		for ( int i = num - 1; i >= 0; i-- ) {
			obj = ( FromServer ) elements.get ( i );
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
	}

	public void refreshElement ( FromServer element ) {
		int cols;
		int row;
		Label lab;
		FromServerTableColumn c;
		FromServerWidget wid;

		cols = columns.size ();
		row = retrieveElementRow ( element );

		for ( int i = 0; i < cols; i++ ) {
			c = ( FromServerTableColumn ) columns.get ( i );

			if ( c.edit == true ) {
				wid = ( FromServerWidget ) main.getWidget ( row, i );
				wid.set ( element );
			}
			else {
				lab = ( Label ) main.getWidget ( row, i );
				lab.setText ( element.getString ( c.attr ) );
			}
		}
	}

	public ArrayList getElements () {
		syncRowsContents ();
		return rows;
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
