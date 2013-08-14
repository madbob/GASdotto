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
import com.google.gwt.event.dom.client.*;

public class NumberBox extends TextBox implements IntNumericWidget {
	public NumberBox () {
		setText ( "0" );

		addBlurHandler ( new BlurHandler () {
			public void onBlur ( BlurEvent event ) {
				if ( getText ().equals ( "" ) )
					setText ( "0" );
			}
		} );

		addKeyDownHandler ( new KeyDownHandler () {
			public void onKeyDown ( KeyDownEvent event ) {
				int keycode;

				keycode = event.getNativeKeyCode();

				if ( ( keycode < 48 || keycode > 57 ) && (
						( keycode != (char) KeyCodes.KEY_TAB ) &&
						( keycode != (char) KeyCodes.KEY_BACKSPACE ) &&
						( keycode != (char) KeyCodes.KEY_LEFT ) &&
						( keycode != (char) KeyCodes.KEY_UP ) &&
						( keycode != (char) KeyCodes.KEY_RIGHT ) &&
						( keycode != (char) KeyCodes.KEY_DOWN ) )
					) {

					event.preventDefault ();
					event.stopPropagation ();
				}
			}
		} );
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
