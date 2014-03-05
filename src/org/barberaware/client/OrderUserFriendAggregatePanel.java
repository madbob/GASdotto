/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderUserFriendAggregatePanel extends OrderUserManagerMode {
	private ArrayList		singles;
	private FromServer		baseOrder;

	private VerticalPanel		main;
	private TabPanel		friends;
	private TotalRow		mainTotal;
	private PriceViewer		totalWithFriends;

	private boolean			editable;
	private boolean			freeEditable;
	private boolean			fullEditable;
	private FromServer		currentValue;

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

	/*
		Attenzione a non confondersi: addTotalRow() gestisce la riga che
		riassume il totale di tutti gli ordini, mentre le TotalRow
		sparpagliate gestiscono il totale del singolo ordine
	*/
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

			name.addDomHandler ( new ChangeHandler () {
				public void onChange ( ChangeEvent event ) {
					TextBox t;

					t = ( TextBox ) event.getSource ();
					friends.getTabBar ().setTabText ( friends.getWidgetIndex ( t.getParent ().getParent () ), t.getText () );
				}
			}, ChangeEvent.getType () );

			pan.add ( name );
		}

		data.add ( new TotalRow () );

		createSelectors ( data, orders, order_friend, false );

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

		button = new PushButton ( new Image ( "images/delete.png" ), new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				Widget sender;

				if ( Window.confirm ( "Sei sicuro di voler eliminare l'elemento?" ) == true ) {
					sender = ( Widget ) event.getSource ();
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

	private void emptySubOrder ( ProductsUserSelectionWrapper selection, boolean maintab, FromServer order ) {
		FromServer uorder;

		if ( maintab == true ) {
			uorder = new OrderUser ();
			uorder.setObject ( "baseorder", order );
		}
		else {
			uorder = new OrderUserFriend ();
		}

		selection.setValue ( uorder );
	}

	private void assignOrderToSelection ( ProductsUserSelectionWrapper selection, ArrayList orders, FromServer order, boolean maintab ) {
		FromServer uorder;
		FromServer parent;

		if ( orders != null ) {
			for ( int i = 0; i < orders.size (); i++ ) {
				uorder = ( FromServer ) orders.get ( i );
				parent = uorder.getObject ( "parent" );

				if ( parent != null && parent.getObject ( "baseorder" ).equals ( order ) ) {
					selection.setValue ( uorder );
					return;
				}
			}
		}

		emptySubOrder ( selection, maintab, order );
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

		tabs = activeTabs ();
		for ( int i = 1; i < tabs; i++ )
			friends.remove ( 1 );

		orders = currentValue.getArray ( "orders" );

		if ( orders != null ) {
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
				if ( f != null ) {
					for ( int i = 0; i < f.size (); i++ ) {
						friend = ( FromServer ) f.get ( i );
						name = friend.getString ( "friendname" );
						found = false;

						for ( int e = 0; e < dispatched_friends.size (); e++ ) {
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
			}

			for ( int e = 0; e < dispatched_friends.size (); e++ ) {
				forder = ( OrderUserFriendAggregate ) dispatched_friends.get ( e );
				addFriendPanel ( forder );
			}
		}

		friends.selectTab ( 0 );
	}

	private ProductsUserSelectionWrapper getInnerSelection ( VerticalPanel panel, int index ) {
		CaptionPanel frame;
		Iterator<Widget> it;

		frame = ( CaptionPanel ) panel.getWidget ( index );
		it = frame.iterator ();
		return ( ProductsUserSelectionWrapper ) it.next ();
	}

	/*
		Questa funzione e' da invocare solo se il pannello e' editabile
	*/
	private ArrayList<FromServer> retrieveFriendsOrders ( int index, FromServer parent ) {
		int tabs;
		ArrayList<FromServer> ret;
		Hidden id;
		VerticalPanel cell;
		ProductsUserSelectionWrapper prod_sel;
		HorizontalPanel name_container;
		TextBox name;
		FromServer order;

		ret = new ArrayList<FromServer> ();
		tabs = activeTabs ();

		/*
			In posizione 0 c'e' l'ordine dell'utente corrente
		*/
		for ( int i = 1; i < tabs; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );

			/*
				In posizione 0 c'e' la casella di testo col nome dell'amico
			*/
			prod_sel = getInnerSelection ( cell, 1 + index );
			order = prod_sel.getValue ();
			if ( order.getArray ( "products" ) == null )
				continue;

			name_container = ( HorizontalPanel ) cell.getWidget ( 0 );
			name = ( TextBox ) name_container.getWidget ( 1 );
			order.setString ( "friendname", name.getText () );
			order.setObject ( "parent", parent );

			ret.add ( order );
		}

		return ret;
	}

	private void updateTotal () {
		int tabs;
		float total;
		float local_tot;
		VerticalPanel cell;
		TotalRow total_label;
		ProductsUserSelectionWrapper prod_sel;

		total = 0;

		for ( int i = 0; i < singles.size (); i++ ) {
			prod_sel = ( ProductsUserSelectionWrapper ) singles.get ( i );
			total += prod_sel.getTotalPrice ();
		}

		mainTotal.setVal ( total );

		tabs = activeTabs ();

		for ( int i = 1; i < tabs; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );
			local_tot = 0;

			for ( int e = 1; e < cell.getWidgetCount () - 2; e++ ) {
				prod_sel = getInnerSelection ( cell, e );
				local_tot += prod_sel.getTotalPrice ();
			}

			/*
				Se editabile, in ultima posizione c'e' il tasto per eliminare l'ordine
			*/
			if ( editable == true )
				total_label = ( TotalRow ) cell.getWidget ( cell.getWidgetCount () - 2 );
			else
				total_label = ( TotalRow ) cell.getWidget ( cell.getWidgetCount () - 1 );

			total_label.setVal ( local_tot );
			total += local_tot;
		}

		totalWithFriends.setVal ( total );
	}

	/*
		orders = gli ordini aggregati nell'OrdersAggregate
		uorder = l'ordine utente aggregato
	*/
	private void createSelectors ( VerticalPanel panel, ArrayList orders, FromServer uorder, boolean maintab ) {
		CaptionPanel frame;
		FromServer ord;
		OrderUser uord;
		ProductsUserSelectionWrapper selector;

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( FromServer ) orders.get ( i );
			frame = new CaptionPanel ( ord.getObject ( "supplier" ).getString ( "name" ) );
			panel.insert ( frame, panel.getWidgetCount () - 1 );

			selector = new ProductsUserSelectionWrapper ( ord, editable, freeEditable );
			frame.add ( selector );

			if ( maintab == true ) {
				singles.add ( selector );
				addChild ( selector );
			}

			selector.addDomHandler ( new ChangeHandler () {
				public void onChange ( ChangeEvent event ) {
					updateTotal ();
				}
			}, ChangeEvent.getType () );

			if ( uorder != null )
				assignOrderToSelection ( selector, uorder.getArray ( "orders" ), ord, maintab );
			else
				emptySubOrder ( selector, maintab, ord );
		}
	}

	private void initGraphic () {
		VerticalPanel panel;
		CaptionPanel frame;
		DummyTextArea notes;

		friends = new TabPanel ();
		main.add ( friends );
		friends.setWidth ( "100%" );

		notes = new DummyTextArea ();
		notes.addStyleName ( "top-spaced" );
		main.add ( getPersonalizedWidget ( "notes", notes ) );

		addTotalRow ( main );

		singles = new ArrayList ();

		panel = new VerticalPanel ();
		friends.add ( panel, "Il Mio Ordine" );

		mainTotal = new TotalRow ();
		panel.add ( mainTotal );

		createSelectors ( panel, baseOrder.getArray ( "orders" ), null, true );

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

	private FromServer fakeOrderUserAggregate () {
		OrderUserAggregate ret;

		ret = new OrderUserAggregate ();
		ret.setObject ( "baseorder", baseOrder );
		ret.setObject ( "baseuser", Session.getUser () );
		return ret;
	}

	private void alignSelectors ( FromServer order ) {
		int tabs;
		boolean found;
		ArrayList orders;
		ArrayList uorders;
		ArrayList to_add;
		FromServer ord;
		FromServer cmp_ord;
		FromServer uorder;
		FromServer user;
		VerticalPanel panel;
		VerticalPanel cell;
		ProductsUserSelectionWrapper selection;

		orders = order.getArray ( "orders" );
		to_add = new ArrayList ();
		tabs = activeTabs ();

		for ( int i = 0; i < singles.size (); ) {
			selection = ( ProductsUserSelectionWrapper ) singles.get ( i );
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
				singles.remove ( selection );
				removeChild ( cmp_ord );

				for ( int a = 1; a < tabs; a++ ) {
					cell = ( VerticalPanel ) friends.getWidget ( a );
					selection = getInnerSelection ( cell, 1 + i );
					selection.removeFromParent ();
				}

				to_add.add ( cmp_ord );
			}
			else {
				i++;
			}
		}

		if ( to_add.size () != 0 ) {
			uorders = Utils.getServer ().getObjectsFromCache ( "OrderUserAggregate" );
			user = Session.getUser ();

			panel = ( VerticalPanel ) friends.getWidget ( 0 );
			found = false;

			for ( int i = 0; i < uorders.size (); i++ ) {
				uorder = ( FromServer ) uorders.get ( i );

				if ( uorder.getObject ( "baseorder" ).equals ( order ) && uorder.getObject ( "baseuser" ).equals ( user ) ) {
					createSelectors ( panel, to_add, uorder, true );
					found = true;
					break;
				}
			}

			if ( found == false ) {
				uorder = fakeOrderUserAggregate ();
				createSelectors ( panel, to_add, uorder, true );
			}
		}
	}

	private int activeTabs () {
		if ( fullEditable == true )
			return friends.getWidgetCount () - 1;
		else
			return friends.getWidgetCount ();
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer uorder_aggregate ) {
		if ( uorder_aggregate == null )
			uorder_aggregate = fakeOrderUserAggregate ();

		currentValue = uorder_aggregate;
		super.setValue ( currentValue );

		if ( friends == null )
			initGraphic ();

		setMultiOrder ();
		updateTotal ();
	}

	public FromServer getValue () {
		FromServer order;
		FromServer ret;
		ArrayList orders;

		if ( editable == true ) {
			rebuildObject ();
			ret = super.getValue ();

			orders = ret.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				order = ( FromServer ) orders.get ( i );
				order.setArray ( "friends", retrieveFriendsOrders ( i, order ) );
			}
		}
		else {
			ret = super.getValue ();
		}

		return ret;
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
			baseOrder = order;
			alignSelectors ( order );
		}
		else {
			orders = baseOrder.getArray ( "orders" );
			tabs = activeTabs ();

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( FromServer ) orders.get ( i );
				if ( ord.equals ( order ) ) {
					selection = ( ProductsUserSelectionWrapper ) singles.get ( i );
					selection.upgradeProductsList ( order.getArray ( "products" ) );

					for ( int a = 1; a < tabs; a++ ) {
						cell = ( VerticalPanel ) friends.getWidget ( a );
						selection = getInnerSelection ( cell, 1 + i );
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
}
