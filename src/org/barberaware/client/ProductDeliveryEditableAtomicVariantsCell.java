/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class ProductDeliveryEditableAtomicVariantsCell extends Composite implements ProductDeliveryEditableCell {
	private FlexTable		main;

	private ArrayList		currentProducts;

	private ArrayList		changeCallbacks;

	public ProductDeliveryEditableAtomicVariantsCell () {
		main = new FlexTable ();
		initWidget ( main );

		currentProducts = new ArrayList ();
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( changeCallbacks, this );
	}

	private int createBox ( int rows_num, String id_variant, boolean delivered, float value ) {
		FloatBox box;

		main.insertRow ( rows_num );
		main.setWidget ( rows_num, 0, new Hidden ( id_variant ) );

		box = new FloatBox ();

		if ( delivered == true )
			box.setVal ( value );

		box.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				triggerChange ();
			}
		} );

		main.setWidget ( rows_num, 1, box );
		return rows_num + 1;
	}

	/****************************************************************** ProductDeliveryEditableCell */

	public void addProductUser ( FromServer product ) {
		int rows_num;
		int row;
		boolean found;
		boolean delivered;
		String id_variant;
		ArrayList variants;
		Hidden id;
		FloatBox box;
		ProductUserVariant variant;

		variants = product.getArray ( "variants" );
		if ( variants == null || variants.size () == 0 )
			return;

		rows_num = main.getRowCount ();

		variant = ( ProductUserVariant ) variants.get ( 0 );
		id_variant = variant.getTextSummary ();
		delivered = variant.getBool ( "delivered" );
		found = false;

		for ( int i = 0; i < rows_num; i++ ) {
			id = ( Hidden ) main.getWidget ( i, 0 );

			if ( id.getName () == id_variant ) {
				if ( delivered == true ) {
					box = ( FloatBox ) main.getWidget ( i, 1 );
					box.setVal ( box.getVal () + product.getFloat ( "delivered" ) );
				}

				found = true;
				break;
			}
		}

		if ( found == false )
			createBox ( rows_num, id_variant, delivered, product.getFloat ( "delivered" ) );

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

			variant = ( ProductUserVariant ) variants.get ( 0 );
			id_variant = variant.getTextSummary ();

			for ( int a = 0; a < rows; a++ ) {
				id = ( Hidden ) main.getWidget ( a, 0 );

				if ( id.getName () == id_variant ) {
					box = ( FloatBox ) main.getWidget ( a, 1 );
					box.setVal ( box.getVal () + prod.getFloat ( "quantity" ) );
					total += prod.getFloat ( "quantity" );
					break;
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
		float total;
		String id_variant;
		ArrayList variants;
		ArrayList more_variants;
		FloatBox box;
		Hidden id;
		FromServer prod;
		FromServer dup;
		ProductUserVariant variant;

		values = new float [ main.getRowCount () ];
		rows = main.getRowCount ();

		for ( int a = 0; a < rows; a++ ) {
			box = ( FloatBox ) main.getWidget ( a, 1 );
			values [ a ] = box.getVal ();
		}

		for ( int i = 0; i < currentProducts.size (); i++ ) {
			total = 0;
			prod = ( FromServer ) currentProducts.get ( i );
			variants = prod.getArray ( "variants" );
			variant = ( ProductUserVariant ) variants.get ( 0 );
			id_variant = variant.getTextSummary ();

			for ( int a = 0; a < rows; a++ ) {
				id = ( Hidden ) main.getWidget ( a, 0 );

				if ( id.getName () == id_variant ) {
					if ( values [ a ] > 0 ) {
						variant.setBool ( "delivered", true );
						box = ( FloatBox ) main.getWidget ( a, 1 );
						total = box.getVal ();

						if ( total > prod.getFloat ( "quantity" ) )
							total = prod.getFloat ( "quantity" );

						values [ a ] = values [ a ] - total;
					}
					else {
						variant.setBool ( "delivered", false );
					}

					break;
				}
			}

			prod.setFloat ( "delivered", total );
		}

		/*
			Nel caso in cui vengano consegnate un numero di varianti
			superiore a quelle ordinate, quelle in piu' devono essere
			aggiunte al primo ProductUser
		*/

		for ( int e = 0; e < main.getRowCount (); e++ ) {
			total = 0;

			if ( values [ e ] != 0 ) {
				id = ( Hidden ) main.getWidget ( e, 0 );

				for ( int i = 0; i < currentProducts.size (); i++ ) {
					prod = ( FromServer ) currentProducts.get ( i );
					variants = prod.getArray ( "variants" );
					variant = ( ProductUserVariant ) variants.get ( 0 );
					id_variant = variant.getTextSummary ();
					box = ( FloatBox ) main.getWidget ( e, 1 );

					if ( id.getName () == id_variant ) {
						prod.setFloat ( "delivered", prod.getFloat ( "delivered" ) + values [ e ] );
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
