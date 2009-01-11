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

public class FromServerFactory {
	public static FromServer create ( String name ) {
		name = Utils.classFinalName ( name );

		if ( name.equals ( "Category" ) )
			return new Category ();
		else if ( name.equals ( "GAS" ) )
			return new GAS ();
		else if ( name.equals ( "Measure" ) )
			return new Measure ();
		else if ( name.equals ( "Notification" ) )
			return new Notification ();
		else if ( name.equals ( "Order" ) )
			return new Order ();
		else if ( name.equals ( "OrderStatus" ) )
			return new OrderStatus ();
		else if ( name.equals ( "OrderUser" ) )
			return new OrderUser ();
		else if ( name.equals ( "Product" ) )
			return new Product ();
		else if ( name.equals ( "ProductUser" ) )
			return new ProductUser ();
		else if ( name.equals ( "Supplier" ) )
			return new Supplier ();
		else if ( name.equals ( "User" ) )
			return new User ();
		else
			return null;
	}
}
