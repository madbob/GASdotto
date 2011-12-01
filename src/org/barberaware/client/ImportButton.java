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
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class ImportButton extends FileUploadDialog {
	private JSONValue		originalResponse;
	private DialogBox		dialog;
	private RadioButton		existingNo;
	private ListBox			suppliersList;

	public ImportButton () {
		super.setEmptyString ( "Importa" );
		super.setDestination ( "importer.php" );

		/*
			Per resettare il file appena caricato
		*/
		super.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				setValue ( null );
			}
		} );
	}

	protected boolean manageUploadResponse ( JSONValue response ) {
		VerticalPanel pan;
		HorizontalPanel buttons;
		Button but;
		JSONObject contents;
		FromServer supplier;
		FromServer order;

		contents = response.isObject ();
		if ( contents == null )
			return false;

		originalResponse = response;

		dialog = new DialogBox ();

		pan = new VerticalPanel ();
		dialog.setWidget ( pan );

		supplier = new Supplier ();
		supplier.fromJSONObject ( contents.get ( "supplier" ).isObject (), true );
		pan.add ( doSupplierBox ( supplier ) );

		if ( contents.get ( "order" ) != null ) {
			pan.add ( new HTML ( "<hr>" ) );

			order = new Order ();
			order.fromJSONObject ( contents.get ( "order" ).isObject () );
			pan.add ( doOrderBox ( order ) );

			dialog.setText ( "Importa Ordine" );
		}
		else {
			dialog.setText ( "Importa Fornitore" );
		}

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				alignResponse ();
				replyServer ( "write" );
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				replyServer ( "cancel" );
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		dialog.show ();
		dialog.center ();
		return true;
	}

	private void replyServer ( String action ) {
		RequestBuilder builder;
		Request response;

		builder = new RequestBuilder ( RequestBuilder.POST, Utils.getServer ().getURL () + "importer.php?action=" + action );

		try {
			response = builder.sendRequest ( originalResponse.toString (), new ServerResponse () {
				public void onComplete ( JSONValue response ) {
					/*
						dummy
					*/
				}
			} );
		}
		catch ( RequestException e ) {
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
		}
	}

	private void alignResponse () {
		String id;
		JSONObject contents;

		if ( existingNo.isChecked () == true ) {
			contents = originalResponse.isObject ();
			id = suppliersList.getValue ( suppliersList.getSelectedIndex () );
			contents.get ( "supplier" ).isObject ().put ( "id", new JSONString ( id ) );
		}
	}

	private Widget doSupplierBox ( FromServer supplier ) {
		int selected;
		String tmp;
		ArrayList supps;
		VerticalPanel pan;
		VerticalPanel details;
		Grid supplier_check;
		RadioButton existing;
		Label name;
		FromServer s;

		pan = new VerticalPanel ();
		pan.setSpacing ( 10 );

		name = new Label ( supplier.getString ( "name" ) );
		pan.add ( name );
		pan.setCellHorizontalAlignment ( name, HasHorizontalAlignment.ALIGN_CENTER );
		pan.add ( new HTML ( "<hr>" ) );

		/*
			Selezione nuovo/esistente
		*/

		supplier_check = new Grid ( 2, 2 );
		pan.add ( supplier_check );

		existing = new RadioButton ( "supplier_exists", "Nuovo Fornitore" );
		existing.setChecked ( true );
		supplier_check.setWidget ( 0, 0, existing );
		existingNo = new RadioButton ( "supplier_exists", "Aggiornamento di un Fornitore Esistente" );
		supplier_check.setWidget ( 1, 0, existingNo );

		suppliersList = new ListBox ();
		suppliersList.setEnabled ( false );
		supplier_check.setWidget ( 1, 1, suppliersList );

		selected = -1;
		supps = Utils.getServer ().getObjectsFromCache ( "Supplier" );

		for ( int i = 0; i < supps.size (); i++ ) {
			s = ( FromServer ) supps.get ( i );
			if ( s.getLocalID () == -1 )
				continue;

			suppliersList.addItem ( s.getString ( "name" ), Integer.toString ( s.getLocalID () ) );

			if ( checkSameSupplier ( supplier, s ) ) {
				selected = i;
				suppliersList.setItemSelected ( i, true );
			}
		}

		if ( selected != -1 ) {
			existingNo.setChecked ( true );
			suppliersList.setEnabled ( true );
		}

		existingNo.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( ( ( RadioButton ) sender ).isChecked () )
					suppliersList.setEnabled ( true );
			}
		});

		existing.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( ( ( RadioButton ) sender ).isChecked () )
					suppliersList.setEnabled ( false );
			}
		});

		return pan;
	}

	private boolean checkSameSupplier ( FromServer first, FromServer second ) {
		String tmp;

		tmp = first.getString ( "tax_code" );
		if ( tmp != "" )
			return second.getString ( "tax_code" ) == tmp;

		tmp = first.getString ( "vat_number" );
		if ( tmp != "" )
			return second.getString ( "vat_number" ) == tmp;

		return second.getString ( "name" ) == first.getString ( "name" );
	}

	private Widget doOrderBox ( FromServer order ) {
		ArrayList products;
		HorizontalPanel pan;
		VerticalPanel details;
		FromServer p;

		pan = new HorizontalPanel ();

		/*
			Dettagli essenziali
		*/

		details = new VerticalPanel ();
		pan.add ( details );

		details.add ( new Label ( "Data Apertura: " + Utils.printableDate ( order.getDate ( "startdate" ) ) ) );
		details.add ( new Label ( "Data Chiusura: " + Utils.printableDate ( order.getDate ( "enddate" ) ) ) );
		details.add ( new Label ( "Data Consegna: " + Utils.printableDate ( order.getDate ( "shippingdate" ) ) ) );

		/*
			Lista prodotti
		*/

		details = new VerticalPanel ();
		pan.add ( details );

		products = order.getArray ( "products" );

		for ( int i = 0; i < products.size (); i++ ) {
			p = ( FromServer ) products.get ( i );
			details.add ( new Label ( p.getString ( "name" ) ) );
		}

		return pan;
	}
}

