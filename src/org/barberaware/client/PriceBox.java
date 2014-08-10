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

import com.allen_sauer.gwt.log.client.Log;

public class PriceBox extends TextBox implements FloatWidget {
	private float			currentValue;
	private boolean			negative;

	public PriceBox () {
		setText ( Utils.priceToString ( 0 ) );
		currentValue = 0;
		negative = false;

		addFocusHandler ( new FocusHandler () {
			public void onFocus ( FocusEvent event ) {
				setText ( Utils.floatToString ( currentValue ) );
			}
		} );

		addBlurHandler ( new BlurHandler () {
			public void onBlur ( BlurEvent event ) {
				float val;
				String converted;

				if ( getText ().equals ( "" ) ) {
					setText ( Utils.priceToString ( 0 ) );
				}
				else {
					try {
						/*
							La doppia conversione e' per accertarsi che in
							currentValue ci sia sempre il valore correttamente
							arrotondato
						*/
						val = Utils.stringToFloat ( getText () );
						converted = Utils.priceToString ( val );
						setText ( converted );
						currentValue = Utils.stringToPrice ( converted );
					}
					catch ( NumberFormatException e ) {
						setText ( Utils.priceToString ( currentValue ) );
					}
				}
			}
		} );

		addKeyDownHandler ( new KeyDownHandler () {
			public void onKeyDown ( KeyDownEvent event ) {
				int keycode;

				keycode = event.getNativeKeyCode();

				if ( negative == true ) {
					/*
						Il pulsante '-' viene mappato in modi diversi su Firefox e Chrome, non
						ho trovato modo per uniformare questi codici (neanche chiedendo la
						stringa nativamente con getCodeAt())
					*/
					if ( keycode == 45 || keycode == 189 )
						return;
				}

				if ( ( keycode < 48 || keycode > 57 ) && (
						( keycode != (char) KeyCodes.KEY_TAB ) &&
						( keycode != (char) KeyCodes.KEY_BACKSPACE ) &&
						( keycode != (char) KeyCodes.KEY_LEFT ) &&
						( keycode != (char) KeyCodes.KEY_UP ) &&
						( keycode != (char) KeyCodes.KEY_RIGHT ) &&
						( keycode != (char) KeyCodes.KEY_DOWN ) &&
						( keycode != 188 ) )
					) {

					event.preventDefault ();
					event.stopPropagation ();
				}
			}
		} );

		setVisibleLength ( 6 );
	}

	public void acceptsNegative ( boolean accepts ) {
		negative = accepts;
	}

	/****************************************************************** FloatWidget */

	public void setVal ( float value ) {
		currentValue = value;
		setText ( Utils.priceToString ( value ) );
	}

	public float getVal () {
		return currentValue;
	}
}
