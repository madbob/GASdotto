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
	private TotalRow		totalLabel;

	private ArrayList		selections;
	private FromServer		baseOrder;
	private FromServer		currentValue;

	private boolean			editable;
	private boolean			freeEditable;

	public OrderUserPlainAggregatePanel ( FromServer order, boolean edit, boolean freedit ) {
		main = new VerticalPanel ();
		initWidget ( main );

		editable = edit;
		freeEditable = freedit;
		baseOrder = order;
		selections = null;
		currentValue = null;

		/*
			TODO	Questa callback va sistemata per fare quel che
				invece fa il codice sparpagliato sotto
		*/
		setCallback ( new FromServerRappresentationCallbacks () {
			public boolean onRemoveChild ( FromServerRappresentation form ) {
				return false;
			}
		} );

		totalLabel = new TotalRow ();
		main.add ( totalLabel );
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
			element = fakeOrderUserAggregate ();
			user_orders = new ArrayList ();
		}
		else {
			user_orders = element.getArray ( "orders" );
		}

		super.setValue ( element );
		orders = baseOrder.getArray ( "orders" );

		if ( selections == null ) {
			selections = new ArrayList ();
			createSelectors ( orders, element );
			main.add ( getPersonalizedWidget ( "notes", new DummyTextArea () ) );
		}
		else {
			has_friends = ( ( OrderUserInterface ) element ).hasFriends ();

			for ( int i = 0; i < orders.size (); i++ ) {
				selection = ( ProductsUserSelectionWrapper ) selections.get ( i );
				selection.setEditable ( !has_friends );
				assignOrderToSelection ( element, selection, ( FromServer ) orders.get ( i ) );
			}
		}

		updateTotal ();
	}

	public FromServer getValue () {
		if ( editable == true )
			rebuildObject ();

		return super.getValue ();
	}

	private void updateTotal () {
		float total;
		ProductsUserSelectionWrapper iter;

		total = 0;

		for ( int i = 0; i < selections.size (); i++ ) {
			iter = ( ProductsUserSelectionWrapper ) selections.get ( i );
			total += iter.getTotalPrice ();
		}

		totalLabel.setVal ( total );
	}

	private void createSelectors ( ArrayList orders, FromServer uorder ) {
		boolean has_friends;
		FromServer ord;
		ProductsUserSelectionWrapper selection;

		has_friends = ( ( OrderUserInterface ) uorder ).hasFriends ();

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( FromServer ) orders.get ( i );
			selection = new ProductsUserSelectionWrapper ( ord, editable, freeEditable );
			main.insert ( selection, main.getWidgetCount () - 1 );
			addChild ( selection );
			selection.setEditable ( !has_friends );

			assignOrderToSelection ( uorder, selection, ord );
			selections.add ( selection );

			selection.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget wid ) {
					updateTotal ();
				}
			} );
		}
	}

	private FromServer fakeOrderUserAggregate () {
		OrderUserAggregate ret;

		ret = new OrderUserAggregate ();
		ret.setObject ( "baseorder", baseOrder );
		ret.setObject ( "baseuser", Session.getUser () );
		return ret;
	}

	private void alignSelectors ( FromServer order ) {
		boolean found;
		ArrayList orders;
		ArrayList uorders;
		ArrayList to_add;
		FromServer ord;
		FromServer cmp_ord;
		FromServer uorder;
		FromServer user;
		ProductsUserSelectionWrapper selection;

		orders = order.getArray ( "orders" );
		to_add = new ArrayList ();

		for ( int i = 0; i < selections.size (); ) {
			selection = ( ProductsUserSelectionWrapper ) selections.get ( i );
			cmp_ord = selection.getValue ().getObject ( "baseorder" );
			found = false;

			for ( int a = 0; a < orders.size (); a++ ) {
				ord = ( FromServer ) orders.get ( a );

				if ( ord.equals ( cmp_ord ) ) {
					found = true;
					break;
				}
			}

			if ( found == false ) {
				selection.removeFromParent ();
				selections.remove ( selection );
				removeChild ( cmp_ord );
				to_add.add ( cmp_ord );
			}
			else {
				i++;
			}
		}

		if ( to_add.size () != 0 ) {
			uorders = Utils.getServer ().getObjectsFromCache ( "OrderUserAggregate" );
			user = Session.getUser ();
			found = false;

			for ( int i = 0; i < uorders.size (); i++ ) {
				uorder = ( FromServer ) uorders.get ( i );

				if ( uorder.getObject ( "baseorder" ).equals ( order ) && uorder.getObject ( "baseuser" ).equals ( user ) ) {
					createSelectors ( to_add, uorder );
					found = true;
					break;
				}
			}

			if ( found == false ) {
				uorder = fakeOrderUserAggregate ();
				createSelectors ( to_add, uorder );
			}
		}
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeOrder ( FromServer order ) {
		ArrayList orders;
		FromServer ord;
		ProductsUserSelectionWrapper selection;

		if ( selections == null )
			setValue ( null );

		if ( order instanceof OrderAggregate ) {
			baseOrder = order;
			alignSelectors ( order );
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
