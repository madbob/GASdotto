/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class OrderUserManager extends FromServerRappresentation implements ObjectWidget, SourcesChangeEvents {
	private DeckPanel		deck;
	private RadioButtons		buttons		= null;
	private ObjectLinksDialog	exportFiles;

	private FromServer		baseOrder;

	private OrderUserManagerMode	plain;
	private OrderUserManagerMode	friends;
	private OrderUserManagerMode	multi;

	public OrderUserManager ( FromServer order, boolean editable ) {
		boolean aggregate;
		boolean reference;
		boolean freedit;
		String order_identifier;
		VerticalPanel main;
		CaptionPanel frame;

		main = new VerticalPanel ();
		initWidget ( main );

		main.setStyleName ( "order-manager" );

		reference = ( ( OrderInterface ) order ).iAmReference ();
		freedit = reference && ( order.getInt ( "status" ) != Order.OPENED );
		baseOrder = order;

		if ( editable == true ) {
			buttons = new RadioButtons ();
			main.add ( buttons );
			main.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );

			buttons.add ( new Image ( "images/plain_order.png" ), "Per me, " );
			if ( reference ) {
				buttons.add ( new Image ( "images/friends_order.png" ), "gli amici, " );
				buttons.add ( new Image ( "images/multi_order.png" ), "e gli altri" );
			}
			else {
				buttons.add ( new Image ( "images/friends_order.png" ), "e gli amici" );
			}

			buttons.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					int index;
					FromServer order;
					ObjectWidget current;
					OrderUserManagerMode next;

					index = buttons.getToggled ();

					current = ( ObjectWidget ) deck.getWidget ( deck.getVisibleWidget () );
					next = ( OrderUserManagerMode ) deck.getWidget ( index );

					order = current.getValue ();
					if ( order.getObject ( "baseuser" ).equals ( Session.getUser () ) )
						next.setValue ( order );

					next.unlock ();
					deck.showWidget ( index );
				}
			} );

			buttons.setToggled ( 0 );
		}

		frame = new CaptionPanel ( "Esporta Report" );
		frame.addStyleName ( "print-reports-box" );
		main.add ( frame );

		aggregate = ( order instanceof OrderAggregate );

		order_identifier = Integer.toString ( order.getLocalID () );
		if ( aggregate == true )
			order_identifier += "&amp;aggregate=true";

		exportFiles = new ObjectLinksDialog ( "Esporta Report" );
		exportFiles.addLinkTemplate ( "CSV", "order_friends.php?format=csv&amp;user=#&amp;id=" + order_identifier );
		exportFiles.addLinkTemplate ( "PDF", "order_friends.php?format=pdf&amp;user=#&amp;id=" + order_identifier );
		frame.add ( exportFiles );

		deck = new DeckPanel ();
		main.add ( deck );

		if ( aggregate == false )
			plain = new OrderUserPlainPanel ( order, editable, freedit );
		else
			plain = new OrderUserPlainAggregatePanel ( order, editable, freedit );

		deck.add ( plain );

		if ( aggregate == false )
			friends = new OrderUserFriendPanel ( order, editable, freedit, true );
		else
			friends = new OrderUserFriendAggregatePanel ( order, editable, freedit, true );

		deck.add ( friends );

		if ( reference ) {
			if ( aggregate == false )
				multi = new OrderUserMultiPanel ( order, editable, freedit );
			else
				multi = new OrderUserMultiAggregatePanel ( order, editable, freedit );

			multi.addOrderListener ( new FromServerCallback () {
				public void execute ( FromServer object ) {
					setupExportingFile ( object );
				}
			} );

			deck.add ( multi );
		}
		else {
			multi = null;
		}

		deck.showWidget ( 0 );
	}

	public void upgradeProductsList ( FromServer order ) {
		OrderUserManagerMode panel;

		for ( int i = 0; i < deck.getWidgetCount (); i++ ) {
			panel = ( OrderUserManagerMode ) deck.getWidget ( i );
			panel.upgradeOrder ( order );
		}
	}

	public void clean () {
		activateLayer ( 0, null );
	}

	public void forceLayer ( int index ) {
		buttons.propagateToggled ( index );
	}

	public FromServer getBaseOrder () {
		return baseOrder;
	}

	private void setupExportingFile ( FromServer element ) {
		boolean manage;
		ArrayList orders;

		manage = true;

		if ( element == null || element.getLocalID () == -1 ) {
			manage = false;
		}
		else {
			if ( element instanceof FromServerAggregateVirtual ) {
				orders = element.getArray ( "orders" );
				if ( orders == null || orders.size () == 0 )
					manage = false;
			}
		}

		if ( manage == true )
			exportFiles.setValue ( element.getObject ( "baseuser" ) );
		else
			exportFiles.setValue ( null );
	}

	private void activateLayer ( int index, FromServer element ) {
		ObjectWidget panel;

		panel = ( ObjectWidget ) deck.getWidget ( index );
		panel.setValue ( element );
		setupExportingFile ( element );

		deck.showWidget ( index );

		if ( buttons != null )
			buttons.setToggled ( index );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		int index;

		if ( element == null )
			element = OrderUser.findMine ( baseOrder );

		if ( element == null ) {
			index = 0;
		}
		else {
			if ( element.getObject ( "baseuser" ).equals ( Session.getUser () ) == false ) {
				if ( deck.getWidgetCount () == 2 ) {
					setValue ( null );
					return;
				}

				index = 2;
			}
			else {
				if ( ( ( OrderUserInterface ) element ).hasFriends () )
					index = 1;
				else
					index = 0;
			}
		}

		activateLayer ( index, element );
	}

	public FromServer getValue () {
		int index;
		FromServer ret;
		ObjectWidget panel;

		index = deck.getVisibleWidget ();
		panel = ( ObjectWidget ) deck.getWidget ( index );
		return panel.getValue ();
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		plain.addChangeListener ( listener );
		friends.addChangeListener ( listener );
		if ( multi != null )
			multi.addChangeListener ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		plain.removeChangeListener ( listener );
		friends.removeChangeListener ( listener );
		if ( multi != null )
			multi.removeChangeListener ( listener );
	}
}
