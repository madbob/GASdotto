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

import com.allen_sauer.gwt.log.client.Log;

public class ProductsUserSelection extends Composite implements FromServerArray {
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

			for ( i = 0, a = 0; i < num_products; i++ ) {
				prod = ( Product ) products.get ( i );
				if ( prod.getBool ( "available" ) == false )
					continue;

				addProductRow ( prod );
				a += 2;
			}
		}

		addTotalRow ( a );
	}

	private void addTotalRow ( int index ) {
		main.setWidget ( index, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( index, 0, 2 );

		index++;

		totalLabel = new PriceViewer ();
		totalLabel.setStyleName ( "bigger-text" );
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
		boolean to_remove;
		ArrayList track_existing;
		Product prod;
		ProductUserSelector selector;

		num_products = products.size ();
		original_rows = main.getRowCount () - 2;

		for ( i = 0; i < num_products; i++ ) {
			prod = ( Product ) products.get ( i );

			if ( prod.getBool ( "available" ) == false )
				continue;

			rows = main.getRowCount () - 2;

			for ( a = 0; a < rows; a += 2 ) {
				selector = ( ProductUserSelector ) main.getWidget ( a, 1 );
				if ( selector.getValue ().getObject ( "product" ).equals ( prod ) ) {
					modProductRow ( a, prod );
					break;
				}
			}

			if ( a == rows )
				addProductRow ( prod );
		}

		/**
			TODO	Il controllo sugli elementi rimossi puo' essere ampliamente
				migliorato...
		*/

		for ( a = 0; a < original_rows; a += 2 ) {
			selector = ( ProductUserSelector ) main.getWidget ( a, 1 );
			to_remove = true;

			for ( i = 0; i < num_products; i++ ) {
				prod = ( Product ) products.get ( i );

				if ( selector.getValue ().getObject ( "product" ).equals ( prod ) ) {
					if ( prod.getBool ( "available" ) == true )
						to_remove = false;
					break;
				}
			}

			if ( to_remove ) {
				main.removeRow ( a );
				main.removeRow ( a + 1 );
				a = a - 2;
				original_rows--;
			}
		}
	}

	public float getTotalPrice () {
		return total;
	}

	private String getPriceInfo ( Product product ) {
		String info_str;
		String plus_str;
		float plus;
		boolean raw;
		Measure measure;

		measure = ( Measure ) product.getObject ( "measure" );

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

	private void addProductRow ( Product product ) {
		int row;
		String info_str;
		FlexTable.FlexCellFormatter formatter;
		ProductUserSelector sel;

		row = getProductAddingPosition ( product );
		main.insertRow ( row );

		formatter = main.getFlexCellFormatter ();

		main.setWidget ( row, 0, new Label ( product.getString ( "name" ) ) );
		formatter.setWidth ( row, 0, "20%" );

		sel = new ProductUserSelector ( product );
		main.setWidget ( row, 1, sel );
		formatter.setWidth ( row, 1, "20%" );
		sel.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				updateTotal ();
			}
		} );

		/*
			Prezzo
		*/

		info_str = getPriceInfo ( product );
		main.setWidget ( row, 2, new Label ( info_str ) );
		formatter.setWidth ( row, 2, "60%" );

		formatter.setHorizontalAlignment ( row, 2, HasHorizontalAlignment.ALIGN_RIGHT );

		/*
			Informazioni aggiuntive
		*/

		main.insertRow ( row + 1 );
		info_str = product.getString ( "description" );
		main.setWidget ( row + 1, 0, new Label ( info_str ) );
		formatter.setColSpan ( row + 1, 0, 3 );
		formatter.setStyleName ( row + 1, 0, "description" );
	}

	private int getProductAddingPosition ( Product prod ) {
		int i;
		int tot;
		String name;
		Label existing_name;

		name = prod.getString ( "name" );
		tot = main.getRowCount () - 2;

		for ( i = 0; i < tot; i += 2 ) {
			existing_name = ( Label ) main.getWidget ( i, 0 );
			if ( name.compareTo ( existing_name.getText () ) <= 0 )
				break;
		}

		return i;
	}

	private void modProductRow ( int index, Product product ) {
		String info_str;
		FlexTable.FlexCellFormatter formatter;
		Label lab;
		ProductUserSelector sel;

		formatter = main.getFlexCellFormatter ();

		lab = ( Label ) main.getWidget ( index, 0 );
		lab.setText ( product.getString ( "name" ) );

		sel = ( ProductUserSelector ) main.getWidget ( index, 1 );
		sel.setProduct ( product );

		/*
			Prezzo
		*/

		info_str = getPriceInfo ( product );
		lab = ( Label ) main.getWidget ( index, 2 );
		lab.setText ( info_str );

		/*
			Informazioni aggiuntive
		*/

		info_str = product.getString ( "description" );
		lab = ( Label ) main.getWidget ( index + 1, 0 );
		lab.setText ( info_str );
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
				if ( sel_prod.getBool ( "available" ) == false )
					continue;

				found = false;

				for ( int a = 0; a < num_elements; a++ ) {
					prod = ( ProductUser ) elements.get ( a );
					prod_internal = ( Product ) prod.getObject ( "product" );
					id_target = Integer.toString ( prod.getObject ( "product" ).getLocalID () );

					/**
						TODO	Sia qui che in getElements() duplico oggetti a tutto andare,
							per evitare sovrapposizioni; tutto dipende dal fatto che in
							OrderPrivilegedPanel uso sempre lo stesso form, inizializzato
							e reinizializzato un po' a casaccio, ci sarebbe da
							perfezionare quella parte ed evitare qui tante copie (di cui
							alla fine la stragrande maggioranza sono perse nel nulla)
					*/

					if ( sel_prod.equals ( prod_internal ) ) {
						selector.setValue ( prod.duplicate () );
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

			if ( prod.getFloat ( "quantity" ) > 0 ) {
				/*
					Onde evitare di impazzire troppo in OrderPrivilegedPanel,
					in cui lo stesso OrderUser viene riusato e sovrascritto,
					qui duplico gli elementi validi per evitare
					sovrapposizioni
				*/
				list.add ( prod.duplicate () );
			}
		}

		return list;
	}
}
