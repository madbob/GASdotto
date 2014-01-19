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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class HomePanel extends GenericPanel {
	private class OrderToCloseDialog extends DialogBox {
		private FromServer ord;

		public OrderToCloseDialog ( FromServer order ) {
			String condition;
			Date close;
			VerticalPanel container;
			HorizontalPanel buttons;
			Button but;

			ord = order;

			close = order.getDate ( "shippingdate" );
			if ( close == null ) {
				close = order.getDate ( "enddate" );
				condition = "chiuso il " + Utils.printableDate ( close );
			}
			else {
				condition = "consegnato il " + Utils.printableDate ( close );
			}

			container = new VerticalPanel ();

			container.add ( new HTML ( "<p>Un ordine per " + ord.getObject ( "supplier" ).getString ( "name" ) + " risulta essere stato " + condition + ". " +
					"E' fortemente consigliato marcare tale ordine con lo stato di \"Consegnato\", in modo che non sia più visualizzato nella lista di quelli validi. " +
					"Tale elemento sarà comunque sempre reperibile in futuro con la \"Modalità Ricerca Ordini\" nel pannello \"Gestione Ordini\".</p>" ) );

			buttons = new HorizontalPanel ();
			buttons.setWidth ( "100%" );
			container.add ( buttons );

			but = new Button ( "Va bene, fallo sparire", new ClickHandler () {
				public void onClick ( ClickEvent event ) {
					ord.setInt ( "status", Order.SHIPPED );
					ord.save ( null );
					hide ();
				}
			} );
			buttons.add ( but );
			buttons.setCellHorizontalAlignment ( but, HasHorizontalAlignment.ALIGN_CENTER );

			but = new Button ( "No, non fare nulla", new ClickHandler () {
				public void onClick ( ClickEvent event ) {
					hide ();
				}
			} );
			buttons.add ( but );
			buttons.setCellHorizontalAlignment ( but, HasHorizontalAlignment.ALIGN_CENTER );

			this.setText ( "Chiudi Ordine" );
			this.setWidget ( container );
		}
	}

	private NotificationsBox	notifications;
	private PlainOrdersBox		openedOrders;
	private PlainOrdersBox		closedOrders;
	private CaptionPanel		creditBox			= null;

	/****************************************************************** init */

	public HomePanel () {
		super ();

		notifications = new NotificationsBox ();
		add ( notifications );

		if ( Session.getGAS ().getBool ( "use_bank" ) == true )
			add ( doCreditBox () );

		openedOrders = doOrdersSummary ( "Ordini aperti in questo momento (in rosso quelli in chiusura)", "Non ci sono ordini aperti in questo momento.", 0 );
		add ( openedOrders );

		closedOrders = doOrdersSummary ( "Ordini ora in consegna (in rosso quelli che saranno consegnati a breve)", "Non ci sono ordini in consegna.", 1 );
		add ( closedOrders );

		add ( new HTML ( "<hr />" ) );
		add ( new LinksBox () );

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
				ArrayList friends;
				FromServer order;
				OrderAggregate aggregate;
				OrderUserAggregate uaggregate;
				FromServer user;

				/*
					L'attributo "parent" in OrderUserFriend e' virtuale, non viene mappato sul
					database, dunque lo forzo qui (vedi la classe OrderUserFriend per altri
					dettagli).
					E' importante che cio' accada prima dell'aggregazione in OrderUserAggregate,
					implementata sotto
				*/

				friends = object.getArray ( "friends" );

				for ( int i = 0; i < friends.size (); i++ ) {
					user = ( FromServer ) friends.get ( i );
					user.setObject ( "parent", object );
				}

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
				int status;

				status = object.getInt ( "status" );

				if ( object.getBool ( "parent_aggregate" ) == false && status == Order.OPENED )
					openedOrders.addRow ( object );

				if ( status == Order.CLOSED )
					checkOrderToClose ( object );

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
	
	private void populateCreditBox () {
		User user;
		
		if ( Session.getGAS ().getBool ( "use_bank" ) == false || creditBox == null )
			return;

		user = Session.getUser ();
		creditBox.clear ();
		creditBox.add ( new HTML ( "Il tuo credito disponibile ammonta a <span class=\"big-text\">" + Utils.priceToString ( user.getFloat ( "current_balance" ) ) + "</span>" ) );
	}

	private Widget doCreditBox () {
		creditBox = new CaptionPanel ( "Credito" );
		populateCreditBox ();		
		return creditBox;
	}

	private PlainOrdersBox doOrdersSummary ( String title, String empty, int expiry ) {
		PlainOrdersBox orders;

		orders = new PlainOrdersBox ( expiry );
		orders.setStrings ( title, empty );

		return orders;
	}

	private void checkOrderToClose ( FromServer order ) {
		int month;
		int year;
		Date close;
		OrderInterface ord;
		OrderToCloseDialog dialog;

		/*
			Somma un mese alla data di consegna, o a quella di
			chiusura, e controlla se la data finale e' precedente a
			quella attuale. Banalmente ispirato da
			http://code.google.com/p/gwt-examples/wiki/gwtDateTime#Subtract_Months
		*/

		ord = ( OrderInterface ) order;
		if ( ord.iAmReference () == false )
			return;

		close = order.getDate ( "shippingdate" );
		if ( close == null )
			close = order.getDate ( "enddate" );

		close = ( Date ) close.clone ();
		month = close.getMonth ();
		year = close.getYear ();

		if ( month >= 11 ) {
			month = 1;
			year += 1;
		}
		else {
			month += 1;
		}

		close.setMonth ( month );
		close.setYear ( year );

		if ( close.before ( new Date ( System.currentTimeMillis () ) ) ) {
			dialog = new OrderToCloseDialog ( order );
			dialog.center ();
			dialog.show ();
		}
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

		populateCreditBox ();
	}
}
