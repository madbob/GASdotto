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
	private DeckPanel		main;

	private FormCluster		list;
	private FromServerTable		table;

	private Supplier		supplier;

	/**
		TODO	Ordinare i prodotti per categoria
	*/

	public ProductsEditPanel ( Supplier supp ) {
		VerticalPanel container;
		ToggleButton switchable;

		supplier = supp;

		container = new VerticalPanel ();
		container.setWidth ( "100%" );
		initWidget ( container );

		main = new DeckPanel ();
		container.add ( main );

		main.add ( doListView () );
		main.add ( doTableView () );
		main.showWidget ( 0 );

		switchable = new ToggleButton ( "Visualizza Tabella", "Visualizza Lista" );
		switchable.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				ToggleButton but;

				but = ( ToggleButton ) sender;
				if ( but.isDown () )
					main.showWidget ( 1 );
				else
					main.showWidget ( 0 );
			}
		} );
		switchable.setStyleName ( "text-button" );
		container.add ( switchable );
		container.setCellHorizontalAlignment ( switchable, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	public void addProduct ( Product product ) {
		list.addElement ( product );
		table.addElement ( product );
	}

	public void refreshProduct ( Product product ) {
		list.refreshElement ( product );
		table.refreshElement ( product );
	}

	public void deleteProduct ( Product product ) {
		list.deleteElement ( product );
		table.removeElement ( product );
	}

	/****************************************************************** lista */

	private Widget doListView () {
		list = new FormCluster ( "Product", "images/new_product.png", false ) {
				protected FromServerForm doEditableRow ( FromServer product ) {
					FromServerForm ver;
					HorizontalPanel hor;
					FlexTable fields;
					FromServerSelector select;

					ver = new FromServerForm ( product );

					hor = new HorizontalPanel ();
					ver.add ( hor );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Nome" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "name" ) );

					select = new FromServerSelector ( "Category", true, true );
					fields.setWidget ( 1, 0, new Label ( "Categoria" ) );
					fields.setWidget ( 1, 1, ver.getPersonalizedWidget ( "category", select ) );

					select = new FromServerSelector ( "Measure", true, true );
					fields.setWidget ( 2, 0, new Label ( "Unità di misura" ) );
					fields.setWidget ( 2, 1, ver.getPersonalizedWidget ( "measure", select ) );

					fields.setWidget ( 3, 0, new Label ( "Ordinabile" ) );
					fields.setWidget ( 3, 1, ver.getWidget ( "available" ) );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Prezzo unitario (€)" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "unit_price" ) );

					fields.setWidget ( 1, 0, new Label ( "Prezzo trasporto (€)" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "shipping_price" ) );

					fields.setWidget ( 2, 0, new Label ( "Prezzo variabile" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "mutable_price" ) );

					/**
						TODO	Gestire in qualche modo strutturato le
							motivazioni per i sovrapprezzi
					*/

					fields.setWidget ( 3, 0, new Label ( "Sovrapprezzo (€)" ) );
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

				protected FromServerForm doNewEditableRow () {
					Product product;

					product = new Product ();
					product.setObject ( "supplier", supplier );
					return doEditableRow ( product );
				}
		};

		return list;
	}

	/****************************************************************** tabella */

	private Widget doTableView () {
		VerticalPanel container;
		PushButton button;
		ButtonsBar buttons;

		container = new VerticalPanel ();

		table = new FromServerTable ();
		table.addColumn ( "Nome", "name", false );
		table.addColumn ( "Prezzo Unitario", "unit_price", true );
		table.addColumn ( "Ordinabile", "available", true );
		container.add ( table );

		buttons = new ButtonsBar ();
		container.add ( buttons );

		button = new PushButton ( new Image ( "images/cancel.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				table.revertChanges ();
			}
		} );
		buttons.add ( button, "Annulla" );

		button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				table.saveChanges ();
			}
		} );
		buttons.add ( button, "Salva" );

		return container;
	}
}
