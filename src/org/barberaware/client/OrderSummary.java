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

import com.allen_sauer.gwt.log.client.Log;

public class OrderSummary extends Composite {
	private Order			currentOrder;
	private FlexTable		main;
	private PriceViewer		totalLabel;
	private ArrayList		ordersUsers;

	public OrderSummary ( Order order ) {
		currentOrder = order;

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		initWidget ( main );

		main.setWidget ( 0, 1, new Label ( "Prodotto" ) );
		main.setWidget ( 0, 2, new Label ( "Quantità Ordinata" ) );
		main.setWidget ( 0, 3, new Label ( "Prezzo Totale" ) );
		main.setWidget ( 0, 4, new Label ( "Notifiche" ) );

		main.getRowFormatter ().setStyleName ( 0, "table-header" );

		/*
			La prima colonna e' ad uso e consumo interno e non mostra alcuna
			informazione utile, dunque viene nascosta
		*/
		main.getColumnFormatter ().setStyleName ( 0, "hidden" );

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
		float total_price;

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
		if ( cached_orders == null )
			return;

		total_price = 0;

		for ( int i = 0; i < cached_orders.size (); i++ ) {
			user_ord = ( OrderUser ) cached_orders.get ( i );

			if ( user_ord.getObject ( "baseorder" ).getLocalID () == my_id ) {
				total_price += user_ord.getTotalPrice ();
				user_products = user_ord.getArray ( "products" );

				for ( int a = 0; a < user_products.size (); a++ ) {
					user_product = ( ProductUser ) user_products.get ( a );

					order_product = ( Product ) user_product.getObject ( "product" );
					if ( order_product.getBool ( "available" ) == false )
						continue;

					user_product_ref = order_product.getLocalID ();

					for ( int e = 0; e < products.size (); e++ ) {
						order_product = ( Product ) products.get ( e );

						if ( user_product_ref == order_product.getLocalID () ) {
							if ( order_product.getBool ( "available" ) == true ) {
								quantities [ e ] = quantities [ e ] + user_product.getFloat ( "quantity" );
								prices [ e ] = prices [ e ] + user_product.getTotalPrice ();
							}

							break;
						}
					}
				}

				ordersUsers.add ( user_ord );
			}
		}

		syncTable ( products, quantities, prices );
		totalLabel.setValue ( total_price );
	}

	private void syncTable ( ArrayList products, float [] quantities, float [] prices ) {
		int i;
		int e;
		int current_children;
		boolean new_row;
		Hidden product_id;
		Product product;

		current_children = main.getRowCount () - 2;

		for ( i = 0, e = 1; i < products.size (); i++ ) {
			product = ( Product ) products.get ( i );

			if ( product.getBool ( "available" ) == false )
				continue;

			new_row = false;

			if ( e >= current_children ) {
				new_row = true;
			}
			else {
				product_id = ( Hidden ) main.getWidget ( e, 0 );
				if ( Integer.toString ( product.getLocalID () ) != product_id.getName () )
					new_row = true;
			}

			if ( new_row == true )
				main.insertRow ( e );

			setDataRow ( e, product, quantities [ i ], prices [ i ], new_row );
			e++;
		}
	}

	private void addManualAdjustIcon ( int row, Product prod ) {
		RefineProductDialog cell;

		cell = new RefineProductDialog ( ordersUsers, prod );
		cell.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				syncOrders ();
			}
		} );

		main.setWidget ( row, 4, cell );
	}

	private String measureSymbol ( Product prod ) {
		Measure measure;

		measure = ( Measure ) prod.getObject ( "measure" );
		if ( measure != null )
			return measure.getString ( "symbol" );
		else
			return "";
	}

	private Label editableLabel ( int row, int col, boolean new_row ) {
		Label lab;

		if ( new_row == false ) {
			lab = ( Label ) main.getWidget ( row, col );
		}
		else {
			lab = new Label ();
			main.setWidget ( row, col, lab );
		}

		return lab;
	}

	private void setDataRow ( int index, Product product, float quantity, float price, boolean new_row ) {
		float stock;
		Label lab;

		if ( new_row == true )
			main.setWidget ( index, 0, new Hidden ( Integer.toString ( product.getLocalID () ) ) );

		lab = editableLabel ( index, 1, new_row );
		lab.setText ( product.getString ( "name" ) );

		lab = editableLabel ( index, 2, new_row );
		lab.setText ( quantity + " " + measureSymbol ( product ) );

		lab = editableLabel ( index, 3, new_row );
		lab.setText ( Utils.priceToString ( price ) + " €" );

		stock = product.getFloat ( "stock_size" );
		if ( ( stock != 0 ) && ( quantity != 0 ) && ( quantity % stock != 0 ) )
			addManualAdjustIcon ( index, product );
	}

	private void fillList () {
		int i;
		int e;
		ArrayList products;
		Product prod;

		products = currentOrder.getArray ( "products" );
		if ( products == null )
			return;

		for ( i = 0, e = 1; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );

			if ( prod.getBool ( "available" ) == false )
				continue;

			setDataRow ( e, prod, 0, 0, true );
			e++;
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( e, 0, 5 );

		e++;

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalLabel.setStyleName ( "bigger-text" );
		}
		else
			totalLabel.setValue ( 0 );

		main.setWidget ( e, 3, totalLabel );
	}
}
