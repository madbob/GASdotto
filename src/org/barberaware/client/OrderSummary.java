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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderSummary extends Composite implements Lockable {
	private boolean			locked;
	private Order			currentOrder;
	private FlexTable		main;
	private PriceViewer		totalLabel;
	private PriceViewer		totalshipLabel;
	private PriceViewer		totalOverpriceLabel;
	private ArrayList		ordersUsers;

	private boolean			hasTransport;
	private boolean			hasSurplus;
	private boolean			hasStock;
	private boolean			hasMaxAvailable;
	private boolean			hasConstraints;

	private int			PRODUCT_NAME_COLUMN		= 1;
	private int			PRODUCT_PRICE_COLUMN		= 2;
	private int			PRODUCT_TRANSPORT_COLUMN	= 3;
	private int			PRODUCT_OVERPRICE_COLUMN	= 4;
	private int			PRODUCT_MEASURE_COLUMN		= 5;
	private int			PRODUCT_STOCK_COLUMN		= 6;
	private int			PRODUCT_AVAILQUANT_COLUMN	= 7;
	private int			PRODUCT_ORDQUANT_COLUMN		= 8;
	private int			PRODUCT_TOTALPRICE_COLUMN	= 9;
	private int			PRODUCT_TOTALTRANSPORT_COLUMN	= 10;
	private int			PRODUCT_TOTALOVERPRICE_COLUMN	= 11;
	private int			PRODUCT_SHIPQUANT_COLUMN	= 12;
	private int			PRODUCT_NOTIFICATIONS_COLUMN	= 13;

	public OrderSummary ( Order order ) {
		currentOrder = order;
		locked = true;

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		initWidget ( main );

		main.setWidget ( 0, PRODUCT_NAME_COLUMN, new Label ( "Prodotto" ) );
		main.setWidget ( 0, PRODUCT_PRICE_COLUMN, new Label ( "Prezzo Unitario" ) );
		main.setWidget ( 0, PRODUCT_TRANSPORT_COLUMN, new Label ( "Trasporto Unitario" ) );
		main.setWidget ( 0, PRODUCT_OVERPRICE_COLUMN, new Label ( "Sovrapprezzo Unitario (€/%)" ) );
		main.setWidget ( 0, PRODUCT_MEASURE_COLUMN, new Label ( "Unità Misura" ) );
		main.setWidget ( 0, PRODUCT_STOCK_COLUMN, new Label ( "Dimensione Confezione" ) );
		main.setWidget ( 0, PRODUCT_AVAILQUANT_COLUMN, new Label ( "Quantità Disponibile" ) );
		main.setWidget ( 0, PRODUCT_ORDQUANT_COLUMN, new Label ( "Quantità Ordinata" ) );
		main.setWidget ( 0, PRODUCT_TOTALPRICE_COLUMN, new Label ( "Totale Prezzo" ) );
		main.setWidget ( 0, PRODUCT_TOTALTRANSPORT_COLUMN, new Label ( "Totale Trasporto" ) );
		main.setWidget ( 0, PRODUCT_TOTALOVERPRICE_COLUMN, new Label ( "Totale Sovrapprezzo" ) );
		main.setWidget ( 0, PRODUCT_SHIPQUANT_COLUMN, new Label ( "Quantità Consegnata" ) );
		main.setWidget ( 0, PRODUCT_NOTIFICATIONS_COLUMN, new Label ( "Notifiche" ) );

		main.getRowFormatter ().setStyleName ( 0, "table-header" );

		/*
			La prima colonna e' ad uso e consumo interno e non mostra alcuna
			informazione utile, dunque viene nascosta
		*/
		main.getColumnFormatter ().setStyleName ( 0, "hidden" );

		totalLabel = null;
		totalshipLabel = null;
		totalOverpriceLabel = null;
		ordersUsers = new ArrayList ();
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
		float [] overprices;
		float prod_total_price;
		float det;
		float over;
		float total_price;
		float total_ship_price;
		float total_overprice;
		ArrayList cached_orders;
		ArrayList products;
		ArrayList user_products;
		FromServer user;
		FromServer user_ord;
		FromServer order_product;
		ProductUser user_product;

		if ( locked == true )
			return;

		my_id = currentOrder.getLocalID ();
		products = currentOrder.getArray ( "products" );

		quantities = new float [ products.size () ];
		delivered = new float [ products.size () ];
		prices = new float [ products.size () ];
		details = new float [ products.size () ];
		overprices = new float [ products.size () ];

		for ( int i = 0; i < products.size (); i++ ) {
			quantities [ i ] = 0;
			delivered [ i ] = 0;
			prices [ i ] = 0;
			details [ i ] = 0;
			overprices [ i ] = 0;
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
		total_overprice = 0;

		for ( int i = 0; i < cached_orders.size (); i++ ) {
			user_ord = ( FromServer ) cached_orders.get ( i );

			if ( user_ord.isValid () == false )
				continue;

			user = user_ord.getObject ( "baseorder" );

			if ( user != null && user.getLocalID () == my_id ) {
				user_products = user_ord.getArray ( "allproducts" );

				for ( int a = 0; a < user_products.size (); a++ ) {
					user_product = ( ProductUser ) user_products.get ( a );
					order_product = user_product.getObject ( "product" );
					user_product_ref = order_product.getLocalID ();

					for ( int e = 0; e < products.size (); e++ ) {
						order_product = ( FromServer ) products.get ( e );

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
								over = user_product.getSurplus ();

								prices [ e ] += prod_total_price;
								details [ e ] += det;
								overprices [ e ] += over;
								total_price += prod_total_price;
								total_ship_price += det;
								total_overprice += over;
							}

							break;
						}
					}
				}

				ordersUsers.add ( user_ord );
			}
		}

		syncTable ( products, quantities, delivered, prices, details, overprices );
		totalLabel.setVal ( total_price );
		totalshipLabel.setVal ( total_ship_price );
		totalOverpriceLabel.setVal ( total_overprice );
	}

	public boolean saveContents () {
		int index;
		boolean ret;
		ArrayList products;
		Product prod;
		PriceBox price_unit;
		PriceBox price_transport;
		PercentageBox overprice;
		FloatBox maxtotal;

		ret = false;
		products = currentOrder.getArray ( "products" );

		for ( int i = 0; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );
			index = searchProduct ( prod );

			if ( index != -1 ) {
				price_unit = ( PriceBox ) main.getWidget ( index, PRODUCT_PRICE_COLUMN );
				price_transport = ( PriceBox ) main.getWidget ( index, PRODUCT_TRANSPORT_COLUMN );
				overprice = ( PercentageBox ) main.getWidget ( index, PRODUCT_OVERPRICE_COLUMN );
				maxtotal = ( FloatBox ) main.getWidget ( index, PRODUCT_AVAILQUANT_COLUMN );

				if ( prod.getFloat ( "unit_price" ) != price_unit.getVal () ||
						prod.getFloat ( "shipping_price" ) != price_transport.getVal () ||
						prod.getString ( "surplus" ) != overprice.getValue () ||
						prod.getFloat ( "total_max_order" ) != maxtotal.getVal () ) {

					prod.setFloat ( "unit_price", price_unit.getVal () );
					prod.setFloat ( "shipping_price", price_transport.getVal () );
					prod.setString ( "surplus", overprice.getValue () );
					prod.setFloat ( "total_max_order", maxtotal.getVal () );
					ret = true;
				}
			}
		}

		return ret;
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

	private void initHiddenColumns () {
		hasTransport = false;
		hasSurplus = false;
		hasStock = false;
		hasMaxAvailable = false;
		hasConstraints = false;
	}

	private void checkHiddenColumns () {
		Widget widget;
		Widget widget2;

		for ( int i = 0; i < main.getRowCount () - 2; i++ ) {
			widget = main.getWidget ( i, PRODUCT_TRANSPORT_COLUMN );
			widget2 = main.getWidget ( i, PRODUCT_TOTALTRANSPORT_COLUMN );

			if ( hasTransport == false ) {
				widget.addStyleName ( "hidden" );
				widget2.addStyleName ( "hidden" );
			}
			else {
				widget.removeStyleName ( "hidden" );
				widget2.removeStyleName ( "hidden" );
			}

			widget = main.getWidget ( i, PRODUCT_STOCK_COLUMN );

			if ( widget != null ) {
				if ( hasStock == false )
					widget.addStyleName ( "hidden" );
				else
					widget.removeStyleName ( "hidden" );
			}

			widget = main.getWidget ( i, PRODUCT_AVAILQUANT_COLUMN );

			if ( hasMaxAvailable == false )
				widget.addStyleName ( "hidden" );
			else
				widget.removeStyleName ( "hidden" );

			widget = main.getWidget ( i, PRODUCT_OVERPRICE_COLUMN );
			widget2 = main.getWidget ( i, PRODUCT_TOTALOVERPRICE_COLUMN );

			if ( hasSurplus == false ) {
				widget.addStyleName ( "hidden" );
				widget2.addStyleName ( "hidden" );
			}
			else {
				widget.removeStyleName ( "hidden" );
				widget2.removeStyleName ( "hidden" );
			}

			widget = main.getWidget ( i, PRODUCT_NOTIFICATIONS_COLUMN );

			if ( hasConstraints == false )
				widget.addStyleName ( "hidden" );
			else
				widget.removeStyleName ( "hidden" );
		}

		if ( hasTransport == false )
			totalshipLabel.addStyleName ( "hidden" );
		else
			totalshipLabel.removeStyleName ( "hidden" );

		if ( hasSurplus == false )
			totalOverpriceLabel.addStyleName ( "hidden" );
		else
			totalOverpriceLabel.removeStyleName ( "hidden" );
	}

	private void syncTable ( ArrayList products, float [] quantities, float [] delivered,
					float [] prices, float [] prices_details, float [] overprices_details ) {
		int i;
		int e;
		boolean new_row;
		Product product;

		initHiddenColumns ();

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

			setDataRow ( e, product, quantities [ i ], delivered [ i ], prices [ i ],
					prices_details [ i ], overprices_details [ i ], new_row );
		}

		checkHiddenColumns ();
	}

	private void addManualAdjustIcon ( int row, Product prod ) {
		RefineProductDialog cell;

		cell = new RefineProductDialog ( ordersUsers, prod );
		cell.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				syncOrders ();
			}
		} );

		main.setWidget ( row, PRODUCT_NOTIFICATIONS_COLUMN, cell );
	}

	private void removeManualAdjustIcon ( int row ) {
		main.setWidget ( row, PRODUCT_NOTIFICATIONS_COLUMN, new Label () );
	}

	private String measureSymbol ( Product prod ) {
		FromServer measure;

		measure = prod.getObject ( "measure" );
		if ( measure != null )
			return measure.getString ( "name" );
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
				lab = ( Label ) main.getWidget ( i, PRODUCT_ORDQUANT_COLUMN );
				quantity = Float.parseFloat ( lab.getText () );

				switch ( id ) {
					case 2:
						label_index = PRODUCT_TOTALPRICE_COLUMN;
						break;

					case 3:
						label_index = PRODUCT_TOTALTRANSPORT_COLUMN;
						break;

					default:
						Window.alert ( "Criterio di modifica non gestito" );
						return;
				}

				lab = ( Label ) main.getWidget ( i, label_index );
				lab.setText ( Utils.priceToString ( price * quantity ) );
				alignTotalPrice ( label_index );

				if ( id == PRODUCT_PRICE_COLUMN ) {
					alignRowOverprice ( main.getWidget ( i, PRODUCT_OVERPRICE_COLUMN ) );
				}

				break;
			}
		}
	}

	private void alignRowOverprice ( Widget sender ) {
		int num;
		float quantity;
		float unit;
		String over;
		Widget cmp;
		Label lab;
		PriceBox unit_w;

		num = main.getRowCount () - 2;

		for ( int i = 1; i < num; i++ ) {
			cmp = main.getWidget ( i, PRODUCT_OVERPRICE_COLUMN );

			if ( cmp == sender ) {
				over = ( ( PercentageBox ) sender ).getValue ();

				lab = ( Label ) main.getWidget ( i, PRODUCT_ORDQUANT_COLUMN );
				quantity = Float.parseFloat ( lab.getText () );

				unit_w = ( PriceBox ) main.getWidget ( i, PRODUCT_PRICE_COLUMN );
				unit = unit_w.getVal ();

				lab = ( Label ) main.getWidget ( i, PRODUCT_TOTALOVERPRICE_COLUMN );
				lab.setText ( Utils.priceToString ( Utils.sumPercentage ( unit, over ) * quantity ) );

				alignTotalPrice ( PRODUCT_TOTALOVERPRICE_COLUMN );
				break;
			}
		}
	}

	private void setDataRow ( int index, Product product, float quantity, float delivered, float price, float price_details, float overprice, boolean new_row ) {
		float stock;
		float max_avail;
		float transport;
		String surplus;
		Label lab;
		FloatBox quan;
		PriceBox box;
		PercentageBox perc;

		stock = product.getFloat ( "stock_size" );
		if ( stock != 0 )
			hasStock = true;

		transport = product.getFloat ( "shipping_price" );
		if ( transport > 0 )
			hasTransport = true;

		surplus = product.getString ( "surplus" );
		if ( surplus != null && surplus != "0" )
			hasSurplus = true;

		max_avail = product.getFloat ( "total_max_order" );
		if ( max_avail > 0 )
			hasMaxAvailable = true;

		if ( new_row == true ) {
			main.setWidget ( index, 0, new Hidden ( Integer.toString ( product.getLocalID () ) ) );

			box = new PriceBox ();
			box.setVal ( product.getFloat ( "unit_price" ) );
			box.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
				}

				public void onLostFocus ( Widget sender ) {
					alignRow ( sender, PRODUCT_PRICE_COLUMN );
				}
			} );
			main.setWidget ( index, PRODUCT_PRICE_COLUMN, box );

			box = new PriceBox ();
			box.setVal ( transport );
			box.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
				}

				public void onLostFocus ( Widget sender ) {
					alignRow ( sender, PRODUCT_TRANSPORT_COLUMN );
				}
			} );
			main.setWidget ( index, PRODUCT_TRANSPORT_COLUMN, box );

			perc = new PercentageBox ();
			perc.setValue ( surplus );
			perc.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
				}

				public void onLostFocus ( Widget sender ) {
					alignRowOverprice ( sender );
				}
			} );
			main.setWidget ( index, PRODUCT_OVERPRICE_COLUMN, perc );

			main.setWidget ( index, PRODUCT_MEASURE_COLUMN, new Label ( measureSymbol ( product ) ) );

			if ( stock != 0 )
				main.setWidget ( index, PRODUCT_STOCK_COLUMN, new Label ( Float.toString ( stock ) ) );
		}

		lab = editableLabel ( index, PRODUCT_NAME_COLUMN, new_row );
		lab.setText ( product.getString ( "name" ) );

		quan = new FloatBox ();
		quan.setVal ( max_avail );
		main.setWidget ( index, PRODUCT_AVAILQUANT_COLUMN, quan );

		lab = editableLabel ( index, PRODUCT_ORDQUANT_COLUMN, new_row );
		lab.setText ( Utils.floatToString ( quantity ) );

		lab = editableLabel ( index, PRODUCT_TOTALPRICE_COLUMN, new_row );
		lab.setText ( Utils.priceToString ( price ) );

		lab = editableLabel ( index, PRODUCT_TOTALTRANSPORT_COLUMN, new_row );
		lab.setText ( Utils.priceToString ( price_details ) );

		lab = editableLabel ( index, PRODUCT_TOTALOVERPRICE_COLUMN, new_row );
		lab.setText ( Utils.priceToString ( overprice ) );

		lab = editableLabel ( index, PRODUCT_SHIPQUANT_COLUMN, new_row );
		lab.setText ( Utils.floatToString ( delivered ) );

		if ( ( stock != 0 ) && ( quantity != 0 ) && ( quantity % stock != 0 ) ) {
			hasConstraints = true;
			addManualAdjustIcon ( index, product );
		}
		else {
			removeManualAdjustIcon ( index );
		}
	}

	private void fillList () {
		int i;
		int e;
		ArrayList products;
		Product prod;

		products = currentOrder.getArray ( "products" );
		if ( products == null )
			return;

		initHiddenColumns ();

		products = Utils.sortArrayByName ( products );

		for ( i = 0, e = 1; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );

			if ( prod.getBool ( "available" ) == false )
				continue;

			setDataRow ( e, prod, 0, 0, 0, 0, 0, true );
			e++;
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( e, 0, 13 );

		e++;

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalshipLabel = new PriceViewer ();
			totalOverpriceLabel = new PriceViewer ();
		}
		else {
			totalLabel.setVal ( 0 );
			totalshipLabel.setVal ( 0 );
			totalOverpriceLabel.setVal ( 0 );
		}

		main.setWidget ( e, PRODUCT_TOTALPRICE_COLUMN, totalLabel );
		main.setWidget ( e, PRODUCT_TOTALTRANSPORT_COLUMN, totalshipLabel );
		main.setWidget ( e, PRODUCT_TOTALOVERPRICE_COLUMN, totalOverpriceLabel );

		checkHiddenColumns ();
	}

	/****************************************************************** Lockable */

	public void unlock () {
		locked = false;

		/*
		if ( locked == true ) {
			locked = false;
			fillList ();
			syncOrders ();
		}
		*/
	}
}
