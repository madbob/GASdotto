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
	private FlexTable		orders;
	private boolean			hasOrders;

	/****************************************************************** init */

	public HomePanel () {
		super ();

		notifications = new NotificationsBox ();
		add ( notifications );

		add ( doOrdersSummary () );
	}

	private Panel doOrdersSummary () {
		hasOrders = false;

		orders = new FlexTable ();

		orders.addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				Hidden id;

				if ( row == 0 )
					return;

				id = ( Hidden ) orders.getWidget ( row, 0 );
				goTo ( "orders::" + id.getValue () );
			}
		} );

		checkNoOrders ();

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int index;

					index = retrieveOrderRow ( ( Order ) object.getObject ( "baseorder" ) );
					if ( index != -1 ) {
						Label total;
						OrderUser uorder;

						uorder = ( OrderUser ) object;
						total = new Label ( " (hai già ordinato " + Utils.priceToString ( uorder.getTotalPrice () ) + " €)" );
						total.setStyleName ( "smaller-text" );
						orders.setWidget ( index, 3, total );
					}
				}
			}

			public void onModify ( FromServer object ) {
				if ( Session.getUser ().equals ( object.getObject ( "baseuser" ) ) ) {
					int index;

					index = retrieveOrderRow ( ( Order ) object.getObject ( "baseorder" ) );
					if ( index == -1 ) {
						OrderUser uorder;
						Label total;

						uorder = ( OrderUser ) object;
						total = ( Label ) orders.getWidget ( index, 3 );

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

					index = retrieveOrderRow ( ( Order ) object.getObject ( "baseorder" ) );
					if ( index == -1 )
						orders.setWidget ( index, 3, new Label ( "" ) );
				}
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( object.getInt ( "status" ) == Order.OPENED ) {
					if ( hasOrders == false ) {
						orders.removeRow ( 0 );
						orders.setWidget ( 0, 0, new Label ( "Ordini aperti in questo momento: " ) );
						hasOrders = true;
					}

					doOrderRow ( ( Order ) object );
				}
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveOrderRow ( object );

				if ( index != -1 ) {
					if ( object.getInt ( "status" ) == Order.OPENED ) {
						modOrderRow ( ( Order ) object );
					}
					else {
						orders.removeRow ( index );
						checkNoOrders ();
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderRow ( object );
				if ( index != -1 ) {
					orders.removeRow ( index );
					checkNoOrders ();
				}
			}
		} );

		return orders;
	}

	private void checkNoOrders () {
		/*
			Se c'e' una sola riga, e' l'header della lista degli ordini aperti e/o la
			notifica dell'assenza di ordini
		*/
		if ( orders.getRowCount () <= 1 ) {
			hasOrders = false;
			orders.setWidget ( 0, 0, new Label ( "Non ci sono ordini aperti in questo momento." ) );
		}
	}

	private void doOrderRow ( Order order ) {
		int row;
		int index;
		String name;
		Label text;

		index = retrieveOrderRow ( order );
		if ( index == -1 ) {
			row = orders.getRowCount ();
			name = order.getString ( "name" );

			orders.setWidget ( row, 0, new Hidden ( "id", Integer.toString ( order.getLocalID () ) ) );

			text = new Label ( name );
			text.setStyleName ( "clickable" );
			orders.setWidget ( row, 1, text );
		}
	}

	private void modOrderRow ( Order order ) {
		int index;
		String name;
		Label label;

		index = retrieveOrderRow ( order );
		if ( index != -1 ) {
			name = order.getString ( "name" );
			label = ( Label ) orders.getWidget ( index, 1 );
			label.setText ( "name" );
		}
	}

	private int retrieveOrderRow ( FromServer target ) {
		String target_id_str;
		Hidden id;

		target_id_str = Integer.toString ( target.getLocalID () );

		/*
			Come al solito, qui si parte da 1 perche' in 0 c'e' l'intestazione
		*/
		for ( int i = 1; i < orders.getRowCount (); i++ ) {
			id = ( Hidden ) orders.getWidget ( i, 0 );

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
