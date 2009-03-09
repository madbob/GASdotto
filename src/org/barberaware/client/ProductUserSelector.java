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
	private Label					info;
	private ProductUser				currentValue;
	private DelegatingChangeListenerCollection	changeListeners;

	public ProductUserSelector ( Product prod ) {
		FocusPanel focusable;
		FlexTable container;
		HorizontalPanel main;
		HTMLTable.RowFormatter formatter;

		currentValue = new ProductUser ();
		currentValue.setObject ( "product", prod );

		focusable = new FocusPanel ();
		initWidget ( focusable );

		focusable.addMouseListener ( new MouseListener () {
			public void onMouseEnter ( Widget sender ) {
				showCells ( true );
			}

			public void onMouseLeave ( Widget sender ) {
				showCells ( false );
			}

			public void onMouseMove ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseDown ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseUp ( Widget sender, int x, int y ) {
				/* dummy */
			}
		} );

		container = new FlexTable ();
		container.setStyleName ( "buttons-bar" );
		focusable.add ( container );

		main = new HorizontalPanel ();
		container.setWidget ( 0, 0, main );

		quantity = new FloatBox ();
		hookChecks ( quantity, prod );
		main.add ( quantity );

		measure = new Label ();
		main.add ( measure );

		info = doDetails ( prod );
		container.setWidget ( 1, 0, info );
		info.setVisible ( false );

		formatter = container.getRowFormatter ();
		formatter.addStyleName ( 0, "icons" );
		formatter.addStyleName ( 1, "help" );

		setProduct ( prod );
	}

	private Label doDetails ( Product prod ) {
		Label details;
		FromServer measure;
		String symbol;
		String info;
		int quantity;

		info = "";

		measure = prod.getObject ( "measure" );
		if ( measure != null )
			symbol = measure.getString ( "symbol" );
		else
			symbol = "";

		quantity = prod.getInt ( "minimum_order" );
		if ( quantity != 0 )
			info += " Ordine minimo di " + quantity + symbol + " ";

		quantity = prod.getInt ( "multiple_order" );
		if ( quantity != 0 )
			info += " Ordinabili multipli di " + quantity + symbol + " ";

		details = new Label ( info );
		return details;
	}

	private void undoChange () {
		quantity.setValue ( 0 );
		changeListeners.fireChange ( quantity );
	}

	private void hookChecks ( FloatBox quantity, Product prod ) {
		int check;

		check = prod.getInt ( "minimum_order" );
		if ( check != 0 ) {
			quantity.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
					/* dummy */
				}

				public void onLostFocus ( Widget sender ) {
					FloatBox input;
					float quantity;
					float check;

					input = ( FloatBox ) sender;
					quantity = input.getValue ();
					check = currentValue.getObject ( "product" ).getInt ( "minimum_order" );

					if ( ( quantity != 0 ) && ( quantity < check ) ) {
						Utils.showNotification ( "La quantità deve essere non minore di " + check );
						undoChange ();
					}
				}
			} );
		}

		check = prod.getInt ( "multiple_order" );
		if ( check != 0 ) {
			quantity.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
					/* dummy */
				}

				public void onLostFocus ( Widget sender ) {
					FloatBox input;
					float quantity;
					float check;

					input = ( FloatBox ) sender;
					quantity = input.getValue ();
					check = currentValue.getObject ( "product" ).getInt ( "multiple_order" );

					if ( ( quantity != 0 ) && ( quantity % check != 0 ) ) {
						Utils.showNotification ( "La quantità deve essere multipla di " + check );
						undoChange ();
					}
				}
			} );
		}
	}

	private void setProduct ( Product prod ) {
		Measure m;

		m = ( Measure ) prod.getObject ( "measure" );
		if ( m != null )
			measure.setText ( m.getString ( "symbol" ) );
	}

	private void showCells ( boolean show ) {
		info.setVisible ( show );
	}

	public void setQuantity ( float quant ) {
		quantity.setValue ( quant );
	}

	public float getQuantity () {
		return quantity.getValue ();
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

		quantity.setValue ( element.getFloat ( "quantity" ) );

		prod = ( Product ) element.getObject ( "product" );
		setProduct ( prod );
	}

	public FromServer getValue () {
		currentValue.setFloat ( "quantity", quantity.getValue () );
		return currentValue;
	}
}
