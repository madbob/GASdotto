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

	private void commonActionsOnEdit ( FromServerForm row ) {
		FromServer uorder;

		uorder = row.getObject ();
		main.insert ( row, getSortedIndex ( uorder, uorder.getObject ( "baseuser" ) ) );
		row.savingObject ();
		row.open ( false );
	}

	public void addOrder ( OrderUser uorder ) {
		final FromServerForm row;
		FromServer user;
		CustomCaptionPanel frame;
		StringLabel phone;
		ProductsDeliveryTable products;

		if ( numOrders == 0 )
			main.remove ( 0 );

		if ( uorder.getRelatedInfo ( "DeliverySummary" ) != null )
			return;

		user = uorder.getObject ( "baseuser" );
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
				row.getObject ().setInt ( "status", OrderUser.SAVED );
				commonActionsOnEdit ( row );
			}
		} );

		row.addBottomButton ( "images/confirm.png", "Consegna<br/>Completata", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getObject ().setInt ( "status", OrderUser.COMPLETE_DELIVERY );
				commonActionsOnEdit ( row );
			}
		} );

		row.addBottomButton ( "images/part.png", "Consegna<br/>Parziale", new ClickListener () {
			public void onClick ( Widget sender ) {
				row.getObject ().setInt ( "status", OrderUser.PARTIAL_DELIVERY );
				commonActionsOnEdit ( row );
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
		row.add ( row.getPersonalizedWidget ( "allproducts", products ) );

		row.setCallback ( new FromServerFormCallbacks () {
			public String getName ( FromServerForm form ) {
				return form.getObject ().getObject ( "baseuser" ).getString ( "name" );
			}
		} );

		checkPay ( user, row );

		setStatusIcon ( row, uorder );
		main.insert ( row, getSortedIndex ( uorder, user ) );
		numOrders += 1;

		uorder.addRelatedInfo ( "DeliverySummary", row );
		user.addRelatedInfo ( "DeliverySummary" + identifier, row );
	}

	private void setStatusIcon ( FromServerForm form, OrderUser order ) {
		EmblemsBar bar;

		bar = form.emblems ();
		bar.activate ( "status", order.getInt ( "status" ) );
	}

	public void modOrder ( OrderUser uorder ) {
		FromServerForm form;

		form = ( FromServerForm ) uorder.getRelatedInfo ( "DeliverySummary" );
		if ( form != null ) {
			form.setObject ( uorder );
			form.refreshContents ( null );
			setStatusIcon ( form, uorder );
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
			o_iter = row.getObject ();
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
