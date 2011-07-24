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

public class HomePanel extends GenericPanel {
	private NotificationsBox	notifications;
	private PlainOrdersBox		openedOrders;
	private PlainOrdersBox		closedOrders;

	/****************************************************************** init */

	public HomePanel () {
		super ();

		notifications = new NotificationsBox ();
		add ( notifications );

		openedOrders = doOrdersSummary ( "Ordini aperti in questo momento (in rosso quelli in chiusura)", "Non ci sono ordini aperti in questo momento.", 0 );
		add ( openedOrders );

		closedOrders = doOrdersSummary ( "Ordini ora in consegna (in rosso quelli che saranno consegnati a breve)", "Non ci sono ordini in consegna.", 1 );
		add ( closedOrders );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int ord_status;
					Order ord;

					ord = ( Order ) object.getObject ( "baseorder" );
					ord_status = ord.getInt ( "status" );

					if ( ord_status == Order.OPENED ) {
						if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY )
							openedOrders.removeOrder ( ord );
						else
							openedOrders.addOrderUser ( object );
					}
					else if ( ord_status == Order.CLOSED ) {
						if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY )
							closedOrders.removeOrder ( ord );
						else
							closedOrders.addOrderUser ( object );
					}
				}
			}

			public void onModify ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int ord_status;
					Order ord;

					ord = ( Order ) object.getObject ( "baseorder" );
					ord_status = ord.getInt ( "status" );

					if ( ord_status == Order.OPENED ) {
						if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY )
							openedOrders.removeOrder ( ord );
						else
							openedOrders.addOrderUser ( object );
					}
					else if ( ord_status == Order.CLOSED ) {
						if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY )
							closedOrders.removeOrder ( ord );
						else
							closedOrders.addOrderUser ( object );
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					openedOrders.removeOrderUser ( object );
					closedOrders.removeOrderUser ( object );
				}
			}

			/*
				Questo potrebbe essere messo ovunque, lo piazzo qui perche'
				questa e' una delle prime callbacks che vengono invocate durante
				l'inizializzazione dell'applicazione
			*/
			public void onReceivePreemptive ( FromServer object ) {
				FromServer order;
				OrderAggregate aggregate;
				OrderUserAggregate uaggregate;
				FromServer user;

				order = object.getObject ( "baseorder" );

				if ( order.getBool ( "parent_aggregate" ) == true ) {
					aggregate = OrderAggregate.retrieveAggregate ( order );
					if ( aggregate != null ) {
						user = object.getObject ( "baseuser" );

						uaggregate = OrderUserAggregate.retrieveAggregate ( aggregate, user );

						if ( uaggregate == null ) {
							uaggregate = new OrderUserAggregate ();
							uaggregate.setObject ( "baseorder", aggregate );
							uaggregate.setObject ( "baseuser", user );
							uaggregate.addObject ( object );
							Utils.getServer ().triggerObjectCreation ( uaggregate );
						}
						else {
							uaggregate.addObject ( object );
							Utils.getServer ().triggerObjectModification ( uaggregate );
						}
					}
				}
			}

			protected String debugName () {
				return "OrderUser in HomePanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( object.getBool ( "parent_aggregate" ) == false && object.getInt ( "status" ) == Order.OPENED )
					openedOrders.addRow ( object );

				/*
					Gli ordini gia' chiusi sono eventualmente messi nella lista se e quando si
					trova il relativo ordine da parte dell'utente corrente, altrimenti si evita
					proprio di visualizzarli
				*/
			}

			public void onModify ( FromServer object ) {
				int status;

				if ( object.getBool ( "parent_aggregate" ) == true )
					return;

				status = object.getInt ( "status" );

				if ( status == Order.OPENED ) {
					closedOrders.removeOrder ( object );
					openedOrders.addRow ( object );
				}
				else if ( status == Order.CLOSED ) {
					openedOrders.removeOrder ( object );
					closedOrders.addRow ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				openedOrders.removeOrder ( object );
				closedOrders.removeOrder ( object );
			}

			protected String debugName () {
				return "Order in HomePanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderAggregate", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( object.getInt ( "status" ) == Order.OPENED )
					openedOrders.addRow ( object );
			}

			public void onModify ( FromServer object ) {
				int status;

				status = object.getInt ( "status" );

				if ( status == Order.OPENED ) {
					closedOrders.removeOrder ( object );
					openedOrders.addRow ( object );
				}
				else if ( status == Order.CLOSED ) {
					openedOrders.removeOrder ( object );
					closedOrders.addRow ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				openedOrders.removeOrder ( object );
				closedOrders.removeOrder ( object );
			}

			protected String debugName () {
				return "Order in HomePanel";
			}
		} );
	}

	private PlainOrdersBox doOrdersSummary ( String title, String empty, int expiry ) {
		PlainOrdersBox orders;

		orders = new PlainOrdersBox ( expiry );
		orders.setStrings ( title, empty );

		return orders;
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Home";
	}

	public String getSystemID () {
		return "home";
	}

	public String getCurrentInternalReference () {
		return "";
	}

	public Image getIcon () {
		return new Image ( "images/path_home.png" );
	}

	public void initView () {
		ObjectRequest params;

		notifications.syncList ();
		Utils.getServer ().testObjectReceive ( "OrderAggregate" );
		Utils.getServer ().testObjectReceive ( "Order" );

		params = new ObjectRequest ( "OrderUser" );
		params.add ( "baseuser", Session.getUser ().getLocalID () );
		Utils.getServer ().testObjectReceive ( params );
	}
}
