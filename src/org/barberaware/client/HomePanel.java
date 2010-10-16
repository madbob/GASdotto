/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	private PlainFillBox		openedOrders;
	private PlainFillBox		closedOrders;

	/****************************************************************** init */

	public HomePanel () {
		super ();

		notifications = new NotificationsBox ();
		add ( notifications );

		openedOrders = doOrdersSummary ( "Ordini aperti in questo momento (in rosso quelli in chiusura)", "Non ci sono ordini aperti in questo momento." );
		add ( openedOrders );

		closedOrders = doOrdersSummary ( "Ordini ora in consegna (in rosso quelli che saranno consegnati a breve)", "Non ci sono ordini in consegna." );
		add ( closedOrders );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private void fillTotalText ( Label total, OrderUser order ) {
				total.setText ( " (hai ordinato " + Utils.priceToString ( order.getTotalPrice () ) + ")" );
			}

			private void setOrderedText ( PlainFillBox panel, int index, OrderUser orderuser ) {
				Label total;

				total = new Label ();
				fillTotalText ( total, orderuser );
				total.setStyleName ( "highlight-text" );
				panel.getTable ().setWidget ( index, 3, total );
			}

			public void onReceive ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int index;
					int ord_status;
					Order ord;

					ord = ( Order ) object.getObject ( "baseorder" );
					ord_status = ord.getInt ( "status" );

					if ( ord_status == Order.OPENED ) {
						index = retrieveOrderRow ( openedOrders, ord );

						if ( index != -1 ) {
							if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY )
								openedOrders.removeRow ( index );
							else
								setOrderedText ( openedOrders, index, ( OrderUser ) object );
						}
					}
					else if ( ord_status == Order.CLOSED ) {
						index = retrieveOrderRow ( closedOrders, ord );

						if ( index != -1 ) {
							if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY ) {
								closedOrders.removeRow ( index );
								return;
							}
						}
						else {
							/*
								Se l'ordine e' chiuso e gia' consegnato, non lo visualizzo
							*/
							if ( object.getInt ( "status" ) != OrderUser.COMPLETE_DELIVERY )
								index = doOrderRow ( closedOrders, ord );
							else
								return;
						}

						setOrderedText ( closedOrders, index, ( OrderUser ) object );
					}
				}
			}

			public void onModify ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int index;
					Order ord;
					PlainFillBox tab;

					ord = ( Order ) object.getObject ( "baseorder" );

					index = retrieveOrderRow ( openedOrders, ord );
					if ( index == -1 ) {
						index = retrieveOrderRow ( closedOrders, ord );
						tab = closedOrders;
					}
					else
						tab = openedOrders;

					if ( index != -1 ) {
						if ( object.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY ) {
							tab.removeRow ( index );
						}
						else {
							OrderUser uorder;
							Label total;

							uorder = ( OrderUser ) object;
							total = ( Label ) tab.getTable ().getWidget ( index, 3 );

							if ( total != null )
								fillTotalText ( total, uorder );
							else
								onReceive ( object );
						}
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int index;
					Order ord;
					PlainFillBox tab;

					ord = ( Order ) object.getObject ( "baseorder" );

					index = retrieveOrderRow ( openedOrders, ord );
					if ( index == -1 ) {
						index = retrieveOrderRow ( closedOrders, ord );
						tab = closedOrders;
					}
					else
						tab = openedOrders;

					if ( index != -1 )
						tab.getTable ().setWidget ( index, 3, new Label ( "" ) );
				}
			}

			protected String debugName () {
				return "OrderUser in HomePanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int status;
				Order ord;

				ord = ( Order ) object;
				status = object.getInt ( "status" );

				if ( status == Order.OPENED )
					doOrderRow ( openedOrders, ord );

				/*
					Gli ordini gia' chiusi sono eventualmente messi nella lista se e quando si
					trova il relativo ordine da parte dell'utente corrente, altrimenti si evita
					proprio di visualizzarli
				*/
			}

			public void onModify ( FromServer object ) {
				int index;
				Order ord;

				ord = ( Order ) object;
				index = retrieveOrderRow ( openedOrders, object );

				if ( index != -1 ) {
					if ( object.getInt ( "status" ) == Order.OPENED )
						modOrderRow ( openedOrders, ord );
					else
						openedOrders.removeRow ( index );
				}
				else {
					/*
						Questo e' per gestire ordini che sono stati riaperti
					*/
					if ( object.getInt ( "status" ) == Order.OPENED )
						doOrderRow ( openedOrders, ord );
				}

				index = retrieveOrderRow ( closedOrders, object );

				if ( index != -1 ) {
					if ( object.getInt ( "status" ) == Order.CLOSED )
						modOrderRow ( closedOrders, ord );
					else
						closedOrders.removeRow ( index );
				}
				else {
					/*
						Questo e' per gestire ordini che sono stati richiusi
					*/
					if ( object.getInt ( "status" ) == Order.CLOSED )
						doOrderRow ( closedOrders, ord );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderRow ( openedOrders, object );
				if ( index != -1 )
					openedOrders.removeRow ( index );

				index = retrieveOrderRow ( closedOrders, object );
				if ( index != -1 )
					closedOrders.removeRow ( index );
			}

			protected String debugName () {
				return "Order in HomePanel";
			}
		} );
	}

	private PlainFillBox doOrdersSummary ( String title, String empty ) {
		PlainFillBox orders;

		orders = new PlainFillBox ();
		orders.setStrings ( title, empty );

		orders.getTable ().addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				Hidden id;
				FlexTable table;

				table = ( FlexTable ) sender;

				try {
					id = ( Hidden ) table.getWidget ( row, 1 );
					if ( id != null )
						goTo ( "orders::" + id.getValue () );
				}
				catch ( Exception e ) {
					/* dummy */
				}
			}
		} );

		return orders;
	}

	private void checkOrderExpiry ( PlainFillBox orders, Order order, Label text ) {
		long now;
		long d;
		Date date;

		now = System.currentTimeMillis ();

		if ( orders == openedOrders ) {
			d = order.getDate ( "enddate" ).getTime ();
		}
		else {
			date = order.getDate ( "shippingdate" );

			if ( date != null )
				d = date.getTime ();
			else
				d = -1;
		}

		if ( d != -1 && d - now < ( 1000 * 60 * 60 * 24 * 2 ) )
			text.addStyleName ( "highlight-text" );
		else
			text.removeStyleName ( "highlight-text" );
	}

	private int doOrderRow ( PlainFillBox orders, Order order ) {
		int index;
		String name;
		Label text;
		ArrayList data;

		index = retrieveOrderRow ( orders, order );
		if ( index == -1 ) {
			name = order.getString ( "name" );
			text = new Label ( name );
			text.setStyleName ( "clickable" );
			checkOrderExpiry ( orders, order, text );

			data = new ArrayList ();
			data.add ( text );
			data.add ( new Hidden ( "id", Integer.toString ( order.getLocalID () ) ) );

			return orders.addRow ( data );
		}
		else {
			return -1;
		}
	}

	private void modOrderRow ( PlainFillBox orders, Order order ) {
		int index;
		Label label;

		index = retrieveOrderRow ( orders, order );
		if ( index != -1 ) {
			label = ( Label ) orders.getTable ().getWidget ( index, 0 );
			label.setText ( order.getString ( "name" ) );
			checkOrderExpiry ( orders, order, label );
		}
	}

	private int retrieveOrderRow ( PlainFillBox table, FromServer target ) {
		String target_id_str;
		Hidden id;
		FlexTable contents;

		if ( table.isEmpty () == true )
			return -1;

		target_id_str = Integer.toString ( target.getLocalID () );
		contents = table.getTable ();

		for ( int i = 0; i < contents.getRowCount (); i++ ) {
			id = ( Hidden ) contents.getWidget ( i, 1 );
			if ( target_id_str.equals ( id.getValue () ) )
				return i;
		}

		return -1;
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
		Utils.getServer ().testObjectReceive ( "Order" );

		params = new ObjectRequest ( "OrderUser" );
		params.add ( "baseuser", Session.getUser ().getLocalID () );
		Utils.getServer ().testObjectReceive ( params );
	}
}
