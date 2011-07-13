/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class OrderUserAggregateManager extends FromServerRappresentation implements ObjectWidget, SourcesChangeEvents {
	private int			currentLayer;
	private boolean			fullEditable;

	private VerticalPanel		list;
	private RadioButtons		buttons		= null;
	private ObjectLinksDialog	exportFiles;

	private FromServer		baseOrder;

	public OrderUserAggregateManager ( FromServer order, boolean editable, String name ) {
		boolean freedit;
		VerticalPanel main;
		ArrayList orders;
		Order sub_order;
		OrderUser sub_uorder;
		OrderUserManager box;
		CaptionPanel frame;

		main = new VerticalPanel ();
		initWidget ( main );

		main.setStyleName ( "order-manager" );

		fullEditable = ( ( ( OrderAggregate ) order ).iAmReference () );
		freedit = fullEditable && ( order.getInt ( "status" ) != Order.OPENED );
		baseOrder = order;

		if ( editable == true ) {
			buttons = new RadioButtons ();
			main.add ( buttons );
			main.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );

			buttons.add ( new Image ( "images/plain_order.png" ), "Per me, " );
			if ( fullEditable ) {
				buttons.add ( new Image ( "images/friends_order.png" ), "gli amici, " );
				buttons.add ( new Image ( "images/multi_order.png" ), "e gli altri" );
			}
			else {
				buttons.add ( new Image ( "images/friends_order.png" ), "e gli amici" );
			}

			buttons.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					int index;
					OrderUserManager box;

					index = buttons.getToggled ();

					for ( int i = 0; i < list.getWidgetCount (); i++ ) {
						box = ( OrderUserManager ) list.getWidget ( i );
						box.forceLayer ( index );
					}

					currentLayer = index;
				}
			} );

			buttons.setToggled ( 0 );
		}

		frame = new CaptionPanel ( "Esporta Report" );
		frame.addStyleName ( "print-reports-box" );
		main.add ( frame );

		exportFiles = new ObjectLinksDialog ( "Esporta Report" );
		exportFiles.addLinkTemplate ( "CSV", "order_friends.php?format=csv&amp;aggregate=#&amp;user=#" );
		exportFiles.addLinkTemplate ( "PDF", "order_friends.php?format=pdf&amp;aggregate=#&amp;user=#" );

		/*
			L'oggetto cui deve far riferimento l'ObjectLinksDialog e' l'aggregazione degli ordini
			dell'utente, che pero' sul server non e' mappata (e' mappata solo l'aggregazione degli ordini
			presso i fornitori). Dunque devo identificare l'aggregazione degli ordini utente come la summa
			dell'ID dell'aggregato e l'ID dell'utente
		*/
		exportFiles.setValues ( order, Session.getUser () );

		frame.add ( exportFiles );

		list = new VerticalPanel ();
		list.setWidth ( "100%" );
		main.add ( list );

		orders = order.getArray ( "orders" );

		for ( int i = 0; i < orders.size (); i++ ) {
			sub_order = ( Order ) orders.get ( i );
			box = new OrderUserManager ( sub_order, editable );
			box.embeddedMode ( true );

			sub_uorder = new OrderUser ();
			sub_uorder.setObject ( "baseorder", sub_order );
			sub_uorder.setObject ( "baseuser", Session.getUser () );

			box.setValue ( sub_uorder );

			list.add ( box );
			addChild ( box );

			sub_order.addRelatedInfo ( "OrdersPrivilegedPanel", box );
		}

		currentLayer = 0;
	}

	public void upgradeProductsList ( FromServer order ) {
		OrderUserManager panel;

		for ( int a = 0; a < list.getWidgetCount (); a++ ) {
			panel = ( OrderUserManager ) list.getWidget ( a );

			if ( panel.getBaseOrder ().equals ( order ) ) {
				panel.upgradeProductsList ( order );
				break;
			}
		}
	}

	public void clean () {
		activateLayer ( 0, null );
		exportFiles.setValue ( null );
	}

	private void activateLayer ( int index, FromServer element ) {
		OrderUserManager panel;
		FromServer order;

		if ( element != null ) {
			order = element.getObject ( "baseorder" );

			for ( int a = 0; a < list.getWidgetCount (); a++ ) {
				panel = ( OrderUserManager ) list.getWidget ( a );

				if ( panel.getBaseOrder ().equals ( order ) ) {
					panel.setValue ( element );
					break;
				}
			}
		}
		else {
			for ( int a = 0; a < list.getWidgetCount (); a++ ) {
				panel = ( OrderUserManager ) list.getWidget ( a );
				panel.setValue ( null );
			}
		}

		if ( buttons != null )
			buttons.setToggled ( index );

		currentLayer = index;
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		/*
			Qui e' ben difficile che entrera' mai, in quanto gli ordini utente aggregati sono gestiti da
			tutt'altra parte. Quando arriva un OrderUser viene assegnato direttamente
			all'OrderUserManager di riferimento per l'ordine specifico
		*/

		int index;
		ArrayList f;
		ArrayList orders;

		index = currentLayer;

		if ( element instanceof OrderUserAggregate ) {
			orders = element.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ )
				setValue ( ( FromServer ) orders.get ( i )  );
		}
		else {
			if ( element == null )
				element = OrderUser.findMine ( baseOrder );

			if ( element == null ) {
				index = 0;
			}
			else {
				if ( element.getObject ( "baseuser" ).equals ( Session.getUser () ) == false ) {
					if ( fullEditable == false ) {
						setValue ( null );
						return;
					}

					index = 2;
				}
				else {
					f = element.getArray ( "friends" );
					if ( f != null && f.size () != 0 )
						index = 1;
				}
			}

			activateLayer ( index, element );
		}
	}

	public FromServer getValue () {
		ArrayList array;
		OrderUserAggregate aggr;
		OrderUserManager box;

		aggr = new OrderUserAggregate ();

		array = new ArrayList ();

		for ( int i = 0; i < list.getWidgetCount (); i++ ) {
			box = ( OrderUserManager ) list.getWidget ( i );
			array.add ( box.getValue () );
		}

		aggr.setObject ( "baseorder", baseOrder );
		aggr.setArray ( "orders", array );
		return aggr;
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		OrderUserManager box;

		for ( int i = 0; i < list.getWidgetCount (); i++ ) {
			box = ( OrderUserManager ) list.getWidget ( i );
			box.addChangeListener ( listener );
		}
	}

	public void removeChangeListener ( ChangeListener listener ) {
		OrderUserManager box;

		for ( int i = 0; i < list.getWidgetCount (); i++ ) {
			box = ( OrderUserManager ) list.getWidget ( i );
			box.removeChangeListener ( listener );
		}
	}
}
