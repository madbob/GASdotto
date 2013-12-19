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
	private CustomFormTable		main;
	private CyclicToggle		method;
	private Date			defaultDate;
	private float			defaultAmount;
	private int			defaultMethod;
	private String			defaultNote;
	private FromServer		targetUser;
	private FromServer		targetSupplier;
	private boolean			displayCro;
	private boolean			justDate;
	private boolean			editable;

	public BankMovementForm () {
		targetUser = null;
		targetSupplier = null;
		defaultAmount = 0;
		defaultDate = new Date ( System.currentTimeMillis () );
		displayCro = true;
		justDate = false;
		editable = true;

		main = new CustomFormTable ();
		initWidget ( main );
	}

	private void populateWidgets () {
		DateSelector custom_date;

		if ( main.getRowCount () == 0 ) {
			if ( justDate == false ) {
				if ( editable == true ) {
					main.addPair ( "Data", getWidget ( "date" ) );
					main.addPair ( "Importo", getWidget ( "amount" ) );
				}
				else {
					main.addPair ( "Data", getPersonalizedWidget ( "date", new DateViewer () ) );
					main.addPair ( "Importo", getPersonalizedWidget ( "amount", new PriceViewer () ) );
				}

				method = new CyclicToggle ( true );
				method.addState ( "images/by_bank.png" );
				method.addState ( "images/by_cash.png" );
				main.addPair ( "Metodo", getPersonalizedWidget ( "method", method ) );

				method.addValueChangeHandler ( new ValueChangeHandler<Integer> () {
					public void onValueChange ( ValueChangeEvent<Integer> event ) {
						renderCro ();
					}
				} );

				main.addPair ( "CRO", getWidget ( "cro" ) );

				if ( editable == true )
					main.addPair ( "Descrizione", getWidget ( "notes" ) );
				else
					main.addPair ( "Descrizione", getPersonalizedWidget ( "notes", new StringLabel () ) );
			}
			else {
				custom_date = new DateSelector ();
				custom_date.yearSelectable ( true );

				main.addPair ( "Data", getPersonalizedWidget ( "date", custom_date ) );
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

	public void setDefaultTargetUser ( FromServer t ) {
		targetUser = t;
	}

	public void setDefaultTargetSupplier ( FromServer t ) {
		targetSupplier = t;
	}

	public void setDefaultMethod ( int method ) {
		FromServer movement;

		defaultMethod = method;
		movement = getValue ();

		if ( movement != null ) {
			movement.setInt ( "method", defaultMethod );
			refreshContents ( movement );
			renderCro ();
		}
	}

	public void showCro ( boolean show ) {
		displayCro = show;
		renderCro ();
	}

	public void showJustDate ( boolean just ) {
		justDate = just;
	}

	public void setEditable ( boolean edit ) {
		editable = edit;
	}

	public void showMethod ( boolean show ) {
		main.showByLabel ( "Metodo", show );
	}

	private void renderCro () {
		StringWidget cro;

		/*
			Se sono in modalita' "solo data", il widget del CRO non
			viene manco inizializzato dunque non c'e' nulla da
			gestire qui
		*/
		if ( justDate == true )
			return;

		if ( method.getVal () == BankMovement.BY_BANK && displayCro == true ) {
			main.showByLabel ( "CRO", true );

			cro = ( StringWidget ) retriveInternalWidget ( "cro" );
			cro.setValue ( "" );
		}
		else {
			main.showByLabel ( "CRO", false );
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

		if ( ret != null ) {
			if ( targetUser != null )
				ret.setInt ( "payuser", targetUser.getLocalID () );
			if ( targetSupplier != null )
				ret.setInt ( "paysupplier", targetSupplier.getLocalID () );
		}

		return ret;
	}
}

