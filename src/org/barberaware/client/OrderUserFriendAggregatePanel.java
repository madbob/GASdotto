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

public class OrderUserFriendAggregatePanel extends OrderUserManagerMode {
	private ArrayList		singles;
	private FromServer		baseOrder;

	private VerticalPanel		main;
	private TabPanel		friends;
	private PriceViewer		totalWithFriends;

	private boolean			editable;
	private boolean			freeEditable;
	private boolean			fullEditable;
	private FromServer		currentValue;

	private ArrayList		changeCallbacks			= null;

	public OrderUserFriendAggregatePanel ( FromServer order, boolean edit, boolean freedit, boolean full ) {
		baseOrder = order;
		editable = edit;
		freeEditable = freedit;
		fullEditable = full;

		main = new VerticalPanel ();
		main.addStyleName ( "subelement" );
		initWidget ( main );

		friends = null;
	}

	private void addTotalRow ( VerticalPanel main ) {
		Label header;
		HorizontalPanel total;

		total = new HorizontalPanel ();
		total.setSpacing ( 5 );
		main.add ( total );
		main.setCellHorizontalAlignment ( total, HasHorizontalAlignment.ALIGN_CENTER );

		header = new Label ( "Somma di tutti i Totali: " );
		total.add ( header );
		total.setCellVerticalAlignment ( header, HasVerticalAlignment.ALIGN_MIDDLE );

		totalWithFriends = new PriceViewer ();
		totalWithFriends.setStyleName ( "bigger-text" );
		total.add ( totalWithFriends );
		total.setCellVerticalAlignment ( totalWithFriends, HasVerticalAlignment.ALIGN_MIDDLE );
	}

	private int addFriendPanel ( FromServer order_friend ) {
		int index;
		String n;
		ArrayList orders;
		FromServer ord;
		Label header;
		TextBox name;
		VerticalPanel data;
		HorizontalPanel pan;
		ProductsUserSelectionWrapper selection;

		data = new VerticalPanel ();
		n = null;

		if ( order_friend != null )
			n = order_friend.getString ( "friendname" );

		if ( n == null || n == "" )
			n = "Nuovo Ordine";

		orders = baseOrder.getArray ( "orders" );

		/*
			Questo lo metto sempre, cosi' mi tornano sempre gli indici quando devo
			iterare i widgets nel pannello
		*/
		pan = new HorizontalPanel ();
		data.add ( pan );

		if ( editable == true ) {
			pan.setStyleName ( "highlight-part" );

			header = new Label ( "Ordine eseguito a nome di " );
			pan.add ( header );
			pan.setCellVerticalAlignment ( header, HasVerticalAlignment.ALIGN_MIDDLE );

			name = new TextBox ();
			name.setText ( n );

			name.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					TextBox t;

					t = ( TextBox ) sender;
					friends.getTabBar ().setTabText ( friends.getWidgetIndex ( t.getParent ().getParent () ), t.getText () );
				}
			} );

			pan.add ( name );
		}

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( FromServer ) orders.get ( i );
			selection = new ProductsUserSelectionWrapper ( ord, editable, freeEditable );
			data.add ( selection );

			selection.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					updateTotal ();
				}
			} );

			if ( changeCallbacks != null ) {
				ChangeListener listener;

				for ( int a = 0; a < changeCallbacks.size (); a++ ) {
					listener = ( ChangeListener ) changeCallbacks.get ( a );
					selection.addChangeListener ( listener );
				}
			}

			if ( order_friend != null )
				assignOrderToSelection ( selection, order_friend.getArray ( "orders" ), ord );
			else
				emptySubOrder ( selection );
		}

		if ( editable == true )
			doButtons ( data );

		if ( editable == true && fullEditable == true ) {
			index = friends.getWidgetCount () - 1;
			friends.insert ( data, n, index );
		}
		else {
			index = friends.getWidgetCount ();
			friends.add ( data, n );
		}

		return index;
	}

	private void doButtons ( VerticalPanel main ) {
		PushButton button;
		ButtonsBar panel;

		panel = new ButtonsBar ();

		button = new PushButton ( new Image ( "images/delete.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( Window.confirm ( "Sei sicuro di voler eliminare l'elemento?" ) == true ) {
					friends.remove ( sender.getParent ().getParent ().getParent ().getParent () );
					friends.selectTab ( 0 );
					updateTotal ();
				}
			}
		} );
		panel.add ( button, "Elimina" );

		main.add ( panel );
		main.setCellHorizontalAlignment ( panel, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	private void emptySubOrder ( ProductsUserSelectionWrapper selection ) {
		OrderUserFriend uorder;

		uorder = new OrderUserFriend ();
		selection.setValue ( uorder );
	}

	private void assignOrderToSelection ( ProductsUserSelectionWrapper selection, ArrayList orders, FromServer order ) {
		FromServer uorder;

		if ( orders == null ) {
			emptySubOrder ( selection );
			return;
		}

		for ( int i = 0; i < orders.size (); i++ ) {
			uorder = ( FromServer ) orders.get ( i );
			if ( uorder.getObject ( "parent" ).getObject ( "baseorder" ).equals ( order ) ) {
				selection.setValue ( uorder );
				return;
			}
		}

		emptySubOrder ( selection );
	}

	private void setMultiOrder () {
		boolean found;
		int tabs;
		String name;
		ArrayList orders;
		ArrayList products;
		ArrayList f;
		ArrayList dispatched_friends;
		FromServer ord;
		FromServer friend;
		OrderUserFriendAggregate forder;
		ProductsUserSelectionWrapper selection;

		if ( editable == true && fullEditable == true )
			tabs = friends.getWidgetCount () - 1;
		else
			tabs = friends.getWidgetCount ();

		for ( int i = 1; i < tabs; i++ )
			friends.remove ( 1 );

		orders = currentValue.getArray ( "orders" );
		dispatched_friends = new ArrayList ();

		for ( int a = 0; a < orders.size (); a++ ) {
			ord = ( FromServer ) orders.get ( a );

			for ( int i = 0; i < singles.size (); i++ ) {
				selection = ( ProductsUserSelectionWrapper ) singles.get ( i );
				if ( selection.getValue ().getObject ( "baseorder" ).equals ( ord.getObject ( "baseorder" ) ) ) {
					selection.setValue ( ord );
					break;
				}
			}

			f = ord.getArray ( "friends" );
			if ( f != null )
				for ( int i = 0; i < f.size (); i++ ) {
					friend = ( FromServer ) f.get ( i );
					name = friend.getString ( "friendname" );
					found = false;

					for ( int e = 1; e < dispatched_friends.size (); e++ ) {
						forder = ( OrderUserFriendAggregate ) dispatched_friends.get ( e );
						if ( forder.getString ( "friendname" ).equals ( name ) ) {
							forder.addObject ( friend );
							found = true;
							break;
						}
					}

					if ( found == false ) {
						forder = new OrderUserFriendAggregate ();
						forder.setString ( "friendname", name );
						forder.addObject ( friend );
						dispatched_friends.add ( forder );
					}
				}
		}

		for ( int e = 0; e < dispatched_friends.size (); e++ ) {
			forder = ( OrderUserFriendAggregate ) dispatched_friends.get ( e );
			addFriendPanel ( forder );
		}

		friends.selectTab ( 0 );
		updateTotal ();
	}

	/*
		Questa funzione e' da invocare solo se il pannello e' editabile
	*/
	private ArrayList retrieveFriendsOrders ( int index ) {
		int tabs;
		ArrayList ret;
		Hidden id;
		VerticalPanel cell;
		ProductsUserSelectionWrapper prod_sel;
		HorizontalPanel name_container;
		TextBox name;
		FromServer order;
		ArrayList products;

		ret = new ArrayList ();

		if ( fullEditable == true )
			tabs = friends.getWidgetCount () - 1;
		else
			tabs = friends.getWidgetCount ();

		for ( int i = 1; i < tabs; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );

			prod_sel = ( ProductsUserSelectionWrapper ) cell.getWidget ( 1 + index );
			order = prod_sel.getValue ();
			if ( order.getArray ( "products" ) == null )
				continue;

			name_container = ( HorizontalPanel ) cell.getWidget ( 0 );
			name = ( TextBox ) name_container.getWidget ( 2 );
			order.setString ( "friendname", name.getText () );

			ret.add ( order );
		}

		return ret;
	}

	private void updateTotal () {
		int tabs;
		float total;
		VerticalPanel cell;
		ProductsUserSelectionWrapper prod_sel;

		total = 0;

		for ( int i = 1; i < singles.size (); i++ ) {
			prod_sel = ( ProductsUserSelectionWrapper ) singles.get ( i );
			total += prod_sel.getTotalPrice ();
		}

		if ( editable == true )
			tabs = friends.getWidgetCount () - 1;
		else
			tabs = friends.getWidgetCount ();

		for ( int i = 1; i < tabs; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );

			for ( int e = 1; e < cell.getWidgetCount () - 1; e++ ) {
				prod_sel = ( ProductsUserSelectionWrapper ) cell.getWidget ( e );
				total += prod_sel.getTotalPrice ();
			}
		}

		totalWithFriends.setVal ( total );
	}

	private void initGraphic () {
		ArrayList orders;
		FromServer ord;
		FromServer uord;
		VerticalPanel panel;
		CaptionPanel frame;
		DummyTextArea notes;
		ProductsUserSelectionWrapper selection;

		friends = new TabPanel ();
		main.add ( friends );
		friends.setWidth ( "100%" );

		notes = new DummyTextArea ();
		notes.addStyleName ( "top-spaced" );
		main.add ( getPersonalizedWidget ( "notes", notes ) );

		addTotalRow ( main );

		orders = baseOrder.getArray ( "orders" );

		singles = new ArrayList ();
		panel = new VerticalPanel ();

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( FromServer ) orders.get ( i );
			selection = new ProductsUserSelectionWrapper ( ord, editable, freeEditable );
			panel.add ( selection );
			singles.add ( selection );
			addChild ( selection );

			selection.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					updateTotal ();
				}
			} );

			uord = new OrderUser ();
			uord.setObject ( "baseorder", ord );
			uord.setObject ( "baseuser", Session.getUser () );
			selection.setValue ( uord );
		}

		friends.add ( panel, "Il Mio Ordine" );

		if ( editable == true && fullEditable == true ) {
			friends.add ( new Label (), "Aggiungi Nuovo Ordine" );
			friends.addTabListener ( new TabListener () {
				public boolean onBeforeTabSelected ( SourcesTabEvents sender, int tabIndex ) {
					if ( tabIndex == friends.getWidgetCount () - 1 ) {
						friends.selectTab ( addFriendPanel ( null ) );
						return false;
					}
					else {
						return true;
					}
				}

				public void onTabSelected ( SourcesTabEvents sender, int tabIndex ) {
					/* dummy */
				}
			} );
		}

		friends.selectTab ( 0 );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		currentValue = element;
		super.setValue ( currentValue );

		if ( friends == null )
			initGraphic ();

		setMultiOrder ();
	}

	public FromServer getValue () {
		FromServer order;
		ArrayList orders;

		if ( editable == true ) {
			rebuildObject ();

			orders = super.getValue ().getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				order = ( FromServer ) orders.get ( i );
				order.setArray ( "friends", retrieveFriendsOrders ( i ) );
			}
		}

		return super.getValue ();
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeOrder ( FromServer order ) {
		int tabs;
		ArrayList orders;
		FromServer ord;
		VerticalPanel cell;
		ProductsUserSelectionWrapper selection;

		if ( singles == null )
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
					selection = ( ProductsUserSelectionWrapper ) singles.get ( i );
					selection.upgradeProductsList ( order.getArray ( "products" ) );

					if ( fullEditable == true )
						tabs = friends.getWidgetCount () - 1;
					else
						tabs = friends.getWidgetCount ();

					for ( int a = 1; a < tabs; a++ ) {
						cell = ( VerticalPanel ) friends.getWidget ( a );
						selection = ( ProductsUserSelectionWrapper ) cell.getWidget ( 1 + i );
						selection.upgradeProductsList ( order.getArray ( "products" ) );
					}

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
		int tabs;
		VerticalPanel cell;
		ProductsUserSelectionWrapper selection;

		if ( changeCallbacks == null )
			changeCallbacks = new ArrayList ();
		changeCallbacks.add ( listener );

		for ( int i = 0; i < singles.size (); i++ ) {
			selection = ( ProductsUserSelectionWrapper ) singles.get ( i );
			selection.addChangeListener ( listener );
		}

		if ( fullEditable == true )
			tabs = friends.getWidgetCount () - 1;
		else
			tabs = friends.getWidgetCount ();

		for ( int a = 1; a < tabs; a++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( a );

			for ( int i = 1; i < cell.getWidgetCount () - 1; i++ ) {
				selection = ( ProductsUserSelectionWrapper ) cell.getWidget ( i );
				selection.addChangeListener ( listener );
			}
		}
	}

	public void removeChangeListener ( ChangeListener listener ) {
		int tabs;
		VerticalPanel cell;
		ProductsUserSelectionWrapper selection;

		if ( changeCallbacks != null ) {
			for ( int i = 0; i < singles.size (); i++ ) {
				selection = ( ProductsUserSelectionWrapper ) singles.get ( i );
				selection.addChangeListener ( listener );
			}

			if ( fullEditable == true )
				tabs = friends.getWidgetCount () - 1;
			else
				tabs = friends.getWidgetCount ();

			for ( int a = 1; a < tabs; a++ ) {
				cell = ( VerticalPanel ) friends.getWidget ( a );

				for ( int i = 1; i < cell.getWidgetCount () - 1; i++ ) {
					selection = ( ProductsUserSelectionWrapper ) cell.getWidget ( i );
					selection.addChangeListener ( listener );
				}
			}
		}
	}
}
