/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProductsDeliveryTable extends Composite implements FromServerArray {
	private FlexTable		main;
	private float			total;
	private PriceViewer		totalLabel;
	private ArrayList		currentValues;

	public ProductsDeliveryTable () {
		HTMLTable.RowFormatter formatter;

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		initWidget ( main );

		main.setWidget ( 0, 1, new Label ( "Prodotto" ) );
		main.setWidget ( 0, 2, new Label ( "Quantità Ordinata" ) );
		main.setWidget ( 0, 3, new Label ( "Quantità Consegnata" ) );
		main.setWidget ( 0, 4, new Label ( "Prezzo Totale" ) );

		formatter = main.getRowFormatter ();
		formatter.addStyleName ( 0, "table-header" );
	}

	private void newInputToCheck ( FloatBox box ) {
		int num_rows;
		float input;
		float row_sum;
		float total_sum;
		FloatBox iter;
		ProductUser prod_user;
		FromServer prod;

		total_sum = 0;
		num_rows = currentValues.size ();

		for ( int a = 0, i = 1; a < num_rows; a++, i++ ) {
			iter = ( FloatBox ) main.getWidget ( i, 3 );
			input = iter.getVal ();

			prod_user = ( ProductUser ) currentValues.get ( a );
			row_sum = prod_user.getTotalPrice ( input );

			prod = prod_user.getObject ( "product" );

			if ( iter == box ) {
				if ( input < 0 ) {
					Utils.showNotification ( "Il valore immesso non è valido" );
					box.setVal ( 0 );
				}
				else {
					Label total_label;

					/*
						Se la quantita' immessa e' diversa da quella ordinata si limita a
						mostrare una notifica, ma non blocca l'operazione in quanto puo'
						succedere che avanzi qualche prodotto (ordinato per arrotondamento) e
						si distribuisca arbitrariamente
					*/
					if ( prod.getBool ( "mutable_price" ) == false && input > prod_user.getFloat ( "quantity" ) )
						Utils.showNotification ( "Hai immesso una quantità diversa da quella ordinata",
										SmoothingNotify.NOTIFY_INFO );

					total_label = ( Label ) main.getWidget ( i, 4 );
					total_label.setText ( Utils.priceToString ( row_sum ) );
				}
			}

			total_sum = row_sum + total_sum;
		}

		totalLabel.setVal ( total_sum );
	}

	private Button createAutoCompleteButton () {
		Button ret;

		ret = new Button ( "Consegna Tutto" );
		ret.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				int num_rows;
				float input;
				float row_sum;
				float total_sum;
				Label total_label;
				FloatBox iter;
				ProductUser prod_user;

				total_sum = 0;
				num_rows = currentValues.size ();

				for ( int a = 0, i = 1; a < num_rows; a++, i++ ) {
					iter = ( FloatBox ) main.getWidget ( i, 3 );
					prod_user = ( ProductUser ) currentValues.get ( a );

					input = prod_user.getFloat ( "quantity" );

					row_sum = prod_user.getTotalPrice ( input );
					total_label = ( Label ) main.getWidget ( i, 4 );
					total_label.setText ( Utils.priceToString ( row_sum ) );

					iter.setVal ( input );
					total_sum = row_sum + total_sum;
				}

				totalLabel.setVal ( total_sum );
			}
		} );

		return ret;
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int i;
		int e;
		ProductUser prod_user;
		FromServer prod;
		FromServer measure;
		FloatBox del;
		float delivered;
		float price_product;
		float price_total;

		currentValues = elements;
		price_total = 0;

		for ( i = 0; i < main.getRowCount () - 1; i++ )
			main.removeRow ( 1 );

		/*
			Questo e' per i casi in cui la lista dei prodotti viene aggiornata
			(magari perche' e' cambiato l'ordine) ed il colspan della cella in cui si
			trovava la riga che separa il totale va invalidato (per poi essere
			ri-settato quando si ri-piazza tale riga)
		*/
		main.getFlexCellFormatter ().setColSpan ( 1, 0, 1 );

		for ( e = 1, i = 0; i < elements.size (); i++ ) {
			prod_user = ( ProductUser ) elements.get ( i );

			prod = prod_user.getObject ( "product" );
			if ( prod.getBool ( "available" ) == false )
				continue;

			measure = prod.getObject ( "measure" );

			main.setWidget ( e, 0, new Hidden ( "id", Integer.toString ( prod.getLocalID () ) ) );
			main.setWidget ( e, 1, new Label ( prod.getString ( "name" ) ) );
			main.setWidget ( e, 2, new Label ( Utils.floatToString ( prod_user.getFloat ( "quantity" ) ) + " " + measure.getString ( "symbol" ) ) );

			del = new FloatBox ();
			delivered = prod_user.getFloat ( "delivered" );
			del.setVal ( delivered );
			main.setWidget ( e, 3, del );

			del.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
					/* dummy */
				}

				public void onLostFocus ( Widget sender ) {
					newInputToCheck ( ( FloatBox ) sender );
				}
			} );

			price_product = prod_user.getTotalPrice ( delivered );
			main.setWidget ( e, 4, new Label ( Utils.priceToString ( price_product ) ) );
			price_total = price_total + price_product;

			e++;
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( e, 0, 5 );

		e++;

		main.setWidget ( e, 3, createAutoCompleteButton () );

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalLabel.setStyleName ( "bigger-text" );
		}

		totalLabel.setVal ( price_total );
		main.setWidget ( e, 4, totalLabel );
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		FloatBox del;
		FromServer produser;

		for ( int i = 0; i < currentValues.size (); i++ ) {
			del = ( FloatBox ) main.getWidget ( i + 1, 3 );
			produser = ( FromServer ) currentValues.get ( i );
			produser.setFloat ( "delivered", del.getVal () );
		}

		return currentValues;
	}

	public void refreshElement ( FromServer element ) {
		/* dummy */
	}
}
