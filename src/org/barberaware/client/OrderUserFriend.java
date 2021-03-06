/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class OrderUserFriend extends FromServer {
	public OrderUserFriend () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				return obj.getString ( "friendname" );
			}
		} );

		addAttribute ( "friendname", FromServer.STRING );
		addAttribute ( "products", FromServer.ARRAY, ProductUser.class );

		/*
			Questo parametro non viene mappato sul database, e'
			virtuale e viene forzato solo localmente. Cio' accade
			perche' altrimenti si verificherebbe un loop difficile
			da gestire (l'OrderUser contiene l'OrderUserFriend, che
			contiene l'OrderUser...), e del resto si tratterebbe di
			una ridondanza
		*/
		addAttribute ( "parent", FromServer.OBJECT, OrderUser.class );

		alwaysReload ( true );
		alwaysSendObject ( "products", true );
	}

	public float getTotalPrice () {
		ArrayList products;

		products = getArray ( "products" );

		if ( products == null )
			return 0;
		else
			return ProductUser.sumProductUserArray ( products, "quantity" );
	}
}
