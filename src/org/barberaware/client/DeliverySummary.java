/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class DeliverySummary extends Composite {
	private VerticalPanel		main;
	private int			numOrders;

	public DeliverySummary () {
		main = new VerticalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		Utils.getServer ().onObjectEvent ( "User", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				/* dummy */
			}

			public void onModify ( FromServer object ) {
				int index;
				FromServerForm form;
				User user;
				StringLabel phone;

				user = ( User ) object;
				index = retrieveUser ( user );

				if ( index != -1 ) {
					form = ( FromServerForm ) main.getWidget ( index );

					if ( Session.getGAS ().getBool ( "payments" ) == true )
						user.checkUserPaying ( form );

					phone = ( StringLabel ) form.retriveInternalWidget ( "phone" );
					phone.setValue ( user.getString ( "phone" ) );

					phone = ( StringLabel ) form.retriveInternalWidget ( "mobile" );
					phone.setValue ( user.getString ( "mobile" ) );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;
				User user;

				user = ( User ) object;
				index = retrieveUser ( user );
				if ( index != -1 )
					main.remove ( index );
			}

			protected String debugName () {
				return "DeliverySummary";
			}
		} );

		numOrders = 0;
		cleanUp ();
	}

	public void addOrder ( OrderUser uorder ) {
		int index;
		final FromServerForm row;
		User user;
		CustomCaptionPanel frame;
		StringLabel phone;
		ProductsDeliveryTable products;

		if ( numOrders == 0 )
			main.remove ( 0 );

		index = retrieveOrder ( uorder );
		if ( index != -1 )
			return;

		user = ( User ) uorder.getObject ( "baseuser" );
		row = new FromServerForm ( uorder, FromServerForm.NOT_EDITABLE );

		/**
			TODO	Sarebbe assai meglio avere due modalita' per il pannello consegne, selezionabili in
				cima: una per la fase di "prezzatura" (quando si compilano i form e si salvano
				semplicemente) ed una per la fase di "consegna" vera e propria. Questo per mostrare
				solo i tasti funzione utili in quel dato contesto, anziche' la sfilza presente adesso
		*/

		row.addBottomButton ( "images/save.png", "Salva<br/>Informazioni", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getObject ().setInt ( "status", OrderUser.SAVED );
				row.savingObject ();
				row.open ( false );
			}
		} );

		row.addBottomButton ( "images/confirm.png", "Consegna<br/>Completata", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getObject ().setInt ( "status", OrderUser.COMPLETE_DELIVERY );
				row.savingObject ();
				row.open ( false );
			}
		} );

		row.addBottomButton ( "images/part.png", "Consegna<br/>Parziale", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getObject ().setInt ( "status", OrderUser.PARTIAL_DELIVERY );
				row.savingObject ();
				row.open ( false );
			}
		} );

		frame = new CustomCaptionPanel ( "Informazioni Utente" );
		row.add ( frame );

		phone = new StringLabel ();
		phone.setValue ( user.getString ( "phone" ) );
		row.setExtraWidget ( "phone", phone );
		frame.addPair ( "Telefono", phone );

		phone = new StringLabel ();
		phone.setValue ( user.getString ( "mobile" ) );
		row.setExtraWidget ( "mobile", phone );
		frame.addPair ( "Cellulare", phone );

		products = new ProductsDeliveryTable ();
		row.add ( row.getPersonalizedWidget ( "products", products ) );

		row.setCallback ( new FromServerFormCallbacks () {
			public String getName ( FromServerForm form ) {
				return form.getObject ().getObject ( "baseuser" ).getString ( "name" );
			}
		} );

		if ( Session.getGAS ().getBool ( "payments" ) == true )
			user.checkUserPaying ( row );

		setStatusIcon ( row, uorder );
		main.insert ( row, getSortedIndex ( user ) );
		numOrders += 1;
	}

	private void setStatusIcon ( FromServerForm form, OrderUser order ) {
		int status;
		IconsBar bar;

		status = order.getInt ( "status" );
		bar = form.getIconsBar ();

		bar.delImage ( "images/notifications/order_saved.png" );
		bar.delImage ( "images/notifications/order_shipped.png" );
		bar.delImage ( "images/notifications/order_shipping.png" );

		if ( status == OrderUser.COMPLETE_DELIVERY )
			bar.addImage ( "images/notifications/order_shipped.png" );
		else if ( status == OrderUser.PARTIAL_DELIVERY )
			bar.addImage ( "images/notifications/order_shipping.png" );
		else if ( status == OrderUser.SAVED )
			bar.addImage ( "images/notifications/order_saved.png" );
	}

	public void modOrder ( OrderUser uorder ) {
		int index;
		FromServerForm form;

		index = retrieveOrder ( uorder );
		if ( index != -1 ) {
			form = ( FromServerForm ) main.getWidget ( index );
			form.setObject ( uorder );
			form.refreshContents ( null );
			setStatusIcon ( form, uorder );
		}
	}

	public void delOrder ( OrderUser uorder ) {
		int index;

		index = retrieveOrder ( uorder );

		if ( index != -1 ) {
			main.remove ( index );
			numOrders -= 1;
			cleanUp ();
		}
	}

	private int retrieveOrder ( OrderUser uorder ) {
		int index;
		FromServerForm row;

		if ( numOrders != 0 ) {
			index = uorder.getLocalID ();

			for ( int i = 0; i < main.getWidgetCount (); i++ ) {
				row = ( FromServerForm ) main.getWidget ( i );
				if ( row.getObject ().getLocalID () == index )
					return i;
			}
		}

		return -1;
	}

	private int retrieveUser ( User user ) {
		int index;
		FromServerForm row;
		User test;
		OrderUser uorder;

		if ( numOrders == 0 )
			return -1;

		index = user.getLocalID ();

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );
			uorder = ( OrderUser ) row.getObject ();

			if ( uorder != null ) {
				test = ( User ) uorder.getObject ( "baseuser" );
				if ( test != null && test.getLocalID () == index )
					return i;
			}
		}

		return -1;
	}

	private int getSortedIndex ( User to_place ) {
		int i;
		FromServerForm row;
		User u_iter;
		String name_iter;
		String name_to_place;

		name_to_place = to_place.getString ( "name" );

		for ( i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );
			u_iter = ( User ) row.getObject ().getObject ( "baseuser" );
			name_iter = u_iter.getString ( "name" );

			if ( name_iter.compareTo ( name_to_place ) > 0 )
				return i;
		}

		return i;
	}

	private void cleanUp () {
		if ( numOrders == 0 )
			main.add ( new Label ( "Non sono stati avanzati ordini" ) );
	}
}
