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

public class Product extends FromServer {
	public Product () {
		super ();

		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "category", FromServer.OBJECT, Category.class );
		addAttribute ( "supplier", FromServer.OBJECT, Supplier.class );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "unit_price", FromServer.FLOAT );
		addAttribute ( "surplus", FromServer.PERCENTAGE );
		addAttribute ( "shipping_price", FromServer.FLOAT );
		addAttribute ( "measure", FromServer.OBJECT, Measure.class );
		addAttribute ( "minimum_order", FromServer.INTEGER );
		addAttribute ( "multiple_order", FromServer.INTEGER );
		addAttribute ( "stock_size", FromServer.INTEGER );
		addAttribute ( "mutable_price", FromServer.BOOLEAN );
		addAttribute ( "available", FromServer.BOOLEAN );

		setString ( "name", "Nuovo Prodotto" );
	}

	public float getTotalPrice () {
		float tot;
		float price;

		tot = getFloat ( "unit_price" );

		price = getFloat ( "shipping_price" );
		if ( price != 0 )
			tot += price;

		/**
			TODO	Gestire anche il surplus, che e' una percentuale
		*/

		return tot;
	}
}
