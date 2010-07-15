/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class OrdersHubWidget extends Composite {
	private boolean			engaged;
	private CheckBox		toggle;
	private DateSelector		startdate;
	private DateSelector		enddate;

	public OrdersHubWidget () {
		Date now;
		CaptionPanel frame;
		HorizontalPanel main;

		engaged = true;

		frame = new CaptionPanel ( "Opzioni Avanzate" );
		frame.setStyleName ( "orders-advanced-options" );
		initWidget ( frame );

		main = new HorizontalPanel ();
		main.setSpacing ( 5 );
		frame.add ( main );

		toggle = new CheckBox ( "Mostra Ordini Vecchi" );
		main.add ( toggle );

		main.add ( new Label ( "Da: " ) );
		startdate = new DateSelector ();
		main.add ( startdate );

		main.add ( new Label ( "A: " ) );
		enddate = new DateSelector ();
		main.add ( enddate );

		now = new Date ( System.currentTimeMillis () );
		enddate.setValue ( now );
		now = ( Date ) now.clone ();
		now.setYear ( now.getYear () - 1 );
		startdate.setValue ( now );

		toggle.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				triggerFilters ();
			}
		} );

		startdate.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				triggerFilters ();
			}
		} );

		enddate.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				triggerFilters ();
			}
		} );

		OrdersHub.syncCheckboxOnShippedOrders ( this );
	}

	public void engage ( boolean e ) {
		engaged = e;
	}

	public void setContents ( boolean show, Date s, Date e ) {
		toggle.setChecked ( show );
		startdate.setValue ( s );
		enddate.setValue ( e );
	}

	private void triggerFilters () {
		boolean show;
		Date s;
		Date e;

		show = toggle.isChecked ();
		s = startdate.getValue ();
		e = enddate.getValue ();

		if ( engaged == true )
			OrdersHub.toggleShippedOrdersStatus ( show, s, e );

		doFilter ( show, s, e );
	}

	public abstract void doFilter ( boolean show, Date start, Date end );
}
