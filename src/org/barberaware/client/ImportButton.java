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
import com.google.gwt.dom.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

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
		super.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				setValue ( null );
			}
		}, ChangeEvent.getType () );
	}

	protected boolean manageUploadResponse ( JSONValue response ) {
		JSONArray orders;
		VerticalPanel pan;
		DialogButtons buttons;
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

		if ( contents.get ( "orders" ) != null ) {
			pan.add ( new HTML ( "<hr>" ) );
			orders = contents.get ( "orders" ).isArray ();

			for ( int i = 0; i < orders.size (); i++ ) {
				order = new Order ();
				order.fromJSONObject ( orders.get ( i ).isObject (), true );
				pan.add ( doOrderBox ( order, contents.get ( "products" ).isArray () ) );
			}

			dialog.setText ( "Importa Ordine" );
		}
		else {
			dialog.setText ( "Importa Fornitore" );
		}

		buttons = new DialogButtons ();
		pan.add ( buttons );
		buttons.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog sender ) {
				alignResponse ();
				replyServer ( "write" );
				dialog.hide ();
			}

			public void onCancel ( SavingDialog sender ) {
				replyServer ( "cancel" );
				dialog.hide ();
			}
		} );

		dialog.show ();
		dialog.center ();
		return true;
	}

	private void replyServer ( String action ) {
		RequestBuilder builder;
		Request response;

		builder = new RequestBuilder ( RequestBuilder.POST, Utils.getServer ().getURL () + "importer.php?action=" + action );
		Utils.getServer ().loadingAlert ( true );

		try {
			response = builder.sendRequest ( originalResponse.toString (), new ServerResponse () {
				public void onComplete ( JSONValue response ) {
					JSONString s;

					s = response.isString ();
					Utils.getServer ().loadingAlert ( false );

					if ( s != null && s.stringValue () == "ok" )
						Utils.showNotification ( "Importazione avvenuta", Notification.INFO );
				}
			} );
		}
		catch ( RequestException e ) {
			Utils.getServer ().loadingAlert ( false );
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
		}
	}

	private void alignResponse () {
		String id;
		JSONObject contents;

		if ( existingNo.getValue () == true ) {
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
		existing.setValue ( true );
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
			existingNo.setValue ( true );
			suppliersList.setEnabled ( true );
		}

		existingNo.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				if ( ( ( RadioButton ) event.getSource () ).getValue () )
					suppliersList.setEnabled ( true );
			}
		});

		existing.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				if ( ( ( RadioButton ) event.getSource () ).getValue () )
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

	private Widget doOrderBox ( FromServer order, JSONArray products ) {
		HorizontalPanel pan;
		VerticalPanel details;
		ListBox list;
		FromServer p;

		pan = new HorizontalPanel ();
		pan.setSpacing ( 10 );
		pan.setWidth ( "100%" );

		/*
			Dettagli essenziali
		*/

		details = new VerticalPanel ();
		pan.add ( details );
		pan.setCellHorizontalAlignment ( details, HasHorizontalAlignment.ALIGN_LEFT );

		details.add ( new Label ( "Data Apertura: " + Utils.printableDate ( order.getDate ( "startdate" ) ) ) );
		details.add ( new Label ( "Data Chiusura: " + Utils.printableDate ( order.getDate ( "enddate" ) ) ) );
		details.add ( new Label ( "Data Consegna: " + Utils.printableDate ( order.getDate ( "shippingdate" ) ) ) );

		/*
			Lista prodotti
		*/

		list = new ListBox ();
		list.addStyleName ( "multirow-select" );
		list.setWidth ( "100%" );
		list.setVisibleItemCount ( 10 );
		pan.add ( list );
		pan.setCellHorizontalAlignment ( list, HasHorizontalAlignment.ALIGN_RIGHT );

		p = new Product ();

		for ( int i = 0; i < products.size (); i++ ) {
			p.fromJSONObject ( products.get ( i ).isObject () );
			list.addItem ( p.getString ( "name" ) );
		}

		return pan;
	}
}

