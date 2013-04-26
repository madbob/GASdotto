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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class ProductsPlainList extends Composite implements FromServerArray {
	private FlexTable		main;

	/*
		Il dialog per le descrizioni dei prodotti viene creato la
		prima volta che viene richiesto
	*/
	private DialogBox		descDialog			= null;
	private Label			descDialogText;

	private ArrayList		products			= null;

	public ProductsPlainList () {
		main = new FlexTable ();
		main.setStyleName ( "products-selection" );
		initWidget ( main );
		main.setWidth ( "100%" );
		main.setCellSpacing ( 5 );

		main.addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				if ( products == null )
					return;

				showProductDescriptionDialog ( ( FromServer ) products.get ( row ) );
			}
		} );
	}

	private void initDescriptionDialog () {
		VerticalPanel pan;
		Button but;

		descDialog = new DialogBox ();

		pan = new VerticalPanel ();
		pan.setWidth ( "400px" );
		descDialog.setWidget ( pan );

		descDialogText = new Label ();
		pan.add ( descDialogText );

		but = new Button ( "OK", new ClickListener () {
			public void onClick ( Widget sender ) {
				descDialog.hide ();
			}
		} );
		pan.add ( but );
	}

	private void showProductDescriptionDialog ( FromServer product ) {
		String desc;

		if ( descDialog == null )
			initDescriptionDialog ();

		descDialog.setText ( product.getString ( "name" ) );
		desc = product.getString ( "description" );
		descDialogText.setText ( desc != "" ? desc : "Nessuna descrizione" );

		descDialog.center ();
		descDialog.show ();
	}

	private int retrieveRowByProduct ( FromServer prod ) {
		int rows;
		FromServer p;

		rows = products.size ();

		for ( int a = 0; a < rows; a++ ) {
			p = ( FromServer ) products.get ( a );

			if ( p.equals ( prod ) )
				return a;
		}

		return -1;
	}

	private String getPriceInfo ( Product product ) {
		String info_str;
		String plus_str;
		float plus;
		boolean raw;
		FromServer measure;

		measure = product.getObject ( "measure" );

		plus = product.getFloat ( "unit_price" );
		info_str = plus + " € / " + measure.getString ( "symbol" );

		/**
			TODO	Aggiungere una icona per indicare i prodotti il cui prezzo viene
				fissato alla consegna
		*/

		raw = product.getBool ( "mutable_price" );
		if ( raw == true )
			info_str += " (prodotto misurato alla consegna)";

		plus = product.getFloat ( "shipping_price" );
		if ( plus != 0 )
			info_str += " + " + plus + " € trasporto";

		plus_str = product.getString ( "surplus" );
		if ( plus_str != null && plus_str != "" && plus_str != "0" )
			info_str += " + " + Utils.showPercentage ( plus_str ) + " surplus";

		return info_str;
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int num_elements;
		Product prod;

		num_elements = main.getRowCount ();
		for ( int i = 0; i < num_elements; i++ )
			main.removeRow ( 0 );

		if ( elements == null )
			return;

		num_elements = elements.size ();

		for ( int i = 0; i < num_elements; i++ ) {
			prod = ( Product ) elements.get ( i );
			main.setWidget ( i, 0, new Hidden ( Integer.toString ( prod.getLocalID () ) ) );
			main.setWidget ( i, 1, new Label ( prod.getString ( "name" ) ) );
			main.setWidget ( i, 2, new Label ( getPriceInfo ( prod ) ) );
		}

		products = elements;
	}

	public void removeElement ( FromServer element ) {
		int a;

		a = retrieveRowByProduct ( element );
		if ( a != -1 ) {
			main.removeRow ( a );
			products.remove ( a );
		}
	}

	public ArrayList getElements () {
		return products;
	}

	public void refreshElement ( FromServer element ) {
		int a;

		a = retrieveRowByProduct ( element );
		if ( a != -1 ) {
			main.setWidget ( a, 1, new Label ( element.getString ( "name" ) ) );
			main.setWidget ( a, 2, new Label ( getPriceInfo ( ( Product ) element ) ) );
		}
	}
}
