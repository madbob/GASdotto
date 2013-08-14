/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.logical.shared.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankMovementForm extends FromServerRappresentation {
	private FlexTable		main;
	private CyclicToggle		method;
	private Date			defaultDate;
	private float			defaultAmount;
	private String			defaultNote;
	private FromServer		target;
	private boolean			displayCro;
	private boolean			justDate;

	public BankMovementForm () {
		target = null;
		defaultAmount = 0;
		defaultDate = new Date ( System.currentTimeMillis () );
		displayCro = true;
		justDate = false;

		main = new FlexTable ();
		main.setWidth ( "100%" );
		initWidget ( main );
	}

	private void populateWidgets () {
		DateSelector custom_date;

		if ( main.getRowCount () == 0 ) {
			if ( justDate == false ) {
				main.setWidget ( 0, 0, new Label ( "Data" ) );
				main.setWidget ( 0, 1, getWidget ( "date" ) );

				main.setWidget ( 1, 0, new Label ( "Importo" ) );
				main.setWidget ( 1, 1, getWidget ( "amount" ) );

				main.setWidget ( 2, 0, new Label ( "Metodo" ) );
				method = new CyclicToggle ( true );
				method.addState ( "images/by_bank.png" );
				method.addState ( "images/by_cash.png" );
				main.setWidget ( 2, 1, getPersonalizedWidget ( "method", method ) );

				method.addValueChangeHandler ( new ValueChangeHandler<Integer> () {
					public void onValueChange ( ValueChangeEvent<Integer> event ) {
						renderCro ();
					}
				} );

				main.setWidget ( 3, 0, new Label ( "CRO" ) );
				main.setWidget ( 3, 1, getWidget ( "cro" ) );

				main.setWidget ( 4, 0, new Label ( "Note" ) );
				main.setWidget ( 4, 1, getWidget ( "notes" ) );
			}
			else {
				custom_date = new DateSelector ();
				custom_date.yearSelectable ( true );

				main.setWidget ( 0, 0, new Label ( "Data" ) );
				main.setWidget ( 0, 1, getPersonalizedWidget ( "date", custom_date ) );
			}
		}
	}

	/*
		Tutte le funzioni usate per settare dei valori di default
		devono essere invocate dopo setValue()
	*/

	public void setDefaultAmount ( float amount ) {
		FromServer movement;

		defaultAmount = amount;
		movement = getValue ();

		if ( movement != null && ( movement.getFloat ( "amount" ) == 0 ) ) {
			movement.setFloat ( "amount", defaultAmount );
			refreshContents ( movement );
		}
	}

	public void setDefaultDate ( Date date ) {
		FromServer movement;

		defaultDate = date;
		movement = getValue ();

		if ( movement != null && ( movement.getDate ( "date" ) == null ) ) {
			movement.setDate ( "date", defaultDate );
			refreshContents ( movement );
		}
	}

	public void setDefaultNote ( String note ) {
		FromServer movement;

		defaultNote = note;
		movement = getValue ();

		if ( movement != null && ( movement.getString ( "notes" ) == "" ) ) {
			movement.setString ( "notes", defaultNote );
			refreshContents ( movement );
		}
	}

	public void setDefaultTarget ( FromServer t ) {
		target = t;
	}

	public void showCro ( boolean show ) {
		displayCro = show;
		renderCro ();
	}

	public void showJustDate ( boolean just ) {
		justDate = just;
	}

	private void renderCro () {
		HTMLTable.RowFormatter format;
		StringWidget cro;

		format = main.getRowFormatter ();

		if ( method.getVal () == BankMovement.BY_BANK && displayCro == true ) {
			format.removeStyleName ( 3, "hidden" );

			cro = ( StringWidget ) retriveInternalWidget ( "cro" );
			cro.setValue ( "" );
		}
		else {
			format.addStyleName ( 3, "hidden" );
		}
	}

	/****************************************************************** FromServerRappresentation */

	public void setValue ( FromServer obj ) {
		super.setValue ( obj );
		setDefaultDate ( defaultDate );
		setDefaultAmount ( defaultAmount );
		setDefaultNote ( defaultNote );
		populateWidgets ();
	}

	public FromServer getValue () {
		FromServer ret;

		rebuildObject ();
		ret = super.getValue ();

		if ( ret != null && target != null ) {
			if ( target instanceof User )
				ret.setObject ( "payuser", target );
			else if ( target instanceof Supplier )
				ret.setObject ( "paysupplier", target );
		}

		return ret;
	}
}

