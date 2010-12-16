/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProductDeliveryCell extends Composite implements SourcesChangeEvents {
	private HorizontalPanel			main;

	private float				currentQuantity;
	private Widget				quantityLabel;
	private float				currentDelivery;
	private ProductDeliveryEditableCell	box;
	private float				currentPrice;
	private PriceViewer			priceLabel;

	private ProductUser			referenceProd		= null;

	private ArrayList			changeCallbacks		= null;

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
			if ( prod.getBool ( "mutable_price" ) == false && input > currentQuantity )
				Utils.showNotification ( "Hai immesso una quantità diversa da quella ordinata", Notification.INFO );

			price = referenceProd.getTotalPrice ( input );
		}

		total_label.setText ( Utils.priceToString ( price ) );
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( changeCallbacks, this );
	}

	public void setCell ( Widget wid, String width ) {
		main.setCellWidth ( wid, width );
		main.setCellVerticalAlignment ( wid, HasVerticalAlignment.ALIGN_MIDDLE );
	}

	private void quantifyVariants ( ArrayList variants ) {
		boolean found;
		int a;
		String id_variant;
		Label q;
		Label id;
		FlexTable quantityTable;
		ProductUserVariant variant;

		quantityTable = ( FlexTable ) quantityLabel;

		for ( int i = 0; i < variants.size (); i++ ) {
			variant = ( ProductUserVariant ) variants.get ( i );
			id_variant = variant.getTextSummary ();
			found = false;

			for ( a = 0; a < quantityTable.getRowCount (); a++ ) {
				id = ( Label ) quantityTable.getWidget ( a, 1 );

				if ( id.getText () == id_variant ) {
					q = ( Label ) quantityTable.getWidget ( a, 0 );
					q.setText ( Integer.toString ( Integer.parseInt ( q.getText () ) + 1 ) );
					found = true;
					break;
				}
			}

			if ( found == false ) {
				quantityTable.setWidget ( a, 0, new Label ( "1" ) );
				quantityTable.setWidget ( a, 1, new Label ( id_variant ) );
			}
		}
	}

	public void addProductUser ( FromServer prod_user ) {
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

		variants = prod_user.getArray ( "variants" );

		if ( variants == null || variants.size () == 0 ) {
			measure = prod.getObject ( "measure" );
			if ( measure != null )
				symbol = " " + measure.getString ( "name" );
			else
				symbol = "";

			if ( first_round == true ) {
				currentQuantity = prod_user.getFloat ( "quantity" );
				quantityLabel = new Label ();
				main.add ( quantityLabel );
				setCell ( quantityLabel, "20%" );

				currentDelivery = prod_user.getFloat ( "delivered" );

				if ( prod.getFloat ( "unit_size" ) != 0 )
					box = new ProductDeliveryEditablePiecesCell ();
				else
					box = new ProductDeliveryEditablePlainCell ();

				main.add ( ( Widget ) box );
				setCell ( ( Widget ) box, "20%" );

				box.addChangeListener ( new ChangeListener () {
					public void onChange ( Widget sender ) {
						newInputToCheck ();
						triggerChange ();
					}
				} );

				currentPrice = referenceProd.getTotalPrice ( currentDelivery );
				priceLabel = new PriceViewer ();
				main.add ( priceLabel );
				setCell ( priceLabel, "20%" );
			}
			else {
				currentQuantity += prod_user.getFloat ( "quantity" );
				currentDelivery += prod_user.getFloat ( "delivered" );
				currentPrice = referenceProd.getTotalPrice ( currentDelivery );
			}

			( ( Label ) quantityLabel ).setText ( Utils.floatToString ( currentQuantity ) + symbol );
		}
		else {
			if ( first_round == true ) {
				box = new ProductDeliveryEditableVariantsCell ();
				quantityLabel = new FlexTable ();
				main.add ( quantityLabel );
				setCell ( quantityLabel, "20%" );
			}

			quantifyVariants ( variants );
		}

		priceLabel.setVal ( currentPrice );
		box.addProductUser ( prod_user );
	}

	public ArrayList getAlignedProducts () {
		return box.getAlignedProducts ();
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

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks == null )
			changeCallbacks = new ArrayList ();
		changeCallbacks.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks != null )
			changeCallbacks.remove ( listener );
	}
}
