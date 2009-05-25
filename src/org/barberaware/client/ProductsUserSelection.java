/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProductsUserSelection extends FromServerArray {
	private FlexTable		main;
	private float			total;
	private PriceViewer		totalLabel;

	public ProductsUserSelection ( ArrayList products ) {
		int i;
		int a;
		int num_products;
		Product prod;

		main = new FlexTable ();
		main.setStyleName ( "products-selection" );
		initWidget ( main );
		main.setWidth ( "100%" );
		main.setCellSpacing ( 5 );

		i = 0;
		a = 0;

		if ( products != null ) {
			num_products = products.size ();

			for ( i = 0, a = 0; i < num_products; i++, a += 2 ) {
				prod = ( Product ) products.get ( i );
				addProductRow ( a, prod );
			}
		}

		addTotalRow ( a );
	}

	private void addTotalRow ( int index ) {
		main.setWidget ( index, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( index, 0, 2 );

		index++;

		totalLabel = new PriceViewer ();
		main.setWidget ( index, 0, new Label ( "Totale" ) );
		main.setWidget ( index, 1, totalLabel );
	}

	public void upgradeProductsList ( ArrayList products ) {
		int i;
		int a;
		int num_products;
		int original_rows;
		int rows;
		int index;
		ArrayList track_existing;
		Product prod;
		ProductUserSelector selector;

		num_products = products.size ();
		original_rows = main.getRowCount () - 2;

		for ( i = 0; i < num_products; i++ ) {
			prod = ( Product ) products.get ( i );
			rows = main.getRowCount ();

			for ( a = 0; a < rows; a += 2 ) {
				selector = ( ProductUserSelector ) main.getWidget ( a, 1 );
				if ( selector.getValue ().equals ( prod ) )
					break;
			}

			if ( a == rows )
				addProductRow ( rows, prod );
		}

		/**
			TODO	Il controllo sugli elementi rimossi puo' essere ampliamente
				migliorato...
		*/

		for ( a = 0; a < original_rows; a++ ) {
			selector = ( ProductUserSelector ) main.getWidget ( a, 1 );

			for ( i = 0; i < num_products; i++ ) {
				prod = ( Product ) products.get ( i );
				if ( selector.getValue ().equals ( prod ) )
					break;
			}

			if ( i == num_products ) {
				main.removeRow ( a );
				a--;
				original_rows--;
			}
		}
	}

	public float getTotalPrice () {
		return total;
	}

	private void addProductRow ( int index, Product product ) {
		String info_str;
		String plus_str;
		float plus;
		boolean raw;
		FlexTable.FlexCellFormatter formatter;
		Measure measure;
		ProductUserSelector sel;

		formatter = main.getFlexCellFormatter ();

		measure = ( Measure ) product.getObject ( "measure" );

		main.setWidget ( index, 0, new Label ( product.getString ( "name" ) ) );

		sel = new ProductUserSelector ( product );
		main.setWidget ( index, 1, sel );
		sel.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				updateTotal ();
			}
		} );

		/*
			Prezzo
		*/

		plus = product.getFloat ( "unit_price" );
		info_str = plus + " € / " + measure.getString ( "symbol" );

		/**
			TODO	Aggiungere una icona per indicare i prodotti il cui prezzo viene
				fissato alla consegna
		*/

		raw = product.getBool ( "mutable_price" );
		if ( raw == true )
			info_str += " (il prodotto viene misurato alla consegna)";

		plus = product.getFloat ( "shipping_price" );
		if ( plus != 0 )
			info_str += " + " + plus + " € trasporto";

		plus_str = product.getString ( "surplus" );
		if ( plus_str != null && plus_str != "" && plus_str != "0" )
			info_str += " + " + Utils.showPercentage ( plus_str ) + " surplus";

		main.setWidget ( index, 2, new Label ( info_str ) );

		formatter.setHorizontalAlignment ( index, 2, HasHorizontalAlignment.ALIGN_RIGHT );

		/*
			Informazioni aggiuntive
		*/

		info_str = product.getString ( "description" );
		main.setWidget ( index + 1, 0, new Label ( info_str ) );
		formatter.setColSpan ( index + 1, 0, 3 );
		formatter.setStyleName ( index + 1, 0, "description" );
	}

	private void updateTotal () {
		int rows;
		float price;
		ProductUserSelector selector;

		rows = main.getRowCount () - 2;
		price = 0;

		for ( int i = 0; i < rows; i += 2 ) {
			selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
			price += selector.getTotalPrice ();
		}

		total = price;
		totalLabel.setValue ( price );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int num_elements;
		int rows;
		ProductUser prod;
		Product prod_internal;
		String id_target;
		boolean found;
		ProductUserSelector selector;
		Product sel_prod;

		if ( elements == null ) {
			rows = main.getRowCount () - 2;

			for ( int i = 0; i < rows; i += 2 ) {
				selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
				selector.setQuantity ( 0 );
			}
		}

		else {
			num_elements = elements.size ();

			/**
				TODO	Qui un ordinamento degli array eviterebbe di fare tanti
					giri ed ottimizzare
			*/

			rows = main.getRowCount () - 2;

			for ( int i = 0; i < rows; i += 2 ) {
				selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
				sel_prod = ( Product ) selector.getValue ().getObject ( "product" );
				found = false;

				for ( int a = 0; a < num_elements; a++ ) {
					prod = ( ProductUser ) elements.get ( a );
					prod_internal = ( Product ) prod.getObject ( "product" );
					id_target = Integer.toString ( prod.getObject ( "product" ).getLocalID () );

					if ( sel_prod.equals ( prod_internal ) ) {
						selector.setValue ( prod );
						found = true;
						break;
					}
				}

				if ( found == false )
					selector.setQuantity ( 0 );
			}
		}

		updateTotal ();
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		int num_rows;
		ArrayList list;
		ProductUserSelector selector;
		Float quant;
		ProductUser prod;
		Hidden id;

		list = new ArrayList ();
		num_rows = main.getRowCount () - 2;

		for ( int i = 0; i < num_rows; i += 2 ) {
			selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
			prod = ( ProductUser ) selector.getValue ();

			if ( prod.getFloat ( "quantity" ) > 0 )
				list.add ( prod.duplicate () );
		}

		return list;
	}
}
