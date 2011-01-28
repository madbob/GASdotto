/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class CashCount extends Composite {
	private HorizontalPanel		main;

	private Date			now;
	private float			tot;
	private	Label			totLabel;

	public CashCount () {
		Label text;

		main = new HorizontalPanel ();
		initWidget ( main );
		main.setStyleName ( "top-spaced" );

		now = new Date ( System.currentTimeMillis () );
		now = new Date ( now.getYear (), now.getMonth (), now.getDate () );

		text = new Label ( "Consegne di oggi: " );
		main.add ( text );
		main.setCellHorizontalAlignment ( text, HasHorizontalAlignment.ALIGN_LEFT );
		main.setCellVerticalAlignment ( text, HasVerticalAlignment.ALIGN_MIDDLE );

		totLabel = new Label ();
		totLabel.setStyleName ( "bigger-text" );
		totLabel.addStyleName ( "contents-on-right" );
		main.add ( totLabel );
		main.setCellHorizontalAlignment ( totLabel, HasHorizontalAlignment.ALIGN_LEFT );
		main.setCellVerticalAlignment ( totLabel, HasVerticalAlignment.ALIGN_MIDDLE );

		tot = 0;
		totLabel.setText ( Utils.priceToString ( tot ) );
	}

	public void addOrder ( OrderUser uorder ) {
		if ( checkEligibility ( uorder ) == false )
			return;

		tot += uorder.getDeliveredPriceWithFriends ();
		totLabel.setText ( Utils.priceToString ( tot ) );
	}

	public void modOrder ( OrderUser uorder ) {
		FromServer baseorder;
		OrderUser past_user_order;
		ArrayList past_orders;

		if ( checkEligibility ( uorder ) == false )
			return;

		tot = 0;
		totLabel.setText ( Utils.priceToString ( tot ) );

		baseorder = uorder.getObject ( "baseorder" );
		past_orders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );

		for ( int i = 0; i < past_orders.size (); i++ ) {
			past_user_order = ( OrderUser ) past_orders.get ( i );

			if ( past_user_order.getObject ( "baseorder" ).equals ( baseorder ) )
				addOrder ( past_user_order );
		}

		/*
			Qui non occorre invocare nuovamente addOrder() sull'ordine in input, in quanto esso gia'
			esiste nella cache dunque viene comunque gestito nel ciclo qui sopra
		*/
	}

	public void delOrder ( OrderUser uorder ) {
		if ( checkEligibility ( uorder ) == false )
			return;

		tot -= uorder.getDeliveredPriceWithFriends ();
		totLabel.setText ( Utils.priceToString ( tot ) );
	}

	private boolean checkEligibility ( OrderUser uorder ) {
		int status;
		Date d;
		Date dup_d;

		d = uorder.getDate ( "deliverydate" );
		if ( d == null )
			return false;

		status = uorder.getInt ( "status" );
		if ( status != OrderUser.PARTIAL_DELIVERY && status != OrderUser.COMPLETE_DELIVERY )
			return false;

		dup_d = new Date ( d.getYear (), d.getMonth (), d.getDate () );
		return dup_d.equals ( now );
	}
}
