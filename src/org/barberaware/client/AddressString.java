/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class AddressString extends Label implements AddressWidget {
	private Address			currentValue;
	private String			defaultString;
	private boolean			defaultSet;

	public AddressString () {
		defaultString = "Non è stato settato alcun contenuto";
		defaultSet = true;
	}

	public void setDefault ( String def ) {
		defaultString = def;
	}

	public void setValue ( Address value ) {
		String street;
		String cap;
		String city;

		if ( value == null ) {
			defaultSet = true;
			setText ( defaultString );
		}
		else {
			street = value.getStreet ();
			cap = value.getCap ();
			city = value.getCity ();

			if ( street == "" && cap == "" && city == "" ) {
				defaultSet = true;
				setText ( defaultString );
			}
			else {
				defaultSet = false;
				setText ( street + " " + cap + " " + city );
			}
		}

		currentValue = value;
	}

	public Address getValue () {
		return currentValue;
	}
}
