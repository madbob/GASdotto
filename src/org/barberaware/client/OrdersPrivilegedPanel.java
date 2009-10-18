/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

/**
	TODO	Probabilmente questo puo' sostituire in toto OrdersPanel, da verificare
*/

public class OrdersPrivilegedPanel extends GenericPanel {
	private boolean		hasOrders;

	public OrdersPrivilegedPanel () {
		super ();

		checkNoAvailableOrders ();

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 2 );
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Order ord;

				ord = ( Order ) object;
				if ( ord.getInt ( "status" ) == Order.OPENED )
					addTop ( doOrderRow ( ord, canMultiUser ( ord ) ) );
			}

			public void onModify ( FromServer object ) {
				int index;
				int status;
				Order ord;

				ord = ( Order ) object;

				index = retrieveOrderForm ( ord );
				status = object.getInt ( "status" );

				if ( index != -1 ) {
					if ( status == Order.OPENED ) {
						syncProductsInForm ( ( FromServerForm ) getWidget ( index ), ord );
					}
					else {
						remove ( index );
						checkNoAvailableOrders ();
					}
				}
				else {
					/*
						Questo per gestire il ben raro caso in cui un
						ordine viene ri-aperto
					*/
					if ( status == Order.OPENED ) {
						ArrayList uorders;
						OrderUser uorder;

						onReceive ( object );

						/*
							Quando un Order viene riaperto mi tocca forzare un nuovo
							controllo sugli OrderUser gia' prelevati dal server. Per
							fortuna e' un evento che capita raramente...
						*/

						uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
						for ( int i = 0; i < uorders.size (); i++ ) {
							uorder = ( OrderUser ) uorders.get ( i );
							findAndAlign ( uorder, 0 );
						}
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 ) {
					remove ( index );
					checkNoAvailableOrders ();
				}
			}
		} );
	}

	private void checkNoAvailableOrders () {
		if ( getWidgetCount () == 1 ) {
			hasOrders = false;
			addTop ( new Label ( "Non ci sono ordini aperti" ) );
		}
	}

	private boolean canMultiUser ( Order order ) {
		Supplier supplier;

		supplier = ( Supplier ) order.getObject ( "supplier" );
		return supplier.iAmReference ();
	}

	private Widget doOrderRow ( Order order, boolean editable ) {
		final FromServerForm ver;
		HorizontalPanel pan;
		OrderUser uorder;
		UserSelector users;
		ProductsUserSelection products;
		IconsBar bar;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );

		ver = new FromServerForm ( uorder, FromServerForm.EDITABLE_UNDELETABLE );

		if ( editable == true ) {
			ver.setCallback ( new FromServerFormCallbacks () {
				public void onClose ( FromServerForm form ) {
					cleanForm ( form, true );
				}

				public void onOpen ( FromServerForm form ) {
					UserSelector selector;

					selector = ( UserSelector ) form.retriveInternalWidget ( "baseuser" );
					retrieveCurrentOrderByUser ( form, ( User ) selector.getValue () );
				}
			} );

			pan = new HorizontalPanel ();
			pan.setStyleName ( "highlight-part" );
			pan.add ( new Label ( "Ordine eseguito a nome di " ) );

			users = new UserSelector ();
			users.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					/**
						TODO	Accertarsi che non siano stati modificati i
							valori nel frattempo, selezionando un utente
							diverso.
							Per far le cose per bene si potrebbe aggiungere
							una funzione compareValue() in FromServerArray e
							ObjectWidget, ma implica correggere un po' tante
							cose in giro...
					*/

					UserSelector selector;
					selector = ( UserSelector ) sender;
					retrieveCurrentOrderByUser ( ver, ( User ) selector.getValue () );
				}
			} );
			pan.add ( ver.getPersonalizedWidget ( "baseuser", users ) );
			ver.add ( pan );

			bar = ver.getIconsBar ();
			bar.addImage ( "images/notifications/multiuser_order.png" );
		}
		else {
			uorder.setObject ( "baseuser", Session.getUser () );
		}

		products = new ProductsUserSelection ( order.getArray ( "products" ) );
		ver.add ( ver.getPersonalizedWidget ( "products", products ) );

		return ver;
	}

	/*
		action: gli stessi valori di syncUserOrder
	*/
	private void findAndAlign ( OrderUser uorder, int action ) {
		int index;
		Order order;
		FromServerForm form;

		order = ( Order ) uorder.getObject ( "baseorder" );

		index = retrieveOrderForm ( order );
		if ( index != -1 ) {
			form = ( FromServerForm ) getWidget ( index );

			if ( canMultiUser ( order ) ) {
				syncUserOrder ( form, uorder, action );
			}
			else {
				if ( action == 2 )
					resetProducts ( form );
				else
					alignOrderRow ( form, uorder );
			}
		}
	}

	/*
		action:
			0 = ordine aggiunto
			1 = ordine modificato, aggiorna
			2 = ordine eliminato
	*/
	private void syncUserOrder ( FromServerForm ver, OrderUser uorder, int action ) {
		int uorder_id;
		ArrayList orders;
		OrderUser iter;

		uorder_id = uorder.getLocalID ();
		orders = ver.getAddictionalData ();

		for ( int i = 0; i < orders.size (); i++ ) {
			iter = ( OrderUser ) orders.get ( i );

			/*
				Forse inefficiente, ma qui faccio insieme il controllo
				sull'esistenza dell'ordine utente e la somma dei prodotti. Se
				l'ordine viene effettivamente trovato, ritorna e tutto il resto
				del lavoro viene perso
			*/
			if ( iter.getLocalID () == uorder_id ) {
				if ( action == 0 ) {
					return;
				}
				else if ( action == 1 ) {
					orders.remove ( i );

					/*
						Se se ne rimuove uno il totale degli elementi
						nell'array scende di uno, dunque devo allineare
						l'indice
					*/
					i--;

					continue;
				}
				else if ( action == 2 ) {
					continue;
				}
			}
		}

		if ( action == 0 || action == 1 )
			orders.add ( uorder );
	}

	private void retrieveCurrentOrderByUser ( FromServerForm form, User user ) {
		int user_id;
		ArrayList orders;
		OrderUser iter;
		User existing_user;

		user_id = user.getLocalID ();
		orders = form.getAddictionalData ();

		for ( int i = 0; i < orders.size (); i++ ) {
			iter = ( OrderUser ) orders.get ( i );
			existing_user = ( User ) iter.getObject ( "baseuser" );

			if ( existing_user.getLocalID () == user_id ) {
				alignOrderRow ( form, iter );
				form.setObject ( iter );
				return;
			}
		}

		cleanForm ( form, false );
	}

	private void alignOrderRow ( FromServerForm ver, OrderUser uorder ) {
		ArrayList products;
		ProductsUserSelection table;

		products = uorder.getArray ( "products" );
		table = ( ProductsUserSelection ) ver.retriveInternalWidget ( "products" );
		table.setElements ( products );

		ver.setObject ( uorder );
	}

	private void resetProducts ( FromServerForm ver ) {
		ProductsUserSelection table;

		table = ( ProductsUserSelection ) ver.retriveInternalWidget ( "products" );
		table.setElements ( null );
	}

	private void cleanForm ( FromServerForm form, boolean complete ) {
		OrderUser uorder;
		OrderUser original_uorder;

		uorder = new OrderUser ();
		original_uorder = ( OrderUser ) form.getObject ();
		uorder.setObject ( "baseorder", original_uorder.getObject ( "baseorder" ) );
		form.setObject ( uorder );

		if ( complete == true )
			form.refreshContents ( uorder );
		else
			resetProducts ( form );
	}

	private int retrieveOrderForm ( Order parent ) {
		FromServerForm form;
		OrderUser tmp_order;

		for ( int i = ( hasOrders == true ? 1 : 2 ); i < getWidgetCount (); i++ ) {
			form = ( FromServerForm ) getWidget ( i );
			tmp_order = ( OrderUser ) form.getObject ();

			if ( parent.getLocalID () == tmp_order.getObject ( "baseorder" ).getLocalID () )
				return i;
		}

		return -1;
	}

	private void syncProductsInForm ( FromServerForm form, Order order ) {
		ProductsUserSelection select;

		select = ( ProductsUserSelection ) form.retriveInternalWidget ( "products" );
		select.upgradeProductsList ( order.getArray ( "products" ) );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Ordini";
	}

	public String getSystemID () {
		return "orders";
	}

	public Image getIcon () {
		return new Image ( "images/path_orders.png" );
	}

	public String getCurrentInternalReference () {
		int index;
		FromServerForm iter;

		index = -1;

		for ( int i = ( hasOrders == true ? 1 : 2 ); i < getWidgetCount (); i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			if ( iter.isOpen () == true ) {
				index = iter.getObject ().getObject ( "baseorder" ).getLocalID ();
				break;
			}
		}

		return Integer.toString ( index );
	}

	public void initView () {
		ServerRequest params;

		params = new ServerRequest ( "Order" );
		params.add ( "status", Order.OPENED );
		Utils.getServer ().testObjectReceive ( params );

		Utils.getServer ().testObjectReceive ( "OrderUser" );
		Utils.getServer ().testObjectReceive ( "User" );
	}
}
