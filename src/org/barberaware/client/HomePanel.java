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

public class HomePanel extends GenericPanel {
	private NotificationsBox	notifications;
	private PlainFillBox		openedOrders;
	private PlainFillBox		closedOrders;

	/****************************************************************** init */

	public HomePanel () {
		super ();

		notifications = new NotificationsBox ();
		add ( notifications );

		openedOrders = doOrdersSummary ( "Non ci sono ordini aperti in questo momento.", "Ordini aperti in questo momento: " );
		add ( openedOrders );

		add ( new HTML ( "<hr>" ) );

		closedOrders = doOrdersSummary ( "Non ci sono ordini in consegna.", "Ordini ora in consegna: " );
		add ( closedOrders );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
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
						Label total;
						OrderUser uorder;

						uorder = ( OrderUser ) object;
						total = new Label ( " (hai ordinato " + Utils.priceToString ( uorder.getTotalPrice () ) + " €)" );
						total.setStyleName ( "smaller-text" );
						tab.setWidget ( index, 3, total );
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
						OrderUser uorder;
						Label total;

						uorder = ( OrderUser ) object;
						total = ( Label ) tab.getWidget ( index, 3 );

						if ( total != null )
							total.setText ( Utils.priceToString ( uorder.getTotalPrice () ) );
						else
							onReceive ( object );
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
						tab.setWidget ( index, 3, new Label ( "" ) );
				}
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
				else if ( status == Order.CLOSED )
					doOrderRow ( closedOrders, ord );
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
		} );
	}

	private PlainFillBox doOrdersSummary ( String empty, String full ) {
		PlainFillBox orders;

		orders = new PlainFillBox ();
		orders.setStrings ( empty, full );

		orders.addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				Hidden id;
				PlainFillBox table;

				if ( row == 0 )
					return;

				table = ( PlainFillBox ) sender;
				id = ( Hidden ) table.getWidget ( row, 1 );
				goTo ( "orders::" + id.getValue () );
			}
		} );

		return orders;
	}

	private void doOrderRow ( PlainFillBox orders, Order order ) {
		int index;
		String name;
		Label text;
		ArrayList data;

		index = retrieveOrderRow ( orders, order );
		if ( index == -1 ) {
			name = order.getString ( "name" );
			text = new Label ( name );
			text.setStyleName ( "clickable" );

			data = new ArrayList ();
			data.add ( text );
			data.add ( new Hidden ( "id", Integer.toString ( order.getLocalID () ) ) );
			orders.addRow ( data );
		}
	}

	private void modOrderRow ( PlainFillBox orders, Order order ) {
		int index;
		String name;
		Label label;

		index = retrieveOrderRow ( orders, order );
		if ( index != -1 ) {
			name = order.getString ( "name" );
			label = ( Label ) orders.getWidget ( index, 0 );
			label.setText ( "name" );
		}
	}

	private int retrieveOrderRow ( PlainFillBox table, FromServer target ) {
		String target_id_str;
		Hidden id;

		target_id_str = Integer.toString ( target.getLocalID () );

		/*
			Come al solito, qui si parte da 1 perche' in 0 c'e' l'intestazione
		*/
		for ( int i = 1; i < table.getRowCount (); i++ ) {
			id = ( Hidden ) table.getWidget ( i, 1 );

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
		notifications.syncList ();
		Utils.getServer ().testObjectReceive ( "Order" );
		Utils.getServer ().testObjectReceive ( "OrderUser" );
	}
}
