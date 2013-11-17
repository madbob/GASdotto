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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class ProductDeliveryCell extends Composite {
	private HorizontalPanel			main;

	private float				currentQuantity		= 0;
	private Widget				quantityLabel;
	private float				currentDelivery		= 0;
	private ProductDeliveryEditableCell	box;
	private float				currentPrice		= 0;
	private PriceViewer			priceLabel;

	private boolean				dynamic			= false;
	private FromServerSelector		dynamicProductSelect;

	private ProductUser			referenceProd		= null;

	public ProductDeliveryCell () {
		main = new HorizontalPanel ();
		main.setWidth ( "100%" );
		initWidget ( main );
	}

	public float getCurrentQuantity () {
		return box.getCurrentQuantity ();
	}

	private void newInputToCheck () {
		float input;
		float price;
		Label total_label;
		FromServer prod;

		input = box.getCurrentQuantity ();
		total_label = ( Label ) main.getWidget ( 3 );

		if ( input < 0 ) {
			Utils.showNotification ( "Il valore immesso non è valido" );
			box.clear ();
			price = 0;
		}
		else {
			prod = referenceProd.getObject ( "product" );

			/*
				Se la quantita' immessa e' diversa da quella ordinata si limita a
				mostrare una notifica, ma non blocca l'operazione in quanto puo'
				succedere che avanzi qualche prodotto (ordinato per arrotondamento) e
				si distribuisca arbitrariamente
			*/
			if ( currentQuantity != -1 && prod.getBool ( "mutable_price" ) == false && input > currentQuantity )
				Utils.showNotification ( "Hai immesso una quantità diversa da quella ordinata", Notification.INFO );

			price = referenceProd.getTotalPrice ( input );
		}

		total_label.setText ( Utils.priceToString ( price ) );
	}

	private void triggerChange () {
		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
	}

	public void setCell ( Widget wid, String width ) {
		main.setCellWidth ( wid, width );
		main.setCellVerticalAlignment ( wid, HasVerticalAlignment.ALIGN_MIDDLE );
	}

	private void quantifyVariants ( FromServer prod_user, ArrayList variants ) {
		boolean found;
		float default_quantity;
		int a;
		String id_variant;
		Label q;
		Label id;
		FlexTable quantityTable;
		ProductUserVariant variant;
		Product prod;

		quantityTable = ( FlexTable ) quantityLabel;
		prod = ( Product ) prod_user.getObject ( "product" );

		if ( prod.hasAtomicQuantity () == true )
			default_quantity = prod_user.getFloat ( "quantity" );
		else
			default_quantity = 1;

		for ( int i = 0; i < variants.size (); i++ ) {
			variant = ( ProductUserVariant ) variants.get ( i );
			id_variant = variant.getTextSummary ();
			found = false;

			for ( a = 0; a < quantityTable.getRowCount (); a++ ) {
				id = ( Label ) quantityTable.getWidget ( a, 1 );

				if ( id.getText () == id_variant ) {
					q = ( Label ) quantityTable.getWidget ( a, 0 );
					q.setText ( Utils.floatToString ( Utils.stringToFloat ( q.getText () ) + default_quantity ) );
					found = true;
					break;
				}
			}

			if ( found == false ) {
				quantityTable.setWidget ( a, 0, new Label ( Utils.floatToString ( default_quantity ) ) );
				quantityTable.setWidget ( a, 1, new Label ( id_variant ) );
			}
		}
	}

	private void prepareEditableCell ( ProductDeliveryEditableCell cell ) {
		( ( Widget ) cell ).addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				newInputToCheck ();
				triggerChange ();
			}
		}, ChangeEvent.getType () );
	}

	public void addProductUser ( FromServer prod_user ) {
		/*
			first_round e' per discriminare se sto aggiungendo il prodotto per la
			prima volta (solitamente quando ho appena creato l'oggetto) o ne sto
			aggiungendo altre istanze (quando in ProductsDeliveryTable itero anche
			i prodotti negli ordini degli amici dell'ordine principale)
		*/
		boolean first_round;
		String symbol;
		ArrayList variants;
		Label header;
		FromServer prod;
		FromServer measure;
		ProductUser pu;

		prod = prod_user.getObject ( "product" );

		if ( referenceProd == null ) {
			first_round = true;
			referenceProd = ( ProductUser ) prod_user;

			header = new Label ( prod.getString ( "name" ) );
			main.add ( header );
			setCell ( header, "40%" );
		}
		else {
			first_round = false;
		}

		currentQuantity += prod_user.getFloat ( "quantity" );
		currentDelivery += prod_user.getFloat ( "delivered" );
		currentPrice = referenceProd.getTotalPrice ( currentDelivery );

		variants = prod_user.getArray ( "variants" );

		if ( variants == null || variants.size () == 0 ) {
			measure = prod.getObject ( "measure" );
			if ( measure != null )
				symbol = " " + measure.getString ( "name" );
			else
				symbol = "";

			if ( first_round == true ) {
				quantityLabel = new Label ();
				main.add ( quantityLabel );
				setCell ( quantityLabel, "20%" );

				if ( prod.getFloat ( "unit_size" ) != 0 )
					box = new ProductDeliveryEditablePiecesCell ();
				else
					box = new ProductDeliveryEditablePlainCell ();

				main.add ( ( Widget ) box );
				setCell ( ( Widget ) box, "20%" );
			}

			( ( Label ) quantityLabel ).setText ( Utils.floatToString ( currentQuantity ) + symbol );
		}
		else {
			if ( first_round == true ) {
				quantityLabel = new FlexTable ();
				main.add ( quantityLabel );
				setCell ( quantityLabel, "20%" );

				if ( ( ( Product ) prod ).hasAtomicQuantity () == true )
					box = new ProductDeliveryEditableAtomicVariantsCell ();
				else
					box = new ProductDeliveryEditableVariantsCell ();

				main.add ( ( Widget ) box );
				setCell ( ( Widget ) box, "20%" );
			}

			quantifyVariants ( prod_user, variants );
		}

		if ( first_round == true ) {
			priceLabel = new PriceViewer ();
			main.add ( priceLabel );
			setCell ( priceLabel, "20%" );

			prepareEditableCell ( box );
		}

		priceLabel.setVal ( currentPrice );
		box.addProductUser ( prod_user );
	}

	public void goDynamic ( final Order reference_order ) {
		referenceProd = new ProductUser ();
		currentQuantity = -1;
		dynamic = true;

		dynamicProductSelect = new FromServerSelector ( "Product", true, true, true );

		dynamicProductSelect.addFilter ( new FromServerValidateCallback () {
			public boolean checkObject ( FromServer object ) {
				return reference_order.hasProduct ( ( Product ) object );
			}
		} );

		dynamicProductSelect.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				referenceProd.setObject ( "product", dynamicProductSelect.getValue () );
				newInputToCheck ();
				triggerChange ();
			}
		} );

		dynamicProductSelect.unlock ();
		main.add ( dynamicProductSelect );
		setCell ( dynamicProductSelect, "40%" );

		quantityLabel = new Label ();
		main.add ( quantityLabel );
		setCell ( quantityLabel, "20%" );

		box = new ProductDeliveryEditablePlainCell ();
		main.add ( ( Widget ) box );
		setCell ( ( Widget ) box, "20%" );
		prepareEditableCell ( box );

		priceLabel = new PriceViewer ();
		main.add ( priceLabel );
		setCell ( priceLabel, "20%" );

		box.addProductUser ( referenceProd );
	}

	public boolean previouslyExisting () {
		return ( dynamic == false );
	}

	/*
		If previouslyExisting() returns true
	*/
	public void alignProducts () {
		box.alignProducts ();
	}

	/*
		If previouslyExisting() returns false get this return value and insert into the
		reference OrderUser
	*/
	public FromServer getDynamicValue () {
		float quantity;

		quantity = box.getCurrentQuantity ();
		referenceProd.setFloat ( "quantity", quantity );
		referenceProd.setFloat ( "delivered", quantity );
		referenceProd.setObject ( "product", dynamicProductSelect.getValue () );
		referenceProd.setDate ( "orderdate", new Date ( System.currentTimeMillis () ) );
		referenceProd.setObject ( "orderperson", Session.getUser () );
		return referenceProd;
	}

	/*
		Attenzione: questa funzione ritorna il prezzo complessivo, l'omonima funzione in
		ProductDeliveryEditableCell torna la quantita'
	*/
	public float shipAll () {
		float quantity;
		float price;

		quantity = box.shipAll ();
		price = referenceProd.getTotalPrice ( quantity );
		priceLabel.setVal ( price );
		return price;
	}

	public FromServer getReferenceProduct () {
		return referenceProd;
	}
}
