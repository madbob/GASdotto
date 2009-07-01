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
					CustomCaptionPanel frame;
					CaptionPanel sframe;
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
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );

					frame.addPair ( "Fornitore", new Label ( supplier.getString ( "name" ) ) );

					status = new CyclicToggle ();
					status.addState ( "images/order_status_opened.png" );
					status.addState ( "images/order_status_closed.png" );
					status.addState ( "images/order_status_suspended.png" );
					frame.addPair ( "Stato", ver.getPersonalizedWidget ( "status", status ) );

					frame.addPair ( "Anticipo", ver.getWidget ( "anticipated" ) );

					frame = new CustomCaptionPanel ( "Date" );
					hor.add ( frame );

					frame.addPair ( "Data apertura", ver.getWidget ( "startdate" ) );
					frame.addPair ( "Data chiusura", ver.getWidget ( "enddate" ) );
					frame.addPair ( "Data consegna", ver.getWidget ( "shippingdate" ) );
					frame.addPair ( "Si ripete", ver.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );

					/* riassunto ordine */

					sframe = new CaptionPanel ( "Stato Ordini" );
					complete_list = new OrderSummary ( order );
					ver.setExtraWidget ( "summary", complete_list );
					sframe.add ( complete_list );
					ver.add ( sframe );

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					Order order;
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					Widget suppliers_main;
					Date now;
					FromServerSelector suppliers;
					DateSelector date;

					order = new Order ();

					ver = new FromServerForm ( order );

					ver.setCallback ( new FromServerFormCallbacks () {
						public void onSave ( FromServerForm form ) {
							Utils.showNotification ( "Un nuovo ordine è ora disponibile nel pannello 'Ordini'", SmoothingNotify.NOTIFY_INFO );
							ver.setCallback ( null );
						}
					} );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );

					suppliers = new FromServerSelector ( "Supplier", true, true );
					suppliers.addFilter ( new FromServerValidateCallback () {
						public boolean checkObject ( FromServer object ) {
							Supplier sup;
							sup = ( Supplier ) object;
							return sup.iAmReference ();
						}
					} );

					/**
						TODO	Avvertire se il fornitore selezionato non ha prodotti caricati
					*/

					frame.addPair ( "Fornitore", ver.getPersonalizedWidget ( "supplier", suppliers ) );
					ver.setValidation ( "supplier", FromServerValidateCallback.defaultObjectValidationCallback () );

					frame.addPair ( "Stato", new Label ( "Nuovo" ) );
					frame.addPair ( "Anticipo", ver.getWidget ( "anticipated" ) );

					/* seconda colonna */

					frame = new CustomCaptionPanel ( "Date" );
					hor.add ( frame );

					frame.addPair ( "Data apertura", ver.getWidget ( "startdate" ) );
					frame.addPair ( "Data chiusura", ver.getWidget ( "enddate" ) );
					frame.addPair ( "Data consegna", ver.getWidget ( "shippingdate" ) );
					frame.addPair ( "Si ripete", ver.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );

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

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
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
