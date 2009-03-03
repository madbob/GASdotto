/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class Order extends FromServer {
	public static int	OPENED		= 0;
	public static int	CLOSED		= 1;
	public static int	SUSPENDED	= 2;

	public Order () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new StringFromObjectClosure () {
			public String retrive ( FromServer obj ) {
				Supplier supplier;
				String sup;
				String start;
				String end;
				User current;

				supplier = ( Supplier ) obj.getObject ( "supplier" );
				if ( supplier == null )
					return "Nuovo Ordine";

				current = Session.getUser ();
				sup = supplier.getString ( "name" );

				/*
					Si assume che all'utente comune non importi la data di
					apertura dell'ordine ma solo entro quando puo' avanzare
					la richiesta, dunque e' inutile visualizzare informazioni
					ridondanti
				*/
				if ( current.getInt ( "privileges" ) == User.USER_COMMON ) {
					end = Utils.printableDate ( obj.getDate ( "enddate" ) );
					return sup + " (fino al " + end + ")";
				}
				else {
					start = Utils.printableDate ( obj.getDate ( "startdate" ) );
					end = Utils.printableDate ( obj.getDate ( "enddate" ) );
					return sup + " (" + start + " / " + end + ")";
				}
			}
		} );

		addAttribute ( "supplier", FromServer.OBJECT, Supplier.class );
		addAttribute ( "products", FromServer.ARRAY, Product.class );
		addAttribute ( "startdate", FromServer.DATE );
		addAttribute ( "enddate", FromServer.DATE );
		addAttribute ( "status", FromServer.INTEGER );
		addAttribute ( "shippingdate", FromServer.DATE );
		addAttribute ( "nextdate", FromServer.STRING );
		addAttribute ( "anticipated", FromServer.PERCENTAGE );

		setDate ( "startdate", new Date ( System.currentTimeMillis () ) );
		setInt ( "status", OPENED );
	}
}
