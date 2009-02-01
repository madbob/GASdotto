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

	public SuppliersEditPanel () {
		super ();

		main = new FormCluster ( "Supplier", "images/new_supplier.png" ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				final FromServerForm ver;
				HorizontalPanel hor;
				VerticalPanel vertical;
				FlexTable fields;
				Supplier supplier;
				ProductsEditPanel products;
				ReferenceList references;

				supplier = ( Supplier ) supp;
				ver = new FromServerForm ( supplier );

				hor = new HorizontalPanel ();
				ver.add ( hor );

				fields = new FlexTable ();
				hor.add ( fields );

				fields.setWidget ( 0, 0, new Label ( "Nome" ) );
				fields.setWidget ( 0, 1, ver.getWidget ( "name" ) );

				fields.setWidget ( 1, 0, new Label ( "Indirizzo" ) );
				fields.setWidget ( 1, 1, ver.getWidget ( "address" ) );

				fields.setWidget ( 2, 0, new Label ( "Referenti" ) );
				references = new ReferenceList ();
				fields.setWidget ( 2, 1, ver.getPersonalizedWidget ( "references", references ) );

				fields = new FlexTable ();
				hor.add ( fields );

				fields.setWidget ( 0, 0, new Label ( "Nome Contatto" ) );
				fields.setWidget ( 0, 1, ver.getWidget ( "contact" ) );

				fields.setWidget ( 1, 0, new Label ( "Telefono" ) );
				fields.setWidget ( 1, 1, ver.getWidget ( "phone" ) );
				ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

				fields.setWidget ( 2, 0, new Label ( "Fax" ) );
				fields.setWidget ( 2, 1, ver.getWidget ( "fax" ) );
				ver.setValidation ( "fax", FromServerValidateCallback.defaultPhoneValidationCallback () );

				fields.setWidget ( 3, 0, new Label ( "Mail" ) );
				fields.setWidget ( 3, 1, ver.getWidget ( "mail" ) );
				ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

				ver.add ( new Label ( "Descrizione (pubblicamente leggibile)" ) );
				ver.add ( ver.getWidget ( "description" ) );

				ver.add ( new Label ( "Modalità avanzamento ordini" ) );
				ver.add ( ver.getWidget ( "order_mode" ) );

				ver.add ( new Label ( "Modalità pagamento" ) );
				ver.add ( ver.getWidget ( "paying_mode" ) );

				vertical = new VerticalPanel ();
				vertical.addStyleName ( "sub-elements-details" );
				ver.add ( vertical );
				vertical.add ( new Label ( "Prodotti" ) );
				products = new ProductsEditPanel ( supplier );
				ver.setExtraWidget ( "products", products );
				vertical.add ( products );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new Supplier () );
			}

		};

		addTop ( main );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Product product;
				ProductsEditPanel panel;

				product = ( Product ) object;
				panel = retrieveProductsPanel ( product );
				if ( panel != null )
					panel.addProduct ( product );
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

	public Image getIcon () {
		return new Image ( "images/path_suppliers.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Supplier" );
		Utils.getServer ().testObjectReceive ( "Product" );
		Utils.getServer ().testObjectReceive ( "Measure" );
		Utils.getServer ().testObjectReceive ( "Category" );
	}
}
