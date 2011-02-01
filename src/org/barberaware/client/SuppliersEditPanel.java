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

public class SuppliersEditPanel extends GenericPanel {
	private FormCluster		main;

	public SuppliersEditPanel () {
		super ();

		main = new FormCluster ( "Supplier", "Nuovo Fornitore" ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				Supplier supplier;
				FromServerForm ver;

				supplier = ( Supplier ) supp;

				if ( supp.isValid () == false || supplier.iAmReference () == true ||
						( Session.getGAS ().getBool ( "admin_power" ) == true && Session.getUser ().getInt ( "privileges" ) == User.USER_ADMIN ) )
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

					if ( form != null ) {
						products = ( ProductsEditPanel ) form.retriveInternalWidget ( "products" );
						products.enable ( true );
					}
				}
			}

			protected void asyncLoad ( FromServerForm form ) {
				ObjectRequest params;
				Lockable products;
				Lockable references;
				FromServer supplier;

				supplier = form.getObject ();

				products = ( Lockable ) form.retriveInternalWidget ( "products" );
				products.unlock ();

				params = new ObjectRequest ( "Product" );
				params.add ( "supplier", supplier.getLocalID () );
				Utils.getServer ().testObjectReceive ( params );

				params = new ObjectRequest ( "Order" );
				params.add ( "status", "any" );
				params.add ( "supplier", supplier.getLocalID () );
				params.add ( "query_limit", 10 );
				Utils.getServer ().testObjectReceive ( params );

				params = new ObjectRequest ( "OrderUser" );
				params.add ( "all", 1 );
				params.add ( "supplier", supplier.getLocalID () );
				params.add ( "baseuser", Session.getUser ().getLocalID () );
				params.add ( "query_limit", 10 );
				Utils.getServer ().testObjectReceive ( params );
			}
		};

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				SuppliersPanel.sharedOrderManagement ( object, main, 0 );
			}

			public void onModify ( FromServer object ) {
				SuppliersPanel.sharedOrderManagement ( object, main, 1 );
			}

			public void onDestroy ( FromServer object ) {
				SuppliersPanel.sharedOrderManagement ( object, main, 2 );
			}

			protected String debugName () {
				return "SuppliersEditPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				SuppliersPanel.sharedOrderUserManagement ( object, main, 0 );
			}

			public void onModify ( FromServer object ) {
				SuppliersPanel.sharedOrderUserManagement ( object, main, 1 );
			}

			public void onDestroy ( FromServer object ) {
				SuppliersPanel.sharedOrderUserManagement ( object, main, 2 );
			}

			protected String debugName () {
				return "SuppliersEditPanel";
			}
		} );

		addTop ( Utils.getEmblemsCache ( "supplier" ).getLegend () );
		addTop ( main );

		Utils.getServer ().testObjectReceive ( "Supplier" );
	}

	private Widget attributesBuilder ( FromServerForm ver, Supplier supp ) {
		VerticalPanel vertical;
		HorizontalPanel hor;
		MultiSelector references;
		CustomCaptionPanel frame;
		CaptionPanel sframe;
		OpenedOrdersList orders;
		PastOrdersList past_orders;
		FilterCallback filter;

		vertical = new VerticalPanel ();

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		vertical.add ( hor );

		frame = new CustomCaptionPanel ( "Attributi" );
		hor.add ( frame );
		hor.setCellWidth ( frame, "50%" );

		frame.addPair ( "Nome", ver.getWidget ( "name" ) );
		frame.addPair ( "Nome Contatto", ver.getWidget ( "contact" ) );

		filter = new FilterCallback () {
			public boolean check ( FromServer obj, String text ) {
				int priv;
				priv = obj.getInt ( "privileges" );
				return ( priv == User.USER_RESPONSABLE || priv == User.USER_ADMIN );
			}
		};

		references = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_MULTI, filter );
		frame.addPair ( "Referenti", ver.getPersonalizedWidget ( "references", references ) );

		/*
			Di default, l'utente che crea il fornitore ne e' anche referente.
			Il settaggio viene settato dopo l'immissione del widget nel form per
			evitare che il valore venga sovrascritto nel FromServerForm con la lista
			(vuota) di referenti del nuovo fornitore
		*/
		if ( supp.isValid () == false )
			references.addElement ( Session.getUser () );

		references = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_MULTI, filter );
		frame.addPair ( "Addetti Consegne", ver.getPersonalizedWidget ( "carriers", references ) );

		frame.addPair ( "Calendario Ordini", ver.getPersonalizedWidget ( "orders_months", new MonthsSelector ( true ) ) );

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

		sframe = new CaptionPanel ( "Storico ultimi 10 ordini" );
		orders = new OpenedOrdersList ( supp );
		sframe.add ( orders );
		ver.setExtraWidget ( "orders", orders );
		vertical.add ( sframe );

		sframe = new CaptionPanel ( "Ultimi 10 ordini effettuati da me" );
		past_orders = new PastOrdersList ( supp );
		sframe.add ( past_orders );
		ver.setExtraWidget ( "past_orders", past_orders );
		vertical.add ( sframe );

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

		if ( supplier.iAmReference () == true )
			ver.emblems ().activate ( "iamreference" );

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
	}
}
