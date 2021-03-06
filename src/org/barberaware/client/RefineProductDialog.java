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
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

/**
	TODO	Aggiungere funzione per ricaricare lista di ordini, in modo da poter usare sempre
		le stesse istanze di RefineProductDialog in OrderSummary ed aggiornarle alla
		bisogna
*/

public class RefineProductDialog extends Composite {
	private FromServer			targetProduct;
	private ArrayList			userOrders;
	private FlexTable			optionsTable;
	private DialogBox			dialog;

	public RefineProductDialog ( ArrayList orders, FromServer prod ) {
		Image main;

		targetProduct = prod;
		userOrders = orders;

		doDialog ();

		main = new Image ( "images/notify-warning.png" );
		main.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				dialog.center ();
				dialog.show ();
			}
		} );

		initWidget ( main );
	}

	private Widget doButtons () {
		Button but;
		HorizontalPanel buttons;

		buttons = new HorizontalPanel ();
		buttons.setWidth ( "100%" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );

		but = new Button ( "Salva", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				triggerSave ();
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		return buttons;
	}

	private boolean orderMatchesFill ( FromServer order, FlexTable table, int row ) {
		boolean has;
		int num;
		float tot;
		ArrayList products;
		Label username;
		FloatBox quantity;
		FromServer pu;

		has = false;
		tot = 0;

		products = order.getArray ( "allproducts" );
		num = products.size ();

		for ( int i = 0; i < num; i++ ) {
			pu = ( FromServer ) products.get ( i );

			if ( pu.getObject ( "product" ).equals ( targetProduct ) ) {
				has = true;
				tot += pu.getFloat ( "quantity" );
			}
		}

		if ( has == true ) {
			username = new Label ( order.getObject ( "baseuser" ).getString ( "name" ) );
			optionsTable.setWidget ( row, 0, username );

			quantity = new FloatBox ();
			quantity.setVal ( tot );
			optionsTable.setWidget ( row, 1, quantity );
		}

		return has;
	}

	private Widget doChooses () {
		int i;
		int e;
		int tot;
		ArrayList sub_user_orders;
		FromServer order_user;

		optionsTable = new FlexTable ();
		tot = userOrders.size ();
		sub_user_orders = new ArrayList ();

		for ( i = 0, e = 0; i < tot; i++ ) {
			order_user = ( FromServer ) userOrders.get ( i );
			if ( orderMatchesFill ( order_user, optionsTable, e ) ) {
				e++;
				sub_user_orders.add ( order_user );
			}
		}

		userOrders = sub_user_orders;
		return optionsTable;
	}

	private void doDialog () {
		VerticalPanel container;
		Label info;

		dialog = new DialogBox ();
		dialog.setText ( "Sistema Ordine Manualmente" );

		container = new VerticalPanel ();

		info = new Label ( "Dimensione Stock: " + targetProduct.getFloat ( "stock_size" ) );
		info.setStyleName ( "smaller-text" );
		container.add ( info );

		info = new Label ( "Minimo Utente: " + targetProduct.getFloat ( "minimum_order" ) );
		info.setStyleName ( "smaller-text" );
		container.add ( info );

		info = new Label ( "Multiplo Utente: " + targetProduct.getFloat ( "multiple_order" ) );
		info.setStyleName ( "smaller-text" );
		container.add ( info );

		container.add ( doChooses () );
		container.add ( doButtons () );

		dialog.setWidget ( container );
	}

	private void fireChanges () {
		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
	}

	private void alignQuantities ( FloatBox q, FromServer order ) {
		int num;
		float tot;
		float user_selected;
		float difference;
		ArrayList products;
		ArrayList<ProductUser> products_user;
		ProductUser pu;

		tot = 0;
		products_user = new ArrayList<ProductUser> ();

		products = order.getArray ( "allproducts" );
		num = products.size ();

		for ( int i = 0; i < num; i++ ) {
			pu = ( ProductUser ) products.get ( i );

			if ( pu.getObject ( "product" ).equals ( targetProduct ) ) {
				user_selected = pu.getFloat ( "quantity" );

				if ( user_selected != 0 ) {
					tot += user_selected;
					products_user.add ( pu );
				}
			}
		}

		user_selected = q.getVal ();

		if ( user_selected != tot ) {
			if ( user_selected > tot ) {
				pu = products_user.get ( 0 );
				pu.setFloat ( "quantity", pu.getFloat ( "quantity" ) + ( user_selected - tot ) );
				pu.setCurrentUser ();
			}
			else {
				difference = tot - user_selected;
				num = products_user.size ();

				for ( int i = 0; i < num; i++ ) {
					pu = products_user.get ( i );
					user_selected = pu.getFloat ( "quantity" );

					if ( user_selected >= difference ) {
						pu.setFloat ( "quantity", pu.getFloat ( "quantity" ) - difference );
						pu.setCurrentUser ();
						break;
					}
					else {
						pu.setFloat ( "quantity", 0 );
						pu.setCurrentUser ();
						difference = difference - user_selected;
					}
				}
			}

			order.save ( new ServerResponse () {
				public void onComplete ( JSONValue response ) {
					fireChanges ();
				}
			} );
		}
	}

	private void triggerSave () {
		int tot;
		FromServer order_user;
		FloatBox q;

		tot = optionsTable.getRowCount ();

		for ( int i = 0; i < tot; i++ ) {
			q = ( FloatBox ) optionsTable.getWidget ( i, 1 );
			order_user = ( FromServer ) userOrders.get ( i );
			alignQuantities ( q, order_user );
		}
	}
}
