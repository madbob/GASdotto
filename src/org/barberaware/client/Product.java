/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		addAttribute ( "code", FromServer.STRING );
		addAttribute ( "category", FromServer.OBJECT, Category.class );
		addAttribute ( "supplier", FromServer.OBJECT, Supplier.class );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "unit_price", FromServer.PRICE );
		addAttribute ( "surplus", FromServer.PERCENTAGE );
		addAttribute ( "shipping_price", FromServer.PRICE );
		addAttribute ( "measure", FromServer.OBJECT, Measure.class );
		addAttribute ( "minimum_order", FromServer.FLOAT );
		addAttribute ( "multiple_order", FromServer.FLOAT );
		addAttribute ( "stock_size", FromServer.FLOAT );
		addAttribute ( "unit_size", FromServer.FLOAT );
		addAttribute ( "mutable_price", FromServer.BOOLEAN );
		addAttribute ( "available", FromServer.BOOLEAN );
		addAttribute ( "variants", FromServer.ARRAY, ProductVariant.class );
		addAttribute ( "archived", FromServer.BOOLEAN );
		addAttribute ( "previous_description", FromServer.INTEGER );

		setString ( "name", "Nuovo Prodotto" );
		setBool ( "available", true );

		/*
			Il ricaricamento serve per pescare gli ID delle eventuali varianti che
			sono state assegnate ad un nuovo prodotto
		*/
		alwaysReload ( true );
	}

	public float getTotalPrice () {
		float tot;

		tot = getFloat ( "unit_price" );
		tot += getFloat ( "shipping_price" );
		tot += Utils.sumPercentage ( tot, getString ( "surplus" ) );
		return tot;
	}
}
