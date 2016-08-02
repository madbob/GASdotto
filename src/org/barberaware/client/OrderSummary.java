/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderSummary extends Composite implements Lockable {
	private boolean			locked;
	private ArrayList		currentOrders;
	private FlexTable		main;
	private PriceViewer		totalLabel;
	private PriceViewer		totalshipLabel;
	private PriceViewer		totalOverpriceLabel;
	private PriceViewer		totalDeliveredLabel;
	private ArrayList		ordersUsers;
	private OrderNotesDialog	notes;

	private boolean			hasTransport;
	private boolean			hasSurplus;
	private boolean			hasStock;
	private boolean			hasMaxAvailable;
	private boolean			hasConstraints;

	private int			ORDER_ID_COLUMN			= 0;
	private int			PRODUCT_ID_COLUMN		= 1;
	private int			PRODUCT_NAME_COLUMN		= 2;
	private int			PRODUCT_PRICE_COLUMN		= 3;
	private int			PRODUCT_TRANSPORT_COLUMN	= 4;
	private int			PRODUCT_OVERPRICE_COLUMN	= 5;
	private int			PRODUCT_MEASURE_COLUMN		= 6;
	private int			PRODUCT_STOCK_COLUMN		= 7;
	private int			PRODUCT_AVAILQUANT_COLUMN	= 8;
	private int			PRODUCT_ORDQUANT_COLUMN		= 9;
	private int			PRODUCT_TOTALPRICE_COLUMN	= 10;
	private int			PRODUCT_TOTALTRANSPORT_COLUMN	= 11;
	private int			PRODUCT_TOTALOVERPRICE_COLUMN	= 12;
	private int			PRODUCT_SHIPTOT_COLUMN		= 13;
	private int			PRODUCT_SHIPQUANT_COLUMN	= 14;
	private int			PRODUCT_NOTIFICATIONS_COLUMN	= 15;

	public OrderSummary () {
		VerticalPanel container;

		locked = true;

		container = new VerticalPanel ();
		initWidget ( container );

		notes = new OrderNotesDialog ();
		container.add ( notes );

		main = new FlexTable ();
		initMainFlex ();
		container.add ( main );

		totalLabel = null;
		totalshipLabel = null;
		totalOverpriceLabel = null;
		totalDeliveredLabel = null;
		ordersUsers = new ArrayList ();
	}

	public void reFill ( FromServer order ) {
		int num;

		if ( order.getType () == "Order" ) {
			currentOrders = new ArrayList ();
			currentOrders.add ( order );
		}
		else {
			currentOrders = order.getArray ( "orders" );
		}

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
		float [] delivered_prices;
		float [] prices;
		float [] details;
		float [] overprices;
		float prod_total_price;
		float det;
		float over;
		float del;
		float total_price;
		float total_ship_price;
		float total_overprice;
		float total_delivered_price;
		ArrayList cached_orders;
		ArrayList products;
		ArrayList user_products;
		Order order;
		FromServer user;
		FromServer user_ord;
		FromServer order_product;
		ProductUser user_product;

		if ( locked == true )
			return;

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
		total_delivered_price = 0;
		ordersUsers.clear ();

		for ( int j = 0; j < currentOrders.size (); j++ ) {
			order = ( Order ) currentOrders.get ( j );
			my_id = order.getLocalID ();
			products = order.getArray ( "products" );

			quantities = new float [ products.size () ];
			delivered = new float [ products.size () ];
			delivered_prices = new float [ products.size () ];
			prices = new float [ products.size () ];
			details = new float [ products.size () ];
			overprices = new float [ products.size () ];

			for ( int i = 0; i < products.size (); i++ ) {
				quantities [ i ] = 0;
				delivered [ i ] = 0;
				delivered_prices [ i ] = 0;
				prices [ i ] = 0;
				details [ i ] = 0;
				overprices [ i ] = 0;
			}

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
									del = user_product.getDeliveredPrice ();
									delivered_prices [ e ] = delivered_prices [ e ] + del;

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
									total_delivered_price += del;
								}

								break;
							}
						}
					}

					ordersUsers.add ( user_ord );
				}
			}

			syncTable ( order, products, quantities, delivered, delivered_prices, prices, details, overprices );
		}

		totalLabel.setVal ( total_price );
		totalshipLabel.setVal ( total_ship_price );
		totalOverpriceLabel.setVal ( total_overprice );
		totalDeliveredLabel.setVal ( total_delivered_price );
		notes.setOrders ( ordersUsers );
	}

	public boolean saveContents () {
		int index;
		boolean ret;
		ArrayList products;
		Product prod;
		FloatWidget price_unit;
		FloatWidget price_transport;
		PercentageWidget overprice;
		FloatBox maxtotal;
		FromServer order;

		ret = false;

		for ( int j = 0; j < currentOrders.size (); j++ ) {
			order = ( FromServer ) currentOrders.get ( j );
			products = order.getArray ( "products" );

			for ( int i = 0; i < products.size (); i++ ) {
				prod = ( Product ) products.get ( i );
				index = searchProduct ( order, prod );

				if ( index != -1 ) {
					price_unit = ( FloatWidget ) main.getWidget ( index, PRODUCT_PRICE_COLUMN );
					price_transport = ( FloatWidget ) main.getWidget ( index, PRODUCT_TRANSPORT_COLUMN );
					overprice = ( PercentageWidget ) main.getWidget ( index, PRODUCT_OVERPRICE_COLUMN );
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
		}

		return ret;
	}

	private void initMainFlex () {
		main.setStyleName ( "elements-table" );

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
		main.setWidget ( 0, PRODUCT_SHIPTOT_COLUMN, new Label ( "Totale Consegnato" ) );
		main.setWidget ( 0, PRODUCT_SHIPQUANT_COLUMN, new Label ( "Quantità Consegnata" ) );
		main.setWidget ( 0, PRODUCT_NOTIFICATIONS_COLUMN, new Label ( "Notifiche" ) );

		main.getRowFormatter ().setStyleName ( 0, "table-header" );

		/*
			La prima colonna e' ad uso e consumo interno e non mostra alcuna
			informazione utile, dunque viene nascosta
		*/
		main.getColumnFormatter ().setStyleName ( 0, "hidden" );
	}

	private int searchProduct ( FromServer order, FromServer prod ) {
		int current_children;
		String ord_id;
		String prod_id;
		Hidden existing_id;

		current_children = main.getRowCount () - 2;
		ord_id = Integer.toString ( order.getLocalID () );
		prod_id = Integer.toString ( prod.getLocalID () );

		for ( int i = 1; i < current_children; i++ ) {
			existing_id = ( Hidden ) main.getWidget ( i, ORDER_ID_COLUMN );
			if ( ord_id == existing_id.getName () ) {
				do {
					existing_id = ( Hidden ) main.getWidget ( i, PRODUCT_ID_COLUMN );
					if ( existing_id != null && prod_id == existing_id.getName () )
						return i;
					else
						i++;

				} while ( ( ( Hidden ) main.getWidget ( i, ORDER_ID_COLUMN ) ).getName () == ord_id );
			}
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

	private void checkHiddenColumn ( int row, int index1, int index2, boolean show ) {
		Widget widget;

		/*
			Questo e' per saltare le righe di intestazione nel caso di ordini
			aggregati
		*/
		if ( row != 0 ) {
			widget = main.getWidget ( row, PRODUCT_ID_COLUMN );
			if ( widget == null )
				return;
		}

		widget = main.getWidget ( row, index1 );
		if ( widget == null )
			return;

		if ( show == false ) {
			widget.addStyleName ( "hidden" );
			if ( index2 != -1 ) {
				widget = main.getWidget ( row, index2 );
				if ( widget != null )
					widget.addStyleName ( "hidden" );
			}
		}
		else {
			widget.removeStyleName ( "hidden" );
			if ( index2 != -1 ) {
				widget = main.getWidget ( row, index2 );
				if ( widget != null )
					widget.removeStyleName ( "hidden" );
			}
		}
	}

	private void checkHiddenColumns () {
		/*
			Quali colonne sono da nascondere, e quali sono sempre da visualizzare, e'
			argomento di discussione. E' corretto voler ridurre il numero di
			variazioni possibili nella tabella, ma mettere tutti gli attributi genera
			solo una gran griglia di cui magari solo una parte e' significativa.
			Commentando e decommentando le righe nel for() si opta per quali colonne
			visualizzare sempre e quali rendere dinamiche, da tenere a portata di mano
			per eventuali cambiamenti futuri
		*/
		for ( int i = 0; i < main.getRowCount () - 2; i++ ) {
			// checkHiddenColumn ( i, PRODUCT_TRANSPORT_COLUMN, PRODUCT_TOTALTRANSPORT_COLUMN, hasTransport );
			checkHiddenColumn ( i, PRODUCT_STOCK_COLUMN, -1, hasStock );
			checkHiddenColumn ( i, PRODUCT_AVAILQUANT_COLUMN, -1, hasMaxAvailable );
			// checkHiddenColumn ( i, PRODUCT_OVERPRICE_COLUMN, PRODUCT_TOTALOVERPRICE_COLUMN, hasSurplus );
			// checkHiddenColumn ( i, PRODUCT_NOTIFICATIONS_COLUMN, -1, hasConstraints );
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

	private void syncTable ( Order order, ArrayList products, float [] quantities, float [] delivered, float [] delivered_prices,
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

			e = searchProduct ( order, product );
			if ( e == -1 ) {
				e = main.getRowCount () - 2;
				main.insertRow ( e );
				new_row = true;
			}
			else {
				new_row = false;
			}

			setDataRow ( e, order, product, quantities [ i ], delivered [ i ], delivered_prices [ i ], prices [ i ],
					prices_details [ i ], overprices_details [ i ], new_row );
		}

		checkHiddenColumns ();
	}

	private void addManualAdjustIcon ( int row, FromServer prod ) {
		RefineProductDialog cell;

		cell = new RefineProductDialog ( ordersUsers, prod );
		cell.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				syncOrders ();
			}
		}, ChangeEvent.getType () );

		main.setWidget ( row, PRODUCT_NOTIFICATIONS_COLUMN, cell );
	}

	private void removeManualAdjustIcon ( int row ) {
		main.setWidget ( row, PRODUCT_NOTIFICATIONS_COLUMN, new Label () );
	}

	private String measureSymbol ( FromServer prod ) {
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
			/*
				Questo "if" e' per saltare le righe che
				contengono le intestazioni dei fornitori negli
				ordini aggregati
			*/
			if ( main.getCellCount ( i ) != 3 ) {
				lab = ( Label ) main.getWidget ( i, column );
				price += Utils.stringToPrice ( lab.getText () );
			}
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
			/*
				Questo "if" e' per saltare le righe che
				contengono le intestazioni dei fornitori negli
				ordini aggregati
			*/
			if ( main.getCellCount ( i ) == 3 )
				continue;

			cmp = main.getWidget ( i, id );

			if ( cmp == sender ) {
				price = ( ( PriceBox ) sender ).getVal ();
				lab = ( Label ) main.getWidget ( i, PRODUCT_ORDQUANT_COLUMN );
				quantity = Utils.stringToFloat ( lab.getText () );

				if ( id ==  PRODUCT_PRICE_COLUMN ) {
					label_index = PRODUCT_TOTALPRICE_COLUMN;
				}
				else if ( id == PRODUCT_TRANSPORT_COLUMN ) {
					label_index = PRODUCT_TOTALTRANSPORT_COLUMN;
				}
				else {
					Window.alert ( "Criterio di modifica non gestito" );
					return;
				}

				lab = ( Label ) main.getWidget ( i, label_index );
				lab.setText ( Utils.priceToString ( price * quantity ) );
				alignTotalPrice ( label_index );

				if ( id == PRODUCT_PRICE_COLUMN )
					alignRowOverprice ( main.getWidget ( i, PRODUCT_OVERPRICE_COLUMN ) );

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
			/*
				Questo "if" e' per saltare le righe che
				contengono le intestazioni dei fornitori negli
				ordini aggregati
			*/
			if ( main.getCellCount ( i ) == 3 )
				continue;

			cmp = main.getWidget ( i, PRODUCT_OVERPRICE_COLUMN );

			if ( cmp == sender ) {
				over = ( ( PercentageBox ) sender ).getValue ();

				lab = ( Label ) main.getWidget ( i, PRODUCT_ORDQUANT_COLUMN );
				quantity = Utils.stringToFloat ( lab.getText () );

				unit_w = ( PriceBox ) main.getWidget ( i, PRODUCT_PRICE_COLUMN );
				unit = unit_w.getVal ();

				lab = ( Label ) main.getWidget ( i, PRODUCT_TOTALOVERPRICE_COLUMN );
				lab.setText ( Utils.priceToString ( Utils.sumPercentage ( unit, over ) * quantity ) );

				alignTotalPrice ( PRODUCT_TOTALOVERPRICE_COLUMN );
				break;
			}
		}
	}

	private void setDataRow ( int index, FromServer order, FromServer product, float quantity, float delivered, float delivered_price, float price, float price_details, float overprice, boolean new_row ) {
		float stock;
		float max_avail;
		float transport;
		float unit_size;
		String surplus;
		Label lab;
		FloatBox quan;
		PriceBox box;
		PercentageBox perc;
		PriceViewer box_unedit;
		PercentageViewer perc_unedit;

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
			/*
			if ( main.getRowCount () > index )
				main.insertRow ( index );
			*/

			main.setWidget ( index, ORDER_ID_COLUMN, new Hidden ( Integer.toString ( order.getLocalID () ) ) );
			main.setWidget ( index, PRODUCT_ID_COLUMN, new Hidden ( Integer.toString ( product.getLocalID () ) ) );

			if ( order.sharingStatus ()<= ACL.ACL_READWRITE ) {
				box = new PriceBox ();
				box.setVal ( product.getFloat ( "unit_price" ) );
				box.addBlurHandler ( new BlurHandler () {
					public void onBlur ( BlurEvent event ) {
						alignRow ( ( Widget ) event.getSource (), PRODUCT_PRICE_COLUMN );
					}
				} );
				main.setWidget ( index, PRODUCT_PRICE_COLUMN, box );

				box = new PriceBox ();
				box.setVal ( transport );
				box.addBlurHandler ( new BlurHandler () {
					public void onBlur ( BlurEvent event ) {
						alignRow ( ( Widget ) event.getSource (), PRODUCT_TRANSPORT_COLUMN );
					}
				} );
				main.setWidget ( index, PRODUCT_TRANSPORT_COLUMN, box );

				perc = new PercentageBox ();
				perc.setValue ( surplus );
				perc.addBlurHandler ( new BlurHandler () {
					public void onBlur ( BlurEvent event ) {
						alignRowOverprice ( ( Widget ) event.getSource () );
					}
				} );
				main.setWidget ( index, PRODUCT_OVERPRICE_COLUMN, perc );
			}
			else {
				box_unedit = new PriceViewer ();
				box_unedit.setVal ( product.getFloat ( "unit_price" ) );
				main.setWidget ( index, PRODUCT_PRICE_COLUMN, box_unedit );

				box_unedit = new PriceViewer ();
				box_unedit.setVal ( transport );
				main.setWidget ( index, PRODUCT_TRANSPORT_COLUMN, box_unedit );

				perc_unedit = new PercentageViewer ();
				perc_unedit.setValue ( surplus );
				main.setWidget ( index, PRODUCT_OVERPRICE_COLUMN, perc_unedit );
			}

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

		lab = editableLabel ( index, PRODUCT_SHIPTOT_COLUMN, new_row );
		lab.setText ( Utils.priceToString ( delivered_price ) );

		if ( ( stock != 0 ) && ( quantity != 0 ) ) {
			unit_size = product.getFloat ( "unit_size" );
			if ( unit_size <= 0 )
				unit_size = stock;
			else
				unit_size = Utils.roundNumber ( stock * unit_size );

			quantity = Utils.roundNumber ( Utils.roundNumber ( quantity ) % unit_size );

			if ( quantity != 0 ) {
				hasConstraints = true;
				addManualAdjustIcon ( index, product );
			}
			else {
				removeManualAdjustIcon ( index );
			}
		}
		else {
			removeManualAdjustIcon ( index );
		}
	}

	private int fillRows ( FromServer order, int e ) {
		ArrayList products;
		Product prod;

		products = order.getArray ( "products" );
		if ( products == null )
			return e;

		products = Utils.sortArrayByName ( products );

		for ( int i = 0; i < products.size (); i++ ) {
			prod = ( Product ) products.get ( i );

			if ( prod.getBool ( "available" ) == false )
				continue;

			setDataRow ( e, order, prod, 0, 0, 0, 0, 0, 0, true );
			e++;
		}

		return e;
	}

	private void fillList () {
		int j;
		int i;
		int e;
		int num;
		FromServer order;
		Product prod;
		Label head;
		FlexTable.FlexCellFormatter formatter;

		initHiddenColumns ();

		formatter = main.getFlexCellFormatter ();
		num = currentOrders.size ();
		main.clear ();
		initMainFlex ();

		if ( num == 1 ) {
			order = ( FromServer ) currentOrders.get ( 0 );
			e = fillRows ( order, 1 );
		}
		else {
			for ( j = 0, e = 1; j < num; j++ ) {
				order = ( FromServer ) currentOrders.get ( j );
				main.setWidget ( e, ORDER_ID_COLUMN, new Hidden ( Integer.toString ( order.getLocalID () ) ) );

				head = new Label ( order.getObject ( "supplier" ).getString ( "name" ) );
				head.setStyleName ( "aggregate-order-header" );
				main.setWidget ( e, PRODUCT_NAME_COLUMN, head );
				formatter.setColSpan ( e, PRODUCT_NAME_COLUMN, PRODUCT_NOTIFICATIONS_COLUMN - PRODUCT_NAME_COLUMN + 1 );
				formatter.setHorizontalAlignment ( e, PRODUCT_NAME_COLUMN, HasHorizontalAlignment.ALIGN_LEFT );

				e = fillRows ( order, ++e );
			}
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		formatter.setColSpan ( e, 0, 15 );
		e++;

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalshipLabel = new PriceViewer ();
			totalOverpriceLabel = new PriceViewer ();
			totalDeliveredLabel = new PriceViewer ();
		}
		else {
			totalLabel.setVal ( 0 );
			totalshipLabel.setVal ( 0 );
			totalOverpriceLabel.setVal ( 0 );
			totalDeliveredLabel.setVal ( 0 );
		}

		main.setWidget ( e, PRODUCT_TOTALPRICE_COLUMN, totalLabel );
		main.setWidget ( e, PRODUCT_TOTALTRANSPORT_COLUMN, totalshipLabel );
		main.setWidget ( e, PRODUCT_TOTALOVERPRICE_COLUMN, totalOverpriceLabel );
		main.setWidget ( e, PRODUCT_SHIPTOT_COLUMN, totalDeliveredLabel );

		checkHiddenColumns ();
	}

	/****************************************************************** Lockable */

	public void unlock () {
		if ( locked == true ) {
			locked = false;
			fillList ();
			syncOrders ();
		}
	}
}
