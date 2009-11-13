/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.i18n.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class PriceBox extends TextBox implements FloatWidget {
	private float			currentValue;
	private NumberFormat		plainFormatter;
	private NumberFormat		priceFormatter;

	public PriceBox () {
		setText ( Utils.priceToString ( 0 ) );
		currentValue = 0;

		plainFormatter = NumberFormat.getDecimalFormat ();
		priceFormatter = NumberFormat.getCurrencyFormat ();

		addFocusListener (
			new FocusListener () {
				public void onFocus ( Widget sender ) {
					setText ( plainFormatter.format ( currentValue ) );
				}

				public void onLostFocus ( Widget sender ) {
					float val;
					String converted;

					if ( getText ().equals ( "" ) ) {
						setText ( Utils.priceToString ( 0 ) );
					}
					else {
						try {
							val = ( float ) plainFormatter.parse ( getText () );
							converted = priceFormatter.format ( val );
							setText ( converted );
							currentValue = ( float ) priceFormatter.parse ( converted );
						}
						catch ( NumberFormatException e ) {
							setText ( priceFormatter.format ( currentValue ) );
						}
					}
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
							( keyCode != KeyboardListener.KEY_DOWN ) &&
							( keyCode != ',' ) ) {

						cancelKey ();
					}
				}
			}
		);

		setVisibleLength ( 6 );
	}

	/****************************************************************** FloatWidget */

	public void setVal ( float value ) {
		currentValue = value;
		setText ( priceFormatter.format ( value ) );
	}

	public float getVal () {
		return currentValue;
	}
}
