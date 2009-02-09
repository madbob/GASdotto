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
	public OrdersPanel () {
		super ();

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Order ord;

				ord = ( Order ) object;
				if ( ord.getInt ( "status" ) == Order.OPENED ) {
					ServerRequest params;

					insert ( doOrderRow ( ord ), 1 );

					params = new ServerRequest ( "OrderUser" );
					params.add ( "order", ord );
					params.add ( "user", Session.getUser () );
					Utils.getServer ().testObjectReceive ( params );
				}
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 )
					syncProductsInForm ( ( FromServerForm ) getWidget ( index ), ( Order ) object );
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 )
					remove ( index );
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm order_form;
				Order order;
				Order tmp_order;

				order = ( Order ) object.getObject ( "order" );

				for ( int i = 0; i < getWidgetCount (); i++ ) {
					order_form = ( FromServerForm ) getWidget ( i );
					tmp_order = ( Order ) order_form.getObject ();

					if ( order.getLocalID () == tmp_order.getObject ( "order" ).getLocalID () ) {
						alignOrderRow ( order_form, ( OrderUser ) object );
						break;
					}
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				FromServerForm form;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "order" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					form.refreshContents ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "order" ) );
				if ( index != -1 )
					remove ( index );
			}
		} );
	}

	private Widget doOrderRow ( Order order ) {
		FromServerForm ver;
		OrderUser uorder;
		ProductsUserSelection products;

		uorder = new OrderUser ();
		uorder.setObject ( "order", order );

		/**
			TODO	Validare l'utente sul lato server
		*/
		uorder.setObject ( "user", Session.getUser () );

		ver = new FromServerForm ( uorder );
		products = new ProductsUserSelection ( order.getArray ( "products" ) );
		ver.add ( ver.getPersonalizedWidget ( "products", products ) );

		return ver;
	}

	private void alignOrderRow ( FromServerForm ver, OrderUser uorder ) {
		ProductsUserSelection products;

		products = ( ProductsUserSelection ) ver.getWidget ( "products" );
		products.setElements ( uorder.getArray ( "products" ) );
	}

	private int retrieveOrderForm ( Order parent ) {
		FromServerForm form;
		Order tmp_order;

		for ( int i = 0; i < getWidgetCount (); i++ ) {
			form = ( FromServerForm ) getWidget ( i );
			tmp_order = ( Order ) form.getObject ();

			if ( parent.getLocalID () == tmp_order.getObject ( "order" ).getLocalID () )
				return i;
		}

		return -1;
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

	public Image getIcon () {
		return new Image ( "images/path_orders.png" );
	}

	public void initView () {
		ServerRequest params;

		params = new ServerRequest ( "Order" );
		params.add ( "status", Order.OPENED );
		Utils.getServer ().testObjectReceive ( params );
	}
}
