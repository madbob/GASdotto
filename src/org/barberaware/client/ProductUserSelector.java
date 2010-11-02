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

import com.allen_sauer.gwt.log.client.Log;

/**
	TODO	Implementare la box per la gestione delle varianti in un widget dedicato (e
		magari meglio formattato)
*/

public class ProductUserSelector extends Composite implements ObjectWidget {
	private boolean					editable;
	private boolean					freeEditable;

	private VerticalPanel				main;
	private HorizontalPanel				firstRow;
	private VerticalPanel				variantsBoxes;

	private FloatWidget				quantity;
	private Label					measure;
	private Label					effectiveQuantity;
	private SuggestionBox				constraintsDialog;
	private FromServer				currentValue;

	private DelegatingChangeListenerCollection	changeListeners;

	public ProductUserSelector ( Product prod, boolean edit, boolean freeedit ) {
		FloatBox qb;
		FloatViewer qv;

		currentValue = new ProductUser ();
		currentValue.setObject ( "product", prod );

		main = new VerticalPanel ();
		initWidget ( main );

		firstRow = new HorizontalPanel ();
		main.add ( firstRow );

		variantsBoxes = null;

		editable = edit;
		freeEditable = freeedit;
		constraintsDialog = null;

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
					FromServer prod;
					Supplier supp;

					if ( constraintsDialog != null )
						constraintsDialog.hide ();

					input = quantity.getVal ();
					if ( input == 0 ) {
						setEffectiveQuantity ( input );
						return;
					}

					prod = currentValue.getObject ( "product" );

					if ( freeEditable == false ) {
						val = prod.getFloat ( "minimum_order" );
						if ( val != 0 && input < val ) {
							Utils.showNotification ( "La quantità specificata è inferiore al minimo consentito" );
							undoChange ();
							return;
						}

						supp = ( Supplier ) prod.getObject ( "supplier" );

						if ( supp.iAmReference () == false ) {
							val = prod.getFloat ( "multiple_order" );
							if ( ( val != 0 ) && ( input % val ) != 0 ) {
								Utils.showNotification ( "La quantità specificata non è multipla del valore consentito" );
								undoChange ();
								return;
							}
						}
					}

					val = prod.getFloat ( "unit_size" );
					setEffectiveQuantity ( input );
				}
			} );

			firstRow.add ( qb );
			quantity = qb;
		}
		else {
			qv = new FloatViewer ();
			firstRow.add ( qv );
			quantity = qv;
		}

		measure = new Label ();
		measure.addStyleName ( "contents-on-right" );
		firstRow.add ( measure );

		effectiveQuantity = new Label ();
		effectiveQuantity.addStyleName ( "contents-on-right" );
		effectiveQuantity.setVisible ( false );
		firstRow.add ( effectiveQuantity );

		defineOnProduct ( prod );
	}

	private void disposeConstraints ( FromServer prod, Widget quantity ) {
		float min;
		float mult;
		String text;

		min = prod.getFloat ( "minimum_order" );
		mult = prod.getFloat ( "multiple_order" );

		if ( constraintsDialog != null )
			firstRow.remove ( constraintsDialog );

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
			firstRow.add ( constraintsDialog );
		}
	}

	private void undoChange () {
		if ( editable == true ) {
			quantity.setVal ( 0 );
			changeListeners.fireChange ( ( FloatBox ) quantity );
		}
	}

	private void hideVariants () {
		int num;

		if ( variantsBoxes == null )
			return;

		num = variantsBoxes.getWidgetCount ();

		for ( int i = 0; i < num; i++ )
			variantsBoxes.remove ( 0 );

		variantsBoxes.setVisible ( false );
	}

	private Widget doVariantRow ( ArrayList variants, FromServer current ) {
		int selected_index;
		ArrayList values;
		ArrayList current_components;
		HorizontalPanel ret;
		ListBox sel;
		ProductVariant var;
		ProductVariantValue val;
		ProductUserVariantComponent component;
		ProductUserVariantComponent current_component;

		ret = new HorizontalPanel ();

		if ( current != null ) {
			ret.add ( new Hidden ( Integer.toString ( current.getLocalID () ) ) );
			current_components = current.getArray ( "components" );
		}
		else {
			ret.add ( new Hidden ( "-1" ) );
			current_components = null;
		}

		for ( int i = 0; i < variants.size (); i++ ) {
			var = ( ProductVariant ) variants.get ( i );

			ret.add ( new Hidden ( Integer.toString ( var.getLocalID () ) ) );

			current_component = null;

			if ( current_components != null )
				for ( int a = 0; a < current_components.size (); a++ ) {
					component = ( ProductUserVariantComponent ) current_components.get ( a );
					if ( component.getObject ( "variant" ).getLocalID () == var.getLocalID () ) {
						current_component = component;
						break;
					}
				}

			ret.add ( new Label ( var.getString ( "name" ) + ": " ) );
			values = var.getArray ( "values" );

			sel = new ListBox ();
			ret.add ( sel );
			selected_index = 0;

			for ( int a = 0; a < values.size (); a++ ) {
				val = ( ProductVariantValue ) values.get ( a );
				sel.addItem ( val.getString ( "name" ), Integer.toString ( val.getLocalID () ) );

				if ( current_component != null )
					if ( current_component.getObject ( "value" ).getLocalID () == val.getLocalID () )
						selected_index = a;
			}

			sel.setSelectedIndex ( selected_index );
		}

		return ret;
	}

	private void doVariantsMainBox () {
		if ( variantsBoxes == null ) {
			variantsBoxes = new VerticalPanel ();
			main.add ( variantsBoxes );
		}

		variantsBoxes.setVisible ( true );
	}

	private void defineOnProduct ( FromServer prod ) {
		float unit;
		ArrayList variants;
		FromServer m;

		unit = prod.getFloat ( "unit_size" );
		m = prod.getObject ( "measure" );

		if ( unit != 0 ) {
			if ( m != null )
				measure.setText ( "pezzi da " + unit + m.getString ( "symbol" ) );
			else
				measure.setText ( "pezzi" );

			setEffectiveQuantity ( quantity.getVal () );
		}
		else {
			if ( m != null )
				measure.setText ( m.getString ( "name" ) );

			effectiveQuantity.setVisible ( false );
		}

		if ( editable == true ) {
			disposeConstraints ( prod, ( FloatBox ) quantity );

			variants = prod.getArray ( "variants" );

			if ( variants != null && variants.size () != 0 ) {
				( ( FloatBox ) quantity ).addChangeListener ( new ChangeListener () {
					public void onChange ( Widget sender ) {
						alignVariants ( currentValue, ( int ) quantity.getVal (), null );
					}
				} );
			}
			else {
				hideVariants ();
			}
		}
	}

	/*
		Questa funzione viene usata in due casi totalmente diversi: quando devo riempire le caselle per un
		prodotto che gia' ha delle varianti (durante l'inizializzazione della visualizzazione di un ordine
		completo), oppure quando viene immessa una quantita' ordinata e devo predisporre le caselle per le
		nuove varianti.
		Dunque:
		- se variants non e' null, num equivale alla sua grandezza
		- se variants e' null, num e' la quantita' immessa nella apposita casella
	*/
	private void alignVariants ( FromServer productuser, int num, ArrayList variants ) {
		int i;
		ArrayList v;

		if ( num == 0 ) {
			hideVariants ();
			return;
		}

		v = productuser.getObject ( "product" ).getArray ( "variants" );

		if ( v != null && v.size () != 0 ) {
			doVariantsMainBox ();

			for ( i = variantsBoxes.getWidgetCount (); i < num; i++ ) {
				if ( variants == null )
					variantsBoxes.add ( doVariantRow ( v, null ) );
				else
					variantsBoxes.add ( doVariantRow ( v, ( FromServer ) variants.get ( i ) ) );
			}

			if ( i > num )
				while ( variantsBoxes.getWidgetCount () != num )
					variantsBoxes.remove ( variantsBoxes.getWidgetCount () - 1 );
		}
		else {
			hideVariants ();
		}
	}

	private void setEffectiveQuantity ( float quantity ) {
		String ms;
		FromServer prod;
		FromServer m;

		prod = currentValue.getObject ( "product" );
		m = prod.getObject ( "measure" );
		if ( m != null )
			ms = " " + m.getString ( "name" );
		else
			ms = "";

		effectiveQuantity.setVisible ( true );
		effectiveQuantity.setText ( "( " + ( Utils.floatToString ( quantity * prod.getFloat ( "unit_size" ) ) ) + ms + " )" );
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

		hideVariants ();
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

	private ArrayList retrieveVariants () {
		int variant_id;
		ArrayList final_array;
		ArrayList existing_variants;
		ArrayList existing_components;
		ArrayList final_components;
		HorizontalPanel row;
		ListBox selector;
		ProductUserVariant existing_var;
		ProductUserVariant variant;
		ProductUserVariantComponent existing_comp;
		ProductUserVariantComponent component;
		FromServer value;

		final_array = new ArrayList ();
		existing_variants = currentValue.getArray ( "variants" );

		for ( int i = 0; i < variantsBoxes.getWidgetCount (); i++ ) {
			row = ( HorizontalPanel ) variantsBoxes.getWidget ( i );
			variant_id = Integer.parseInt ( ( ( Hidden ) row.getWidget ( 0 ) ).getName () );

			existing_components = null;
			variant = null;

			if ( variant_id != -1 && existing_variants != null ) {
				for ( int a = 0; a < existing_variants.size (); a++ ) {
					existing_var = ( ProductUserVariant ) existing_variants.get ( a );
					if ( existing_var.getLocalID () == variant_id ) {
						variant = existing_var;
						existing_components = variant.getArray ( "components" );
						break;
					}
				}
			}

			if ( variant == null )
				variant = new ProductUserVariant ();

			final_components = new ArrayList ();

			for ( int a = 1; a < row.getWidgetCount (); a += 3 ) {
				variant_id = Integer.parseInt ( ( ( Hidden ) row.getWidget ( a ) ).getName () );
				component = null;

				if ( existing_components != null ) {
					for ( int e = 0; e < existing_components.size (); e++ ) {
						existing_comp = ( ProductUserVariantComponent ) existing_components.get ( e );

						if ( existing_comp.getObject ( "variant" ).getLocalID () == variant_id ) {
							component = existing_comp;
							break;
						}
					}
				}

				if ( component == null ) {
					component = new ProductUserVariantComponent ();
					component.setObject ( "variant", Utils.getServer ().getObjectFromCache ( "ProductVariant", variant_id ) );
				}

				selector = ( ListBox ) row.getWidget ( a + 2 );
				value = Utils.getServer ().getObjectFromCache ( "ProductVariantValue", Integer.parseInt ( selector.getValue ( selector.getSelectedIndex () ) ) );
				component.setObject ( "value", value );
				final_components.add ( component );
			}

			variant.setArray ( "components", final_components );
			final_array.add ( variant );
		}

		return final_array;
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		int num_variants;
		float q;
		float unit;
		ArrayList variants;
		FromServer prod;

		currentValue = element;

		q = element.getFloat ( "quantity" );

		prod = element.getObject ( "product" );
		unit = prod.getFloat ( "unit_size" );
		if ( unit != 0 )
			q = Math.round ( q / unit );

		quantity.setVal ( q );
		defineOnProduct ( prod );

		variants = element.getArray ( "variants" );
		if ( variants != null )
			num_variants = variants.size ();
		else
			num_variants = 0;

		alignVariants ( element, num_variants, variants );
	}

	public FromServer getValue () {
		float q;
		float unit;

		q = quantity.getVal ();

		unit = currentValue.getObject ( "product" ).getFloat ( "unit_size" );
		if ( unit != 0 )
			q = q * unit;

		currentValue.setFloat ( "quantity", q );

		if ( variantsBoxes != null && variantsBoxes.isVisible () == true )
			currentValue.setArray ( "variants", retrieveVariants () );

		return currentValue;
	}
}
