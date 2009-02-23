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
		HorizontalPanel main;

		main = new HorizontalPanel ();
		initWidget ( main );

		quantity = new FloatBox ();
		main.add ( quantity );
		measure = new Label ();
		main.add ( measure );

		currentValue = new ProductUser ();
		currentValue.setObject ( "product", prod );

		setProduct ( prod );
	}

	private void setProduct ( Product prod ) {
		Measure m;

		m = ( Measure ) prod.getObject ( "measure" );
		if ( m != null )
			measure.setText ( m.getString ( "symbol" ) );

		/**
			TODO	Aggiungere indicazioni su quantita' minime e multiple
		*/
	}

	public void setQuantity ( float quant ) {
		quantity.setValue ( quant );
	}

	public float getQuantity () {
		return quantity.getValue ();
	}

	public float getTotalPrice () {
		float q;
		float price;
		float tot;
		Product p;

		q = getQuantity ();

		if ( q == 0 )
			return 0;

		else {
			p = ( Product ) currentValue.getObject ( "product" );
			tot = q * p.getFloat ( "unit_price" );

			price = p.getFloat ( "shipping_price" );
			if ( price != 0 )
				tot += q * price;

			/**
				TODO	Gestire anche il surplus, che e' una percentuale
			*/

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

		quantity.setValue ( element.getFloat ( "quantity" ) );

		prod = ( Product ) element.getObject ( "product" );
		setProduct ( prod );
	}

	public FromServer getValue () {
		currentValue.setFloat ( "quantity", quantity.getValue () );
		return currentValue;
	}
}
