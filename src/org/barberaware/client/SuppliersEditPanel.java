/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class SuppliersEditPanel extends GenericPanel {
	private FormCluster		main;
	private CheckBox		toggleHiddenView;

	public SuppliersEditPanel () {
		super ();

		main = new FormCluster ( "Supplier", "Nuovo Fornitore" ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				Supplier supplier;
				FromServerForm ver;

				supplier = ( Supplier ) supp;

				if ( ( supp.isValid () == false || supplier.iAmReference () == true ) && supplier.sharingStatus () <= ACL.ACL_READWRITE )
					ver = commonFormBuilder ( supp );
				else
					ver = new SupplierUneditableForm ( supplier );

				if ( supp.getBool ( "hidden" ) == true && toggleHiddenView.isChecked () == false )
					ver.setVisible ( false );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				return commonFormBuilder ( new Supplier () );
			}

			protected void customNew ( FromServer object, boolean true_new ) {
				if ( true_new == false ) {
					FromServerRappresentation form;
					ProductsEditPanel products;

					form = main.retrieveForm ( object );

					if ( form != null ) {
						products = ( ProductsEditPanel ) form.retriveInternalWidget ( "products" );
						products.enable ( true );
					}
				}
			}

			protected void customModify ( FromServer object, FromServerRappresentation form ) {
				FromServerForm f;
				Supplier supp;

				if ( form == null )
					return;

				supp = ( Supplier ) object;
				f = ( FromServerForm ) form;

				if ( ( supp.iAmReference () == true && form instanceof SupplierUneditableForm ) ||
						( supp.iAmReference () == false && ( ( form instanceof SupplierUneditableForm ) == false ) ) ) {
					main.deleteElement ( object );
					main.addElement ( object );
				}

				if ( supp.getBool ( "hidden" ) == true ) {
					f.emblems ().activate ( "hidden" );
					form.setVisible ( toggleHiddenView.isChecked () );
				}
				else {
					f.emblems ().deactivate ( "hidden" );
					form.setVisible ( true );
				}
			}

			protected void asyncLoad ( FromServerForm form ) {
				ObjectRequest params;
				Lockable products;
				Lockable references;
				FromServer supplier;

				supplier = form.getValue ();

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

		doFilterOptions ();

		Utils.getServer ().testObjectReceive ( "Supplier" );
	}

	private void doFilterOptions () {
		HorizontalPanel pan;

		pan = new HorizontalPanel ();
		pan.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_LEFT );
		pan.setStyleName ( "panel-up" );
		addTop ( pan );

		toggleHiddenView = new CheckBox ( "Mostra Fornitori Disattivati" );
		toggleHiddenView.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				boolean show;
				ArrayList forms;
				CheckBox myself;
				FromServerForm form;

				myself = ( CheckBox ) event.getSource ();
				forms = main.collectForms ();
				show = myself.isChecked ();

				if ( show == true ) {
					ObjectRequest params;
					params = new ObjectRequest ( "Supplier" );
					params.add ( "hidden", "true" );
					Utils.getServer ().testObjectReceive ( params );
				}

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					if ( form.getValue ().getBool ( "hidden" ) == true )
						form.setVisible ( show );
				}
			}
		} );
		pan.add ( toggleHiddenView );
	}

	private Widget attributesBuilder ( FromServerForm ver, Supplier supp ) {
		VerticalPanel vertical;
		HorizontalPanel hor;
		VerticalPanel column;
		FlexTable payments;
		MultiSelector references;
		BooleanSelector notifies;
		CustomCaptionPanel frame;
		CaptionPanel sframe;
		OpenedOrdersList orders;
		PastOrdersList past_orders;
		FilterCallback filter;

		vertical = new VerticalPanel ();

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		vertical.add ( hor );

		column = new VerticalPanel ();
		hor.add ( column );
		hor.setCellWidth ( column, "50%" );

		frame = new CustomCaptionPanel ( "Attributi" );
		column.add ( frame );

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

		if ( Session.getGAS ().getBool ( "use_shipping" ) == true )
			frame.addPair ( "Luogo Consegna", ver.getPersonalizedWidget ( "shipping_manage", Supplier.doSupplierShippingSelector ( true ) ) );

		frame.addPair ( "Calendario Ordini", ver.getPersonalizedWidget ( "orders_months", new MonthsSelector ( true ) ) );
		frame.addPair ( "Codice Fiscale", ver.getWidget ( "tax_code" ) );
		frame.addPair ( "Partita IVA", ver.getWidget ( "vat_number" ) );
		frame.addPair ( "Inattivo", ver.getWidget ( "hidden" ) );

		frame = new CustomCaptionPanel ( "Configurazioni" );
		column.add ( frame );

		if ( Session.getGAS ().getBool ( "use_mail" ) == true ) {
			notifies = supp.doSupplierNotificationsSelector ( Session.getUser () );
			ver.setExtraWidget ( "send_notifies", notifies );
			frame.addPair ( "Invia Notifiche", notifies );
		}

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
		payments = new FlexTable ();
		sframe.add ( payments );
		payments.setWidget ( 0, 0, ver.getWidget ( "paying_mode" ) );
		payments.getFlexCellFormatter ().setColSpan ( 0, 0, 2 );

		if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
			FilteredMovementsSummary movements;

			payments.setWidget ( 1, 0, new Label ( "Accetta Bonifico" ) );
			payments.setWidget ( 1, 1, ver.getWidget ( "paying_by_bank" ) );
			payments.getCellFormatter ().addStyleName ( 1, 0, "custom-label" );

			payments.setWidget ( 2, 0, new Label ( "Totale da Ricevere" ) );
			payments.setWidget ( 2, 1, ver.getPersonalizedWidget ( "current_balance", new PriceViewer () ) );
			payments.getCellFormatter ().addStyleName ( 2, 0, "custom-label" );

			movements = new FilteredMovementsSummary ( null, supp );
			payments.setWidget ( 3, 0, movements );
			payments.getFlexCellFormatter ().setColSpan ( 3, 0, 2 );
			movements.refresh ();
		}

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
		VerticalPanel files_pan;
		final FromServerForm ver;
		Supplier supplier;
		TabPanel tabs;
		EmblemsBar bar;
		LinksDialog productslist;

		supplier = ( Supplier ) supp;

		ver = new FromServerForm ( supplier );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "supplier" ) );

		bar = ver.emblems ();

		if ( supplier.iAmReference () == true )
			bar.activate ( "iamreference" );

		if ( supplier.getBool ( "hidden" ) == true )
			bar.activate ( "hidden" );

		tabs = new TabPanel ();
		tabs.setWidth ( "100%" );
		tabs.setAnimationEnabled ( true );
		ver.add ( tabs );

		tabs.add ( attributesBuilder ( ver, supplier ), "Dettagli" );
		tabs.add ( productsBuilder ( ver, supplier ), "Prodotti" );

		files_pan = new VerticalPanel ();
		tabs.add ( files_pan, "Files" );

		productslist = new LinksDialog ( "Listino Prodotti" );
		productslist.addLink ( "CSV", "suppliers_products.php?supplier=" + supplier.getLocalID () );
		files_pan.add ( productslist );

		if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
			FilesGroup files;

			files = new FilesGroup ();
			files_pan.add ( ver.getPersonalizedWidget ( "files", files ) );
		}

		tabs.selectTab ( 0 );
		return ver;
	}

	private FromServerArray retrieveProductsPanel ( Product product ) {
		FromServer supplier;
		FromServerRappresentation supplier_form;

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

		form = ( FromServerForm ) main.retrieveFormById ( id );
		if ( form != null )
			form.open ( true );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Supplier" );
	}
}
