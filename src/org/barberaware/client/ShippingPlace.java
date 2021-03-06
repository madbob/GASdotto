/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class ShippingPlace extends FromServer {
	public ShippingPlace () {
		super ();
		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "address", FromServer.ADDRESS );
		addAttribute ( "is_default", FromServer.BOOLEAN );

		setString ( "name", "Nuovo Luogo di Consegna" );
	}

	public static FromServer getDefault () {
		ArrayList places;
		FromServer place;

		places = Utils.getServer ().getObjectsFromCache ( "ShippingPlace" );

		for ( int i = 0; i < places.size (); i++ ) {
			place = ( FromServer ) places.get ( i );
			if ( place.getBool ( "is_default" ) == true )
				return place;
		}

		return null;
	}
}
