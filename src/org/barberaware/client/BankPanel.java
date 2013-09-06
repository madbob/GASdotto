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
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankPanel extends GenericPanel {
	private MovementsSummary	mainTable;
	private DateRange		dates;
	private FromServerSelector	userFilter;
	private FromServerSelector	supplierFilter;

	public BankPanel () {
		super ();

		HorizontalPanel hor;
		GAS gas;
		PriceViewer price;
		AddButton button;
		CustomCaptionPanel frame;

		/*
			Tabella principale
		*/

		mainTable = new MovementsSummary ( true );
		mainTable.addEditingColumns ();
		addTop ( mainTable );

		/*
			Pulsanti per creare nuovi movimenti
		*/

		hor = new HorizontalPanel ();
		hor.setStyleName ( "bottom-buttons" );

		button = new AddButton ( "Nuovo Movimento", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				BankManualUpdate update;

				update = new BankManualUpdate ();
				update.center ();
				update.show ();
			}
		} );
		hor.add ( button );

		/**
			TODO	Qui ci dovra' stare il pulsante per importare
				i movimenti estratti da un CSV esterno
		*/

		addTop ( hor );

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		addTop ( hor );

		/*
			Filtri
		*/

		frame = new CustomCaptionPanel ( "Filtri" );
		hor.add ( frame );
		hor.setCellWidth ( frame, "50%" );

		dates = new DateRange ();
		dates.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				loadData ();
			}
		} );
		frame.addPair ( "Dal", dates.getStartDateWidget () );
		frame.addPair ( "Al", dates.getEndDateWidget () );

		userFilter = new FromServerSelector ( "User", true, true, true );
		userFilter.addAllSelector ();
		userFilter.addChangeHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				loadData ();
			}
		} );
		frame.addPair ( "Filtra Utente", userFilter );

		supplierFilter = new FromServerSelector ( "Supplier", true, true, true );
		supplierFilter.addAllSelector ();
		supplierFilter.addChangeHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				loadData ();
			}
		} );
		frame.addPair ( "Filtra Fornitore", supplierFilter );

		/*
			Filtri
		*/

		/*
		frame = new CustomCaptionPanel ( "Saldo" );
		hor.add ( frame );
		hor.setCellWidth ( frame, "50%" );

		gas = Session.getGAS ();

		price = new PriceViewer ();
		price.setVal ( gas.getFloat ( "current_balance" ) );
		frame.addPair ( "Saldo", price );

		price = new PriceViewer ();
		price.setVal ( gas.getFloat ( "current_bank_balance" ) );
		frame.addPair ( "Saldo C/C", price );

		price = new PriceViewer ();
		price.setVal ( gas.getFloat ( "current_cash_balance" ) );
		frame.addPair ( "Saldo Cassa", price );
		*/

		GASBankSummary gas_sum;

		gas_sum = new GASBankSummary ();
		hor.add ( gas_sum );
		hor.setCellWidth ( gas_sum, "50%" );
	}

	private void loadData () {
		FromServer target;
		ObjectRequest params;

		params = new ObjectRequest ( "BankMovement" );
		params.add ( "startdate", Utils.encodeDate ( dates.getStartDate () ) );
		params.add ( "enddate", Utils.encodeDate ( dates.getEndDate () ) );

		target = userFilter.getValue ();
		if ( target != null )
			params.add ( "payuser", Integer.toString ( target.getLocalID () ) );

		target = supplierFilter.getValue ();
		if ( target != null )
			params.add ( "paysupplier", Integer.toString ( target.getLocalID () ) );

		mainTable.refresh ( params );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Contabile";
	}

	public String getSystemID () {
		return "bank";
	}

	public Image getIcon () {
		return new Image ( "images/path_bank.png" );
	}

	public void initView () {
		ObjectRequest params;

		params = new ObjectRequest ( "User" );

		Utils.getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				ObjectRequest params;

				Utils.getServer ().responseToObjects ( response );
				params = new ObjectRequest ( "Supplier" );

				Utils.getServer ().serverGet ( params, new ServerResponse () {
					public void onComplete ( JSONValue response ) {
						supplierFilter.unlock ();
						userFilter.unlock ();
						loadData ();
					}
				} );
			}
		} );
	}
}

