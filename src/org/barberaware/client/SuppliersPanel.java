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

public class SuppliersPanel extends GenericPanel {
	private FormCluster		main;

	public SuppliersPanel () {
		super ();

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

			protected void asyncLoad ( FromServerForm form ) {
				ObjectRequest params;
				Lockable products;
				Lockable references;

				products = ( Lockable ) form.retriveInternalWidget ( "products" );
				products.unlock ();

				params = new ObjectRequest ( "Product" );
				params.add ( "supplier", form.getObject ().getLocalID () );
				Utils.getServer ().testObjectReceive ( params );

				params = new ObjectRequest ( "Order" );
				params.add ( "status", "any" );
				params.add ( "supplier", form.getObject ().getLocalID () );
				params.add ( "query_limit", 10 );
				Utils.getServer ().testObjectReceive ( params );

				params = new ObjectRequest ( "OrderUser" );
				params.add ( "all", 1 );
				params.add ( "supplier", form.getObject ().getLocalID () );
				params.add ( "baseuser", Session.getUser ().getLocalID () );
				params.add ( "query_limit", 10 );
				Utils.getServer ().testObjectReceive ( params );
			}
		};

		addTop ( Utils.getEmblemsCache ( "supplier" ).getLegend () );
		addTop ( main );

		/*
			Questo viene eseguito appunto quando arriva qualche dato nuovo dal
			server, non per inizializzare pannelli esistenti.
			Cfr. OpenedOrdersList::OpenedOrdersList()
		*/
		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				sharedOrderManagement ( object, main, 0 );
			}

			public void onModify ( FromServer object ) {
				sharedOrderManagement ( object, main, 1 );
			}

			public void onDestroy ( FromServer object ) {
				sharedOrderManagement ( object, main, 2 );
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				sharedOrderUserManagement ( object, main, 0 );
			}

			public void onModify ( FromServer object ) {
				sharedOrderUserManagement ( object, main, 1 );
			}

			public void onDestroy ( FromServer object ) {
				sharedOrderUserManagement ( object, main, 2 );
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().testObjectReceive ( "Supplier" );
	}

	/*
		Per comodita' questa funzione viene usata anche in SuppliersEditPanel
	*/
	public static void sharedOrderUserManagement ( FromServer object, FormCluster cluster, int action ) {
		PastOrdersList list;
		FromServerForm form;
		Order ord;

		if ( object.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
			return;

		ord = ( Order ) object.getObject ( "baseorder" );
		form = cluster.retrieveForm ( ord.getObject ( "supplier" ) );

		if ( form != null ) {
			list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );

			switch ( action ) {
				case 0:
					list.addOrder ( object );
					break;

				case 1:
					list.modOrder ( object );
					break;

				case 2:
					list.delOrder ( object );
					break;
			}
		}
	}

	/*
		Per comodita' questa funzione viene usata anche in SuppliersEditPanel
	*/
	public static void sharedOrderManagement ( FromServer object, FormCluster cluster, int action ) {
		FromServerForm form;

		form = cluster.retrieveForm ( object.getObject ( "supplier" ) );

		if ( form != null ) {
			OpenedOrdersList list;

			list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );

			switch ( action ) {
				case 0:
					list.addOrder ( ( Order ) object );
					break;

				case 1:
					list.modOrder ( ( Order ) object );
					break;

				case 2:
					list.delOrder ( ( Order ) object );
					break;
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
		Utils.getServer ().testObjectReceive ( "Supplier" );
	}
}
