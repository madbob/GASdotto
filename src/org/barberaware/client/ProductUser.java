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

import java.util.*;

public class ProductUser extends FromServer {
	public ProductUser () {
		super ();
		addAttribute ( "product", FromServer.OBJECT, Product.class );
		addAttribute ( "quantity", FromServer.FLOAT );
		addAttribute ( "delivered", FromServer.FLOAT );
	}

	public float getTotalPrice ( float quantity ) {
		Product product;

		product = ( Product ) getObject ( "product" );
		return quantity * product.getTotalPrice ();
	}

	public float getTotalPrice () {
		float quantity;

		quantity = getFloat ( "quantity" );
		return getTotalPrice ( quantity );
	}

	public float getPrice () {
		float quantity;

		quantity = getFloat ( "quantity" );
		return quantity * getObject ( "product" ).getFloat ( "unit_price" );
	}

	public float getTransportPrice () {
		float quantity;

		quantity = getFloat ( "quantity" );
		return quantity * getObject ( "product" ).getFloat ( "shipping_price" );
	}

	public float getExternalPrice () {
		float quantity;
		float tot;
		FromServer product;

		quantity = getFloat ( "quantity" );
		product = getObject ( "product" );

		tot = product.getFloat ( "shipping_price" );
		tot = Utils.sumPercentage ( tot, product.getString ( "surplus" ) );
		return tot * quantity;
	}

	public int compare ( Object first, Object second ) {
		int ret;

		ret = super.compare ( first, second );

		if ( ret == 0 ) {
			FromServer f;
			FromServer s;
			float quant_first;
			float quant_second;

			f = ( FromServer ) first;
			s = ( FromServer ) second;

			quant_first = f.getFloat ( "quantity" );
			quant_second = s.getFloat ( "quantity" );

			if ( quant_first < quant_second )
				return -1;
			else if ( quant_first > quant_second )
				return 1;

			quant_first = f.getFloat ( "delivered" );
			quant_second = s.getFloat ( "delivered" );

			if ( quant_first < quant_second )
				return -1;
			else if ( quant_first > quant_second )
				return 1;
		}

		return ret;
	}
}
