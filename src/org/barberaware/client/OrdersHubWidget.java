/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	private boolean			prevShow;
	private HorizontalPanel		main;
	private HTML			notice;
	private CustomCaptionPanel	frame;
	private CheckBox		toggle;
	private DateSelector		startdate;
	private DateSelector		enddate;
	private FromServerSelector	supplier;

	public OrdersHubWidget () {
		Date now;
		HorizontalPanel hor;

		engaged = true;
		prevShow = false;

		main = new HorizontalPanel ();
		main.setStyleName ( "orders-hub-widget" );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		initWidget ( main );

		frame = new CustomCaptionPanel ( "Modalità Ricerca Ordini" );
		frame.setVisible ( false );
		main.add ( frame );

		toggle = new CheckBox ( "Modalità Ricerca Ordini" );
		main.add ( toggle );

		frame.addPair ( "", new Label () );

		startdate = new DateSelector ();
		frame.addPair ( "Dal", startdate );

		enddate = new DateSelector ();
		frame.addPair ( "Al", enddate );

		supplier = new FromServerSelector ( "Supplier", true, true, false );
		supplier.addAllSelector ();
		frame.addPair ( "Fornitore", supplier );

		notice = new HTML ( "<p>In questa modalità di ricerca il pannello visualizza solo gli ordini compresi entro le date specificate qui a lato, ed effettuati presso il fornitore specificato.</p><p>Per tornare a visualizzare gli ordini aperti e in consegna, clicca la prima casella del box a fianco.</p>" );
		notice.setStyleName ( "smaller-text" );
		notice.setWidth ( "40%" );
		notice.setVisible ( false );
		main.add ( notice );

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

		supplier.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				triggerFilters ();
			}
		} );

		OrdersHub.syncCheckboxOnShippedOrders ( this );
	}

	/*
		Questo viene usato come flag da OrdersHub, serve a non far scattare la procedura di download dei dati
		da parte di ogni OrdersHubWidget dell'applicazione quando in uno vengono modificati i parametri e
		vengono riportati negli altri
	*/
	public void engage ( boolean e ) {
		engaged = e;
	}

	public void setContents ( boolean show, Date s, Date e, FromServer supp ) {
		toggle.setChecked ( show );
		startdate.setValue ( s );
		enddate.setValue ( e );
		supplier.setValue ( supp );
	}

	private void triggerFilters () {
		boolean show;
		Date s;
		Date e;
		FromServer sup;

		show = toggle.isChecked ();

		if ( show == false && prevShow == true ) {
			frame.setVisible ( false );
			main.add ( toggle );
			notice.setVisible ( false );
		}
		else if ( show == true && prevShow == false ) {
			supplier.setItemSelected ( 0, true );
			frame.addPair ( "", toggle, 0 );
			frame.setVisible ( true );
			notice.setVisible ( true );
		}

		s = startdate.getValue ();
		e = enddate.getValue ();
		sup = supplier.getValue ();

		if ( engaged == true )
			OrdersHub.toggleShippedOrdersStatus ( show, s, e, sup );

		prevShow = show;
		doFilter ( show, s, e, sup );
	}

	public void doFilter () {
		boolean show;
		Date s;
		Date e;
		FromServer sup;

		show = toggle.isChecked ();

		if ( show == true ) {
			s = startdate.getValue ();
			e = enddate.getValue ();
			sup = supplier.getValue ();

			doFilter ( show, s, e, sup );
		}
	}

	public abstract void doFilter ( boolean show, Date start, Date end, FromServer supplier );
}
