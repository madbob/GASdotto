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
	private boolean			multipleOrders;
	private String			identifier;
	private int			numOrders;
	private int []			statusOrder		= { 0, 2, 3, 1 };

	public DeliverySummary ( boolean multiple ) {
		main = new VerticalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		identifier = Math.random () + "-" + Math.random () + "-" + Math.random () + "-" + Math.random () + "-" + Math.random ();
		numOrders = 0;
		multipleOrders = multiple;
		cleanUp ();
	}

	public void addOrder ( OrderUser uorder ) {
		final FromServerForm row;
		FromServer user;
		FromServer prevmap;
		OrderUserAggregate aggregate;
		HorizontalPanel informations;
		CustomCaptionPanel frame;
		StringLabel phone;
		ProductsDeliveryTable products;
		OrderUserDetails details;

		if ( numOrders == 0 )
			main.remove ( 0 );

		if ( uorder.getRelatedInfo ( "DeliverySummary" ) != null )
			return;

		user = uorder.getObject ( "baseuser" );

		if ( multipleOrders == true && user.getRelatedInfo ( "DeliverySummary" + identifier ) != null ) {
			row = ( FromServerForm ) user.getRelatedInfo ( "DeliverySummary" + identifier );
			prevmap = row.getValue ();

			if ( prevmap instanceof FromServerAggregate == false ) {
				row.removeWidget ( "allproducts" );

				aggregate = new OrderUserAggregate ();
				aggregate.addOrder ( prevmap );
				row.setValue ( aggregate );
			}
			else {
				aggregate = ( OrderUserAggregate ) prevmap;
			}

			details = new OrderUserDetails ();
			details.setValue ( uorder );

			row.add ( details );
			row.addChild ( details );
			aggregate.addOrder ( uorder );
		}
		else {
			row = new FromServerForm ( uorder, FromServerForm.NOT_EDITABLE );
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

			products = new ProductsDeliveryTable ();
			row.add ( row.getPersonalizedWidget ( "allproducts", products ) );

			row.setCallback ( new FromServerFormCallbacks () {
				public String getName ( FromServerForm form ) {
					return form.getValue ().getObject ( "baseuser" ).getString ( "name" );
				}
			} );

			checkPay ( user, row );

			setStatusIcon ( row );
			main.insert ( row, getSortedIndex ( uorder, user ) );
			numOrders += 1;

			user.addRelatedInfo ( "DeliverySummary" + identifier, row );
		}

		uorder.addRelatedInfo ( "DeliverySummary", row );
	}

	public void modOrder ( OrderUser uorder ) {
		FromServerForm form;

		form = ( FromServerForm ) uorder.getRelatedInfo ( "DeliverySummary" );
		if ( form != null ) {
			form.setValue ( uorder );
			form.refreshContents ( null );
			setStatusIcon ( form );
		}
	}

	public void delOrder ( OrderUser uorder ) {
		FromServerForm form;

		form = ( FromServerForm ) uorder.getRelatedInfo ( "DeliverySummary" );
		if ( form != null ) {
			form.invalidate ();
			uorder.delRelatedInfo ( "DeliverySummary" );
			numOrders -= 1;
			cleanUp ();
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

		main.insert ( row, getSortedIndex ( uorder, uorder.getObject ( "baseuser" ) ) );
		row.savingObject ();
		row.open ( false );
	}

	private void setStatusIcon ( FromServerForm form ) {
		EmblemsBar bar;

		bar = form.emblems ();
		bar.activate ( "status", form.getValue ().getInt ( "status" ) );
	}

	private int getSortedIndex ( FromServer order, FromServer to_place ) {
		int i;
		int status_iter;
		int status_to_place;
		FromServerForm row;
		FromServer u_iter;
		FromServer o_iter;
		String name_iter;
		String name_to_place;

		name_to_place = to_place.getString ( "name" );
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
		if ( numOrders == 0 )
			main.add ( new Label ( "Non sono stati avanzati ordini" ) );
	}
}
