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

import com.allen_sauer.gwt.log.client.Log;

/*
	                                                                                  +-------------+
	                                                                                  | ProductUser |
	                                                                                  +-------------+

	                                +--------------------+                                                                      +--------------------+
	                                | ProductUserVariant |                                                                      | ProductUserVariant |
	                                +--------------------+                                                                      +--------------------+

	     +-----------------------------+               +-----------------------------+               +-----------------------------+               +-----------------------------+
	     | ProductUserVariantComponent |               | ProductUserVariantComponent |               | ProductUserVariantComponent |               | ProductUserVariantComponent |
	     +-----------------------------+               +-----------------------------+               +-----------------------------+               +-----------------------------+

	+----------------+ +---------------------+   +----------------+ +---------------------+    +----------------+ +---------------------+   +----------------+ +---------------------+
	| ProductVariant | | ProductVariantValue |   | ProductVariant | | ProductVariantValue |    | ProductVariant | | ProductVariantValue |   | ProductVariant | | ProductVariantValue |
	+----------------+ +---------------------+   +----------------+ +---------------------+    +----------------+ +---------------------+   +----------------+ +---------------------+

	Ogni ProductUser puo' contenere piu' ProductUserVariant, ognuno dei quali rappresentano una specifica
	variante ordinata (ad esempio: "scarpa taglia 45 colore rosso", "scarpa taglia 37 colore nero"...). Il
	ProductUserVariant contiene piu' ProductUserVariantComponent, uno per ogni attributo assegnato al prodotto
	ordinato (ad esempio: "taglia 47"). Ogni ProductUserVariantComponent contiene il riferimento esatto al tipo
	di variante ("taglia") ed al valore scelto dall'utente ("47")
*/

public class ProductUser extends FromServer {
	public ProductUser () {
		super ();
		addAttribute ( "product", FromServer.OBJECT, Product.class );
		addAttribute ( "variants", FromServer.ARRAY, ProductUserVariant.class );
		addAttribute ( "quantity", FromServer.FLOAT );
		addAttribute ( "delivered", FromServer.FLOAT );
		addAttribute ( "orderdate", FromServer.DATE );
		addAttribute ( "orderperson", FromServer.OBJECT, User.class );
	}

	public void setCurrentUser () {
		setDate ( "orderdate", new Date ( System.currentTimeMillis () ) );
		setObject ( "orderperson", Session.getUser () );
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

	public float getSurplus () {
		float price;
		float tot;
		Product product;

		product = ( Product ) getObject ( "product" );
		price = product.getFloat ( "unit_price" );
		tot = Utils.sumPercentage ( price, product.getString ( "surplus" ) );
		return tot * getFloat ( "quantity" );
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

			if ( Utils.compareFromServerArray ( f.getArray ( "variants" ), s.getArray ( "variants" ) ) == false )
				return 1;
		}

		return ret;
	}

	public static float sumProductUserArray ( ArrayList products, String param ) {
		float total;
		ProductUser prod;

		total = 0;

		for ( int i = 0; i < products.size (); i++ ) {
			prod = ( ProductUser ) products.get ( i );

			if ( prod.getObject ( "product" ).getBool ( "available" ) == false )
				continue;

			total += prod.getTotalPrice ( prod.getFloat ( param ) );
		}

		return total;
	}
}
