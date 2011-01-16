/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class PlainFillBox extends CaptionPanel {
	private FlexTable		main;
	private boolean			hasContents;
	private String			emptyString;

	public PlainFillBox () {
		super ();

		main = new FlexTable ();
		add ( main );

		hasContents = false;
		emptyString = "";
	}

	public void setStrings ( String title, String empty ) {
		setCaptionText ( title );
		emptyString = empty;
		checkEmpty ();
	}

	public int addRow ( ArrayList contents ) {
		int row;
		int num;

		if ( hasContents == false ) {
			main.removeRow ( 0 );
			hasContents = true;
		}

		num = contents.size ();
		row = main.getRowCount ();

		for ( int i = 0; i < num; i++ )
			main.setWidget ( row, i, ( Widget ) contents.get ( i ) );

		return row;
	}

	public void removeRow ( int index ) {
		main.removeRow ( index );
		checkEmpty ();
	}

	public FlexTable getTable () {
		return main;
	}

	public boolean isEmpty () {
		return ( hasContents == false );
	}

	private void checkEmpty () {
		if ( hasContents == false || ( hasContents == true && main.getRowCount () == 0 ) ) {
			hasContents = false;
			main.setWidget ( 0, 0, new Label ( emptyString ) );
		}
	}
}
