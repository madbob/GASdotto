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

/*
	Questo e' fondamentalmente come FromServerSelector, ma opera sugli ID degli oggetti
	anziche' sui FromServer interi
*/

public class FromServerIDSelector extends FromServerSelector implements IntNumericWidget {
	public FromServerIDSelector ( String t, boolean hide_void, boolean sort ) {
		super ( t, hide_void, sort );
	}

	/****************************************************************** IntNumericWidget */

	public void setVal ( int value ) {
		FromServer obj;

		obj = Utils.getServer ().getObjectFromCache ( getType (), value );
		setValue ( obj );
	}

	public int getVal () {
		FromServer obj;

		obj = getValue ();

		if ( obj != null )
			return obj.getLocalID ();
		else
			return -1;
	}
}
