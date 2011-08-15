/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import java.lang.*;
import com.google.gwt.user.client.ui.*;

public class PercentageViewer extends Label implements PercentageWidget {
	public PercentageViewer () {
		setText ( "0" );
	}

	/****************************************************************** PercentageWidget */

	public void setValue ( String value ) {
		if ( value == null || value.length () == 0 )
			setText ( "0" );
		else
			setText ( value );
	}

	public String getValue () {
		return getText ();
	}
}
