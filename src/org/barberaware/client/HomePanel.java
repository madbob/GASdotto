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
import com.google.gwt.user.client.ui.*;

public class HomePanel extends GenericPanel {
	private NotificationsBox	notifications;
	private VerticalPanel		orders;
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

		orders = new VerticalPanel ();
		orders.add ( new Label ( "Non ci sono ordini aperti in questo momento." ) );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( hasOrders == false ) {
					orders.remove ( 0 );
					orders.add ( new Label ( "Ordini aperti in questo momento: " ) );
					hasOrders = true;
				}

				if ( object.getInt ( "status" ) == Order.OPENED )
					orders.add ( doOrderRow ( ( Order ) object, null ) );
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveOrderRow ( object );

				if ( index != -1 ) {
					if ( object.getInt ( "status" ) == Order.OPENED )
						doOrderRow ( ( Order ) object, ( HorizontalPanel ) orders.getWidget ( index ) );
					else
						orders.remove ( index );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderRow ( object );
				if ( index != -1 )
					orders.remove ( index );
			}
		} );

		return orders;
	}

	private Widget doOrderRow ( Order order, HorizontalPanel hor ) {
		String name;
		Label label;

		/**
			TODO	Rendere le righe degli ordini cliccabili per raggiungere
				direttamente il form di input
		*/

		name = order.getString ( "name" );

		if ( hor == null ) {
			hor = new HorizontalPanel ();
			hor.add ( new Hidden ( "id", Integer.toString ( order.getLocalID () ) ) );

			label = new Label ( name );
			hor.add ( label );
		}
		else {
			label = ( Label ) hor.getWidget ( 1 );
			label.setText ( name );
		}

		return hor;
	}

	private int retrieveOrderRow ( FromServer target ) {
		String target_id_str;
		HorizontalPanel row;
		Hidden id;

		target_id_str = Integer.toString ( target.getLocalID () );

		/*
			Come al solito, qui si parte da 1 perche' in 0 c'e' l'intestazione
		*/
		for ( int i = 1; i < orders.getWidgetCount (); i++ ) {
			row = ( HorizontalPanel ) orders.getWidget ( i );
			id = ( Hidden ) row.getWidget ( 0 );

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

	public Image getIcon () {
		return new Image ( "images/path_home.png" );
	}

	public void initView () {
		notifications.syncList ();
		Utils.getServer ().testObjectReceive ( "Order" );
	}
}
