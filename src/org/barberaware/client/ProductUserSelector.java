/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import java.lang.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class ProductUserSelector extends ObjectWidget {
	private HorizontalPanel				main;
	private boolean					editable;
	private boolean					freeEditable;
	private FloatWidget				quantity;
	private Label					measure;
	private Label					effectiveQuantity;
	private SuggestionBox				constraintsDialog;
	private ProductUser				currentValue;
	private DelegatingChangeListenerCollection	changeListeners;

	public ProductUserSelector ( Product prod, boolean edit, boolean freeedit ) {
		FloatBox qb;
		FloatViewer qv;

		currentValue = new ProductUser ();
		currentValue.setObject ( "product", prod );

		editable = edit;
		freeEditable = freeedit;
		constraintsDialog = null;

		main = new HorizontalPanel ();
		initWidget ( main );

		if ( edit == true ) {
			qb = new FloatBox ();
			qb.addFocusListener ( new FocusListener () {
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

					if ( freeEditable == false ) {
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

					val = prod.getFloat ( "unit_size" );
					if ( val != 0 )
						setEffectiveQuantity ( input );
				}
			} );

			main.add ( qb );
			quantity = qb;
		}
		else {
			qv = new FloatViewer ();
			main.add ( qv );
			quantity = qv;
		}

		measure = new Label ();
		measure.addStyleName ( "contents-on-right" );
		main.add ( measure );

		effectiveQuantity = new Label ();
		effectiveQuantity.addStyleName ( "contents-on-right" );
		effectiveQuantity.setVisible ( false );
		main.add ( effectiveQuantity );

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
		if ( editable == true ) {
			quantity.setVal ( 0 );
			changeListeners.fireChange ( ( FloatBox ) quantity );
		}
	}

	private void defineOnProduct ( Product prod ) {
		Measure m;

		if ( prod.getFloat ( "unit_size" ) != 0 ) {
			measure.setText ( "pezzi" );
			setEffectiveQuantity ( quantity.getVal () );
		}
		else {
			m = ( Measure ) prod.getObject ( "measure" );
			if ( m != null )
				measure.setText ( m.getString ( "symbol" ) );

			effectiveQuantity.setVisible ( false );
		}

		if ( editable == true )
			disposeConstraints ( prod, ( FloatBox ) quantity );
	}

	private void setEffectiveQuantity ( float quantity ) {
		String ms;
		Product prod;
		Measure m;

		prod = ( Product ) currentValue.getObject ( "product" );
		m = ( Measure ) prod.getObject ( "measure" );
		if ( m != null )
			ms = m.getString ( "symbol" );
		else
			ms = "";

		effectiveQuantity.setVisible ( true );
		effectiveQuantity.setText ( "( " + ( quantity * prod.getFloat ( "unit_size" ) ) + " " + ms + " )" );
	}

	public void setProduct ( Product prod ) {
		currentValue.setObject ( "product", prod );
		defineOnProduct ( prod );
	}

	public void clear () {
		float unit;

		quantity.setVal ( 0 );

		/*
			Ennesima misura per evitare sovrascrittura dei dati: ogni volta l'ID del
			ProductUser locale viene messo a -1, per evitare che sia rimasto qualcosa
			di vecchio e quando si va a salvare l'elemento si vada in realta' a
			sovrascrivere l'ID precedentemente appeso
		*/
		currentValue.setLocalID ( -1 );

		unit = currentValue.getObject ( "product" ).getFloat ( "unit_size" );
		if ( unit != 0 )
			setEffectiveQuantity ( 0 );
	}

	public float getTotalPrice () {
		ProductUser current;

		current = ( ProductUser ) getValue ();
		return current.getTotalPrice ();
	}

	public void addChangeListener ( ChangeListener listener ) {
		if ( editable == true ) {
			if ( changeListeners == null )
				changeListeners = new DelegatingChangeListenerCollection ( this, ( FloatBox ) quantity );
			changeListeners.add ( listener );
		}
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		float q;
		float unit;
		Product prod;

		currentValue = ( ProductUser ) element;

		q = element.getFloat ( "quantity" );

		unit = currentValue.getObject ( "product" ).getFloat ( "unit_size" );
		if ( unit != 0 )
			q = Math.round ( q / unit );

		quantity.setVal ( q );
		prod = ( Product ) element.getObject ( "product" );
		defineOnProduct ( prod );
	}

	public FromServer getValue () {
		float q;
		float unit;

		q = quantity.getVal ();

		unit = currentValue.getObject ( "product" ).getFloat ( "unit_size" );
		if ( unit != 0 )
			q = q * unit;

		currentValue.setFloat ( "quantity", q );
		return currentValue;
	}
}
