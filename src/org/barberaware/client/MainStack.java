/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class MainStack extends Composite {
	private DeckPanel		main;
	private Menu			mainMenu;

	public MainStack () {
		int privileges;
		GenericPanel iter;

		main = new DeckPanel ();
		main.setStyleName ( "main-stack" );
		initWidget ( main );

		Utils.initEmblems ();

		privileges = Session.getPrivileges ();

		/*
			Questo e' per caricare subito categorie e misure, onde evitare che siano
			replicate dozzine di volte quando si andranno a pescare gli ordini (ed i
			relativi prodotti). Non ho trovato un posto migliore dove mettere queste
			due righe...
		*/
		Utils.getServer ().testObjectReceive ( "Category" );
		Utils.getServer ().testObjectReceive ( "Measure" );

		add ( new HomePanel () );
		add ( new ProfilePanel () );

		if ( privileges == User.USER_ADMIN ) {
			add ( new UsersPanel () );
			add ( new SuppliersEditPanel () );
			add ( new OrdersEditPanel () );
			add ( new OrdersPrivilegedPanel () );
			add ( new DeliveryPanel () );
			// add ( new StatisticsPanel () );
			add ( new NotificationPanel () );
			add ( new SystemPanel () );
		}

		else if ( privileges == User.USER_RESPONSABLE ) {
			add ( new UsersUneditablePanel () );
			add ( new SuppliersEditPanel () );
			add ( new OrdersEditPanel () );
			add ( new OrdersPrivilegedPanel () );
			add ( new DeliveryPanel () );
			// add ( new StatisticsPanel () );
			add ( new NotificationPanel () );
			add ( new SystemPanel () );
		}

		else {
			add ( new UsersUneditablePanel () );
			add ( new SuppliersPanel () );
			add ( new OrdersPrivilegedPanel () );
			add ( new NotificationPanel () );
		}

		mainMenu = null;
	}

	public void wireMenu ( Menu menu ) {
		mainMenu = menu;
	}

	private void add ( GenericPanel iter ) {
		iter.setParent ( this );
		main.add ( iter );
	}

	public ArrayList getPanels () {
		int count;
		ArrayList ret;

		ret = new ArrayList ();
		count = main.getWidgetCount ();

		for ( int i = 0; i < count; i++ )
			ret.add ( main.getWidget ( i ) );

		return ret;
	}

	public void showPanelAtPos ( int pos ) {
		GenericPanel to_show;

		to_show = ( GenericPanel ) main.getWidget ( pos );
		to_show.initView ();
		main.showWidget ( pos );
		mainMenu.highlightPos ( pos );
	}

	public void goTo ( String address ) {
		String [] tokens;
		String panel;
		GenericPanel iter;

		tokens = address.split ( "::" );
		panel = tokens [ 0 ];

		/*
			Qui una sorta di conversione di nomi viene compiuta: poiche' alcuni
			pannelli sono raggiungibili solo con certi tipi di utenti, qui l'URL
			viene "normalizzato" permettendo di accedere al pannello piu' simile a
			quello richiesto
		*/

		if ( Session.getUser ().getInt ( "privileges" ) == User.USER_COMMON ) {
			if ( tokens [ 0 ] == "edit_orders" )
				panel = "orders";
		}

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			iter = ( GenericPanel ) main.getWidget ( i );

			if ( iter.getSystemID () == panel ) {
				showPanelAtPos ( i );
				iter.openBookmark ( address );
				break;
			}
		}
	}

	public String getCurrentBookmark () {
		GenericPanel current;

		current = ( GenericPanel ) main.getWidget ( main.getVisibleWidget () );

		return Utils.getServer ().getDomain () +
			"GASdotto.php?internal=" +
			current.getSystemID () + "::" +
			current.getCurrentInternalReference ();
	}

	/*
		Da usare solo in casi estremi!
	*/
	public void addToMain ( Widget widget ) {
		main.add ( widget );
	}
}
