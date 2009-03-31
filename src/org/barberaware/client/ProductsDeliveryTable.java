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

public class ProductsDeliveryTable extends FromServerArray {
	private FlexTable		main;
	private float			total;
	private PriceViewer		totalLabel;
	private ArrayList		currentValues;

	public ProductsDeliveryTable () {
		main = new FlexTable ();
		main.setStyleName ( "products-selection" );
		initWidget ( main );
		main.setWidth ( "100%" );
		main.setCellSpacing ( 5 );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		ProductUser prod_user;
		Product prod;
		Measure measure;
		FloatBox del;
		float delivered;

		currentValues = elements;

		for ( int i = 0; i < main.getRowCount (); i++ )
			main.removeRow ( 0 );

		for ( int i = 0; i < elements.size (); i++ ) {
			prod_user = ( ProductUser ) elements.get ( i );
			prod = ( Product ) prod_user.getObject ( "product" );
			measure = ( Measure ) prod.getObject ( "measure" );

			main.setWidget ( i, 0, new Hidden ( "id", Integer.toString ( prod.getLocalID () ) ) );
			main.setWidget ( i, 1, new Label ( prod.getString ( "name" ) ) );
			main.setWidget ( i, 2, new Label ( prod_user.getFloat ( "quantity" ) + " " + measure.getString ( "symbol" ) ) );

			del = new FloatBox ();
			delivered = prod_user.getFloat ( "delivered" );
			del.setValue ( delivered );
			main.setWidget ( i, 3, del );

			del.addFocusListener ( new FocusListener () {
				public void onFocus ( Widget sender ) {
					/* dummy */
				}

				public void onLostFocus ( Widget sender ) {
					/**
						TODO	Controllo quantita' ed aggiornamento prezzo
					*/
				}
			} );

			main.setWidget ( i, 4, new Label ( ( delivered * prod.getTotalPrice () ) + " €" ) );
		}
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		FloatBox del;
		ProductUser produser;

		for ( int i = 0; i < currentValues.size (); i++ ) {
			del = ( FloatBox ) main.getWidget ( i, 3 );
			produser = ( ProductUser ) currentValues.get ( i );
			produser.setFloat ( "delivered", del.getValue () );
		}

		return currentValues;
	}
}
