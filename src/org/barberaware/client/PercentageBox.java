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

public class PercentageBox extends TextBox implements PercentageWidget {
	private boolean		valid;

	public PercentageBox () {
		valid = true;
		setText ( "0" );

		addBlurHandler ( new BlurHandler () {
			public void onBlur ( BlurEvent event ) {
				String text;

				text = getText ();
				valid = true;

				if ( text.equals ( "" ) )
					setText ( "0" );

				else {
					int len;

					len = text.length ();

					for ( int i = 0; i < len; i++ )
						if ( text.charAt ( i ) == '%' ) {
							if ( i != len - 1 ) {
								Utils.showNotification ( "Valore non valido" );
								valid = false;
							}

							break;
						}
				}
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
						( keycode != (char) KeyCodes.KEY_DOWN ) &&
						( keycode != 190 ) &&
						( keycode != 37 ) )
					) {

					event.preventDefault ();
					event.stopPropagation ();
				}
			}
		} );

		setVisibleLength ( 6 );
	}

	/****************************************************************** PercentageWidget */

	public void setValue ( String value ) {
		if ( value.length () == 0 )
			setText ( "0" );
		else
			setText ( value );
	}

	public String getValue () {
		if ( valid == true )
			return getText ();
		else
			return "0";
	}


}
