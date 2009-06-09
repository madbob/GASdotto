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

public class OrdersPrivilegedPanel extends GenericPanel {
	public OrdersPrivilegedPanel () {
		super ();

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm form;
				Order order;
				Order tmp_order;

				order = ( Order ) object.getObject ( "baseorder" );

				for ( int i = 1; i < getWidgetCount (); i++ ) {
					form = ( FromServerForm ) getWidget ( i );
					tmp_order = ( Order ) form.getObject ().getObject ( "baseorder" );

					if ( order.equals ( tmp_order ) )
						syncUserOrder ( form, ( OrderUser ) object, 0 );
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				FromServerForm form;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					syncUserOrder ( form, ( OrderUser ) object, 1 );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;
				FromServerForm form;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					syncUserOrder ( form, ( OrderUser ) object, 2 );
				}
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Order ord;

				ord = ( Order ) object;

				if ( ord.getInt ( "status" ) == Order.OPENED ) {
					Supplier supplier;

					supplier = ( Supplier ) object.getObject ( "supplier" );
					if ( supplier.iAmReference () == false )
						return;

					insert ( doOrderRow ( ord ), 1 );
				}
			}

			public void onModify ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );

				if ( index != -1 ) {
					if ( object.getInt ( "status" ) == Order.OPENED )
						syncProductsInForm ( ( FromServerForm ) getWidget ( index ), ( Order ) object );
					else
						remove ( index );
				}
				else {
					/*
						Questo per gestire il ben raro caso in cui un
						ordine viene ri-aperto
					*/
					if ( object.getInt ( "status" ) == Order.OPENED )
						onReceive ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;

				/**
					TODO	Se l'ordine viene cancellato dal pannello di edit
						non sembra essere cancellato da qui: controllare
				*/

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 )
					remove ( index );
			}
		} );
	}

	private Widget doOrderRow ( Order order ) {
		final FromServerForm ver;
		OrderUser uorder;
		UserSelector users;
		ProductsUserSelection products;

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );

		ver = new FromServerForm ( uorder, FromServerForm.EDITABLE_UNDELETABLE );

		HorizontalPanel pan;

		pan = new HorizontalPanel ();
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

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onClose ( FromServerForm form ) {
				cleanForm ( form, true );
			}
		} );

		products = new ProductsUserSelection ( order.getArray ( "products" ) );
		ver.add ( ver.getPersonalizedWidget ( "products", products ) );

		return ver;
	}

	/*
		action:
			0 = ordine aggiunto
			1 = ordine modificato, aggiorna
			2 = ordine eliminato
	*/
	private void syncUserOrder ( FromServerForm ver, OrderUser uorder, int action ) {
		int uorder_id;
		float total;
		ArrayList orders;
		OrderUser iter;
		PriceViewer total_view;

		total = 0;
		uorder_id = uorder.getLocalID ();
		orders = ver.getAddictionalData ();

		for ( int i = 0; i < orders.size (); i++ ) {
			iter = ( OrderUser ) orders.get ( i );

			/*
				Forse inefficiente, ma qui faccio insieme il controllo sull
				esistenza dell'ordine utente e la somma dei prodotti. Se l'ordine
				viene effettivamente trovato, ritorna e tutto il resto del lavoro
				viene perso
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

			total += iter.getTotalPrice ();
		}

		if ( action == 0 || action == 1 ) {
			orders.add ( uorder );
			total += uorder.getTotalPrice ();
		}

		total_view = ( PriceViewer ) ver.retriveInternalWidget ( "price_sum" );

		if ( total_view == null ) {
			IconsBar icons;

			icons = ver.getIconsBar ();
			total_view = new PriceViewer ();
			icons.addWidget ( total_view );

			/*
				Qui creo e posiziono la label che appare nel riassunto dell'ordine; tale
				label viene poi eventualmente aggiornata quando il sottopannello relativo
				viene chiuso a seguito di qualche correzione
			*/
			ver.setExtraWidget ( "price_sum", total_view );
		}

		total_view.setValue ( total );
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

	private void cleanForm ( FromServerForm form, boolean complete ) {
		OrderUser uorder;
		OrderUser original_uorder;

		uorder = new OrderUser ();
		original_uorder = ( OrderUser ) form.getObject ();
		uorder.setObject ( "baseorder", original_uorder.getObject ( "baseorder" ) );
		form.setObject ( uorder );

		if ( complete == true ) {
			form.refreshContents ( uorder );
		}
		else {
			ProductsUserSelection table;

			table = ( ProductsUserSelection ) form.retriveInternalWidget ( "products" );
			table.setElements ( null );
		}
	}

	private int retrieveOrderForm ( Order parent ) {
		FromServerForm form;
		OrderUser tmp_order;

		for ( int i = 1; i < getWidgetCount (); i++ ) {
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

		for ( int i = 1; i < getWidgetCount (); i++ ) {
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

		/*
			Questo e' per forzare la creazione della lista di utenti, necessaria
			comunque se e solo se la devo creare (ovvero: se sono loggato come
			responsabile o amministratore e posso fare ordini per conto terzi)
		*/
		Utils.getServer ().testObjectReceive ( "User" );
	}
}
