/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class BooleanSelector extends ToggleButton implements BooleanWidget {
	public BooleanSelector () {
		super ( "NO", "SI" );
		setStyleName ( "boolean-selector" );
	}

	/****************************************************************** BooleanWidget */

	public void setVal ( boolean value ) {
		super.setDown ( value );
	}

	public boolean getVal () {
		return isDown ();
	}
}
