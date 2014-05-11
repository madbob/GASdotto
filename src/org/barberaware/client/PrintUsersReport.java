/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class PrintUsersReport extends DialogBox {
	private FromServerSelector	filterOrder;
	private ListBox			creditSelect;
	private CheckBox		showLeaved;
	private ArrayList<CheckBox>	boxes;

	public PrintUsersReport () {
		VerticalPanel pan;
		CaptionPanel attrs;
		CustomFormTable frame;
		HorizontalPanel hor;
		HorizontalPanel internal_hor;
		CustomCaptionPanel reports;
		DialogButtons buttons;

		boxes = new ArrayList<CheckBox> ();
		this.setText ( "Report Utenti" );

		pan = new VerticalPanel ();
		pan.setWidth ( "100%" );
		this.setWidget ( pan );

		hor = new HorizontalPanel ();
		pan.add ( hor );
		hor.setWidth ( "100%" );

		attrs = new CaptionPanel ( "Attributi" );
		hor.add ( attrs );
		hor.setCellWidth ( attrs, "50%" );

		internal_hor = new HorizontalPanel ();
		attrs.add ( internal_hor );

		frame = new CustomFormTable ();
		internal_hor.add ( frame );
		setCheckBox ( frame, "Login Accesso", "login", false );
		setCheckBox ( frame, "Nome", "firstname", true );
		setCheckBox ( frame, "Cognome", "surname", true );
		setCheckBox ( frame, "Telefono", "phone", false );
		setCheckBox ( frame, "Cellulare", "mobile", false );
		setCheckBox ( frame, "Mail", "mail", true );
		setCheckBox ( frame, "Indirizzo", "address", false );
		setCheckBox ( frame, "Data di Nascita", "birthday", false );
		setCheckBox ( frame, "Codice Fiscale", "codfisc", false );
		setCheckBox ( frame, "Persone in Famiglia", "family", false );

		frame = new CustomFormTable ();
		internal_hor.add ( frame );
		setCheckBox ( frame, "Iscritto da", "join_date", false );
		setCheckBox ( frame, "Numero Tessera", "card_number", false );
		setCheckBox ( frame, "Cauzione", "deposit", false );
		setCheckBox ( frame, "Quota Pagata", "paying", true );
		setCheckBox ( frame, "Ruolo", "privileges", false );
		setCheckBox ( frame, "Data di Cessazione", "leaving_date", false );
		setCheckBox ( frame, "Ultimo Login", "lastlogin", false );

		if ( Session.getGAS ().getBool ( "use_rid" ) ) {
			setCheckBox ( frame, "Conto Corrente", "bank_account", false );
			setCheckBox ( frame, "Sottoscrizione SEPA", "sepa_subscribe", false );
		}

		if ( Session.getGAS ().getBool ( "use_bank" ) == true )
			setCheckBox ( frame, "Credito Disponibile", "current_balance", false );

		reports = new CustomCaptionPanel ( "Selezione" );
		hor.add ( reports );
		hor.setCellWidth ( reports, "50%" );

		filterOrder = new FromServerSelector ( "Order", false, true, false );
		reports.addPair ( "Hanno partecipato all'ordine", filterOrder );

		creditSelect = new ListBox ();
		creditSelect.addItem ( "Indifferente", "none" );
		creditSelect.addItem ( "Uguale a 0", "zero" );
		creditSelect.addItem ( "Diverso da 0", "nonzero" );
		reports.addPair ( "Credito", creditSelect );

		showLeaved = new CheckBox ();
		reports.addPair ( "Includi Utenti Cessati", showLeaved );

		buttons = new DialogButtons ();

		buttons.addCallback (
			new SavingDialogCallback () {
				public void onSave ( SavingDialog d ) {
					String attributes;
					String credit;
					String filters;
					String sep;
					String url;
					FromServer obj;

					attributes = "";
					sep = "";

					for ( CheckBox box : boxes ) {
						if ( box.getValue () == true ) {
							attributes = attributes + sep + box.getName ();
							sep = ",";
						}
					}

					filters = "l";

					obj = filterOrder.getValue ();
					if ( obj != null )
						filters += ",order:" + obj.getLocalID ();

					credit = creditSelect.getValue ( creditSelect.getSelectedIndex () );
					if ( credit != "none" )
						filters += ",credit:" + credit;

					if ( showLeaved.getValue () == true )
						filters += ",leaved:1";

					url = Utils.getServer ().getURL () + "/user_reports.php?attributes=" + attributes + "&filter=" + filters;

					Window.open ( url, "_blank", "" );
					hide ();
				}

				public void onCancel ( SavingDialog d ) {
					hide ();
				}
			}
		);

		pan.add ( buttons );
	}

	private void setCheckBox ( CustomFormTable frame, String label, String name, boolean val ) {
		CheckBox box;

		box = new CheckBox ();
		box.setName ( name );
		box.setValue ( val );
		frame.addPair ( label, box, "large-custom-label" );
		boxes.add ( box );
	}
}

