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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class ProductsDeliveryTable extends Composite implements FromServerArray {
	private FlexTable		main;
	private float			total;
	private PriceViewer		totalLabel;
	private ArrayList		currentValues;

	public ProductsDeliveryTable () {
		HTMLTable.RowFormatter formatter;

		main = new FlexTable ();
		main.setStyleName ( "elements-table" );
		initWidget ( main );

		main.setWidget ( 0, 1, new Label ( "Prodotto" ) );
		main.setWidget ( 0, 2, new Label ( "Quantità Ordinata" ) );
		main.setWidget ( 0, 3, new Label ( "Quantità Consegnata" ) );
		main.setWidget ( 0, 4, new Label ( "Prezzo Totale" ) );

		formatter = main.getRowFormatter ();
		formatter.addStyleName ( 0, "table-header" );
	}

	private void newInputToCheck ( FloatBox box ) {
		int num_rows;
		float input;
		float row_sum;
		float total_sum;
		FloatWidget iter;
		ProductUser prod_user;
		FromServer prod;

		total_sum = 0;
		num_rows = currentValues.size ();

		for ( int a = 0, i = 1; a < num_rows; a++, i++ ) {
			iter = ( FloatWidget ) main.getWidget ( i, 3 );
			input = iter.getVal ();

			prod_user = ( ProductUser ) currentValues.get ( a );
			row_sum = prod_user.getTotalPrice ( input );

			prod = prod_user.getObject ( "product" );

			if ( iter == box ) {
				if ( input < 0 ) {
					Utils.showNotification ( "Il valore immesso non è valido" );
					box.setVal ( 0 );
				}
				else {
					Label total_label;

					/*
						Se la quantita' immessa e' diversa da quella ordinata si limita a
						mostrare una notifica, ma non blocca l'operazione in quanto puo'
						succedere che avanzi qualche prodotto (ordinato per arrotondamento) e
						si distribuisca arbitrariamente
					*/
					if ( prod.getBool ( "mutable_price" ) == false && input > prod_user.getFloat ( "quantity" ) )
						Utils.showNotification ( "Hai immesso una quantità diversa da quella ordinata",
										SmoothingNotify.NOTIFY_INFO );

					total_label = ( Label ) main.getWidget ( i, 4 );
					total_label.setText ( Utils.priceToString ( row_sum ) );
				}
			}

			total_sum = row_sum + total_sum;
		}

		totalLabel.setVal ( total_sum );
	}

	private void newVariantInputToCheck ( FloatBox box ) {
		int num_rows;
		float input;
		float row_sum;
		float total_sum;
		FloatWidget iter;
		FloatBoxes parent;
		ProductUser prod_user;

		total_sum = 0;
		num_rows = currentValues.size ();
		parent = ( FloatBoxes ) box.getParent ();

		for ( int a = 0, i = 1; a < num_rows; a++, i++ ) {
			iter = ( FloatWidget ) main.getWidget ( i, 3 );
			input = iter.getVal ();

			prod_user = ( ProductUser ) currentValues.get ( a );
			row_sum = prod_user.getTotalPrice ( input );

			if ( iter == parent ) {
				if ( input < 0 ) {
					Utils.showNotification ( "Il valore immesso non è valido" );
					box.setVal ( 0 );
				}
				else {
					Label total_label;

					total_label = ( Label ) main.getWidget ( i, 4 );
					total_label.setText ( Utils.priceToString ( row_sum ) );
				}
			}

			total_sum = row_sum + total_sum;
		}

		totalLabel.setVal ( total_sum );
	}

	private void shipAllVariants ( int row ) {
		FlexTable list;
		Label quantity_label;
		FloatBoxes boxes;
		FloatBox box;

		list = ( FlexTable ) main.getWidget ( row, 2 );
		boxes = ( FloatBoxes ) main.getWidget ( row, 3 );

		for ( int i = 0; i < list.getRowCount (); i++ ) {
			quantity_label = ( Label ) list.getWidget ( i, 1 );
			box = ( FloatBox ) boxes.getWidget ( i );
			box.setVal ( Integer.parseInt ( quantity_label.getText () ) );
		}
	}

	private Button createAutoCompleteButton () {
		Button ret;

		ret = new Button ( "Consegna Tutto" );
		ret.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				int num_rows;
				float input;
				float row_sum;
				float total_sum;
				Label total_label;
				FloatWidget iter;
				ProductUser prod_user;

				total_sum = 0;
				num_rows = currentValues.size ();

				for ( int a = 0, i = 1; a < num_rows; a++, i++ ) {
					iter = ( FloatWidget ) main.getWidget ( i, 3 );
					prod_user = ( ProductUser ) currentValues.get ( a );

					input = prod_user.getFloat ( "quantity" );

					row_sum = prod_user.getTotalPrice ( input );
					total_label = ( Label ) main.getWidget ( i, 4 );
					total_label.setText ( Utils.priceToString ( row_sum ) );

					if ( iter instanceof FloatBox == true )
						iter.setVal ( input );
					else
						shipAllVariants ( i );

					total_sum = row_sum + total_sum;
				}

				totalLabel.setVal ( total_sum );
			}
		} );

		return ret;
	}

	private void hookCalculator ( ProductUser prod, FloatBox box ) {
		int pieces;
		float unit;
		final ScreenCalculator calc;

		unit = prod.getObject ( "product" ).getFloat ( "unit_size" );
		if ( unit != 0 ) {
			pieces = Math.round ( prod.getFloat ( "quantity" ) / unit );

			if ( pieces > 1 ) {
				box.addStyleName ( "calculator-addicted" );

				calc = new ScreenCalculator ( pieces );
				calc.setText ( prod.getObject ( "product" ).getString ( "name" ) );
				calc.setTarget ( box );

				box.addFocusListener ( new FocusListener () {
					public void onFocus ( Widget sender ) {
						calc.center ();
						calc.show ();
					}

					public void onLostFocus ( Widget sender ) {
						/*
							dummy
						*/
					}
				} );
			}
		}
	}

	private Widget [] doVariantsList ( ArrayList variants ) {
		int num;
		int num_comps;
		int found;
		String check;
		String label;
		ArrayList components;
		Hidden check_placeholder;
		Label counter;
		FlexTable list;
		FloatBoxes inputs;
		FloatBox del;
		FromServer value;
		ProductUserVariant variant;
		ProductUserVariantComponent component;
		Widget ret [];

		list = new FlexTable ();
		inputs = new FloatBoxes ();

		num = variants.size ();

		for ( int i = 0; i < num; i++ ) {
			check = "";
			label = "";

			variant = ( ProductUserVariant ) variants.get ( i );

			components = variant.getArray ( "components" );
			num_comps = components.size ();

			for ( int a = 0; a < num_comps; a++ ) {
				component = ( ProductUserVariantComponent ) components.get ( a );
				value = component.getObject ( "value" );
				check = check + Integer.toString ( value.getLocalID () ) + ":";
				label = component.getObject ( "variant" ).getString ( "name" ) + ": " + value.getString ( "name" ) + " ";
			}

			found = -1;

			for ( int a = 0; a < list.getRowCount (); a++ ) {
				check_placeholder = ( Hidden ) list.getWidget ( a, 0 );
				if ( check == check_placeholder.getName () ) {
					found = a;
					break;
				}
			}

			if ( found == -1 ) {
				found = list.getRowCount ();
				list.setWidget ( found, 0, new Hidden ( check ) );
				list.setWidget ( found, 1, new Label ( "1" ) );
				list.setWidget ( found, 2, new Label ( label ) );

				del = inputs.addBox ();

				del.addFocusListener ( new FocusListener () {
					public void onFocus ( Widget sender ) {
						/* dummy */
					}

					public void onLostFocus ( Widget sender ) {
						newVariantInputToCheck ( ( FloatBox ) sender );
					}
				} );
			}
			else {
				counter = ( Label ) list.getWidget ( found, 1 );
				counter.setText ( Integer.toString ( Integer.parseInt ( counter.getText () ) + 1 ) );

				del = ( FloatBox ) inputs.getWidget ( found );
			}

			if ( variant.getBool ( "delivered" ) == true )
				del.setVal ( del.getVal () + 1 );
		}

		ret = new Widget [ 2 ];
		ret [ 0 ] = list;
		ret [ 1 ] = inputs;

		return ret;
	}

	private void deliverVariants ( ArrayList variants, FlexTable list, FloatBoxes quantities ) {
		int variants_num;
		float found;
		boolean skip;
		int [] num_ids;
		String [] ids;
		ArrayList components;
		Hidden check;
		FloatBox quantity;
		ProductUserVariant var;
		ProductUserVariantComponent comp;

		variants_num = variants.size ();

		for ( int a = 0; a < variants_num; a++ ) {
			var = ( ProductUserVariant ) variants.get ( a );
			var.setBool ( "delivered", false );
		}

		for ( int i = 0; i < list.getRowCount (); i++ ) {
			quantity = ( FloatBox ) quantities.getWidget ( i );
			if ( quantity.getVal () == 0 )
				continue;

			check = ( Hidden ) list.getWidget ( i, 0 );

			ids = check.getName ().split ( ":" );
			num_ids = new int [ ids.length ];

			for ( int a = 0; a < ids.length; a++ )
				num_ids [ a ] = Integer.parseInt ( ids [ a ] );

			found = 0;

			for ( int a = 0; a < variants_num; a++ ) {
				skip = false;

				var = ( ProductUserVariant ) variants.get ( a );
				components = var.getArray ( "components" );

				for ( int e = 0; e < components.size (); e++ ) {
					comp = ( ProductUserVariantComponent ) components.get ( e );

					if ( comp.getObject ( "value" ).getLocalID () != num_ids [ e ] ) {
						skip = true;
						break;
					}
				}

				if ( skip == false ) {
					var.setBool ( "delivered", true );
					found = found + 1;

					if ( found == quantity.getVal () )
						break;
				}
			}
		}
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int i;
		int e;
		String symbol;
		float delivered;
		float price_product;
		float price_total;
		ArrayList variants;
		ProductUser prod_user;
		FromServer prod;
		FromServer measure;
		FloatBox del;
		FlexTable.FlexCellFormatter formatter;
		Widget variants_widgets [];

		currentValues = elements;
		price_total = 0;

		formatter = main.getFlexCellFormatter ();

		for ( i = 0; i < main.getRowCount () - 1; i++ )
			main.removeRow ( 1 );

		/*
			Questo e' per i casi in cui la lista dei prodotti viene aggiornata
			(magari perche' e' cambiato l'ordine) ed il colspan della cella in cui si
			trovava la riga che separa il totale va invalidato (per poi essere
			ri-settato quando si ri-piazza tale riga)
		*/
		main.getFlexCellFormatter ().setColSpan ( 1, 0, 1 );

		for ( e = 1, i = 0; i < elements.size (); i++ ) {
			prod_user = ( ProductUser ) elements.get ( i );

			prod = prod_user.getObject ( "product" );
			if ( prod.getBool ( "available" ) == false )
				continue;

			measure = prod.getObject ( "measure" );
			if ( measure != null )
				symbol = " " + measure.getString ( "symbol" );
			else
				symbol = "";

			delivered = prod_user.getFloat ( "delivered" );
			variants = prod_user.getArray ( "variants" );

			main.setWidget ( e, 0, new Hidden ( "id", Integer.toString ( prod.getLocalID () ) ) );
			main.setWidget ( e, 1, new Label ( prod.getString ( "name" ) ) );

			if ( variants == null || variants.size () == 0 ) {
				main.setWidget ( e, 2, new Label ( Utils.floatToString ( prod_user.getFloat ( "quantity" ) ) + symbol ) );

				del = new FloatBox ();
				del.setVal ( delivered );
				main.setWidget ( e, 3, del );

				hookCalculator ( prod_user, del );

				del.addFocusListener ( new FocusListener () {
					public void onFocus ( Widget sender ) {
						/* dummy */
					}

					public void onLostFocus ( Widget sender ) {
						newInputToCheck ( ( FloatBox ) sender );
					}
				} );
			}
			else {
				variants_widgets = doVariantsList ( variants );
				main.setWidget ( e, 2, variants_widgets [ 0 ] );
				main.setWidget ( e, 3, variants_widgets [ 1 ] );
			}

			price_product = prod_user.getTotalPrice ( delivered );
			main.setWidget ( e, 4, new Label ( Utils.priceToString ( price_product ) ) );
			price_total = price_total + price_product;

			formatter.setVerticalAlignment ( e, 1, HasVerticalAlignment.ALIGN_TOP );
			formatter.setVerticalAlignment ( e, 4, HasVerticalAlignment.ALIGN_TOP );

			e++;
		}

		main.setWidget ( e, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( e, 0, 5 );

		e++;

		main.setWidget ( e, 3, createAutoCompleteButton () );

		if ( totalLabel == null ) {
			totalLabel = new PriceViewer ();
			totalLabel.setStyleName ( "bigger-text" );
		}

		totalLabel.setVal ( price_total );
		main.setWidget ( e, 4, totalLabel );
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		ArrayList variants;
		FloatWidget del;
		FromServer produser;

		for ( int i = 0; i < currentValues.size (); i++ ) {
			del = ( FloatWidget ) main.getWidget ( i + 1, 3 );
			produser = ( FromServer ) currentValues.get ( i );
			produser.setFloat ( "delivered", del.getVal () );

			variants = produser.getArray ( "variants" );

			if ( variants != null && variants.size () != 0 )
				deliverVariants ( variants, ( FlexTable ) main.getWidget ( i + 1, 2 ), ( FloatBoxes ) main.getWidget ( i + 1, 3 ) );
		}

		return currentValues;
	}

	public void refreshElement ( FromServer element ) {
		/* dummy */
	}
}
