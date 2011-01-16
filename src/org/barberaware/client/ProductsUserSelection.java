/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProductsUserSelection extends Composite implements FromServerArray, SourcesChangeEvents {
	private FlexTable		main;
	private boolean			editable;
	private boolean			freeEditable;
	private boolean			enableShowDetails;
	private boolean			showingPrices;

	/*
		Il dialog per le descrizioni dei prodotti viene creato la
		prima volta che viene richiesto
	*/
	private DialogBox		descDialog			= null;
	private Label			descDialogText;

	private float			total;
	private PriceViewer		totalLabel;

	private ArrayList		changeCallbacks			= null;

	public ProductsUserSelection ( ArrayList products, boolean edit, boolean freeedit ) {
		int num_products;
		FocusPanel container;
		Product prod;

		container = new FocusPanel ();
		container.addMouseListener ( new MouseListener () {
			public void onMouseDown ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseEnter ( Widget sender ) {
				/* dummy */
			}

			public void onMouseLeave ( Widget sender ) {
				/* dummy */
			}

			public void onMouseMove ( Widget sender, int x, int y ) {
				int width;
				Label label;

				if ( enableShowDetails == false )
					return;

				width = main.getOffsetWidth ();

				if ( x > ( ( width * 80 ) / 100 ) ) {
					if ( showingPrices == true ) {
						for ( int i = 0; i < main.getRowCount () - 2; i++ ) {
							label = ( Label ) main.getWidget ( i, 2 );
							label.addStyleName ( "hidden" );
							label = ( Label ) main.getWidget ( i, 3 );
							label.removeStyleName ( "hidden" );
						}

						showingPrices = false;
					}
				}
				else {
					if ( showingPrices == false ) {
						for ( int i = 0; i < main.getRowCount () - 2; i++ ) {
							label = ( Label ) main.getWidget ( i, 2 );
							label.removeStyleName ( "hidden" );
							label = ( Label ) main.getWidget ( i, 3 );
							label.addStyleName ( "hidden" );
						}

						showingPrices = true;
					}
				}
			}

			public void onMouseUp ( Widget sender, int x, int y ) {
				/* dummy */
			}
		} );
		initWidget ( container );

		main = new FlexTable ();
		main.setStyleName ( "products-selection" );
		main.setWidth ( "100%" );
		main.setCellSpacing ( 5 );
		container.add ( main );

		editable = edit;
		freeEditable = freeedit;
		enableShowDetails = false;
		showingPrices = true;

		addTotalRow ();

		if ( products != null ) {
			num_products = products.size ();

			for ( int i = 0; i < num_products; i++ ) {
				prod = ( Product ) products.get ( i );
				if ( prod.getBool ( "available" ) == false )
					continue;

				addProductRow ( prod );
			}
		}

		main.addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				int total_rows;
				ProductUserSelector selector;

				total_rows = main.getRowCount ();
				if ( row >= total_rows - 2 || cell != 0 )
					return;

				selector = ( ProductUserSelector ) main.getWidget ( row, 1 );
				showProductDescriptionDialog ( ( Product ) selector.getValue ().getObject ( "product" ) );
			}
		} );
	}

	/*
		Questo si puo' usare solo su un ProductsUserSelection editabile, rende non
		editabili le caselle di testo per le quantita' dei prodotti
	*/
	public void setEditable ( boolean edit ) {
		int num_rows;
		ProductUserSelector selector;

		if ( editable == true ) {
			num_rows = main.getRowCount () - 2;

			for ( int i = 0; i < num_rows; i++ ) {
				selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
				selector.setEditable ( edit );
			}
		}
	}

	private void initDescriptionDialog () {
		VerticalPanel pan;
		Button but;

		descDialog = new DialogBox ();

		pan = new VerticalPanel ();
		pan.setWidth ( "400px" );
		descDialog.setWidget ( pan );

		descDialogText = new Label ();
		pan.add ( descDialogText );

		but = new Button ( "OK", new ClickListener () {
			public void onClick ( Widget sender ) {
				descDialog.hide ();
			}
		} );
		pan.add ( but );
	}

	private void showProductDescriptionDialog ( Product product ) {
		String desc;

		if ( descDialog == null )
			initDescriptionDialog ();

		descDialog.setText ( product.getString ( "name" ) );
		desc = product.getString ( "description" );
		descDialogText.setText ( desc != "" ? desc : "Nessuna descrizione" );

		descDialog.center ();
		descDialog.show ();
	}

	private void addTotalRow () {
		/*
			La riga dei totali viene piazzata da principio in testa, poi man mano che
			i prodotti vengono inseriti in ordine alfabetico si creano le altre righe
			sopra nelle giuste posizioni
		*/

		main.setWidget ( 0, 0, new HTML ( "<hr>" ) );
		main.getFlexCellFormatter ().setColSpan ( 0, 0, 2 );

		totalLabel = new PriceViewer ();
		totalLabel.setStyleName ( "bigger-text" );
		main.setWidget ( 1, 0, new Label ( "Totale" ) );
		main.setWidget ( 1, 1, totalLabel );
	}

	private int retrieveRowByProduct ( Product prod ) {
		int a;
		int rows;
		ProductUserSelector selector;

		rows = main.getRowCount () - 2;

		for ( a = 0; a < rows; a++ ) {
			selector = ( ProductUserSelector ) main.getWidget ( a, 1 );
			if ( selector.getValue ().getObject ( "product" ).equals ( prod ) )
				return a;
		}

		return -1;
	}

	public void upgradeProductsList ( ArrayList products ) {
		int i;
		int a;
		int num_products;
		int original_rows;
		int index;
		boolean to_remove;
		ArrayList track_existing;
		Product prod;
		ProductUserSelector selector;

		num_products = products.size ();
		original_rows = main.getRowCount () - 2;

		for ( i = 0; i < num_products; i++ ) {
			prod = ( Product ) products.get ( i );

			if ( prod.getBool ( "available" ) == false )
				continue;

			a = retrieveRowByProduct ( prod );
			if ( a != -1 )
				modProductRow ( a, prod );
			else
				addProductRow ( prod );
		}

		/**
			TODO	Il controllo sugli elementi rimossi puo' essere ampliamente
				migliorato...
		*/

		for ( a = 0; a < original_rows; a++ ) {
			selector = ( ProductUserSelector ) main.getWidget ( a, 1 );
			to_remove = true;

			for ( i = 0; i < num_products; i++ ) {
				prod = ( Product ) products.get ( i );

				if ( selector.getValue ().getObject ( "product" ).equals ( prod ) ) {
					if ( prod.getBool ( "available" ) == true )
						to_remove = false;
					break;
				}
			}

			if ( to_remove ) {
				main.removeRow ( a );
				a--;
				original_rows--;
			}
		}
	}

	public float getTotalPrice () {
		return total;
	}

	private String getPriceInfo ( Product product ) {
		String info_str;
		String plus_str;
		float plus;
		boolean raw;
		FromServer measure;

		measure = product.getObject ( "measure" );

		plus = product.getFloat ( "unit_price" );
		info_str = plus + " € / " + measure.getString ( "symbol" );

		/**
			TODO	Aggiungere una icona per indicare i prodotti il cui prezzo viene
				fissato alla consegna
		*/

		raw = product.getBool ( "mutable_price" );
		if ( raw == true )
			info_str += " (prodotto misurato alla consegna)";

		plus = product.getFloat ( "shipping_price" );
		if ( plus != 0 )
			info_str += " + " + plus + " € trasporto";

		plus_str = product.getString ( "surplus" );
		if ( plus_str != null && plus_str != "" && plus_str != "0" )
			info_str += " + " + Utils.showPercentage ( plus_str ) + " surplus";

		return info_str;
	}

	private void addProductRow ( Product product ) {
		int row;
		String info_str;
		Label pname;
		FlexTable.FlexCellFormatter formatter;
		ProductUserSelector sel;

		row = getProductAddingPosition ( product );
		main.insertRow ( row );

		formatter = main.getFlexCellFormatter ();

		pname = new Label ( product.getString ( "name" ) );
		pname.setStyleName ( "product-name" );
		main.setWidget ( row, 0, pname );
		formatter.setWidth ( row, 0, "30%" );
		/*
			Forzo l'allineamento in cima affinche' tutto rimanga in ordine quando (eventualmente) si
			aprono le righe per definire le varianti dei prodotti (in ProductUserSelector)
		*/
		formatter.setVerticalAlignment ( row, 0, HasVerticalAlignment.ALIGN_TOP );

		sel = new ProductUserSelector ( product, editable, freeEditable );
		main.setWidget ( row, 1, sel );
		formatter.setWidth ( row, 1, "40%" );
		formatter.setVerticalAlignment ( row, 1, HasVerticalAlignment.ALIGN_TOP );

		if ( editable == true ) {
			sel.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					updateTotal ();
				}
			} );
		}

		/*
			Prezzo
		*/

		info_str = getPriceInfo ( product );
		main.setWidget ( row, 2, new Label ( info_str ) );
		formatter.setVerticalAlignment ( row, 2, HasVerticalAlignment.ALIGN_TOP );
		formatter.setHorizontalAlignment ( row, 2, HasHorizontalAlignment.ALIGN_RIGHT );

		/*
			Ordinante
		*/

		pname = new Label ( "" );
		pname.addStyleName ( "hidden" );
		main.setWidget ( row, 3, pname );
		formatter.setVerticalAlignment ( row, 3, HasVerticalAlignment.ALIGN_TOP );
		formatter.setHorizontalAlignment ( row, 3, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	private int getProductAddingPosition ( Product prod ) {
		int i;
		int tot;
		String name;
		Label existing_name;

		name = prod.getString ( "name" );
		tot = main.getRowCount () - 2;

		for ( i = 0; i < tot; i++ ) {
			existing_name = ( Label ) main.getWidget ( i, 0 );
			if ( name.compareTo ( existing_name.getText () ) <= 0 )
				break;
		}

		return i;
	}

	private void modProductRow ( int index, Product product ) {
		String info_str;
		FlexTable.FlexCellFormatter formatter;
		Label lab;
		ProductUserSelector sel;

		formatter = main.getFlexCellFormatter ();

		lab = ( Label ) main.getWidget ( index, 0 );
		lab.setText ( product.getString ( "name" ) );

		sel = ( ProductUserSelector ) main.getWidget ( index, 1 );
		sel.setProduct ( product );

		/*
			Prezzo
		*/

		info_str = getPriceInfo ( product );
		lab = ( Label ) main.getWidget ( index, 2 );
		lab.setText ( info_str );

		/*
			Informazioni aggiuntive
		*/

		info_str = product.getString ( "description" );
		lab = ( Label ) main.getWidget ( index + 1, 0 );
		lab.setText ( info_str );
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( changeCallbacks, this );
	}

	private void updateTotal () {
		int rows;
		float price;
		ProductUserSelector selector;

		rows = main.getRowCount () - 2;
		price = 0;

		for ( int i = 0; i < rows; i++ ) {
			selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
			price += selector.getTotalPrice ();
		}

		total = price;
		totalLabel.setVal ( price );

		triggerChange ();
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int num_elements;
		int rows;
		FromServer prod_internal;
		boolean found;
		String details_string;
		Date details_date;
		Label details;
		ProductUserSelector selector;
		HTMLTable.RowFormatter row_format;
		FromServer prod;
		FromServer sel_prod;

		if ( elements == null ) {
			rows = main.getRowCount () - 2;

			for ( int i = 0; i < rows; i++ ) {
				selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
				selector.clear ();
				selector.setVisible ( true );

				/*
					Volontariamente evito di re-inizializzare le Labels nella cella 3, riportanti
					la data dell'ultima modifica, tanto il pannello non viene visualizzato
					(enableShowDetails == false) e quando verra' visualizzato esse saranno
					opportunamente riempite nell'else qui sotto
				*/
			}

			enableShowDetails = false;
		}
		else {
			row_format = main.getRowFormatter ();

			/**
				TODO	Qui un ordinamento degli array eviterebbe di fare tanti
					giri ed ottimizzare
			*/

			num_elements = elements.size ();
			rows = main.getRowCount () - 2;

			for ( int i = 0; i < rows; i++ ) {
				selector = ( ProductUserSelector ) main.getWidget ( i, 1 );

				sel_prod = selector.getValue ().getObject ( "product" );
				if ( sel_prod.getBool ( "available" ) == false )
					continue;

				found = false;
				details_string = "";

				for ( int a = 0; a < num_elements; a++ ) {
					prod = ( FromServer ) elements.get ( a );
					prod_internal = prod.getObject ( "product" );

					/**
						TODO	Sia qui che in getElements() duplico oggetti a tutto andare,
							per evitare sovrapposizioni; tutto dipende dal fatto che in
							OrderPrivilegedPanel uso sempre lo stesso form, inizializzato
							e reinizializzato un po' a casaccio, ci sarebbe da
							perfezionare quella parte ed evitare qui tante copie (di cui
							alla fine la stragrande maggioranza sono perse nel nulla)
					*/

					if ( sel_prod.equals ( prod_internal ) ) {
						selector.setValue ( prod.duplicate () );

						if ( editable == false )
							row_format.setVisible ( i, true );

						details_date = prod.getDate ( "orderdate" );
						if ( details_date != null )
							details_string = prod.getObject ( "orderperson" ).getString ( "name" ) + " - " +
										Utils.printableDate ( details_date );

						found = true;
						break;
					}
				}

				if ( found == false ) {
					selector.clear ();

					if ( editable == false )
						row_format.setVisible ( i, false );
				}

				details = ( Label ) main.getWidget ( i, 3 );
				details.setText ( details_string );
			}

			enableShowDetails = true;
		}

		updateTotal ();
	}

	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		int num_rows;
		ArrayList list;
		ProductUserSelector selector;
		Float quant;
		FromServer prod;
		Hidden id;

		list = null;
		num_rows = main.getRowCount () - 2;

		for ( int i = 0; i < num_rows; i++ ) {
			selector = ( ProductUserSelector ) main.getWidget ( i, 1 );
			prod = selector.getValue ();

			if ( prod.getFloat ( "quantity" ) > 0 ) {
				if ( list == null )
					list = new ArrayList ();

				/*
					Onde evitare di impazzire troppo in OrderPrivilegedPanel,
					in cui lo stesso OrderUser viene riusato e sovrascritto,
					qui duplico gli elementi validi per evitare
					sovrapposizioni
				*/
				list.add ( prod.duplicate () );
			}
		}

		return list;
	}

	/*
		Questa funzione non e' particolarmente completa, provvedere a
		sistemarla qualora servisse davvero
	*/
	public void refreshElement ( FromServer element ) {
		int a;
		Product prod;

		prod = ( Product ) element.getObject ( "product" );

		a = retrieveRowByProduct ( prod );
		if ( a != -1 )
			modProductRow ( a, prod );
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks == null )
			changeCallbacks = new ArrayList ();
		changeCallbacks.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks != null )
			changeCallbacks.remove ( listener );
	}
}
