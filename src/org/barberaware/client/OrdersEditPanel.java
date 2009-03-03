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
	private abstract class ForeachProductListCallback {
		public abstract void doIt ( Order order, FlexTable list, Product product );
	}

	private FormCluster		main;

	public OrdersEditPanel () {
		super ();

		main = new FormCluster ( "Order", "images/new_order.png" ) {
				protected FromServerForm doEditableRow ( FromServer ord ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					FlexTable fields;
					Order order;
					Supplier supplier;
					CyclicToggle status;

					order = ( Order ) ord;
					supplier = ( Supplier ) order.getObject ( "supplier" );

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
					fields.setWidget ( 0, 1, new Label ( supplier.getString ( "name" ) ) );

					fields.setWidget ( 1, 0, new Label ( "Data apertura" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "startdate" ) );

					fields.setWidget ( 2, 0, new Label ( "Data chiusura" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "enddate" ) );

					fields.setWidget ( 3, 0, new Label ( "Data consegna" ) );
					fields.setWidget ( 3, 1, ver.getWidget ( "shippingdate" ) );

					fields = new FlexTable ();
					hor.add ( fields );

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

					addProductsList ( ver, true );
					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					Order order;
					final FromServerForm ver;
					HorizontalPanel hor;
					FlexTable fields;
					Widget suppliers_main;
					Date now;
					FromServerSelector suppliers;
					FlexTable products;
					DateSelector date;

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
							supp = suppliers.getValue ();

							products_list = ( FlexTable ) ver.retriveInternalWidget ( "list" );
							while ( products_list.getRowCount () != 0 )
								products_list.removeRow ( 0 );

							if ( supp != null ) {
								ver.getObject ().setObject ( "supplier", supp );
								loadExistingProducts ( products_list, supp );
								claimProductsBySupplier ( supp );
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

					products = addProductsList ( ver, false );
					products.setWidget ( 0, 0, new Label ( "Nessun fornitore selezionato" ) );
					return ver;
				}
		};

		addTop ( main );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				iterateProductsList ( ( Product ) object, new ForeachProductListCallback () {
					public void doIt ( Order order, FlexTable list, Product product ) {
						ArrayList order_products;
						BooleanSelector sel;

						if ( product.getBool ( "available" ) == false )
							return;

						list.insertRow ( 0 );

						list.setWidget ( 0, 0, new Hidden ( "id", Integer.toString ( product.getLocalID () ) ) );
						list.setWidget ( 0, 1, new Label ( product.getString ( "name" ) ) );

						sel = new BooleanSelector ();
						list.setWidget ( 0, 2, sel );

						order_products = order.getArray ( "products" );

						if ( order_products != null ) {
							Product tmp_product;

							for ( int a = 0; a < order_products.size (); a++ ) {
								tmp_product = ( Product ) order_products.get ( a );

								if ( tmp_product.getLocalID () == product.getLocalID () ) {
									sel.setDown ( true );
									break;
								}
							}
						}
					}
				} );
			}

			public void onModify ( FromServer object ) {
				iterateProductsList ( ( Product ) object, new ForeachProductListCallback () {
					public void doIt ( Order order, FlexTable list, Product product ) {
						int id;
						int iter_id;

						id = product.getLocalID ();

						for ( int i = 0; i < list.getRowCount (); i++ ) {
							iter_id = Integer.parseInt ( ( ( Hidden ) list.getWidget ( i, 0 ) ).getValue () );

							if ( id == iter_id ) {
								( ( Label ) list.getWidget ( i, 1 ) ).setText ( product.getString ( "name" ) );
								break;
							}
						}
					}
				} );
			}

			public void onDestroy ( FromServer object ) {
				iterateProductsList ( ( Product ) object, new ForeachProductListCallback () {
					public void doIt ( Order order, FlexTable list, Product product ) {
						int id;
						int iter_id;

						id = product.getLocalID ();

						for ( int i = 0; i < list.getRowCount (); i++ ) {
							iter_id = Integer.parseInt ( ( ( Hidden ) list.getWidget ( i, 0 ) ).getValue () );

							if ( id == iter_id ) {
								list.removeRow ( i );
								break;
							}
						}
					}
				} );
			}
		} );
	}

	private FlexTable addProductsList ( FromServerForm ver, boolean download ) {
		VerticalPanel vertical;
		FlexTable products;

		vertical = new VerticalPanel ();
		vertical.addStyleName ( "sub-elements-details" );
		ver.add ( vertical );
		vertical.add ( new Label ( "Prodotti" ) );
		products = new FlexTable ();
		ver.setExtraWidget ( "list", products );
		vertical.add ( products );

		if ( download )
			claimProductsBySupplier ( ver.getObject ().getObject ( "supplier" ) );

		return products;
	}

	private void claimProductsBySupplier ( FromServer supplier ) {
		ServerRequest params;

		params = new ServerRequest ( "Product" );
		params.add ( "supplier", supplier );
		Utils.getServer ().testObjectReceive ( params );
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
				list.setWidget ( 0, 0, new Hidden ( "id", Integer.toString ( product.getLocalID () ) ) );
				list.setWidget ( 0, 1, new Label ( product.getString ( "name" ) ) );
				list.setWidget ( 0, 2, new BooleanSelector () );
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
		int rows;
		Order order;
		FlexTable fields;
		ArrayList products;
		ToggleButton toggler;
		int id;
		FromServer prod;

		order = ( Order ) form.getObject ();
		fields = ( FlexTable ) form.retriveInternalWidget ( "list" );
		rows = fields.getRowCount ();

		products = order.getArray ( "products" );
		if ( products == null ) {
			products = new ArrayList ();
			order.setArray ( "products", products );
		}

		products.clear ();

		for ( int i = 0; i < rows; i++ ) {
			toggler = ( ToggleButton ) fields.getWidget ( i, 2 );

			if ( toggler.isDown () == true ) {
				id = Integer.parseInt ( ( ( Hidden ) fields.getWidget ( i, 0 ) ).getValue () );
				prod = Utils.getServer ().getObjectFromCache ( "Product", id );
				products.add ( prod );
			}
		}
	}

	private void iterateProductsList ( Product product, ForeachProductListCallback callback ) {
		FromServerForm order_form;
		Supplier supplier;
		Supplier tmp_supplier;
		Order order;
		FlexTable list;

		supplier = ( Supplier ) product.getObject ( "supplier" );

		for ( int i = 0; i < main.latestIterableIndex (); i++ ) {
			order_form = ( FromServerForm ) main.getWidget ( i );
			order = ( Order ) order_form.getObject ();
			tmp_supplier = ( Supplier ) order.getObject ( "supplier" );

			if ( tmp_supplier.getLocalID () == supplier.getLocalID () ) {
				list = ( FlexTable ) order_form.retriveInternalWidget ( "list" );
				callback.doIt ( order, list, product );
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
