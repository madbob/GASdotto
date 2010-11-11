/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public class LongStringLabel extends HTML implements StringWidget {
	private String			defaultString;
	private String			currentValue;
	private boolean			defaultSet;

	public LongStringLabel () {
		defaultString = "Non è stato settato alcun contenuto";
		defaultSet = true;
		currentValue = null;
	}

	public void setDefault ( String def ) {
		defaultString = def;
	}

	public void setValue ( String value ) {
		if ( value != null && value.equals ( "" ) == false ) {
			defaultSet = false;
			setHTML ( "<p>" + value + "</p>" );
			currentValue = value;
		}
		else {
			defaultSet = true;
			setText ( "<p>" + defaultString + "</p>" );
		}
	}

	public String getValue () {
		if ( defaultSet )
			return "";
		else
			return currentValue;
	}
}
