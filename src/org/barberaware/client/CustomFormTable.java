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

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.*;

public class CustomFormTable extends FlexTable {
	private HTMLTable.CellFormatter		formatter;

	public CustomFormTable () {
		formatter = getCellFormatter ();
		setStyleName ( "custom-form-table" );
	}

	public void addPair ( String name, Widget element, int row ) {
		Label lab;

		lab = new Label ( name );
		setWidget ( row, 0, lab );
		setWidget ( row, 1, element );
		formatter.addStyleName ( row, 0, "custom-label" );
	}

	public void addPair ( String name, Widget element ) {
		addPair ( name, element, getRowCount () );
	}

	public void addRight ( Widget element ) {
		int row;

		row = getRowCount ();
		setWidget ( row, 1, element );
	}

	public void showByLabel ( String label, boolean show ) {
		int index;
		HTMLTable.RowFormatter format;

		format = getRowFormatter ();
		index = getLabelIndex ( label );

		if ( index != -1 ) {
			if ( show == true )
				format.removeStyleName ( index, "hidden" );
			else
				format.addStyleName ( index, "hidden" );
		}
	}

	private int getLabelIndex ( String label ) {
		Label l;

		for ( int i = 0; i < getRowCount (); i++ ) {
			l = ( Label ) getWidget ( i, 0 );
			if ( l.getText () == label )
				return i;
		}

		return -1;
	}
}

