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

public class OrdersPanel extends GenericPanel {
	private Label		emptyLabel		= null;

	public OrdersPanel () {
		super ();

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm order_form;
				Order order;
				Order tmp_order;

				if ( emptyLabel != null )
					return;

				if ( object.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					return;

				order = ( Order ) object.getObject ( "baseorder" );

				for ( int i = 1; i < getWidgetCount (); i++ ) {
					order_form = ( FromServerForm ) getWidget ( i );

					/*
						Provvedo all'allineamento se al form non e'
						ancora stato settato alcun OrderUser realmente
						valido, ma trovo solo quello fittizio assegnato
						in fase di allocazione
					*/
					if ( order_form.getObject ().getLocalID () == -1 ) {
						tmp_order = ( Order ) order_form.getObject ().getObject ( "baseorder" );

						if ( order.getLocalID () == tmp_order.getLocalID () ) {
							order_form.setObject ( object );
							alignOrderRow ( order_form, ( OrderUser ) object );
							break;
						}
					}
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				FromServerForm form;

				if ( object.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					return;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					form.refreshContents ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				if ( object.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					return;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 )
					remove ( index );
			}
		} );

		/*
			A futura memoria: qui non puo' essere applicato un FormCluster perche' ad
			ogni Order devo creare un FromServerForm che ha per oggetto un OrderUser
			di cui l'Order e' il riferimento, dunque la logica differisce troppo da
			quella originaria per essere applicata
		*/

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int index;
				Order ord;

				ord = ( Order ) object;
				if ( ord.getInt ( "status" ) != Order.OPENED )
					return;

				index = retrieveOrderForm ( ord );
				if ( index == -1 )
					insert ( doOrderRow ( ord ), 1 );
			}

			public void onModify ( FromServer object ) {
				int index;
				Order ord;

				ord = ( Order ) object;
				index = retrieveOrderForm ( ord );
				if ( index != -1 ) {
					if ( ord.getInt ( "status" ) == Order.OPENED )
						syncProductsInForm ( ( FromServerForm ) getWidget ( index ), ord );
					else
						remove ( index );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 )
					remove ( index );
			}
		} );

		emptyLabel = new Label ( "Non ci sono ordini aperti in questo momento" );
		addTop ( emptyLabel );
	}

	private Widget doOrderRow ( Order order ) {
		final FromServerForm ver;
		OrderUser uorder;
		ProductsUserSelection products;

		if ( emptyLabel != null ) {
			remove ( emptyLabel );
			emptyLabel = null;
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );
		uorder.setObject ( "baseuser", Session.getUser () );

		ver = new FromServerForm ( uorder, FromServerForm.EDITABLE_UNDELETABLE );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onClose ( FromServerForm form ) {
				ProductsUserSelection products;
				IconsBar icons;
				Label label;
				float total;

				label = ( Label ) form.retriveInternalWidget ( "price_sum" );

				if ( label != null ) {
					products = ( ProductsUserSelection ) form.retriveInternalWidget ( "products" );
					total = products.getTotalPrice ();
					if ( total != 0 )
						label.setText ( total + " €" );
					else
						label.setText ( "" );
				}
			}
		} );

		products = new ProductsUserSelection ( order.getArray ( "products" ) );
		ver.add ( ver.getPersonalizedWidget ( "products", products ) );

		return ver;
	}

	private void alignOrderRow ( FromServerForm ver, OrderUser uorder ) {
		ArrayList products;
		ProductsUserSelection table;
		IconsBar icons;
		float total;
		String total_text;
		Label total_view;

		products = uorder.getArray ( "products" );

		table = ( ProductsUserSelection ) ver.retriveInternalWidget ( "products" );
		table.setElements ( products );

		total = table.getTotalPrice ();

		if ( total != 0 )
			total_text = total + " €";
		else
			total_text = "";

		total_view = ( Label ) ver.retriveInternalWidget ( "price_sum" );

		if ( total_view == null ) {
			icons = ver.getIconsBar ();
			total_view = icons.addText ( total_text );

			/*
				Qui creo e posiziono la label che appare nel riassunto dell'ordine; tale
				label viene poi eventualmente aggiornata quando il sottopannello relativo
				viene chiuso a seguito di qualche correzione
			*/
			ver.setExtraWidget ( "price_sum", total_view );
		}
		else
			total_view.setText ( total_text );
	}

	private int retrieveOrderForm ( int order_id ) {
		FromServerForm form;
		OrderUser tmp_order;

		if ( emptyLabel == null ) {
			for ( int i = 1; i < getWidgetCount (); i++ ) {
				form = ( FromServerForm ) getWidget ( i );
				tmp_order = ( OrderUser ) form.getObject ();

				if ( order_id == tmp_order.getObject ( "baseorder" ).getLocalID () )
					return i;
			}
		}

		return -1;
	}

	private int retrieveOrderForm ( Order parent ) {
		return retrieveOrderForm ( parent.getLocalID () );
	}

	private void syncProductsInForm ( FromServerForm form, Order order ) {
		ProductsUserSelection select;

		select = ( ProductsUserSelection ) form.retriveInternalWidget ( "products" );
		select.upgradeProductsList ( order.getArray ( "products" ) );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Ordini";
	}

	public String getSystemID () {
		return "orders";
	}

	public String getCurrentInternalReference () {
		int index;
		FromServerForm iter;

		index = -1;

		for ( int i = 1; i < getWidgetCount (); i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			if ( iter.isOpen () == true ) {
				index = iter.getObject ().getObject ( "baseorder" ).getLocalID ();
				break;
			}
		}

		return Integer.toString ( index );
	}

	public Image getIcon () {
		return new Image ( "images/path_orders.png" );
	}

	/*
		Formato concesso per address:
		orders::id_ordine_da_mostrare
	*/
	public void openBookmark ( String address ) {
		int id;
		int index;
		String [] tokens;
		FromServerForm form;

		tokens = address.split ( "::" );
		id = Integer.parseInt ( tokens [ 1 ] );

		index = retrieveOrderForm ( id );
		if ( index != -1 ) {
			form = ( FromServerForm ) getWidget ( index );
			form.open ( true );
		}
	}

	public void initView () {
		ServerRequest params;

		params = new ServerRequest ( "Order" );
		params.add ( "status", Order.OPENED );
		Utils.getServer ().testObjectReceive ( params );

		Utils.getServer ().testObjectReceive ( "OrderUser" );
	}
}
