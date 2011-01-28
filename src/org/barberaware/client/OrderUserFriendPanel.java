/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class OrderUserFriendPanel extends Composite implements OrderUserManagerMode, ObjectWidget {
	private ProductsUserSelection	single;
	private FromServer		baseOrder;

	private LinksDialog		files;
	private TabPanel		friends;
	private PriceViewer		totalWithFriends;

	private boolean			editable;
	private boolean			freeEditable;
	private FromServer		currentValue;

	public OrderUserFriendPanel ( FromServer order, boolean edit, boolean freedit ) {
		VerticalPanel main;
		CaptionPanel frame;

		main = new VerticalPanel ();
		main.addStyleName ( "subelement" );
		initWidget ( main );

		frame = new CaptionPanel ( "Esporta Report" );
		frame.addStyleName ( "print-reports-box" );
		main.add ( frame );

		files = new LinksDialog ( "Ordini per gli Amici" );
		frame.add ( files );

		friends = new TabPanel ();
		main.add ( friends );
		friends.setWidth ( "100%" );

		baseOrder = order;
		editable = edit;
		freeEditable = freedit;

		single = new ProductsUserSelection ( baseOrder.getArray ( "products" ), editable, freeEditable );
		friends.add ( single, "Il Mio Ordine" );

		single.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				updateTotal ();
			}
		} );

		if ( editable == true ) {
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

		addTotalRow ( main );
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
		Label header;
		TextBox name;
		Hidden id;
		VerticalPanel data;
		HorizontalPanel pan;
		ProductsUserSelection products;

		data = new VerticalPanel ();
		n = null;

		if ( order_friend != null )
			n = order_friend.getString ( "friendname" );

		if ( n == null || n == "" )
			n = "Nuovo Ordine";

		pan = new HorizontalPanel ();
		data.add ( pan );

		id = new Hidden ();
		if ( order_friend != null )
			id.setValue ( Integer.toString ( order_friend.getLocalID () ) );
		pan.add ( id );
		pan.setCellWidth ( id, "0px" );

		if ( editable == true ) {
			name = new TextBox ();
			name.setText ( n );

			name.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					TextBox t;

					t = ( TextBox ) sender;
					friends.getTabBar ().setTabText ( friends.getWidgetIndex ( t.getParent ().getParent () ), t.getText () );
				}
			} );

			pan.setStyleName ( "highlight-part" );

			header = new Label ( "Ordine eseguito a nome di " );
			pan.add ( header );
			pan.setCellVerticalAlignment ( header, HasVerticalAlignment.ALIGN_MIDDLE );

			pan.add ( name );
		}

		products = new ProductsUserSelection ( baseOrder.getArray ( "products" ), editable, freeEditable );
		data.add ( products );

		products.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				updateTotal ();
			}
		} );

		if ( order_friend != null )
			products.setElements ( order_friend.getArray ( "products" ) );

		if ( editable == true )
			doButtons ( data );

		if ( editable == true ) {
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

	private void setMultiOrder () {
		int tabs;
		ArrayList products;
		ArrayList f;

		if ( editable == true )
			tabs = friends.getWidgetCount () - 1;
		else
			tabs = friends.getWidgetCount ();

		for ( int i = 1; i < tabs; i++ )
			friends.remove ( 1 );

		products = currentValue.getArray ( "products" );
		single.setElements ( products );

		f = currentValue.getArray ( "friends" );
		if ( f != null )
			for ( int i = 0; i < f.size (); i++ )
				addFriendPanel ( ( FromServer ) f.get ( i ) );

		friends.selectTab ( 0 );
		updateTotal ();
	}

	/*
		Questa funzione e' da invocare solo se il pannello e' editabile
	*/
	private ArrayList retrieveFriendsOrders () {
		ArrayList ret;
		Hidden id;
		VerticalPanel cell;
		ProductsUserSelection prod_sel;
		HorizontalPanel name_container;
		TextBox name;
		OrderUserFriend order;
		ArrayList products;

		ret = new ArrayList ();

		for ( int i = 1; i < friends.getWidgetCount () - 1; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );

			prod_sel = ( ProductsUserSelection ) cell.getWidget ( 1 );
			products = prod_sel.getElements ();
			if ( products == null )
				continue;

			name_container = ( HorizontalPanel ) cell.getWidget ( 0 );

			id = ( Hidden ) name_container.getWidget ( 0 );
			if ( id.getValue () != "" )
				order = ( OrderUserFriend ) Utils.getServer ().getObjectFromCache ( "OrderUserFriend", Integer.parseInt ( id.getValue () ) );
			else
				order = new OrderUserFriend ();

			name = ( TextBox ) name_container.getWidget ( 2 );
			order.setString ( "friendname", name.getText () );
			order.setArray ( "products", products );

			ret.add ( order );
		}

		return ret;
	}

	private void updateTotal () {
		int tabs;
		float total;
		VerticalPanel cell;
		ProductsUserSelection prod_sel;

		total = single.getTotalPrice ();

		if ( editable == true )
			tabs = friends.getWidgetCount () - 1;
		else
			tabs = friends.getWidgetCount ();

		for ( int i = 1; i < tabs; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );
			prod_sel = ( ProductsUserSelection ) cell.getWidget ( 1 );
			total += prod_sel.getTotalPrice ();
		}

		totalWithFriends.setVal ( total );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		currentValue = element;
		setMultiOrder ();

		files.emptyBox ();
		files.addLink ( "CSV", "order_friends.php?format=csv&amp;id=" + element.getLocalID () );
		files.addLink ( "PDF", "order_friends.php?format=pdf&amp;id=" + element.getLocalID () );
	}

	public FromServer getValue () {
		if ( editable == true ) {
			currentValue.setArray ( "products", single.getElements () );
			currentValue.setArray ( "friends", retrieveFriendsOrders () );
		}

		return currentValue;
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeProductsList ( ArrayList products ) {
		VerticalPanel cell;
		ProductsUserSelection prod_sel;

		single.upgradeProductsList ( products );

		for ( int i = 1; i < friends.getWidgetCount () - 1; i++ ) {
			cell = ( VerticalPanel ) friends.getWidget ( i );

			prod_sel = ( ProductsUserSelection ) cell.getWidget ( 1 );
			prod_sel.upgradeProductsList ( products );
		}
	}
}
