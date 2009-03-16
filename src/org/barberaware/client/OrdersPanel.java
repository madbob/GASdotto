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

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm order_form;
				Order order;
				Order tmp_order;

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

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					form.refreshContents ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 )
					remove ( index );
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Order ord;

				ord = ( Order ) object;
				if ( ord.getInt ( "status" ) == Order.OPENED )
					insert ( doOrderRow ( ord ), 1 );
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 ) {
					if ( object.getInt ( "status" ) == Order.OPENED )
						syncProductsInForm ( ( FromServerForm ) getWidget ( index ), ( Order ) object );
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
	}

	private Widget doOrderRow ( Order order ) {
		final FromServerForm ver;
		OrderUser uorder;
		User current_user;
		ProductsUserSelection products;

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );

		current_user = Session.getUser ();

		ver = new FromServerForm ( uorder, FromServerForm.EDITABLE_UNDELETABLE );

		if ( current_user.getInt ( "privileges" ) == User.USER_COMMON ) {
			uorder.setObject ( "baseuser", current_user );

			ver.setCallback ( new FromServerFormCallbacks () {
				public void onSave ( FromServerForm form ) {
					/* dummy */
				}

				public void onReset ( FromServerForm form ) {
					/* dummy */
				}

				public void onDelete ( FromServerForm form ) {
					/* dummy */
				}

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
		}
		else {
			HorizontalPanel pan;

			pan = new HorizontalPanel ();
			pan.add ( new Label ( "Ordine eseguito a nome di " ) );

			/**
				TODO	Quando viene selezionato un utente, caricare eventuali
					dati dell'ordine gia' eseguito
			*/

			pan.add ( ver.getWidget ( "baseuser" ) );
			ver.add ( pan );

			ver.setCallback ( new FromServerFormCallbacks () {
				public void onSave ( FromServerForm form ) {
					/* dummy */
				}

				public void onReset ( FromServerForm form ) {
					/* dummy */
				}

				public void onDelete ( FromServerForm form ) {
					/* dummy */
				}

				public void onClose ( FromServerForm form ) {
					FromServerSelector user;
					ProductsUserSelection products;

					/*
						Una volta salvato il tutto, riazzero il form per
						essere pronto ad un ordine a nome terzi
					*/

					user = ( FromServerSelector ) form.retriveInternalWidget ( "baseuser" );
					user.setValue ( null );

					products = ( ProductsUserSelection ) form.retriveInternalWidget ( "products" );
					products.setElements ( null );
				}
			} );
		}

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

	private int retrieveOrderForm ( Order parent ) {
		FromServerForm form;
		OrderUser tmp_order;

		for ( int i = 1; i < getWidgetCount (); i++ ) {
			form = ( FromServerForm ) getWidget ( i );
			tmp_order = ( OrderUser ) form.getObject ();

			if ( parent.getLocalID () == tmp_order.getObject ( "baseorder" ).getLocalID () )
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

		Utils.getServer ().testObjectReceive ( "OrderUser" );
	}
}
