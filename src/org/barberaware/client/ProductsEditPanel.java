/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProductsEditPanel extends Composite {
	private VerticalPanel	main;
	private Supplier	supplier;

	/**
		TODO	Ordinare i prodotti per categoria
	*/

	public ProductsEditPanel ( Supplier supp ) {
		main = new VerticalPanel ();
		initWidget ( main );
		main.add ( doAddProductButton () );

		supplier = supp;
	}

	public void addProduct ( Product product ) {
		main.insert ( doEditableRow ( product ), 0 );
	}

	private FromServerForm doEditableRow ( Product product ) {
		FromServerForm ver;
		HorizontalPanel hor;
		FlexTable fields;

		if ( product == null ) {
			product = new Product ();
			product.setObject ( "supplier", supplier );
		}

		ver = new FromServerForm ( product );

		hor = new HorizontalPanel ();
		ver.add ( hor );

		fields = new FlexTable ();
		hor.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Nome" ) );
		fields.setWidget ( 0, 1, ver.getWidget ( "name" ) );

		fields.setWidget ( 1, 0, new Label ( "Categoria" ) );
		fields.setWidget ( 1, 1, ver.getWidget ( "category" ) );

		fields.setWidget ( 2, 0, new Label ( "Unità di misura" ) );
		fields.setWidget ( 2, 1, ver.getWidget ( "measure" ) );

		fields.setWidget ( 3, 0, new Label ( "Ordinabile" ) );
		fields.setWidget ( 3, 1, ver.getWidget ( "available" ) );

		fields = new FlexTable ();
		hor.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Prezzo unitario" ) );
		fields.setWidget ( 0, 1, ver.getWidget ( "unit_price" ) );

		fields.setWidget ( 1, 0, new Label ( "Prezzo trasporto" ) );
		fields.setWidget ( 1, 1, ver.getWidget ( "shipping_price" ) );

		fields.setWidget ( 2, 0, new Label ( "Prezzo variabile" ) );
		fields.setWidget ( 2, 1, ver.getWidget ( "mutable_price" ) );

		fields.setWidget ( 3, 0, new Label ( "Sovrapprezzo" ) );
		fields.setWidget ( 3, 1, ver.getWidget ( "surplus" ) );

		fields.setWidget ( 4, 0, new Label ( "Dimensione stock" ) );
		fields.setWidget ( 4, 1, ver.getWidget ( "stock_size" ) );

		fields.setWidget ( 5, 0, new Label ( "Minimo per l'utente" ) );
		fields.setWidget ( 5, 1, ver.getWidget ( "minimum_order" ) );

		fields.setWidget ( 6, 0, new Label ( "Multiplo per l'utente" ) );
		fields.setWidget ( 6, 1, ver.getWidget ( "multiple_order" ) );

		ver.add ( new Label ( "Descrizione" ) );
		ver.add ( ver.getWidget ( "description" ) );

		return ver;
	}

	private Panel doAddProductButton () {
		PushButton button;
		HorizontalPanel pan;

		pan = new HorizontalPanel ();
		pan.setStyleName ( "bottom-buttons" );

		button = new PushButton ( new Image ( "images/new_product.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				FromServerForm new_product;
				new_product = doEditableRow ( null );
				new_product.open ( true );
				main.insert ( new_product, main.getWidgetCount () - 1 );
			}
		} );

		pan.add ( button );
		return pan;
	}
}
