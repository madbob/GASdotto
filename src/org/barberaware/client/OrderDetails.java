/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class OrderDetails extends FromServerRappresentationFull {
	private HorizontalPanel		main;
	private FromServerForm		parent;

	private NameLabelWidget		supplier;
	private CyclicToggle		status;
	private PercentageWidget	anticipated;
	private DateWidget		startdate;
	private DateWidget		enddate;
	private DateWidget		shippingdate;

	public OrderDetails () {
		main = new HorizontalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		parent = null;
	}

	public void wireToForm ( FromServerForm form ) {
		parent = form;
	}

	private Widget widgetWarp ( String name, Widget wid ) {
		if ( parent != null )
			return parent.getPersonalizedWidget ( name, wid );
		else
			return getPersonalizedWidget ( name, wid );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		boolean edit;
		Supplier supp;
		CustomCaptionPanel frame;

		super.setValue ( element );
		main.clear ();

		if ( element != null ) {
			supp = ( Supplier ) element.getObject ( "supplier" );
			edit = ( supp.iAmReference () && element.sharingStatus () <= ACL.ACL_READWRITE );
		}
		else {
			edit = false;
		}

		frame = new CustomCaptionPanel ( "Attributi" );
		main.add ( frame );
		main.setCellWidth ( frame, "50%" );

		supplier = new NameLabelWidget ();
		frame.addPair ( "Fornitore", widgetWarp ( "supplier", supplier ) );

		status = Order.doOrderStatusSelector ( edit );
		frame.addPair ( "Stato", widgetWarp ( "status", status ) );

		if ( edit == true )
			anticipated = new PercentageBox ();
		else
			anticipated = new PercentageViewer ();

		frame.addPair ( "Anticipo", widgetWarp ( "anticipated", ( Widget ) anticipated ) );

		frame = new CustomCaptionPanel ( "Date" );
		main.add ( frame );
		main.setCellWidth ( frame, "50%" );

		if ( edit == true ) {
			startdate = new DateSelector ();
			enddate = new DateSelector ();
			shippingdate = new DateSelector ();
		}
		else {
			startdate = new DateViewer ();
			enddate = new DateViewer ();
			shippingdate = new DateViewer ();
		}

		frame.addPair ( "Data apertura", widgetWarp ( "startdate", ( Widget ) startdate ) );
		frame.addPair ( "Data chiusura", widgetWarp ( "enddate", ( Widget ) enddate ) );
		frame.addPair ( "Data consegna", widgetWarp ( "shippingdate", ( Widget ) shippingdate ) );

		if ( element != null ) {
			supplier.setValue ( element.getObject ( "supplier" ) );
			status.setVal ( element.getInt ( "status" ) );
			anticipated.setValue ( element.getString ( "anticipated" ) );
			startdate.setValue ( element.getDate ( "startdate" ) );
			enddate.setValue ( element.getDate ( "enddate" ) );
			shippingdate.setValue ( element.getDate ( "shippingdate" ) );
		}
	}

	/****************************************************************** FromServerRappresentationFull */

	protected void beforeSave () {
		/* dummy */
	}

	protected void afterSave () {
		/* dummy */
	}
}
