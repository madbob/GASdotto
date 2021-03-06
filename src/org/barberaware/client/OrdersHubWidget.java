/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

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

	private ArrayList		statusListeners;

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
		supplier.addFilter ( new FromServerValidateCallback () {
			public boolean checkObject ( FromServer object ) {
				boolean ret;
				Supplier supplier;

				supplier = ( Supplier ) object;
				ret = supplier.iAmReference ();
				return ret;
			}
		} );
		frame.addPair ( "Fornitore", supplier );

		notice = new HTML ( "<p>In questa modalità di ricerca il pannello visualizza solo gli ordini compresi entro le date specificate qui a lato, ed effettuati presso il fornitore specificato.</p><p>Per tornare a visualizzare gli ordini aperti e in consegna, clicca la prima casella del box a fianco.</p>" );
		notice.setStyleName ( "small-text" );
		notice.setWidth ( "40%" );
		notice.setVisible ( false );
		main.add ( notice );

		now = new Date ( System.currentTimeMillis () );
		enddate.setValue ( now );
		now = ( Date ) now.clone ();
		now.setMonth ( now.getMonth () - 1 );
		startdate.setValue ( now );

		toggle.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				triggerStatus ();
				triggerFilters ();
			}
		} );

		startdate.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				triggerFilters ();
			}
		}, ChangeEvent.getType () );

		enddate.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				triggerFilters ();
			}
		}, ChangeEvent.getType () );

		supplier.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				triggerFilters ();
			}
		}, ChangeEvent.getType () );

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

	public boolean isEngaged () {
		return engaged;
	}

	public void setEnabled ( boolean enable ) {
		toggle.setEnabled ( enable );
	}

	public boolean getEnabled () {
		return toggle.isChecked ();
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

	public void addStatusListener ( StatusListener listener ) {
		if ( statusListeners == null )
			statusListeners = new ArrayList ();
		statusListeners.add ( listener );
	}

	private void triggerStatus () {
		boolean show;
		StatusListener listen;

		show = toggle.isChecked ();

		if ( statusListeners != null )
			for ( int i = 0; i < statusListeners.size (); i++ ) {
				listen = ( StatusListener ) statusListeners.get ( i );
				listen.onStatusChange ( this, show );
			}
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

	public abstract void doFilter ( boolean show, Date start, Date end, FromServer supplier );
}
