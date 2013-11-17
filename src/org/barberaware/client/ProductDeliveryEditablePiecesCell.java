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

public class ProductDeliveryEditablePiecesCell extends Composite implements ProductDeliveryEditableCell {
	private FloatBox		box;
	private ScreenCalculator	calculator;

	private ArrayList		currentProducts;

	public ProductDeliveryEditablePiecesCell () {
		box = new FloatBox ();
		box.addStyleName ( "calculator-addicted" );
		initWidget ( box );

		currentProducts = new ArrayList ();

		calculator = new ScreenCalculator ();
		calculator.setTarget ( box );

		box.addFocusHandler ( new FocusHandler () {
			public void onFocus ( FocusEvent event ) {
				calculator.center ();
				calculator.show ();
			}
		} );

		calculator.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				triggerChange ();
			}
		} );
	}

	private void triggerChange () {
		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
	}

	/****************************************************************** ProductDeliveryEditableCell */

	public void addProductUser ( FromServer product ) {
		int pieces;
		float unit;

		currentProducts.add ( product );
		box.setVal ( box.getVal () + product.getFloat ( "delivered" ) );

		unit = product.getObject ( "product" ).getFloat ( "unit_size" );
		pieces = Math.round ( product.getFloat ( "quantity" ) / unit );
		calculator.addCells ( pieces );
	}

	public void clear () {
		box.setVal ( 0 );
		calculator.clear ();
	}

	public float getCurrentQuantity () {
		return box.getVal ();
	}

	public float shipAll () {
		float total;
		FromServer prod;

		total = 0;

		for ( int i = 0; i < currentProducts.size (); i++ ) {
			prod = ( FromServer ) currentProducts.get ( i );
			total += prod.getFloat ( "quantity" );
		}

		box.setVal ( total );

		prod = ( FromServer ) currentProducts.get ( 0 );
		calculator.setValue ( prod.getObject ( "product" ).getFloat ( "unit_size" ) );

		return total;
	}

	public void alignProducts () {
		int pieces;
		float del;
		float unit;
		float [] delivered;
		FromServer prod;

		if ( calculator.hasBeenUsed () == true ) {
			delivered = calculator.getValues ();

			prod = ( FromServer ) currentProducts.get ( 0 );
			unit = prod.getObject ( "product" ).getFloat ( "unit_size" );

			for ( int i = 0, e = 0; i < currentProducts.size (); i++ ) {
				prod = ( FromServer ) currentProducts.get ( i );
				pieces = Math.round ( prod.getFloat ( "quantity" ) / unit );

				del = 0;

				for ( int a = 0; a < pieces; a++, e++ )
					del += delivered [ e ];

				prod.setFloat ( "delivered", del );
			}
		}
	}
}
