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

public class SuppliersEditPanel extends GenericPanel {
	private FormCluster		main;
	private ArrayList		scheduledProducts;

	public SuppliersEditPanel () {
		super ();

		scheduledProducts = new ArrayList ();

		main = new FormCluster ( "Supplier", "images/new_supplier.png" ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				FromServerForm ver;
				ver = commonFormBuilder ( supp );
				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new Supplier () );
			}

			protected void customNew ( FromServer object, boolean true_new ) {
				if ( true_new == false ) {
					FromServerForm form;
					ProductsEditPanel products;

					form = main.retrieveForm ( object );
					products = ( ProductsEditPanel ) form.retriveInternalWidget ( "products" );
					products.enable ( true );
				}

				checkProductsSchedule ();
			}
		};

		addTop ( main );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Product product;

				product = ( Product ) object;
				if ( insertProduct ( product ) == false )
					scheduledProducts.add ( product );
			}

			public void onModify ( FromServer object ) {
				Product product;
				ProductsEditPanel panel;

				product = ( Product ) object;

				panel = retrieveProductsPanel ( product );
				if ( panel != null )
					panel.refreshProduct ( product );
			}

			public void onDestroy ( FromServer object ) {
				Product product;
				ProductsEditPanel panel;

				product = ( Product ) object;

				panel = retrieveProductsPanel ( product );
				if ( panel != null )
					panel.deleteProduct ( product );
			}
		} );

		Utils.getServer ().testObjectReceive ( "Supplier" );
	}

	private boolean insertProduct ( Product product ) {
		ProductsEditPanel panel;

		panel = retrieveProductsPanel ( product );

		if ( panel != null ) {
			IconsBar icons;
			FromServerForm supplier_form;

			/*
				Se prima la lista era vuota ed ora non lo e'
				piu', rimuovo l'icona messa "sulla fiducia" al
				momento della creazione
			*/
			supplier_form = main.retrieveForm ( product.getObject ( "supplier" ) );
			icons = supplier_form.getIconsBar ();

			if ( icons.hasImage ( "images/notifications/supplier_no_products.png" ) )
				icons.delImage ( "images/notifications/supplier_no_products.png" );

			panel.addProduct ( product );
			return true;
		}
		else
			return false;
	}

	private Widget attributesBuilder ( FromServerForm ver, Supplier supp ) {
		VerticalPanel vertical;
		HorizontalPanel hor;
		MultiSelector references;
		CustomCaptionPanel frame;
		CaptionPanel sframe;

		vertical = new VerticalPanel ();

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		vertical.add ( hor );

		frame = new CustomCaptionPanel ( "Attributi" );
		hor.add ( frame );

		frame.addPair ( "Nome", ver.getWidget ( "name" ) );
		frame.addPair ( "Nome Contatto", ver.getWidget ( "contact" ) );

		references = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_MULTI, new FilterCallback () {
			public boolean check ( FromServer obj, String text ) {
				return ( obj.getInt ( "privileges" ) >= User.USER_RESPONSABLE );
			}
		} );

		frame.addPair ( "Referenti", ver.getPersonalizedWidget ( "references", references ) );

		/*
			Di default, l'utente che crea il fornitore ne e' anche referente.
			Il settaggio viene settato dopo l'immissione del widget nel form per
			evitare che il valore venga sovrascritto nel FromServerForm con la lista
			(vuota) di referenti del nuovo fornitore
		*/
		if ( supp.isValid () == false )
			references.addElement ( Session.getUser () );

		frame = new CustomCaptionPanel ( "Contatti" );
		hor.add ( frame );

		frame.addPair ( "Indirizzo", ver.getWidget ( "address" ) );

		frame.addPair ( "Telefono", ver.getWidget ( "phone" ) );
		ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

		frame.addPair ( "Fax", ver.getWidget ( "fax" ) );
		ver.setValidation ( "fax", FromServerValidateCallback.defaultPhoneValidationCallback () );

		frame.addPair ( "Mail", ver.getWidget ( "mail" ) );
		ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

		/* dettagli */

		sframe = new CaptionPanel ( "Descrizione (pubblicamente leggibile)" );
		sframe.add ( ver.getWidget ( "description" ) );
		vertical.add ( sframe );

		sframe = new CaptionPanel ( "Modalità avanzamento ordini" );
		sframe.add ( ver.getWidget ( "order_mode" ) );
		vertical.add ( sframe );

		sframe = new CaptionPanel ( "Modalità pagamento" );
		sframe.add ( ver.getWidget ( "paying_mode" ) );
		vertical.add ( sframe );

		return vertical;
	}

	private Widget productsBuilder ( FromServerForm ver, Supplier supp ) {
		ProductsEditPanel products;
		VerticalPanel container;
		Label notify;

		container = new VerticalPanel ();

		notify = new Label ( "Attenzione: i valori qui riportati non fanno riferimento agli ordini già aperti per il fornitore, per modificare tali dati operare sul pannello \"Gestione Ordini\"" );
		notify.setStyleName ( "smaller-text" );
		container.add ( notify );

		products = new ProductsEditPanel ( supp, supp.isValid () );
		ver.setExtraWidget ( "products", products );
		container.add ( products );

		return container;
	}

	private FromServerForm commonFormBuilder ( FromServer supp ) {
		final FromServerForm ver;
		Supplier supplier;
		TabPanel tabs;
		IconsBar icons;

		supplier = ( Supplier ) supp;

		if ( supp.isValid () == true && supplier.iAmReference () == false )
			return null;

		ver = new FromServerForm ( supplier );

		tabs = new TabPanel ();
		tabs.setWidth ( "100%" );
		tabs.setAnimationEnabled ( true );
		ver.add ( tabs );

		tabs.add ( attributesBuilder ( ver, supplier ), "Dettagli" );
		tabs.add ( productsBuilder ( ver, supplier ), "Prodotti" );

		/*
			La tab dei files viene attivata solo se effettivamente e' concesso il
			caricamento dei files sul server
		*/
		if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
			FilesGroup files;
			files = new FilesGroup ();
			tabs.add ( ver.getPersonalizedWidget ( "files", files ), "Files" );
		}

		tabs.selectTab ( 0 );

		/*
			Sulla fiducia assegno a tutti i fornitori l'icona per cui non ci sono
			prodotti caricati per esso, utile all'utente per identificare la
			situazione anomala. Quando poi arriveranno (nel trattamento degli oggetti
			"Product") provvedo a rimuoverla
		*/
		icons = ver.getIconsBar ();
		icons.addImage ( "images/notifications/supplier_no_products.png" );

		return ver;
	}

	private void checkProductsSchedule () {
		Product product;

		for ( int i = 0; i < scheduledProducts.size (); i++ ) {
			product = ( Product ) scheduledProducts.get ( i );

			if ( insertProduct ( product ) == true ) {
				scheduledProducts.remove ( i );
				i--;
			}
		}
	}

	private ProductsEditPanel retrieveProductsPanel ( Product product ) {
		Supplier supplier;
		FromServerForm supplier_form;

		supplier = ( Supplier ) product.getObject ( "supplier" );
		supplier_form = main.retrieveForm ( supplier );

		if ( supplier_form != null )
			return ( ProductsEditPanel ) supplier_form.retriveInternalWidget ( "products" );
		else
			return null;
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Fornitori";
	}

	public String getSystemID () {
		return "suppliers";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_suppliers.png" );
	}

	/*
		Formato concesso per address:
		suppliers::id_fornitore_da_mostrare
	*/
	public void openBookmark ( String address ) {
		int id;
		String [] tokens;
		FromServerForm form;

		tokens = address.split ( "::" );
		id = Integer.parseInt ( tokens [ 1 ] );

		form = main.retrieveFormById ( id );
		if ( form != null )
			form.open ( true );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Supplier" );
		Utils.getServer ().testObjectReceive ( "Product" );
		Utils.getServer ().testObjectReceive ( "Measure" );
		Utils.getServer ().testObjectReceive ( "Category" );
	}
}
