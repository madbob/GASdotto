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
import com.google.gwt.json.client.*;

public class RefineProductDialog extends Composite implements SourcesChangeEvents {
	private Product				targetProduct;
	private ArrayList			userOrders;
	private FlexTable			optionsTable;
	private DialogBox			dialog;
	private ChangeListenerCollection	listeners;

	public RefineProductDialog ( ArrayList orders, Product prod ) {
		Image main;

		targetProduct = prod;
		userOrders = orders;
		listeners = null;

		doDialog ();

		main = new Image ( "images/notify-warning.png" );
		main.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.center ();
				dialog.show ();
			}
		} );

		initWidget ( main );
	}

	public void addChangeListener ( ChangeListener listener ) {
		if ( listeners == null )
			listeners = new ChangeListenerCollection ();
		listeners.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( listeners != null )
			listeners.remove ( listener );
	}

	private Widget doButtons () {
		Button but;
		HorizontalPanel buttons;

		buttons = new HorizontalPanel ();

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				triggerSave ();
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		return buttons;
	}

	private ProductUser retrieveInteresting ( OrderUser order ) {
		int tot;
		ArrayList products;
		ProductUser pu;

		products = order.getArray ( "products" );
		tot = products.size ();

		for ( int i = 0; i < tot; i++ ) {
			pu = ( ProductUser ) products.get ( i );
			if ( pu.getObject ( "product" ).equals ( targetProduct ) )
				return pu;
		}

		return null;
	}

	private Widget doChooses () {
		int tot;
		OrderUser order_user;
		ProductUser pu;
		Label username;
		FloatBox quantity;

		optionsTable = new FlexTable ();
		tot = userOrders.size ();

		for ( int i = 0; i < tot; i++ ) {
			order_user = ( OrderUser ) userOrders.get ( i );

			pu = retrieveInteresting ( order_user );
			if ( pu == null )
				continue;

			username = new Label ( order_user.getObject ( "baseuser" ).getString ( "name" ) );
			optionsTable.setWidget ( i, 0, username );

			quantity = new FloatBox ();
			quantity.setVal ( pu.getFloat ( "quantity" ) );
			optionsTable.setWidget ( i, 1, quantity );
		}

		return optionsTable;
	}

	private void doDialog () {
		VerticalPanel container;

		dialog = new DialogBox ();
		dialog.setText ( "Sistema Ordine Manualmente" );

		container = new VerticalPanel ();
		container.add ( doChooses () );
		container.add ( doButtons () );

		dialog.setWidget ( container );
	}

	private void fireChanges () {
		if ( listeners != null )
			listeners.fireChange ( this );
	}

	private void triggerSave () {
		int tot;
		ProductUser pu;
		OrderUser order_user;
		FloatBox q;

		tot = optionsTable.getRowCount ();

		for ( int i = 0; i < tot; i++ ) {
			q = ( FloatBox ) optionsTable.getWidget ( i, 1 );
			order_user = ( OrderUser ) userOrders.get ( i );

			/**
				TODO	Questo puo' essere migliorato tenendo un array separato
					con i ProductUser giusti
			*/
			pu = retrieveInteresting ( order_user );

			if ( q.getVal () != pu.getFloat ( "quantity" ) ) {
				pu.setFloat ( "quantity", q.getVal () );
				order_user.save ( new ServerResponse () {
					public void onComplete ( JSONValue response ) {
						fireChanges ();
					}
				} );
			}
		}
	}
}