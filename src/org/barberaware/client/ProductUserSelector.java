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

public class ProductUserSelector extends ObjectWidget {
	private FloatBox				quantity;
	private Label					measure;
	private ProductUser				currentValue;
	private DelegatingChangeListenerCollection	changeListeners;

	public ProductUserSelector ( Product prod ) {
		FocusPanel focusable;
		HorizontalPanel main;

		currentValue = new ProductUser ();
		currentValue.setObject ( "product", prod );

		focusable = new FocusPanel ();
		initWidget ( focusable );

		main = new HorizontalPanel ();
		focusable.add ( main );

		quantity = new FloatBox ();
		quantity.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				/* dummy */
			}

			public void onLostFocus ( Widget sender ) {
				float val;
				float input;
				Product prod;

				input = quantity.getVal ();
				if ( input == 0 )
					return;

				prod = ( Product ) currentValue.getObject ( "Product" );

				val = prod.getFloat ( "minimum_order" );
				if ( val != 0 && input < val ) {
					Utils.showNotification ( "La quantità specificata è inferiore al minimo consentito" );
					quantity.setVal ( 0 );
					return;
				}

				val = prod.getFloat ( "multiple_order" );
				if ( ( val != 0 ) && ( input % val ) != 0 ) {
					Utils.showNotification ( "La quantità specificata non è multipla del valore consentito" );
					quantity.setVal ( 0 );
					return;
				}
			}
		} );
		main.add ( quantity );

		measure = new Label ();
		main.add ( measure );

		setProduct ( prod );
	}

	private void undoChange () {
		quantity.setVal ( 0 );
		changeListeners.fireChange ( quantity );
	}

	private void setProduct ( Product prod ) {
		Measure m;

		m = ( Measure ) prod.getObject ( "measure" );
		if ( m != null )
			measure.setText ( m.getString ( "symbol" ) );
	}

	public void setQuantity ( float quant ) {
		quantity.setVal ( quant );
	}

	public float getQuantity () {
		return quantity.getVal ();
	}

	public float getTotalPrice () {
		float q;
		float tot;
		Product p;

		q = getQuantity ();

		if ( q == 0 )
			return 0;

		else {
			p = ( Product ) currentValue.getObject ( "product" );
			tot = q * p.getTotalPrice ();
			return tot;
		}
	}

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeListeners == null )
			changeListeners = new DelegatingChangeListenerCollection ( this, quantity );
		changeListeners.add ( listener );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		Product prod;

		currentValue = ( ProductUser ) element;

		quantity.setVal ( element.getFloat ( "quantity" ) );

		prod = ( Product ) element.getObject ( "product" );
		setProduct ( prod );
	}

	public FromServer getValue () {
		currentValue.setFloat ( "quantity", quantity.getVal () );
		return currentValue;
	}
}
