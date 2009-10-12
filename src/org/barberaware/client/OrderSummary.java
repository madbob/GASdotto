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
	private ArrayList		ordersUsers;

	public OrderSummary ( Order order ) {
		HTMLTable.RowFormatter formatter;

		currentOrder = order;

		main = new FlexTable ();
		main.setCellPadding ( 5 );
		main.setCellSpacing ( 5 );
		initWidget ( main );

		/**
			TODO	Nella colonna 0 della tabella ci stanno gli elementi nascosti con
				gli ID dei prodotti, ma comunque mi resta la casellina nella
				prima riga vuota; sarebbe carino inventarsi qualcosa per non
				visualizzarla, anche se probabilmente c'e' da rivedere la
				formattazione della tabella intera
		*/

		main.setWidget ( 0, 1, new Label ( "Prodotto" ) );
		main.setWidget ( 0, 2, new Label ( "Prezzo Unitario" ) );
		main.setWidget ( 0, 3, new Label ( "Quantità Ordinata" ) );
		main.setWidget ( 0, 4, new Label ( "Prezzo Totale" ) );
		main.setWidget ( 0, 5, new Label ( "Notifiche" ) );

		formatter = main.getRowFormatter ();
		formatter.addStyleName ( 0, "table-header" );

		totalLabel = null;

		/*
			Questo viene creato e riempito in syncOrders()
		*/
		ordersUsers = null;

		fillList ();
		syncOrders ();
	}

	public void reFill ( Order order ) {
		int num;

		currentOrder = order;
		num = main.getRowCount ();

		for ( int i = 1; i < num; i++ )
			main.removeRow ( 1 );

		fillList ();
		syncOrders ();
	}

	public void syncOrders () {
		int my_id;
		ArrayList products;
		float [] quantities;
		float [] prices;
		ArrayList cached_orders;
		OrderUser user_ord;
		ArrayList user_products;
		Product order_product;
		int user_product_ref;
		ProductUser user_product;
		Label product_quantity_sum;
		float total_price;
		float stock;

		my_id = currentOrder.getLocalID ();
		products = currentOrder.getArray ( "products" );

		quantities = new float [ products.size () ];
		prices = new float [ products.size () ];
		for ( int i = 0; i < products.size (); i++ ) {
			quantities [ i ] = 0;
			prices [ i ] = 0;
		}

		if ( ordersUsers == null )
			ordersUsers = new ArrayList ();

		/**
			TODO	Probabilmente l'algoritmo di ricostruzione della lista di
				quantita' puo' essere assai migliorato...
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
							prices [ e ] = prices [ e ] + user_product.getTotalPrice ();
							break;
						}
					}
				}

				ordersUsers.add ( user_ord );
			}
		}

		for ( int i = 0, e = 1; i < products.size (); i++, e++ ) {
			order_product = ( Product ) products.get ( i );

			product_quantity_sum = ( Label ) main.getWidget ( e, 3 );
			product_quantity_sum.setText ( quantities [ i ] + " " + measureSymbol ( order_product ) );

			product_quantity_sum = ( Label ) main.getWidget ( e, 4 );
			product_quantity_sum.setText ( Utils.priceToString ( prices [ i ] ) + " €" );

			stock = order_product.getFloat ( "stock_size" );
			if ( ( stock != 0 ) && ( quantities [ i ] != 0 ) && ( quantities [ i ] % stock != 0 ) )
				addManualAdjustIcon ( i, order_product );
		}

		totalLabel.setValue ( total_price );
	}

	private void addManualAdjustIcon ( int row, Product prod ) {
		RefineProductDialog cell;

		cell = new RefineProductDialog ( ordersUsers, prod );
		cell.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				syncOrders ();
			}
		} );

		main.setWidget ( row, 5, cell );
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
		int e;
		ArrayList products;
		Product prod;
		String measure;

		products = currentOrder.getArray ( "products" );

		if ( products == null )
			return;

		for ( i = 0, e = 1; i < products.size (); i++, e++ ) {
			prod = ( Product ) products.get ( i );
			measure = measureSymbol ( prod );

			main.setWidget ( e, 0, new Hidden ( Integer.toString ( prod.getLocalID () ) ) );
			main.setWidget ( e, 1, new Label ( prod.getString ( "name" ) ) );
			main.setWidget ( e, 2, new Label ( Utils.priceToString ( prod.getTotalPrice () ) + " € / " + measure ) );
			main.setWidget ( e, 3, new Label ( "0 " + measure ) );
			main.setWidget ( e, 4, new Label ( "0.00 €" ) );
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( e, 0, 6 );

		e++;

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalLabel.setStyleName ( "bigger-text" );
		}
		else
			totalLabel.setValue ( 0 );

		main.setWidget ( e, 4, totalLabel );
	}
}
