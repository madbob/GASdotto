/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class FromServerFactory {
	private static HashMap		dummyInstances		= null;

	public static FromServer create ( String name ) {
		char first;

		/**
			TODO	Questa funzione (e le relative invocazioni) e' da correggere in modo che prenda il
				nome gia' "normalizzato" della class che si desidera. In questo modo si puo' usare
				sempre solo tale formato anziche' due diversi (normalizzato ed esteso)
		*/
		name = Utils.classFinalName ( name );

		first = name.charAt ( 0 );

		if ( first == 'P' ) {
			if ( name.equals ( "ProductUser" ) )
				return new ProductUser ();
			else if ( name.equals ( "Product" ) )
				return new Product ();
			else if ( name.equals ( "Probe" ) )
				return new Probe ();
		}
		else if ( first == 'O' ) {
			if ( name.equals ( "OrderUser" ) )
				return new OrderUser ();
			else if ( name.equals ( "Order" ) )
				return new Order ();
		}
		else if ( first == 'S' ) {
			if ( name.equals ( "Supplier" ) )
				return new Supplier ();
			else if ( name.equals ( "SystemConf" ) )
				return new SystemConf ();
		}
		else if ( first == 'C' ) {
			if ( name.equals ( "Category" ) )
				return new Category ();
			else if ( name.equals ( "CustomFile" ) )
				return new CustomFile ();
		}
		else if ( name.equals ( "User" ) ) {
			return new User ();
		}
		else if ( name.equals ( "Measure" ) ) {
			return new Measure ();
		}
		else if ( name.equals ( "Notification" ) ) {
			return new Notification ();
		}
		else if ( name.equals ( "GAS" ) ) {
			return new GAS ();
		}

		return null;
	}

	public static FromServer dummyInstance ( String name ) {
		FromServer ret;

		if ( dummyInstances == null )
			dummyInstances = new HashMap ();

		ret = ( FromServer ) dummyInstances.get ( name );
		if ( ret == null ) {
			ret = create ( name );
			dummyInstances.put ( name, ret );
		}

		return ret;
	}

	public static ArrayList getClasses () {
		ArrayList names;

		names = new ArrayList ();
		names.add ( "Category" );
		names.add ( "GAS" );
		names.add ( "Measure" );
		names.add ( "Order" );
		names.add ( "OrderUser" );
		names.add ( "Product" );
		names.add ( "ProductUser" );
		names.add ( "Supplier" );
		names.add ( "User" );
		names.add ( "CustomFile" );
		names.add ( "Notification" );
		names.add ( "SystemConf" );
		names.add ( "Probe" );
		return names;
	}
}
