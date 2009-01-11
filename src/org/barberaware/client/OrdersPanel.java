/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;

public class OrdersPanel extends GenericPanel {
	public OrdersPanel () {
		super ();

		Utils.getServer ().onObjectReceive ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Order ord;

				ord = ( Order ) object;
				if ( ord.getInt ( "status" ) == Order.OPENED ) {
					ServerRequest params;

					insert ( doOrderRow ( ord ), 0 );

					params = new ServerRequest ( "OrderUser" );
					params.add ( "order", ord );
					Utils.getServer ().testObjectReceive ( params );
				}
			}
		} );

		Utils.getServer ().onObjectReceive ( "OrderUser", new ServerObjectReceive () {
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
		} );

		addFirstTempRow ( new Label ( "Non ci sono ordini aperti in questo momento." ) );
	}

	private Widget doOrderRow ( Order order ) {
		FromServerForm ver;
		FlexTable fields;
		OrderUser uorder;
		ArrayList products;
		int num_products;
		Product prod;

		uorder = new OrderUser ();
		/**
			TODO	Validare l'utente sul lato server
		*/
		uorder.setObject ( "user", Session.getUser () );
		uorder.setObject ( "order", order );

		ver = new FromServerForm ( uorder );
		ver.setCallback ( new FromServerFormCallbacks () {
			public void onSave ( FromServerForm form ) {
				retriveInputData ( form );
			}

			public void onReset ( FromServerForm form ) {
				alignOrderRow ( form, ( OrderUser ) form.getObject () );
			}

			public void onDelete ( FromServerForm form ) {
				/* dummy */
			}
		} );

		fields = new FlexTable ();
		ver.setExtraWidget ( "list", fields );
		ver.add ( fields );

		products = order.getArray ( "products" );
		num_products = products.size ();

		for ( int i = 0; i < num_products; i++ ) {
			prod = ( Product ) products.get ( i );
			fields.setWidget ( i, 0, new Hidden ( "product", Integer.toString ( prod.getLocalID () ) ) );
			fields.setWidget ( i, 1, new Label ( prod.getString ( "name" ) ) );
			fields.setWidget ( i, 2, new FloatBox () );
			fields.setWidget ( i, 3, new Label ( prod.getObject ( "measure" ).getString ( "symbol" ) ) );
		}

		return ver;
	}

	private void alignOrderRow ( FromServerForm ver, OrderUser uorder ) {
		FlexTable fields;
		ArrayList products;
		int num_products;
		ProductUser prod;
		String parent_id;

		fields = ( FlexTable ) ver.getWidget ( "list" );

		products = uorder.getArray ( "products" );
		num_products = products.size ();

		for ( int i = 0; i < num_products; i++ ) {
			prod = ( ProductUser ) products.get ( i );
			parent_id = Integer.toString ( prod.getObject ( "product" ).getLocalID () );

			for ( int a = 0; a < fields.getRowCount (); a++ ) {
				if ( parent_id.equals ( ( ( Hidden ) fields.getWidget ( i, 0 ) ).getValue () ) ) {
					( ( FloatBox ) fields.getWidget ( i, 2 ) ).setValue ( prod.getFloat ( "quantity" ) );
					break;
				}
			}
		}
	}

	private void retriveInputData ( FromServerForm form ) {
		/**
			TODO
		*/
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
