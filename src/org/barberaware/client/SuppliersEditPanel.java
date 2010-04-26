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

public class SuppliersEditPanel extends GenericPanel {
	private FormCluster		main;

	public SuppliersEditPanel () {
		super ();

		main = new FormCluster ( "Supplier", "Nuovo Fornitore" ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				Supplier supplier;
				FromServerForm ver;

				supplier = ( Supplier ) supp;

				if ( supp.isValid () == false || supplier.iAmReference () == true )
					ver = commonFormBuilder ( supp );
				else
					ver = new SupplierUneditableForm ( supplier );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				return commonFormBuilder ( new Supplier () );
			}

			protected void customNew ( FromServer object, boolean true_new ) {
				if ( true_new == false ) {
					FromServerForm form;
					ProductsEditPanel products;

					form = main.retrieveForm ( object );
					products = ( ProductsEditPanel ) form.retriveInternalWidget ( "products" );
					products.enable ( true );
				}
			}

			protected void asyncLoad ( FromServerForm form ) {
				Lockable products;
				products = ( Lockable ) form.retriveInternalWidget ( "products" );
				products.unlock ();
			}
		};

		addTop ( main );

		Utils.getServer ().testObjectReceive ( "Supplier" );
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
		hor.setCellWidth ( frame, "50%" );

		frame.addPair ( "Nome", ver.getWidget ( "name" ) );
		frame.addPair ( "Nome Contatto", ver.getWidget ( "contact" ) );

		references = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_MULTI, new FilterCallback () {
			public boolean check ( FromServer obj, String text ) {
				int priv;
				priv = obj.getInt ( "privileges" );
				return ( priv != User.USER_LEAVED && priv >= User.USER_RESPONSABLE );
			}
		} );
		frame.addPair ( "Referenti", ver.getPersonalizedWidget ( "references", references ) );

		frame.addPair ( "Mesi Suggeriti per Ordini", ver.getPersonalizedWidget ( "orders_months", new MonthsSelector ( true ) ) );

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
		hor.setCellWidth ( frame, "50%" );

		frame.addPair ( "Indirizzo", ver.getWidget ( "address" ) );

		frame.addPair ( "Telefono", ver.getWidget ( "phone" ) );
		ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

		frame.addPair ( "Fax", ver.getWidget ( "fax" ) );
		ver.setValidation ( "fax", FromServerValidateCallback.defaultPhoneValidationCallback () );

		frame.addPair ( "Mail", ver.getWidget ( "mail" ) );
		ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

		frame.addPair ( "Sito Web", ver.getWidget ( "website" ) );

		/* dettagli */

		sframe = new CaptionPanel ( "Descrizione (pubblicamente leggibile)" );
		sframe.add ( ver.getWidget ( "description" ) );
		vertical.add ( sframe );

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		vertical.add ( hor );

		sframe = new CaptionPanel ( "Modalità avanzamento ordini" );
		sframe.add ( ver.getWidget ( "order_mode" ) );
		hor.add ( sframe );
		hor.setCellWidth ( sframe, "50%" );

		sframe = new CaptionPanel ( "Modalità pagamento" );
		sframe.add ( ver.getWidget ( "paying_mode" ) );
		hor.add ( sframe );
		hor.setCellWidth ( sframe, "50%" );

		return vertical;
	}

	private Widget productsBuilder ( FromServerForm ver, Supplier supp ) {
		ProductsEditPanel products;
		VerticalPanel container;
		Label notify;

		container = new VerticalPanel ();

		/**
			TODO	Questa notifica si potrebbe visualizzare solo se ci sono ordini
				effettivamente aperti per il fornitore
		*/
		notify = new Label ( "Attenzione: i valori qui riportati per i prodotti esistenti non fanno riferimento agli ordini già aperti, per modificare tali dati operare sul pannello \"Gestione Ordini\"." );
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

		supplier = ( Supplier ) supp;

		ver = new FromServerForm ( supplier );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "supplier" ) );

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

		return ver;
	}

	private FromServerArray retrieveProductsPanel ( Product product ) {
		FromServer supplier;
		FromServerForm supplier_form;

		supplier = product.getObject ( "supplier" );
		if ( supplier == null )
			return null;

		supplier_form = main.retrieveForm ( supplier );

		if ( supplier_form != null )
			return ( FromServerArray ) supplier_form.retriveInternalWidget ( "products" );
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
	}
}
