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

import com.allen_sauer.gwt.log.client.Log;

public class ProductsEditPanel extends Composite {
	private DeckPanel		main;

	private FormCluster		list;
	private FromServerTable		table;
	private ToggleButton		switchable;

	private Supplier		supplier;

	public ProductsEditPanel ( Supplier supp, boolean enabled ) {
		VerticalPanel container;

		supplier = supp;

		container = new VerticalPanel ();
		container.setWidth ( "100%" );
		initWidget ( container );

		main = new DeckPanel ();
		container.add ( main );

		main.add ( doListView () );
		main.add ( doTableView () );

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
		switchable.setStyleName ( "clickable" );
		container.add ( switchable );
		container.setCellHorizontalAlignment ( switchable, HasHorizontalAlignment.ALIGN_RIGHT );

		if ( enabled == false ) {
			main.add ( new Label ( "Dopo aver confermato il salvataggio del fornitore, sarà qui possibile definirne i prodotti" ) );
			main.showWidget ( 2 );
			switchable.setVisible ( false );
		}
		else
			main.showWidget ( 0 );
	}

	public void enable ( boolean enabled ) {
		if ( enabled == false ) {
			main.showWidget ( 2 );
			switchable.setVisible ( false );
		}
		else {
			main.showWidget ( 0 );
			switchable.setVisible ( true );
		}
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

	public int numProducts () {
		return list.latestIterableIndex ();
	}

	/****************************************************************** lista */

	private Widget doListView () {
		list = new FormCluster ( "Product", "Nuovo Prodotto", false ) {
				protected FromServerForm doEditableRow ( FromServer product ) {
					FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					FromServerSelector select;

					ver = new FromServerForm ( product );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Nome", ver.getWidget ( "name" ) );

					/**
						TODO	Raffinatezza: laddove possibile, al cambio della categoria
							selezionare automaticamente l'unita' di misura piu' adeguata
							(lasciando comunque la possibilita' di modificarla)
					*/
					select = new FromServerSelector ( "Category", true, true );
					frame.addPair ( "Categoria", ver.getPersonalizedWidget ( "category", select ) );

					select = new FromServerSelector ( "Measure", true, true );
					frame.addPair ( "Unità di misura", ver.getPersonalizedWidget ( "measure", select ) );

					frame.addPair ( "Ordinabile", ver.getWidget ( "available" ) );

					/* seconda colonna */

					frame = new CustomCaptionPanel ( "Prezzo" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Unitario (€)", ver.getWidget ( "unit_price" ) );
					frame.addPair ( "Trasporto (€)", ver.getWidget ( "shipping_price" ) );
					frame.addPair ( "Variabile", ver.getWidget ( "mutable_price" ) );

					/**
						TODO	Gestire in qualche modo strutturato le
							motivazioni per i sovrapprezzi
					*/

					frame.addPair ( "Sovrapprezzo (€/%)", ver.getWidget ( "surplus" ) );
					frame.addPair ( "Dimensione stock", ver.getWidget ( "stock_size" ) );
					frame.addPair ( "Minimo per utente", ver.getWidget ( "minimum_order" ) );
					frame.addPair ( "Multiplo per utente", ver.getWidget ( "multiple_order" ) );

					sframe = new CaptionPanel ( "Descrizione" );
					sframe.add ( ver.getWidget ( "description" ) );
					ver.add ( sframe );

					if ( product.getBool ( "archived" ) == true ) {
						IconsBar icons;
						icons = ver.getIconsBar ();
						icons.addImage ( "images/notifications/product_archived.png" );
					}

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					Product product;

					product = new Product ();
					product.setObject ( "supplier", supplier );
					return doEditableRow ( product );
				}

				protected void customModify ( FromServerForm form ) {
					FromServer obj;

					obj = form.getObject ();
					if ( obj.getBool ( "archived" ) == true )
						deleteElement ( obj );
				}
		};

		list.extraAddButton ( new AddButton ( "Duplica Prodotto", new ClickListener () {
			public void onClick ( Widget sender ) {
				duplicateProduct ();
			}
		} ) );

		return list;
	}

	private void duplicateProduct () {
		int num;
		ArrayList products;
		SelectionDialog selector;

		products = list.collectContents ();

		num = products.size ();
		if ( num == 0 ) {
			Utils.showNotification ( "Non ci sono prodotti da duplicare!" );
			return;
		}

		selector = new SelectionDialog ( SelectionDialog.SELECTION_MODE_SINGLE );

		selector.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				SelectionDialog sel;
				ArrayList products;
				FromServer prod;

				sel = ( SelectionDialog ) dialog;

				products = sel.getElements ();
				if ( products.size () == 0 ) {
					Utils.showNotification ( "Non hai specificato il prodotto da duplicare" );
					return;
				}

				prod = ( FromServer ) products.get ( 0 );
				prod = prod.duplicate ();
				prod.setLocalID ( -1 );
				prod.setString ( "name", prod.getString ( "name" ) + " 2" );
				list.addElement ( prod );
			}

			public void onCancel ( SavingDialog dialog ) {
				/* dummy */
			}
		} );

		for ( int i = 0; i < num; i++ )
			selector.addElementInList ( ( FromServer ) products.get ( i ) );

		selector.center ();
		selector.show ();
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
				/*
					La rappresentazione a tabella e' secondaria rispetto alla
					lista, quando si salva o si annulla si torna alla vista
					principale anche per avere un senso di feedback per
					l'operazione
				*/
				main.showWidget ( 0 );
			}
		} );
		buttons.add ( button, "Annulla" );

		button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				table.saveChanges ();
				main.showWidget ( 0 );
			}
		} );
		buttons.add ( button, "Salva" );

		return container;
	}
}
