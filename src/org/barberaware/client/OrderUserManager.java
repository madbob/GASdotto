/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class OrderUserManager extends Composite implements ObjectWidget {
	private DeckPanel		deck;
	private RadioButtons		buttons		= null;

	public OrderUserManager ( FromServer order, boolean editable ) {
		boolean reference;
		boolean freedit;
		VerticalPanel main;

		main = new VerticalPanel ();
		initWidget ( main );

		main.setStyleName ( "order-manager" );

		reference = ( ( ( Supplier ) order.getObject ( "supplier" ) ).iAmReference () );
		freedit = reference && ( order.getInt ( "status" ) != Order.OPENED );

		if ( editable == true ) {
			buttons = new RadioButtons ();
			main.add ( buttons );
			main.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );

			buttons.add ( new Image ( "images/plain_order.png" ) );
			buttons.add ( new Image ( "images/friends_order.png" ) );
			if ( reference )
				buttons.add ( new Image ( "images/multi_order.png" ) );

			buttons.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					int index;
					FromServer order;
					ObjectWidget current;
					ObjectWidget next;
					OrderUserMultiPanel mode;

					index = buttons.getToggled ();

					current = ( ObjectWidget ) deck.getWidget ( deck.getVisibleWidget () );
					next = ( ObjectWidget ) deck.getWidget ( index );

					order = current.getValue ();
					if ( order.getObject ( "baseuser" ).equals ( Session.getUser () ) )
						next.setValue ( order );

					deck.showWidget ( index );

					/*
						Questo non e' molto pulito, serve a sbloccare la lista
						degli utenti nell'eventuale OrderUserMultiPanel
					*/
					if ( index == 2 ) {
						mode = ( OrderUserMultiPanel ) next;
						mode.unlock ();
					}
				}
			} );

			buttons.setToggled ( 0 );
		}

		deck = new DeckPanel ();
		main.add ( deck );

		deck.add ( new OrderUserPlainPanel ( order, editable, freedit ) );
		deck.add ( new OrderUserFriendPanel ( order, editable, freedit ) );
		if ( reference )
			deck.add ( new OrderUserMultiPanel ( order, editable, freedit ) );

		deck.showWidget ( 0 );
	}

	public void upgradeProductsList ( ArrayList products ) {
		OrderUserManagerMode panel;

		for ( int i = 0; i < deck.getWidgetCount (); i++ ) {
			panel = ( OrderUserManagerMode ) deck.getWidget ( i );
			panel.upgradeProductsList ( products );
		}
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		int index;
		ArrayList f;
		ObjectWidget panel;

		f = element.getArray ( "friends" );

		if ( element.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
			index = 2;
		else if ( f != null && f.size () != 0 )
			index = 1;
		else
			index = 0;

		panel = ( ObjectWidget ) deck.getWidget ( index );
		panel.setValue ( element );

		deck.showWidget ( index );

		if ( buttons != null )
			buttons.setToggled ( index );
	}

	public FromServer getValue () {
		int index;
		ObjectWidget panel;

		index = deck.getVisibleWidget ();
		panel = ( ObjectWidget ) deck.getWidget ( index );
		return panel.getValue ();
	}
}
