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

public class ProductDeliveryEditableVariantsCell extends Composite implements ProductDeliveryEditableCell {
	private FlexTable		main;

	private ArrayList		currentProducts;

	private ArrayList		changeCallbacks;

	public ProductDeliveryEditableVariantsCell () {
		main = new FlexTable ();
		initWidget ( main );

		currentProducts = new ArrayList ();
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( changeCallbacks, this );
	}

	/****************************************************************** ProductDeliveryEditableCell */

	public void addProductUser ( FromServer product ) {
		int variants_num;
		int rows_num;
		int row;
		boolean found;
		String id_variant;
		ArrayList variants;
		Hidden id;
		FloatBox box;
		ProductUserVariant variant;

		variants = product.getArray ( "variants" );
		variants_num = variants.size ();

		rows_num = main.getRowCount ();

		for ( int e = 0; e < variants_num; e++ ) {
			variant = ( ProductUserVariant ) variants.get ( e );
			id_variant = variant.getTextSummary ();
			found = false;

			for ( int i = 0; i < rows_num; i++ ) {
				id = ( Hidden ) main.getWidget ( i, 0 );

				if ( id.getName () == id_variant ) {
					box = ( FloatBox ) main.getWidget ( i, 0 );
					box.setVal ( box.getVal () + 1 );

					found = true;
					break;
				}
			}

			if ( found == false ) {
				row = main.getRowCount () - 1;
				main.insertRow ( row );
				rows_num++;

				main.setWidget ( row, 0, new Hidden ( id_variant ) );

				box = new FloatBox ();
				box.setVal ( 1 );

				box.addChangeListener ( new ChangeListener () {
					public void onChange ( Widget sender ) {
						triggerChange ();
					}
				} );

				main.setWidget ( row, 0, box );
			}
		}

		currentProducts.add ( product );
	}

	public void clear () {
		int rows_num;
		FloatBox box;

		rows_num = main.getRowCount ();

		for ( int i = 0; i < rows_num; i++ ) {
			box = ( FloatBox ) main.getWidget ( i, 1 );
			box.setVal ( 0 );
		}
	}

	public float getCurrentQuantity () {
		int rows_num;
		float total;
		FloatBox box;

		total = 0;
		rows_num = main.getRowCount ();

		for ( int i = 0; i < rows_num; i++ ) {
			box = ( FloatBox ) main.getWidget ( i, 1 );
			total += box.getVal ();
		}

		return total;
	}

	public float shipAll () {
		int num_variants;
		int rows;
		float total;
		String id_variant;
		ArrayList variants;
		Hidden id;
		FloatBox box;
		FromServer prod;
		ProductUserVariant variant;

		clear ();

		total = 0;
		rows = main.getRowCount ();

		for ( int i = 0; i < currentProducts.size (); i++ ) {
			prod = ( FromServer ) currentProducts.get ( i );
			variants = prod.getArray ( "variants" );
			num_variants = variants.size ();

			for ( int e = 0; e < num_variants; e++ ) {
				variant = ( ProductUserVariant ) variants.get ( e );
				id_variant = variant.getTextSummary ();

				for ( int a = 0; a < rows; a++ ) {
					id = ( Hidden ) main.getWidget ( a, 0 );

					if ( id.getName () == id_variant ) {
						box = ( FloatBox ) main.getWidget ( a, 1 );
						box.setVal ( box.getVal () + 1 );

						total += 1;
						break;
					}
				}
			}
		}

		return total;
	}

	public ArrayList getAlignedProducts () {
		/*
			Questo algoritmo potrebbe sembrare sbagliato considerando che itero tutte
			le varianti anziche' cercare quelle che sono state consegnate, ma c'e' da
			considerare che comunque me le dovrei iterare per azzerare tutti i
			"delivered" dunque tanto vale
		*/

		int rows;
		int num_variants;
		float [] values;
		String id_variant;
		ArrayList variants;
		FloatBox box;
		Hidden id;
		FromServer prod;
		ProductUserVariant variant;

		values = new float [ main.getRowCount () ];
		rows = main.getRowCount ();

		for ( int a = 0; a < rows; a++ ) {
			box = ( FloatBox ) main.getWidget ( a, 1 );
			values [ a ] = box.getVal ();
		}

		for ( int i = 0; i < currentProducts.size (); i++ ) {
			prod = ( FromServer ) currentProducts.get ( i );
			variants = prod.getArray ( "variants" );
			num_variants = variants.size ();

			for ( int e = 0; e < num_variants; e++ ) {
				variant = ( ProductUserVariant ) variants.get ( e );
				id_variant = variant.getTextSummary ();

				for ( int a = 0; a < rows; a++ ) {
					id = ( Hidden ) main.getWidget ( a, 0 );

					if ( id.getName () == id_variant ) {
						if ( values [ a ] > 0 ) {
							variant.setBool ( "delivered", true );
							values [ a ] = values [ a ] - 1;
						}
						else {
							variant.setBool ( "delivered", false );
						}

						break;
					}
				}
			}
		}

		return currentProducts;
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
