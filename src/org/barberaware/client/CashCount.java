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
	private class CashCountCell extends HorizontalPanel {
		public	float		tot;
		public	Date		referenceDate;
		private	Label		totLabel;

		public CashCountCell ( Date t, boolean today ) {
			Label h;

			totLabel = new Label ();

			if ( today == true ) {
				h = new Label ( "Consegne di oggi: " );
				add ( h );
				totLabel.setStyleName ( "bigger-text" );
			}
			else {
				h = new Label ( "Consegne del " + Utils.printableDate ( t ) + ": " );
				add ( h );
			}

			setCellVerticalAlignment ( h, HasVerticalAlignment.ALIGN_MIDDLE );

			totLabel.addStyleName ( "contents-on-right" );
			add ( totLabel );
			setCellVerticalAlignment ( totLabel, HasVerticalAlignment.ALIGN_MIDDLE );

			referenceDate = t;
			tot = 0;
			totLabel.setText ( Utils.priceToString ( tot ) );
		}

		public void sum ( float price, boolean addict ) {
			if ( addict == true )
				tot = tot + price;
			else
				tot = tot - price;

			totLabel.setText ( Utils.priceToString ( tot ) );
		}
	}

	private HorizontalPanel		main;

	public CashCount () {
		Date now;
		CashCountCell cell;

		main = new HorizontalPanel ();
		initWidget ( main );
		main.setStyleName ( "top-spaced" );

		now = new Date ( System.currentTimeMillis () );
		now = new Date ( now.getYear (), now.getMonth (), now.getDate () );

		cell = new CashCountCell ( now, true );
		main.add ( cell );
		main.setCellVerticalAlignment ( cell, HasVerticalAlignment.ALIGN_MIDDLE );
	}

	public void addOrder ( OrderUser uorder ) {
		Date d;
		CashCountCell cell;

		d = checkEligibility ( uorder );
		if ( d == null )
			return;

		cell = retrieveCell ( d );
		cell.sum ( uorder.getDeliveredPriceWithFriends (), true );
	}

	public void modOrder ( OrderUser uorder ) {
		FromServer baseorder;
		OrderUser past_user_order;
		ArrayList past_orders;

		if ( checkEligibility ( uorder ) == null )
			return;

		resetCells ();

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
		Date d;
		CashCountCell cell;

		d = checkEligibility ( uorder );
		if ( d == null )
			return;

		cell = retrieveCell ( d );
		cell.sum ( uorder.getDeliveredPriceWithFriends (), false );
	}

	private Date checkEligibility ( OrderUser uorder ) {
		int status;
		Date d;

		d = uorder.getDate ( "deliverydate" );
		if ( d == null )
			return null;

		status = uorder.getInt ( "status" );
		if ( status != OrderUser.PARTIAL_DELIVERY && status != OrderUser.COMPLETE_DELIVERY )
			return null;

		return d;
	}

	private CashCountCell retrieveCell ( Date d ) {
		boolean managed;
		Date dup_d;
		CashCountCell cell;

		managed = false;
		cell = null;
		dup_d = new Date ( d.getYear (), d.getMonth (), d.getDate () );

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			cell = ( CashCountCell ) main.getWidget ( i );

			if ( cell.referenceDate.after ( dup_d ) ) {
				cell = new CashCountCell ( dup_d, false );
				main.insert ( cell, i );
				main.setCellVerticalAlignment ( cell, HasVerticalAlignment.ALIGN_MIDDLE );
				managed = true;
				break;
			}
			else if ( cell.referenceDate.equals ( dup_d ) ) {
				managed = true;
				break;
			}
		}

		if ( managed == false ) {
			cell = new CashCountCell ( dup_d, false );
			main.add ( cell );
			main.setCellVerticalAlignment ( cell, HasVerticalAlignment.ALIGN_MIDDLE );
		}

		return cell;
	}

	private void resetCells () {
		CashCountCell cell;

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			cell = ( CashCountCell ) main.getWidget ( i );
			cell.tot = 0;
			cell.sum ( 0, true );
		}
	}
}
