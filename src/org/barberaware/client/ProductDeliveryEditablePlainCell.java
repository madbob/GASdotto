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

public class ProductDeliveryEditablePlainCell extends Composite implements ProductDeliveryEditableCell {
	private FloatBox		box;
	private ArrayList		currentProducts;

	public ProductDeliveryEditablePlainCell () {
		box = new FloatBox ();
		initWidget ( box );

		box.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				triggerChange ();
			}
		} );

		currentProducts = new ArrayList ();
	}

	private void triggerChange () {
		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
	}

	/****************************************************************** ProductDeliveryEditableCell */

	public void addProductUser ( FromServer product ) {
		currentProducts.add ( product );
		box.setVal ( box.getVal () + product.getFloat ( "delivered" ) );
	}

	public void clear () {
		box.setVal ( 0 );
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
		return total;
	}

	public void alignProducts () {
		float total;
		float quantity;
		FromServer prod;

		total = getCurrentQuantity ();

		for ( int i = 0; i < currentProducts.size (); i++ ) {
			prod = ( FromServer ) currentProducts.get ( i );
			quantity = prod.getFloat ( "quantity" );

			if ( total > quantity ) {
				prod.setFloat ( "delivered", quantity );
				total -= quantity;
			}
			else {
				prod.setFloat ( "delivered", total );
				total = 0;

				/*
					Quando finisco le quantita' assegnabili, non esco dal
					ciclo apposta per settare a 0 tutti gli altri valori di
					"delivered" e non rischiare di lasciare quantita' sporche
				*/
			}
		}

		if ( total > 0 ) {
			prod = ( FromServer ) currentProducts.get ( 0 );
			prod.setFloat ( "delivered", prod.getFloat ( "quantity" ) + total );
		}
	}
}
