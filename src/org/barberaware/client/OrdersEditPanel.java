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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class OrdersEditPanel extends GenericPanel {
	private FormCluster		main;

	public OrdersEditPanel () {
		super ();

		main = new FormCluster ( "Order", "images/new_order.png" ) {
				protected FromServerForm doEditableRow ( FromServer ord ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					FlexTable fields;
					Order order;
					CyclicToggle status;

					order = ( Order ) ord;

					ver = new FromServerForm ( order );
					ver.setCallback ( new FromServerFormCallbacks () {
						public void onSave ( FromServerForm form ) {
							retriveInputData ( form );
						}

						public void onReset ( FromServerForm form ) {
							resetOrderRow ( form );
						}

						public void onDelete ( FromServerForm form ) {
							/* dummy */
						}
					} );

					hor = new HorizontalPanel ();
					ver.add ( hor );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Fornitore" ) );
					fields.setWidget ( 0, 1, new Label ( order.getObject ( "supplier" ).getString ( "name" ) ) );

					fields.setWidget ( 1, 0, new Label ( "Data apertura" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "startdate" ) );

					fields.setWidget ( 2, 0, new Label ( "Data chiusura" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "enddate" ) );

					fields.setWidget ( 3, 0, new Label ( "Data consegna" ) );
					fields.setWidget ( 3, 1, ver.getWidget ( "shippingdate" ) );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Referente" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "reference" ) );
					getOrderReferenceSelector ( ver );

					fields.setWidget ( 1, 0, new Label ( "Stato" ) );
					status = new CyclicToggle ();
					status.addState ( "images/order_status_opened.png" );
					status.addState ( "images/order_status_closed.png" );
					status.addState ( "images/order_status_suspended.png" );
					fields.setWidget ( 1, 1, ver.getPersonalizedWidget ( "status", status ) );

					fields.setWidget ( 2, 0, new Label ( "Anticipo" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "anticipated" ) );

					fields.setWidget ( 3, 0, new Label ( "Si ripete" ) );
					fields.setWidget ( 3, 1, ver.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );

					addProductsList ( ver );
					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					Order order;
					final FromServerForm ver;
					HorizontalPanel hor;
					FlexTable fields;
					Widget suppliers_main;
					FromServerSelector suppliers;
					FlexTable products;
					DateSelector date;
					Date now;

					order = new Order ();

					ver = new FromServerForm ( order );
					ver.setCallback ( new FromServerFormCallbacks () {
						public void onSave ( FromServerForm form ) {
							retriveInputData ( form );
						}

						public void onReset ( FromServerForm form ) {
							resetOrderRow ( form );
						}

						public void onDelete ( FromServerForm form ) {
							/* dummy */
						}
					} );

					hor = new HorizontalPanel ();
					ver.add ( hor );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Fornitore" ) );
					suppliers_main = ver.getWidget ( "supplier" );
					suppliers = ( FromServerSelector ) ver.retriveInternalWidget ( "supplier" );

					suppliers.addChangeListener ( new ChangeListener () {
						public void onChange ( Widget sender ) {
							FromServerSelector suppliers;
							FromServer supp;
							FlexTable products_list;

							suppliers = ( FromServerSelector ) sender;
							supp = suppliers.getSelected ();

							products_list = ( FlexTable ) ver.retriveInternalWidget ( "list" );
							while ( products_list.getRowCount () != 0 )
								products_list.removeRow ( 0 );

							if ( supp != null ) {
								ServerRequest params;

								ver.getObject ().setObject ( "supplier", supp );
								loadExistingProducts ( products_list, supp );

								params = new ServerRequest ( "Product" );
								params.add ( "supplier", supp );
								Utils.getServer ().testObjectReceive ( params );
							}
							else
								products_list.setWidget ( 0, 0, new Label ( "Nessun fornitore selezionato" ) );
						}
					} );
					fields.setWidget ( 0, 1, suppliers_main );

					fields.setWidget ( 1, 0, new Label ( "Data apertura" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "startdate" ) );

					fields.setWidget ( 2, 0, new Label ( "Data chiusura" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "enddate" ) );

					fields.setWidget ( 3, 0, new Label ( "Data consegna" ) );
					fields.setWidget ( 3, 1, ver.getWidget ( "shippingdate" ) );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Referente" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "reference" ) );
					getOrderReferenceSelector ( ver );

					fields.setWidget ( 1, 0, new Label ( "Stato" ) );
					fields.setWidget ( 1, 1, new Label ( "Nuovo" ) );

					fields.setWidget ( 2, 0, new Label ( "Anticipo" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "anticipated" ) );

					fields.setWidget ( 3, 0, new Label ( "Si ripete" ) );
					fields.setWidget ( 3, 1, ver.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );

					now = new Date ( System.currentTimeMillis () );
					date = ( DateSelector ) ver.retriveInternalWidget ( "startdate" );
					date.setValue ( now );

					now.setMonth ( now.getMonth () + 3 );
					if ( now.getMonth () < 3 )
						now.setYear ( now.getYear () + 1 );

					date = ( DateSelector ) ver.retriveInternalWidget ( "enddate" );
					date.setValue ( now );
					date = ( DateSelector ) ver.retriveInternalWidget ( "shippingdate" );
					date.setValue ( now );

					fields = new FlexTable ();
					hor.add ( fields );

					products = addProductsList ( ver );
					products.setWidget ( 0, 0, new Label ( "Nessun fornitore selezionato" ) );
					return ver;
				}
		};

		addTop ( main );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Product product;
				Supplier supplier;
				FromServerForm order_form;
				Order tmp_order;
				ArrayList tmp_order_products;
				Supplier tmp_supplier;
				FlexTable product_list;
				BooleanSelector sel;
				Product tmp_product;

				product = ( Product ) object;
				if ( product.getBool ( "available" ) == false )
					return;

				supplier = ( Supplier ) product.getObject ( "supplier" );

				for ( int i = 1; i < main.getWidgetCount () - 1; i++ ) {
					order_form = ( FromServerForm ) main.getWidget ( i );
					tmp_order = ( Order ) order_form.getObject ();
					tmp_supplier = ( Supplier ) tmp_order.getObject ( "supplier" );

					if ( tmp_supplier.getLocalID () == supplier.getLocalID () ) {
						tmp_order_products = tmp_order.getArray ( "products" );

						product_list = ( FlexTable ) order_form.retriveInternalWidget ( "list" );
						product_list.insertRow ( 0 );

						product_list.setWidget ( 0, 0, new Hidden ( "id", Integer.toString ( product.getLocalID () ) ) );
						product_list.setWidget ( 0, 1, new Label ( product.getString ( "name" ) ) );

						sel = new BooleanSelector ();
						product_list.setWidget ( 0, 2, sel );

						if ( tmp_order_products != null ) {
							for ( int a = 0; a < tmp_order_products.size (); a++ ) {
								tmp_product = ( Product ) tmp_order_products.get ( a );

								if ( tmp_product.getLocalID () == product.getLocalID () ) {
									sel.setDown ( true );
									break;
								}
							}
						}
					}

					/*
						Non e' detto che ci sia un solo ordine per il
						dato fornitore, dunque me li passo e li controllo
						tutti
					*/
				}
			}

			public void onModify ( FromServer object ) {
				/**
					TODO
				*/
			}

			public void onDestroy ( FromServer object ) {
				/**
					TODO
				*/
			}
		} );
	}

	private void getOrderReferenceSelector ( final FromServerForm ver ) {
		FromServerSelector reference;

		reference = ( FromServerSelector ) ver.retriveInternalWidget ( "reference" );
		reference.addFilter ( new FromServerValidateCallback () {
			public boolean checkObject ( FromServer obj ) {
				FromServer order;
				FromServer supplier;
				ArrayList available;

				order = ver.getObject ();
				supplier = order.getObject ( "supplier" );
				available = supplier.getArray ( "references" );

				for ( int i = 0; i < available.size (); i++ ) {
					if ( ( ( FromServer ) available.get ( i ) ) == obj )
						return true;
				}

				return false;
			}
		} );
	}

	private FlexTable addProductsList ( FromServerForm ver ) {
		VerticalPanel vertical;
		FlexTable products;

		vertical = new VerticalPanel ();
		vertical.addStyleName ( "sub-elements-details" );
		ver.add ( vertical );
		vertical.add ( new Label ( "Prodotti" ) );
		products = new FlexTable ();
		ver.setExtraWidget ( "list", products );
		vertical.add ( products );
		return products;
	}

	private void loadExistingProducts ( FlexTable list, FromServer supplier ) {
		int supplier_locald_id;
		ArrayList existing;
		Product product;

		supplier_locald_id = supplier.getLocalID ();
		existing = Utils.getServer ().getObjectsFromCache ( "Product" );

		for ( int i = 0; i < existing.size (); i++ ) {
			product = ( Product ) existing.get ( i );

			if ( product.getObject ( "supplier" ).getLocalID () == supplier_locald_id ) {
				list.insertRow ( 0 );
				list.setWidget ( 0, 0, new Label ( product.getString ( "name" ) ) );
				list.setWidget ( 0, 1, new BooleanSelector () );
			}
		}
	}

	private void resetOrderRow ( FromServerForm form ) {
		Order order;
		FlexTable fields;
		ArrayList products;
		int num_products;
		Product prod;
		String id;
		ToggleButton toggler;
		boolean found;

		order = ( Order ) form.getObject ();

		if ( order.isValid () ) {
			fields = ( FlexTable ) form.retriveInternalWidget ( "list" );

			products = order.getArray ( "products" );
			num_products = products.size ();

			for ( int a = 0; a < fields.getRowCount (); a++ ) {
				toggler = ( ToggleButton ) fields.getWidget ( a, 2 );
				found = false;

				for ( int i = 0; i < num_products; i++ ) {
					prod = ( Product ) products.get ( i );
					id = Integer.toString ( prod.getLocalID () );

					if ( id.equals ( ( ( Hidden ) fields.getWidget ( a, 0 ) ).getValue () ) ) {
						found = true;
						break;
					}
				}

				toggler.setDown ( found );
			}
		}
		else
			form.setVisible ( false );
	}

	private void retriveInputData ( FromServerForm form ) {
		Order order;
		FlexTable fields;
		ArrayList products;
		ToggleButton toggler;
		int id;
		FromServer prod;

		order = ( Order ) form.getObject ();
		fields = ( FlexTable ) form.retriveInternalWidget ( "list" );

		products = order.getArray ( "products" );
		if ( products == null ) {
			products = new ArrayList ();
			order.setArray ( "products", products );
		}

		products.clear ();

		for ( int i = 0; i < fields.getRowCount (); i++ ) {
			toggler = ( ToggleButton ) fields.getWidget ( i, 2 );

			if ( toggler.isDown () == true ) {
				id = Integer.parseInt ( ( ( Hidden ) fields.getWidget ( i, 0 ) ).getValue () );
				prod = Utils.getServer ().getObjectFromCache ( "Product", id );
				products.add ( prod );
			}
		}
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Ordini";
	}

	public Image getIcon () {
		return new Image ( "images/path_orders.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Order" );
		Utils.getServer ().testObjectReceive ( "Product" );
	}
}
