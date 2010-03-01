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

public class SuppliersPanel extends GenericPanel {
	private FormCluster		main;
	private ArrayList		scheduledProducts;

	public SuppliersPanel () {
		super ();

		scheduledProducts = new ArrayList ();

		main = new FormCluster ( "Supplier", null ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				return new SupplierUneditableForm ( ( Supplier ) supp );
			}

			protected FromServerForm doNewEditableRow () {
				/*
					Il pannello qui descritto serve solo per mostrare la
					lista di fornitori, dunque nessun form di creazione e'
					previsto
				*/
				return null;
			}

			protected void customNew ( FromServer object, boolean true_new ) {
				checkProductsSchedule ();
			}
		};

		addTop ( main );

		/*
			Questo viene eseguito appunto quando arriva qualche dato nuovo dal
			server, non per inizializzare pannelli esistenti.
			Cfr. OpenedOrdersList::OpenedOrdersList()
		*/
		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm form;

				if ( object.getInt ( "status" ) != Order.OPENED )
					return;

				form = main.retrieveForm ( object.getObject ( "supplier" ) );

				if ( form != null ) {
					OpenedOrdersList list;
					list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );
					list.addOrder ( ( Order ) object );
				}
			}

			public void onModify ( FromServer object ) {
				FromServerForm form;

				form = main.retrieveForm ( object.getObject ( "supplier" ) );

				if ( form != null ) {
					OpenedOrdersList list;
					list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );
					list.modOrder ( ( Order ) object );
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerForm form;

				form = main.retrieveForm ( object.getObject ( "supplier" ) );

				if ( form != null ) {
					OpenedOrdersList list;
					list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );
					list.delOrder ( ( Order ) object );
				}
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm form;
				Order ord;

				if ( object.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					return;

				ord = ( Order ) object.getObject ( "baseorder" );
				form = main.retrieveForm ( ord.getObject ( "supplier" ) );

				if ( form != null ) {
					PastOrdersList list;

					list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );
					list.addOrder ( ord );
				}
			}

			public void onModify ( FromServer object ) {
				FromServerForm form;
				Order ord;

				ord = ( Order ) object.getObject ( "baseorder" );
				form = main.retrieveForm ( ord.getObject ( "supplier" ) );

				if ( form != null ) {
					PastOrdersList list;
					list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );
					list.modOrder ( ord );
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerForm form;
				Order ord;

				ord = ( Order ) object.getObject ( "baseorder" );
				form = main.retrieveForm ( ord.getObject ( "supplier" ) );

				if ( form != null ) {
					PastOrdersList list;
					list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );
					list.delOrder ( ord );
				}
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Product prod;

				prod = ( Product ) object;
				if ( insertProduct ( prod ) == false )
					scheduledProducts.add ( prod );
			}

			public void onModify ( FromServer object ) {
				Product prod;
				ProductsPresentationList panel;

				prod = ( Product ) object;
				panel = retrieveProductsPanel ( prod );
				if ( panel == null )
					panel.refreshElement ( prod );
			}

			public void onDestroy ( FromServer object ) {
				Product prod;
				ProductsPresentationList panel;

				prod = ( Product ) object;
				panel = retrieveProductsPanel ( prod );
				if ( panel == null )
					panel.removeElement ( prod );
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().testObjectReceive ( "Supplier" );
	}

	private ProductsPresentationList retrieveProductsPanel ( Product product ) {
		Supplier supplier;
		FromServerForm supplier_form;

		supplier = ( Supplier ) product.getObject ( "supplier" );
		supplier_form = main.retrieveForm ( supplier );

		if ( supplier_form != null )
			return ( ProductsPresentationList ) supplier_form.retriveInternalWidget ( "products" );
		else
			return null;
	}

	private boolean insertProduct ( Product product ) {
		ProductsPresentationList panel;

		panel = retrieveProductsPanel ( product );
		if ( panel != null ) {
			panel.addElement ( product );
			return true;
		}
		else
			return false;
	}

	private void checkProductsSchedule () {
		Product product;

		for ( int i = 0; i < scheduledProducts.size (); i++ ) {
			product = ( Product ) scheduledProducts.get ( i );

			if ( insertProduct ( product ) == true ) {
				scheduledProducts.remove ( i );
				i--;
			}
		}
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Fornitori";
	}

	public String getSystemID () {
		return "suppliers";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_suppliers.png" );
	}

	/*
		Formato concesso per address:
		suppliers::id_fornitore_da_mostrare
	*/
	public void openBookmark ( String address ) {
		int id;
		String [] tokens;
		FromServerForm form;

		tokens = address.split ( "::" );
		id = Integer.parseInt ( tokens [ 1 ] );

		form = main.retrieveFormById ( id );
		if ( form != null )
			form.open ( true );
	}

	public void initView () {
		ObjectRequest params;

		Utils.getServer ().testObjectReceive ( "Supplier" );
		Utils.getServer ().testObjectReceive ( "Product" );
		Utils.getServer ().testObjectReceive ( "Order" );

		params = new ObjectRequest ( "OrderUser" );
		params.add ( "all", 1 );
		Utils.getServer ().testObjectReceive ( params );
	}
}
