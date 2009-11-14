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
	private HorizontalPanel				main;
	private FloatBox				quantity;
	private Label					measure;
	private SuggestionBox				constraintsDialog;
	private ProductUser				currentValue;
	private DelegatingChangeListenerCollection	changeListeners;

	public ProductUserSelector ( Product prod ) {
		currentValue = new ProductUser ();
		currentValue.setObject ( "product", prod );

		constraintsDialog = null;

		main = new HorizontalPanel ();
		initWidget ( main );

		quantity = new FloatBox ();
		quantity.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( constraintsDialog != null )
					constraintsDialog.show ();
			}

			public void onLostFocus ( Widget sender ) {
				float val;
				float input;
				Product prod;

				if ( constraintsDialog != null )
					constraintsDialog.hide ();

				input = quantity.getVal ();
				if ( input == 0 )
					return;

				prod = ( Product ) currentValue.getObject ( "product" );

				val = prod.getFloat ( "minimum_order" );
				if ( val != 0 && input < val ) {
					Utils.showNotification ( "La quantità specificata è inferiore al minimo consentito" );
					undoChange ();
					return;
				}

				val = prod.getFloat ( "multiple_order" );
				if ( ( val != 0 ) && ( input % val ) != 0 ) {
					Utils.showNotification ( "La quantità specificata non è multipla del valore consentito" );
					undoChange ();
					return;
				}
			}
		} );
		main.add ( quantity );

		measure = new Label ();
		main.add ( measure );

		defineOnProduct ( prod );
	}

	private void disposeConstraints ( Product prod, Widget quantity ) {
		float min;
		float mult;
		String text;

		min = prod.getFloat ( "minimum_order" );
		mult = prod.getFloat ( "multiple_order" );

		if ( constraintsDialog != null )
			main.remove ( constraintsDialog );

		if ( min != 0 || mult != 0 ) {
			constraintsDialog = new SuggestionBox ();
			constraintsDialog.relativeTo ( quantity, SuggestionBox.ALIGN_RIGHT );
			text = "";

			if ( min != 0 )
				text = text + "Quantità minima ordinabile: " + Utils.floatToString ( min ) + " " + measure.getText ();

			if ( mult != 0 ) {
				if ( text != "" )
					text = text + "<br>";
				text = text + "Quantità ordinabile per multipli di: " + Utils.floatToString ( mult ) + " " + measure.getText ();
			}

			constraintsDialog.setHTML ( text );
			main.add ( constraintsDialog );
		}
	}

	private void undoChange () {
		quantity.setVal ( 0 );
		changeListeners.fireChange ( quantity );
	}

	private void defineOnProduct ( Product prod ) {
		Measure m;

		m = ( Measure ) prod.getObject ( "measure" );
		if ( m != null )
			measure.setText ( m.getString ( "symbol" ) );

		disposeConstraints ( prod, quantity );
	}

	public void setProduct ( Product prod ) {
		currentValue.setObject ( "product", prod );
		defineOnProduct ( prod );
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
		defineOnProduct ( prod );
	}

	public FromServer getValue () {
		currentValue.setFloat ( "quantity", quantity.getVal () );
		return currentValue;
	}
}
