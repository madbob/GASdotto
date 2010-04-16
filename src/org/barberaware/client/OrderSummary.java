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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderSummary extends Composite {
	private Order			currentOrder;
	private FlexTable		main;
	private PriceViewer		totalLabel;
	private PriceViewer		totalshipLabel;
	private ArrayList		ordersUsers;

	public OrderSummary ( Order order ) {
		currentOrder = order;

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		initWidget ( main );

		main.setWidget ( 0, 1, new Label ( "Prodotto" ) );
		main.setWidget ( 0, 2, new Label ( "Prezzo Unitario" ) );
		main.setWidget ( 0, 3, new Label ( "Trasporto Unitario" ) );
		main.setWidget ( 0, 4, new Label ( "Unità Misura" ) );
		main.setWidget ( 0, 5, new Label ( "Stock" ) );
		main.setWidget ( 0, 6, new Label ( "Quantità Ordinata" ) );
		main.setWidget ( 0, 7, new Label ( "Prezzo Totale" ) );
		main.setWidget ( 0, 8, new Label ( "Prezzo Trasporto" ) );
		main.setWidget ( 0, 9, new Label ( "Quantità Consegnata" ) );
		main.setWidget ( 0, 10, new Label ( "Notifiche" ) );

		main.getRowFormatter ().setStyleName ( 0, "table-header" );

		/*
			La prima colonna e' ad uso e consumo interno e non mostra alcuna
			informazione utile, dunque viene nascosta
		*/
		main.getColumnFormatter ().setStyleName ( 0, "hidden" );

		totalLabel = null;
		totalshipLabel = null;
		ordersUsers = new ArrayList ();

		fillList ();
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
		int user_product_ref;
		float [] quantities;
		float [] delivered;
		float [] prices;
		float [] details;
		float prod_total_price;
		float det;
		float total_price;
		float total_ship_price;
		ArrayList cached_orders;
		ArrayList products;
		ArrayList user_products;
		OrderUser user_ord;
		Product order_product;
		ProductUser user_product;

		my_id = currentOrder.getLocalID ();
		products = currentOrder.getArray ( "products" );

		quantities = new float [ products.size () ];
		delivered = new float [ products.size () ];
		prices = new float [ products.size () ];
		details = new float [ products.size () ];

		for ( int i = 0; i < products.size (); i++ ) {
			quantities [ i ] = 0;
			delivered [ i ] = 0;
			prices [ i ] = 0;
			details [ i ] = 0;
		}

		ordersUsers.clear ();

		/**
			TODO	Probabilmente l'algoritmo di ricostruzione della lista di
				quantita' puo' essere assai migliorato...
		*/
		cached_orders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		if ( cached_orders == null )
			return;

		total_price = 0;
		total_ship_price = 0;

		for ( int i = 0; i < cached_orders.size (); i++ ) {
			user_ord = ( OrderUser ) cached_orders.get ( i );

			if ( user_ord.isValid () == false )
				continue;

			if ( user_ord.getObject ( "baseorder" ).getLocalID () == my_id ) {
				user_products = user_ord.getArray ( "products" );

				for ( int a = 0; a < user_products.size (); a++ ) {
					user_product = ( ProductUser ) user_products.get ( a );
					order_product = ( Product ) user_product.getObject ( "product" );
					user_product_ref = order_product.getLocalID ();

					for ( int e = 0; e < products.size (); e++ ) {
						order_product = ( Product ) products.get ( e );

						if ( user_product_ref == order_product.getLocalID () ) {
							if ( order_product.getBool ( "available" ) == true ) {
								quantities [ e ] = quantities [ e ] + user_product.getFloat ( "quantity" );
								delivered [ e ] = delivered [ e ] + user_product.getFloat ( "delivered" );

								/*
									Il total_price lo ricostruisco prodotto per
									prodotto anziche' ricorrere alla funzione
									getTotalPrice() di OrderUser per evitare di
									reiterare una volta di troppo l'array di
									prodotti
								*/
								prod_total_price = user_product.getPrice ();
								det = user_product.getTransportPrice ();

								prices [ e ] = prices [ e ] + prod_total_price;
								details [ e ] += det;
								total_price += prod_total_price;
								total_ship_price += det;
							}

							break;
						}
					}
				}

				ordersUsers.add ( user_ord );
			}
		}

		syncTable ( products, quantities, delivered, prices, details );
		totalLabel.setVal ( total_price );
		totalshipLabel.setVal ( total_ship_price );
	}

	public void saveContents () {
		int index;
		ArrayList products;
		Product prod;
		PriceBox price_unit;
		PriceBox price_transport;

		products = currentOrder.getArray ( "products" );

		for ( int i = 0; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );
			index = searchProduct ( prod );

			if ( index != -1 ) {
				price_unit = ( PriceBox ) main.getWidget ( index, 2 );
				price_transport = ( PriceBox ) main.getWidget ( index, 3 );

				if ( prod.getFloat ( "unit_price" ) != price_unit.getVal () ||
						prod.getFloat ( "shipping_price" ) != price_transport.getVal () ) {
					prod.setFloat ( "unit_price", price_unit.getVal () );
					prod.setFloat ( "shipping_price", price_transport.getVal () );
					prod.save ( null );
				}
			}
		}
	}

	private int searchProduct ( Product prod ) {
		int current_children;
		String id;
		Hidden existing_id;

		current_children = main.getRowCount () - 2;
		id = Integer.toString ( prod.getLocalID () );

		for ( int i = 1; i < current_children; i++ ) {
			existing_id = ( Hidden ) main.getWidget ( i, 0 );
			if ( id == existing_id.getName () )
				return i;
		}

		return -1;
	}

	private void syncTable ( ArrayList products, float [] quantities, float [] delivered, float [] prices, float [] prices_details ) {
		int i;
		int e;
		boolean new_row;
		Product product;

		for ( i = 0; i < products.size (); i++ ) {
			product = ( Product ) products.get ( i );

			if ( product.getBool ( "available" ) == false )
				continue;

			e = searchProduct ( product );
			if ( e == -1 ) {
				e = main.getRowCount () - 2;
				main.insertRow ( e );
				new_row = true;
			}
			else {
				new_row = false;
			}

			setDataRow ( e, product, quantities [ i ], delivered [ i ], prices [ i ], prices_details [ i ], new_row );
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

		main.setWidget ( row, 10, cell );
	}

	private void removeManualAdjustIcon ( int row ) {
		main.setWidget ( row, 10, new Label () );
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

	private void alignTotalPrice ( int column ) {
		int i;
		int num;
		float price;
		Label lab;

		num = main.getRowCount () - 2;
		price = 0;

		for ( i = 1; i < num; i++ ) {
			lab = ( Label ) main.getWidget ( i, column );
			price += Utils.stringToPrice ( lab.getText () );
		}

		lab = ( Label ) main.getWidget ( i + 1, column );
		lab.setText ( Utils.priceToString ( price ) );
	}

	private void alignRow ( Widget sender, int id ) {
		int num;
		int label_index;
		float price;
		float quantity;
		Widget cmp;
		Label lab;

		num = main.getRowCount () - 2;

		for ( int i = 1; i < num; i++ ) {
			cmp = main.getWidget ( i, id );

			if ( cmp == sender ) {
				price = ( ( PriceBox ) sender ).getVal ();
				lab = ( Label ) main.getWidget ( i, 6 );
				quantity = Float.parseFloat ( lab.getText () );

				switch ( id ) {
					case 2:
						label_index = 7;
						break;

					case 3:
						label_index = 8;
						break;

					default:
						Window.alert ( "Criterio di modifica non gestito" );
						return;
				}

				lab = ( Label ) main.getWidget ( i, label_index );
				lab.setText ( Utils.priceToString ( price * quantity ) );
				alignTotalPrice ( label_index );
				break;
			}
		}
	}

	private void setDataRow ( int index, Product product, float quantity, float delivered, float price, float price_details, boolean new_row ) {
		float stock;
		Label lab;
		PriceBox box;

		stock = product.getFloat ( "stock_size" );

		if ( new_row == true ) {
			main.setWidget ( index, 0, new Hidden ( Integer.toString ( product.getLocalID () ) ) );

			box = new PriceBox ();
			box.setVal ( product.getFloat ( "unit_price" ) );
			box.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
				}

				public void onLostFocus ( Widget sender ) {
					alignRow ( sender, 2 );
				}
			} );
			main.setWidget ( index, 2, box );

			box = new PriceBox ();
			box.setVal ( product.getFloat ( "shipping_price" ) );
			box.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
				}

				public void onLostFocus ( Widget sender ) {
					alignRow ( sender, 3 );
				}
			} );
			main.setWidget ( index, 3, box );

			main.setWidget ( index, 4, new Label ( measureSymbol ( product ) ) );

			if ( stock != 0 )
				main.setWidget ( index, 5, new Label ( Float.toString ( stock ) ) );
		}

		lab = editableLabel ( index, 1, new_row );
		lab.setText ( product.getString ( "name" ) );

		lab = editableLabel ( index, 6, new_row );
		lab.setText ( Utils.floatToString ( quantity ) );

		lab = editableLabel ( index, 7, new_row );
		lab.setText ( Utils.priceToString ( price ) );

		lab = editableLabel ( index, 8, new_row );
		lab.setText ( Utils.priceToString ( price_details ) );

		lab = editableLabel ( index, 9, new_row );
		lab.setText ( Utils.floatToString ( delivered ) );

		if ( ( stock != 0 ) && ( quantity != 0 ) && ( quantity % stock != 0 ) )
			addManualAdjustIcon ( index, product );
		else
			removeManualAdjustIcon ( index );
	}

	private void fillList () {
		int i;
		int e;
		ArrayList products;
		Product prod;

		products = currentOrder.getArray ( "products" );
		if ( products == null )
			return;

		products = Utils.sortArrayByName ( products );

		for ( i = 0, e = 1; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );

			if ( prod.getBool ( "available" ) == false )
				continue;

			setDataRow ( e, prod, 0, 0, 0, 0, true );
			e++;
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( e, 0, 11 );

		e++;

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalshipLabel = new PriceViewer ();
		}
		else {
			totalLabel.setVal ( 0 );
			totalshipLabel.setVal ( 0 );
		}

		main.setWidget ( e, 7, totalLabel );
		main.setWidget ( e, 8, totalshipLabel );
	}
}
