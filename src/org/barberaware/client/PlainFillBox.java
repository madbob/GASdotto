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

import com.allen_sauer.gwt.log.client.Log;

public class PlainFillBox extends FlexTable {
	private boolean			hasContents;
	private String			emptyString;
	private String			headString;

	public PlainFillBox () {
		hasContents = false;
		emptyString = "";
		headString = "";
	}

	public void setStrings ( String empty, String full ) {
		emptyString = empty;
		headString = full;
		checkEmpty ();
	}

	public void addRow ( ArrayList contents ) {
		int row;
		int num;

		if ( hasContents == false ) {
			super.removeRow ( 0 );
			setWidget ( 0, 0, new Label ( headString ) );
			hasContents = true;
		}

		num = contents.size ();
		row = getRowCount ();

		for ( int i = 0; i < num; i++ )
			setWidget ( row, i, ( Widget ) contents.get ( i ) );
	}

	public void removeRow ( int index ) {
		super.removeRow ( index );
		checkEmpty ();
	}

	private void checkEmpty () {
		if ( getRowCount () <= 1 ) {
			hasContents = false;
			setWidget ( 0, 0, new Label ( emptyString ) );
		}
	}
}
