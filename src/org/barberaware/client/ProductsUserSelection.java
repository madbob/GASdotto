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

	public ProductsUserSelection ( ArrayList products ) {
		int num_products;
		Product prod;

		main = new FlexTable ();

		if ( products != null ) {
			num_products = products.size ();

			for ( int i = 0; i < num_products; i++ ) {
				prod = ( Product ) products.get ( i );
				addProductRow ( i, prod );
			}
		}

		initWidget ( main );
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
		original_rows = main.getRowCount ();

		for ( i = 0; i < num_products; i++ ) {
			prod = ( Product ) products.get ( i );
			rows = main.getRowCount ();

			for ( a = 0; a < rows; a++ ) {
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

	private void addProductRow ( int index, Product product ) {
		ProductUserSelector sel;

		main.setWidget ( index, 0, new Label ( product.getString ( "name" ) ) );

		sel = new ProductUserSelector ( product );
		main.setWidget ( index, 1, sel );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int num_elements;
		ProductUser prod;
		String id_target;
		ProductUserSelector selector;

		if ( elements == null ) {
			for ( int i = 0; i < main.getRowCount (); i++ ) {
				selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
				selector.setQuantity ( 0 );
			}
		}

		else {
			num_elements = elements.size ();

			for ( int a = 0; a < num_elements; a++ ) {
				prod = ( ProductUser ) elements.get ( a );
				id_target = Integer.toString ( prod.getObject ( "product" ).getLocalID () );

				for ( int i = 0; i < main.getRowCount (); i++ ) {
					selector = ( ProductUserSelector ) main.getWidget ( i, 1 );

					if ( selector.getValue ().equals ( prod ) ) {
						selector.setValue ( prod );
						break;
					}
				}
			}
		}
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
		num_rows = main.getRowCount ();

		for ( int i = 0; i < num_rows; i++ ) {
			selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
			prod = ( ProductUser ) selector.getValue ();

			if ( prod.getFloat ( "quantity" ) > 0 )
				list.add ( prod );
		}

		return list;
	}
}