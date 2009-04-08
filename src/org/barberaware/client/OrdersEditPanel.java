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
					OrderSummary complete_list;

					order = ( Order ) ord;
					supplier = ( Supplier ) order.getObject ( "supplier" );

					if ( supplier.iAmReference () == false )
						return null;

					ver = new FromServerForm ( order );

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

					complete_list = new OrderSummary ( order );
					ver.setExtraWidget ( "summary", complete_list );
					ver.add ( complete_list );
					complete_list.addStyleName ( "sub-elements-details" );

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
					DateSelector date;

					order = new Order ();

					ver = new FromServerForm ( order );

					hor = new HorizontalPanel ();
					ver.add ( hor );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Fornitore" ) );

					suppliers = new FromServerSelector ( "Supplier", true, true );
					suppliers.addFilter ( new FromServerValidateCallback () {
						public boolean checkObject ( FromServer object ) {
							Supplier sup;
							sup = ( Supplier ) object;
							return sup.iAmReference ();
						}
					} );
					fields.setWidget ( 0, 1, ver.getPersonalizedWidget ( "supplier", suppliers ) );

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

					return ver;
				}
		};

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private void syncOrder ( OrderUser user ) {
				Order order;
				FromServerForm form;
				OrderSummary summary;

				order = ( Order ) user.getObject ( "baseorder" );
				form = main.retrieveForm ( order );

				if ( form != null ) {
					summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
					summary.syncOrders ();
				}
			}

			public void onReceive ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}

			public void onModify ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}

			public void onDestroy ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}
		} );

		addTop ( main );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Ordini";
	}

	public String getSystemID () {
		return "edit_orders";
	}

	public Image getIcon () {
		return new Image ( "images/path_orders_edit.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Supplier" );
		Utils.getServer ().testObjectReceive ( "Order" );
		Utils.getServer ().testObjectReceive ( "OrderUser" );
	}
}
