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

import com.google.gwt.user.client.ui.*;

/*
	L'unica utilita' di questo widget e' wrappare una semplice TextBox in una interfaccia
	StringWidget
*/

public class DummyTextBox extends TextBox implements StringWidget {
	public DummyTextBox () {
		setVisibleLength ( 45 );
	}

	public void setValue ( String value ) {
		setText ( value );
	}

	public String getValue () {
		return getText ();
	}
}
