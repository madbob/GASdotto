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

import com.google.gwt.user.client.ui.*;

/*
	L'unica utilita' di questo widget e' wrappare una semplice TextArea in una interfaccia
	StringWidget
*/

public class DummyTextArea extends TextArea implements StringWidget {
	public DummyTextArea () {
		setStyleName ( "dummy-text-area" );
		setVisibleLines ( 3 );
		setCharacterWidth ( 40 );
	}

	public void setValue ( String value ) {
		setText ( value );
	}

	public String getValue () {
		return getText ();
	}
}
