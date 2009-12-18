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

import com.google.gwt.user.client.*;

public class OrderUser extends FromServer {
	public static int	TO_DELIVER		= 0;
	public static int	PARTIAL_DELIVERY	= 1;
	public static int	COMPLETE_DELIVERY	= 2;

	public OrderUser () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new StringFromObjectClosure () {
			public String retrive ( FromServer obj ) {
				return obj.getObject ( "baseorder" ).getString ( "name" );
			}
		} );

		addAttribute ( "baseuser", FromServer.OBJECT, User.class );
		addAttribute ( "baseorder", FromServer.OBJECT, Order.class );
		addAttribute ( "products", FromServer.ARRAY, ProductUser.class );
		addAttribute ( "status", FromServer.INTEGER );

		setInt ( "status", TO_DELIVER );
	}

	/*
		Un OrderUser e' sempre valido, o meglio esiste anche se non e' salvato sul
		server. Questo perche' anche se non viene avanzata nessuna richiesta per un dato
		Order di riferimento comunque l'oggetto e' valido in quanto puo' essere editato
		in un qualsiasi altro momento
	*/
	public boolean isValid () {
		return true;
	}

	public float getTotalPrice () {
		float total;
		ArrayList products;
		ProductUser prod;

		products = getArray ( "products" );
		if ( products == null )
			return 0;

		total = 0;

		for ( int i = 0; i < products.size (); i++ ) {
			prod = ( ProductUser ) products.get ( i );

			if ( prod.getObject ( "product" ).getBool ( "available" ) == false )
				continue;

			total += prod.getTotalPrice ();
		}

		return total;
	}
}
