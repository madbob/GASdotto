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

public class OrderUserPlainAggregatePanel extends OrderUserManagerMode {
	private VerticalPanel		main;
	private ArrayList		selections;
	private FromServer		baseOrder;

	private boolean			editable;
	private boolean			freeEditable;

	public OrderUserPlainAggregatePanel ( FromServer order, boolean edit, boolean freedit ) {
		main = new VerticalPanel ();
		initWidget ( main );

		editable = edit;
		freeEditable = freedit;
		baseOrder = order;
		selections = null;
	}

	private void emptySubOrder ( ProductsUserSelectionWrapper selection, FromServer order, FromServer user ) {
		OrderUser uorder;

		uorder = new OrderUser ();
		uorder.setObject ( "baseuser", user );
		uorder.setObject ( "baseorder", order );
		selection.setValue ( uorder );
	}

	private void assignOrderToSelection ( FromServer main_uorder, ProductsUserSelectionWrapper selection, FromServer order ) {
		FromServer uorder;
		ArrayList userorders;

		userorders = main_uorder.getArray ( "orders" );
		if ( userorders == null ) {
			emptySubOrder ( selection, order, main_uorder.getObject ( "baseuser" ) );
			return;
		}

		for ( int i = 0; i < userorders.size (); i++ ) {
			uorder = ( FromServer ) userorders.get ( i );
			if ( uorder.getObject ( "baseorder" ).equals ( order ) ) {
				selection.setValue ( uorder );
				return;
			}
		}

		emptySubOrder ( selection, order, main_uorder.getObject ( "baseuser" ) );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		boolean has_friends;
		ArrayList friends;
		ArrayList orders;
		ArrayList user_orders;
		FromServer ord;
		ProductsUserSelectionWrapper selection;

		has_friends = false;

		if ( element == null ) {
			element = new OrderUserAggregate ();
			element.setObject ( "baseorder", baseOrder );
			element.setObject ( "baseuser", Session.getUser () );
			user_orders = new ArrayList ();
		}
		else {
			user_orders = element.getArray ( "orders" );
			has_friends = ( ( OrderUserInterface ) element ).hasFriends ();
		}

		super.setValue ( element );
		orders = baseOrder.getArray ( "orders" );

		if ( selections == null ) {
			selections = new ArrayList ();

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( FromServer ) orders.get ( i );
				selection = new ProductsUserSelectionWrapper ( ord, editable, freeEditable );
				main.add ( selection );
				addChild ( selection );

				if ( has_friends == true )
					selection.setEditable ( !has_friends );

				assignOrderToSelection ( element, selection, ord );
				selections.add ( selection );
			}

			main.add ( getPersonalizedWidget ( "notes", new DummyTextArea () ) );
		}
		else {
			for ( int i = 0; i < orders.size (); i++ ) {
				selection = ( ProductsUserSelectionWrapper ) selections.get ( i );
				selection.setEditable ( !has_friends );
				assignOrderToSelection ( element, selection, ( FromServer ) orders.get ( i ) );
			}
		}
	}

	public FromServer getValue () {
		if ( editable == true )
			rebuildObject ();

		return super.getValue ();
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeOrder ( FromServer order ) {
		ArrayList orders;
		FromServer ord;
		ProductsUserSelectionWrapper selection;

		if ( selections == null )
			setValue ( null );

		if ( order instanceof OrderAggregate ) {
			/*
				TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
				TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
				TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
			*/
		}
		else {
			orders = baseOrder.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( FromServer ) orders.get ( i );
				if ( ord.equals ( order ) ) {
					selection = ( ProductsUserSelectionWrapper ) selections.get ( i );
					selection.upgradeProductsList ( order.getArray ( "products" ) );
					break;
				}
			}
		}
	}

	public void unlock () {
		/* dummy */
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		ProductsUserSelection selection;

		for ( int i = 0; i < selections.size (); i++ ) {
			selection = ( ProductsUserSelection ) selections.get ( i );
			selection.addChangeListener ( listener );
		}
	}

	public void removeChangeListener ( ChangeListener listener ) {
		ProductsUserSelection selection;

		for ( int i = 0; i < selections.size (); i++ ) {
			selection = ( ProductsUserSelection ) selections.get ( i );
			selection.removeChangeListener ( listener );
		}
	}
}
