/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	private String			identifier;
	private int			numOrders;
	private int []			statusOrder		= { 0, 2, 3, 1 };

	public DeliverySummary () {
		main = new VerticalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		identifier = Math.random () + "-" + Math.random () + "-" + Math.random () + "-" + Math.random () + "-" + Math.random ();
		numOrders = 0;
		cleanUp ();
	}

	private void addAllSubOrders ( FromServer uorder, FromServerForm row ) {
		ArrayList uorders;
		FromServer uord;
		OrderUserDetails details;

		uorders = uorder.getArray ( "orders" );

		for ( int i = 0; i < uorders.size (); i++ ) {
			uord = ( FromServer ) uorders.get ( i );

			details = new OrderUserDetails ();
			details.setValue ( uord );

			row.add ( details );
			row.addChild ( details );
		}
	}

	public void addOrder ( OrderUserInterface uorder ) {
		final FromServerForm row;
		FromServerForm existing;
		FromServer uord;
		FromServer user;
		FromServer prevmap;
		OrderUserAggregate aggregate;
		HorizontalPanel informations;
		CustomCaptionPanel frame;
		StringLabel phone;
		ProductsDeliveryTable products;

		if ( numOrders == 0 && main.getWidgetCount () > 0 )
			main.remove ( 0 );

		uord = ( FromServer ) uorder;

		if ( uord.getRelatedInfo ( "DeliverySummary" ) != null )
			return;

		row = new FromServerForm ( uord, FromServerForm.NOT_EDITABLE );
		row.addStyleName ( "subform" );
		row.emblemsAttach ( Utils.getEmblemsCache ( "delivery" ) );

		/**
			TODO	Sarebbe assai meglio avere due modalita' per il pannello consegne, selezionabili in
				cima: una per la fase di "prezzatura" (quando si compilano i form e si salvano
				semplicemente) ed una per la fase di "consegna" vera e propria. Questo per mostrare
				solo i tasti funzione utili in quel dato contesto, anziche' la sfilza presente adesso
		*/

		row.addBottomButton ( "images/save.png", "Salva<br/>Informazioni", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getValue ().setInt ( "status", OrderUser.SAVED );
				commonActionsOnEdit ( row );
			}
		} );

		row.addBottomButton ( "images/confirm.png", "Consegna<br/>Completata", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getValue ().setInt ( "status", OrderUser.COMPLETE_DELIVERY );
				commonActionsOnEdit ( row );
			}
		} );

		row.addBottomButton ( "images/part.png", "Consegna<br/>Parziale", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getValue ().setInt ( "status", OrderUser.PARTIAL_DELIVERY );
				commonActionsOnEdit ( row );
			}
		} );

		informations = new HorizontalPanel ();
		informations.setWidth ( "100%" );
		row.add ( informations );

		/* utente */

		user = uord.getObject ( "baseuser" );

		frame = new CustomCaptionPanel ( "Informazioni Utente" );
		informations.add ( frame );

		phone = new StringLabel ();
		phone.setValue ( user.getString ( "phone" ) );
		row.setExtraWidget ( "phone", phone );
		frame.addPair ( "Telefono", phone );

		phone = new StringLabel ();
		phone.setValue ( user.getString ( "mobile" ) );
		row.setExtraWidget ( "mobile", phone );
		frame.addPair ( "Cellulare", phone );

		/* ordine */

		frame = new CustomCaptionPanel ( "Informazioni Ordine" );
		informations.add ( frame );

		frame.addPair ( "Ultima modifica", row.getPersonalizedWidget ( "deliverydate", new DateViewer () ) );
		frame.addPair ( "Effettuata da", row.getPersonalizedWidget ( "deliveryperson", new NameLabelWidget () ) );

		/* prodotti */

		if ( uorder instanceof OrderUser ) {
			products = new ProductsDeliveryTable ();
			row.add ( row.getPersonalizedWidget ( "allproducts", products ) );
		}
		else if ( uorder instanceof OrderUserAggregate ) {
			addAllSubOrders ( uord, row );
		}

		row.setCallback ( new FromServerFormCallbacks () {
			public String getName ( FromServerForm form ) {
				return form.getValue ().getObject ( "baseuser" ).getString ( "name" );
			}
		} );

		row.setCallback ( new FromServerRappresentationCallbacks () {
			public FromServerRappresentation onAddChild ( FromServerRappresentation form, FromServer child ) {
				OrderUserDetails details;
				FromServerForm f;

				details = new OrderUserDetails ();
				details.setValue ( child );

				f = ( FromServerForm ) form;
				f.add ( details );

				return details;
			}

			public boolean onRemoveChild ( FromServerRappresentation form ) {
				form.removeFromParent ();
				return true;
			}
		} );

		checkPay ( user, row );

		setStatusIcon ( row );
		main.insert ( row, getSortedIndex ( uord ) );
		numOrders += 1;

		user.addRelatedInfo ( "DeliverySummary" + identifier, row );
		uord.addRelatedInfo ( "DeliverySummary", row );
	}

	public void modOrder ( OrderUserInterface uorder ) {
		FromServer uord;
		FromServerForm form;

		uord = ( FromServer ) uorder;

		form = ( FromServerForm ) uord.getRelatedInfo ( "DeliverySummary" );
		if ( form != null ) {
			form.setValue ( uord );
			main.insert ( form, getSortedIndex ( uord ) );
			setStatusIcon ( form );
		}
	}

	public void delOrder ( OrderUserInterface uorder ) {
		FromServer uord;
		FromServerRappresentation form;

		uord = ( FromServer ) uorder;

		form = ( FromServerRappresentation ) uord.getRelatedInfo ( "DeliverySummary" );
		if ( form != null ) {
			if ( form instanceof FromServerForm ) {
				numOrders -= 1;
				cleanUp ();
			}

			form.invalidate ();
			uord.delRelatedInfo ( "DeliverySummary" );
		}
	}

	public void modUser ( FromServer user ) {
		int index;
		FromServerForm form;
		StringLabel phone;

		form = ( FromServerForm ) user.getRelatedInfo ( "DeliverySummary" + identifier );

		if ( form != null ) {
			checkPay ( user, form );

			phone = ( StringLabel ) form.retriveInternalWidget ( "phone" );
			phone.setValue ( user.getString ( "phone" ) );

			phone = ( StringLabel ) form.retriveInternalWidget ( "mobile" );
			phone.setValue ( user.getString ( "mobile" ) );
		}
	}

	public void delUser ( FromServer user ) {
		FromServerForm form;

		form = ( FromServerForm ) user.getRelatedInfo ( "DeliverySummary" + identifier );
		if ( form != null ) {
			form.invalidate ();
			user.delRelatedInfo ( "DeliverySummary" + identifier );
		}
	}

	private void commonActionsOnEdit ( FromServerForm row ) {
		DateViewer deliverydate;
		NameLabelWidget deliveryperson;
		FromServer uorder;

		uorder = row.getValue ();

		deliverydate = ( DateViewer ) row.retriveInternalWidget ( "deliverydate" );
		deliverydate.setValue ( new Date ( System.currentTimeMillis () ) );

		deliveryperson = ( NameLabelWidget ) row.retriveInternalWidget ( "deliveryperson" );
		deliveryperson.setValue ( Session.getUser () );

		main.insert ( row, getSortedIndex ( uorder ) );
		row.savingObject ();
		row.open ( false );
		setStatusIcon ( row );
	}

	private void setStatusIcon ( FromServerForm form ) {
		EmblemsBar bar;

		bar = form.emblems ();
		bar.activate ( "status", form.getValue ().getInt ( "status" ) );
	}

	private int getSortedIndex ( FromServer order ) {
		int i;
		int status_iter;
		int status_to_place;
		FromServerForm row;
		FromServer u_iter;
		FromServer o_iter;
		String name_iter;
		String name_to_place;

		name_to_place = order.getObject ( "baseuser" ).getString ( "name" );
		status_to_place = order.getInt ( "status" );

		for ( i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );

			o_iter = row.getValue ();
			if ( o_iter == null )
				continue;

			status_iter = o_iter.getInt ( "status" );

			if ( statusOrder [ status_iter ] > statusOrder [ status_to_place ] ) {
				return i;
			}
			else if ( statusOrder [ status_iter ] == statusOrder [ status_to_place ] ) {
				u_iter = o_iter.getObject ( "baseuser" );
				name_iter = u_iter.getString ( "name" );

				if ( name_iter.compareTo ( name_to_place ) > 0 )
					return i;
			}
		}

		return i;
	}

	private void checkPay ( FromServer u, FromServerForm form ) {
		if ( Session.getGAS ().getBool ( "payments" ) == true ) {
			User user;
			user = ( User ) u;
			user.checkUserPaying ( form );
		}
	}

	private void cleanUp () {
		if ( numOrders < 0 )
			numOrders = 0;

		if ( numOrders == 0 )
			main.add ( new Label ( "Non sono stati avanzati ordini" ) );
	}
}
