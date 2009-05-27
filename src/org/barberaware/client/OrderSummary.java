/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;

public class OrderSummary extends Composite {
	private Order			currentOrder;
	private FlexTable		main;
	private PriceViewer		totalLabel;

	public OrderSummary ( Order order ) {
		currentOrder = order;

		/**
			TODO	Aggiungere intestazioni colonne
		*/

		main = new FlexTable ();
		main.setCellPadding ( 5 );
		main.setCellSpacing ( 5 );
		initWidget ( main );

		totalLabel = null;

		fillList ();
		syncOrders ();
	}

	public void reFill ( Order order ) {
		currentOrder = order;

		for ( int i = 0; i < main.getRowCount (); i++ )
			main.removeRow ( i );

		fillList ();
		syncOrders ();
	}

	public void syncOrders () {
		int my_id;
		ArrayList products;
		float [] quantities;
		ArrayList cached_orders;
		OrderUser user_ord;
		ArrayList user_products;
		Product order_product;
		int user_product_ref;
		ProductUser user_product;
		Label product_quantity_sum;
		float total_price;
		int stock;

		my_id = currentOrder.getLocalID ();
		products = currentOrder.getArray ( "products" );

		quantities = new float [ products.size () ];
		for ( int i = 0; i < products.size (); i++ )
			quantities [ i ] = 0;

		/*
			Probabilmente l'algoritmo di ricostruzione della lista di quantita' puo'
			essere assai migliorato...
		*/
		cached_orders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		total_price = 0;

		for ( int i = 0; i < cached_orders.size (); i++ ) {
			user_ord = ( OrderUser ) cached_orders.get ( i );

			if ( user_ord.getObject ( "baseorder" ).getLocalID () == my_id ) {
				total_price += user_ord.getTotalPrice ();
				user_products = user_ord.getArray ( "products" );

				for ( int a = 0; a < user_products.size (); a++ ) {
					user_product = ( ProductUser ) user_products.get ( a );
					user_product_ref = user_product.getObject ( "product" ).getLocalID ();

					for ( int e = 0; e < products.size (); e++ ) {
						order_product = ( Product ) products.get ( e );

						if ( user_product_ref == order_product.getLocalID () ) {
							quantities [ e ] = quantities [ e ] + user_product.getFloat ( "quantity" );
							break;
						}
					}
				}
			}
		}

		/**
			TODO	Aggiungere nella tabella anche totale prezzo per ogni prodotto e
				numero utenti ordinanti
		*/

		for ( int i = 0; i < products.size (); i++ ) {
			order_product = ( Product ) products.get ( i );
			product_quantity_sum = ( Label ) main.getWidget ( i, 3 );
			product_quantity_sum.setText ( quantities [ i ] + " " + measureSymbol ( order_product ) );

			stock = order_product.getInt ( "stock_size" );
			if ( ( stock != 0 ) && ( quantities [ i ] != 0 ) && ( quantities [ i ] / stock != 0 ) )
				main.setWidget ( i, 4, new Image ( "images/info-warning.png" ) );
		}

		totalLabel.setValue ( total_price );
	}

	private String measureSymbol ( Product prod ) {
		Measure measure;

		measure = ( Measure ) prod.getObject ( "measure" );
		if ( measure != null )
			return measure.getString ( "symbol" );
		else
			return "";
	}

	private void fillList () {
		int i;
		ArrayList products;
		Product prod;
		String measure;

		products = currentOrder.getArray ( "products" );

		for ( i = 0; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );
			measure = measureSymbol ( prod );

			main.setWidget ( i, 0, new Hidden ( Integer.toString ( prod.getLocalID () ) ) );
			main.setWidget ( i, 1, new Label ( prod.getString ( "name" ) ) );
			main.setWidget ( i, 2, new Label ( prod.getTotalPrice () + " € / " + measure ) );
			main.setWidget ( i, 3, new Label ( "0 " + measure ) );
		}

		main.setWidget ( i, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( i, 0, 4 );
		i++;

		if ( totalLabel == null )
			totalLabel = new PriceViewer ();
		else
			totalLabel.setValue ( 0 );

		main.setWidget ( i, 2, totalLabel );
	}
}
