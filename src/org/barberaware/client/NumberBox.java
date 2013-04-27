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
import java.lang.*;
import com.google.gwt.user.client.ui.*;

public class NumberBox extends TextBox implements IntNumericWidget {
	public NumberBox () {
		setText ( "0" );

		addFocusListener (
			new FocusListener () {
				public void onFocus ( Widget sender ) {
					/* dummy */
				}

				public void onLostFocus ( Widget sender ) {
					if ( getText ().equals ( "" ) )
						setText ( "0" );
				}
			}
		);

		addKeyboardListener (
			new KeyboardListenerAdapter () {
				public void onKeyPress ( Widget sender, char keyCode, int modifiers ) {
					if ( ( !Character.isDigit ( keyCode ) ) &&
							( keyCode != KeyboardListener.KEY_TAB ) &&
							( keyCode != KeyboardListener.KEY_BACKSPACE ) &&
							( keyCode != KeyboardListener.KEY_LEFT ) &&
							( keyCode != KeyboardListener.KEY_UP ) &&
							( keyCode != KeyboardListener.KEY_RIGHT ) &&
							( keyCode != KeyboardListener.KEY_DOWN ) ) {

						cancelKey ();
					}
				}
			}
		);
	}

	public void setVal ( int value ) {
		setText ( Integer.toString ( value ) );
	}

	public int getVal () {
		String val;

		val = getText ();

		if ( val == "" )
			return 0;
		else
			return Integer.parseInt ( getText () );
	}
}
