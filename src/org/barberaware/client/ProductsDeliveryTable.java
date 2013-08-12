/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class ProductsDeliveryTable extends FromServerRappresentation implements ObjectWidget, SourcesChangeEvents {
	private FromServer		object;
	private VerticalPanel		main;
	private PriceViewer		totalLabel;
	private BankMovementSelector	payment;
	private ArrayList		changeCallbacks		= null;

	public ProductsDeliveryTable () {
		Label header;
		HorizontalPanel hor;
		HTMLTable.RowFormatter formatter;

		main = new VerticalPanel ();
		main.setStyleName ( "elements-table" );
		main.setWidth ( "90%" );
		initWidget ( main );

		hor = new HorizontalPanel ();
		hor.setStyleName ( "table-header" );
		hor.setWidth ( "100%" );
		main.add ( hor );

		header = new Label ( "Prodotto" );
		hor.add ( header );
		hor.setCellWidth ( header, "40%" );

		header = new Label ( "Quantità Ordinata" );
		hor.add ( header );
		hor.setCellWidth ( header, "20%" );

		header = new Label ( "Quantità Consegnata" );
		hor.add ( header );
		hor.setCellWidth ( header, "20%" );

		header = new Label ( "Prezzo Totale" );
		hor.add ( header );
		hor.setCellWidth ( header, "20%" );

		totalLabel = null;
		payment = null;
	}

	public float getTotal () {
		if ( totalLabel != null )
			return totalLabel.getVal ();
		else
			return 0;
	}

	private Button createAddProductButton () {
		Button ret;

		ret = new Button ( "Aggiungi Prodotto" );
		ret.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				ProductDeliveryCell row;

				row = new ProductDeliveryCell ();
				row.goDynamic ( ( Order ) object.getObject ( "baseorder" ) );
				attachDeliveryCell ( row );
				main.insert ( row, main.getWidgetCount () - 2 );
			}
		} );

		return ret;
	}

	private Button createAutoCompleteButton () {
		Button ret;

		ret = new Button ( "Consegna Tutto" );
		ret.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				float total_sum;
				ProductDeliveryCell row;

				total_sum = 0;

				for ( int i = 1; i < main.getWidgetCount () - 2; i++ ) {
					row = ( ProductDeliveryCell ) main.getWidget ( i );
					total_sum = total_sum + row.shipAll ();
				}

				setTotal ( total_sum );
				triggerChange ();
			}
		} );

		return ret;
	}

	private void setTotal ( float total_sum ) {
		totalLabel.setVal ( total_sum );

		if ( payment != null )
			payment.setDefaultAmount ( total_sum );
	}

	private void upgradeTotal () {
		float total_sum;
		ProductUser prod;
		ProductDeliveryCell row;

		if ( totalLabel == null )
			return;

		total_sum = 0;

		for ( int i = 1; i < main.getWidgetCount () - 2; i++ ) {
			row = ( ProductDeliveryCell ) main.getWidget ( i );

			prod = ( ProductUser ) row.getReferenceProduct ();
			total_sum += prod.getTotalPrice ( row.getCurrentQuantity () );
		}

		setTotal ( total_sum );
	}

	private void bottomLine ( VerticalPanel main, float price_total ) {
		Label filler;
		HorizontalPanel bottom;
		Widget button;
		Widget totalcell;

		bottom = new HorizontalPanel ();
		bottom.setWidth ( "100%" );
		main.add ( bottom );

		button = createAddProductButton ();
		bottom.add ( button );
		bottom.setCellVerticalAlignment ( button, HasVerticalAlignment.ALIGN_MIDDLE );
		bottom.setCellWidth ( button, "40%" );

		filler = new Label ();
		bottom.add ( filler );
		bottom.setCellWidth ( filler, "20%" );

		button = createAutoCompleteButton ();
		bottom.add ( button );
		bottom.setCellVerticalAlignment ( button, HasVerticalAlignment.ALIGN_MIDDLE );
		bottom.setCellWidth ( button, "20%" );

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalLabel.setStyleName ( "bigger-text" );
		}

		totalLabel.setVal ( price_total );

		if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
			VerticalPanel totbox;

			totbox = new VerticalPanel ();
			totbox.add ( totalLabel );

			if ( payment == null ) {
				payment = new BankMovementSelector ();
				payment.showCro ( false );
				payment.setDefaultTarget ( object.getObject ( "baseuser" ) );
				payment.setDefaultType ( BankMovement.ORDER_USER_PAYMENT );
				payment.setDefaultNote ( "Pagamento ordine a " + object.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" ) );
			}

			payment.setDefaultAmount ( price_total );
			payment.setValue ( object.getObject ( "payment_event" ) );
			totbox.add ( payment );

			totalcell = totbox;
		}
		else {
			totalcell = totalLabel;
		}

		bottom.add ( totalcell );
		bottom.setCellVerticalAlignment ( totalcell, HasVerticalAlignment.ALIGN_MIDDLE );
		bottom.setCellWidth ( totalcell, "20%" );
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( changeCallbacks, this );
	}

	private void attachDeliveryCell ( ProductDeliveryCell row ) {
		row.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				upgradeTotal ();
				triggerChange ();
			}
		} );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		int rows;
		boolean found;
		float price_total;
		ArrayList elements;
		ProductUser prod_user;
		ProductDeliveryCell row;
		FromServer prod;

		/*
			In questa funzione prendo tutti i prodotti contemplati nell'ordine utente
			(sia quello principale, che quelli degli amici ivi contenuti) per mezzo
			dell'attributo "allproducts", e li assegno per tipo nelle diverse
			ProductDeliveryCell.
			Ogni ProductDeliveryCell tiene riferimento di tutti i ProductUsers e
			le diverse implementazioni di ProductDeliveryEditableCell gestite al loro
			interno provvedono (per mezzo della funzione getAlignedProducts()) a
			distribuire la quantita' ordinata specificata tra i diversi ProductUsers.
		*/

		object = element;
		elements = element.getArray ( "allproducts" );
		price_total = 0;

		rows = main.getWidgetCount () - 1;
		for ( int i = 0; i < rows; i++ )
			main.remove ( 1 );

		for ( int i = 0; i < elements.size (); i++ ) {
			prod_user = ( ProductUser ) elements.get ( i );

			prod = prod_user.getObject ( "product" );
			if ( prod.getBool ( "available" ) == false )
				continue;

			row = null;
			found = false;

			for ( int e = 1; e < main.getWidgetCount (); e++ ) {
				row = ( ProductDeliveryCell ) main.getWidget ( e );
				if ( prod.equals ( row.getReferenceProduct ().getObject ( "product" ) ) == true ) {
					found = true;
					break;
				}
			}

			if ( found == false ) {
				row = new ProductDeliveryCell ();
				attachDeliveryCell ( row );
				main.add ( row );
			}

			row.addProductUser ( prod_user );

			price_total += prod_user.getTotalPrice ( prod_user.getFloat ( "delivered" ) );
		}

		main.add ( new HTML ( "<hr>" ) );
		bottomLine ( main, price_total );
	}

	public FromServer getValue () {
		ProductDeliveryCell row;

		for ( int i = 1; i < main.getWidgetCount () - 2; i++ ) {
			row = ( ProductDeliveryCell ) main.getWidget ( i );

			if ( row.previouslyExisting () ) {
				row.alignProducts ();
			}
			else {
				/*
					Qui entra quando il prodotto in oggetto
					e' stato aggiunto dinamicamente
					(funzione "Aggiungi Prodotto")
				*/
				FromServer prod;
				ArrayList products;

				prod = row.getDynamicValue ();
				products = object.getArray ( "products" );
				products.add ( prod );
				object.setArray ( "products", products );
			}
		}

		if ( payment != null )
			object.setObject ( "payment_event", payment.getValue () );

		return object;
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
