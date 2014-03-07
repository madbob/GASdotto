/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class SupplierUneditableForm extends FromServerForm {
	public SupplierUneditableForm ( Supplier supplier ) {
		super ( supplier, FromServerForm.NOT_EDITABLE );

		String desc;
		CaptionPanel frame;
		CustomCaptionPanel cframe;
		HorizontalPanel hor;
		VerticalPanel column;
		BooleanSelector notifies;
		AddressString addr;
		FilesStaticList files;
		CustomFile products_fake_file;
		ProductsPresentationList products;
		OpenedOrdersList orders;
		PastOrdersList past_orders;
		FromServerTable references;

		emblemsAttach ( Utils.getEmblemsCache ( "supplier" ) );

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		add ( hor );

		column = new VerticalPanel ();
		hor.add ( column );
		hor.setCellWidth ( column, "50%" );

		cframe = new CustomCaptionPanel ( "Attributi" );
		column.add ( cframe );

		cframe.addPair ( "Nome", getPersonalizedWidget ( "name", doString () ) );
		cframe.addPair ( "Nome Contatto", getPersonalizedWidget ( "contact", doString () ) );
		cframe.addPair ( "Calendario Ordini", getPersonalizedWidget ( "orders_months", new MonthsSelector ( false ) ) );

		if ( Session.getGAS ().getBool ( "use_shipping" ) == true )
			cframe.addPair ( "Luogo Consegna", getPersonalizedWidget ( "shipping_manage", Supplier.doSupplierShippingSelector ( false ) ) );

		cframe = new CustomCaptionPanel ( "Configurazioni" );
		column.add ( cframe );

		if ( Session.getGAS ().getBool ( "use_mail" ) == true ) {
			notifies = supplier.doSupplierNotificationsSelector ( Session.getUser () );
			setExtraWidget ( "send_notifies", notifies );
			cframe.addPair ( "Invia Notifiche", notifies );
		}

		cframe = new CustomCaptionPanel ( "Contatti" );
		hor.add ( cframe );
		hor.setCellWidth ( cframe, "50%" );

		addr = new AddressString ();
		addr.setStyleName ( "static-value" );
		cframe.addPair ( "Indirizzo", getPersonalizedWidget ( "address", addr ) );

		cframe.addPair ( "Telefono", getPersonalizedWidget ( "phone", doString () ) );
		cframe.addPair ( "Fax", getPersonalizedWidget ( "fax", doString () ) );
		cframe.addPair ( "Mail", getPersonalizedWidget ( "mail", doString () ) );
		cframe.addPair ( "Sito Web", getPersonalizedWidget ( "website", doString () ) );

		/*
			Dettagli
		*/

		desc = supplier.getString ( "description" );
		if ( desc == "" )
			desc = "Nessuna descrizione disponibile per questo fornitore";
		frame = new CaptionPanel ( "Descrizione" );
		add ( frame );
		frame.add ( new Label ( desc ) );

		orders = new OpenedOrdersList ( supplier );
		setExtraWidget ( "orders", orders );
		frame = new CaptionPanel ( "Storico ultimi 10 ordini" );
		add ( frame );
		frame.add ( orders );

		past_orders = new PastOrdersList ( supplier );
		setExtraWidget ( "past_orders", past_orders );
		frame = new CaptionPanel ( "Ultimi 10 ordini effettuati da me" );
		add ( frame );
		frame.add ( past_orders );

		/*
			Gestione Referenti
			(Solo se l'utente corrente e' amministratore)
		*/

		if ( Session.getUser ().getInt ( "privileges" ) == User.USER_ADMIN ||
				( supplier.iAmReference () == true && supplier.sharingStatus () == ACL.ACL_READONLY ) ) {

			MultiSelector editable_references;
			FilterCallback filter;

			cframe = new CustomCaptionPanel ( "Amministratori" );
			add ( cframe );

			filter = new FilterCallback () {
				public boolean check ( FromServer obj, String text ) {
					int priv;
					priv = obj.getInt ( "privileges" );
					return ( priv == User.USER_RESPONSABLE || priv == User.USER_ADMIN );
				}
			};

			/*
				Questi due MultiSelector sono sbloccati nelle funzioni asincrone
				di SuppliersPanel e SuppliersEditPanel
			*/

			editable_references = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_MULTI, filter );
			cframe.addPair ( "Referenti", this.getPersonalizedWidget ( "references", editable_references ) );

			editable_references = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_MULTI, filter );
			cframe.addPair ( "Addetti Consegne", this.getPersonalizedWidget ( "carriers", editable_references ) );
		}
		else {
			frame = new CaptionPanel ( "Referenti" );
			add ( frame );
			references = new FromServerTable ();
			references.addColumn ( "Nome", "name", false );
			references.addColumn ( "Telefono", "phone", false );
			references.addColumn ( "Mail", "mail", false );
			frame.add ( getPersonalizedWidget ( "references", references ) );
		}

		/*
			Files
		*/

		files = new FilesStaticList ();
		frame = new CaptionPanel ( "Files" );
		add ( frame );
		frame.add ( getPersonalizedWidget ( "files", files ) );

		products_fake_file = new CustomFile ();
		products_fake_file.setString ( "name", "Listino Prodotti CSV" );
		products_fake_file.setString ( "server_path", "server/suppliers_products.php?format=csv&supplier=" + supplier.getLocalID () );
		files.addElement ( products_fake_file );

		products_fake_file = new CustomFile ();
		products_fake_file.setString ( "name", "Listino Prodotti PDF" );
		products_fake_file.setString ( "server_path", "server/suppliers_products.php?format=pdf&supplier=" + supplier.getLocalID () );
		files.addElement ( products_fake_file );

		/*
			Prodotti
		*/

		products = new ProductsPresentationList ( supplier );
		setExtraWidget ( "products", products );
		frame = new CaptionPanel ( "Prodotti" );
		add ( frame );
		frame.add ( products );

		if ( Session.getUser ().getInt ( "privileges" ) == User.USER_ADMIN )
			setEditableMode ( FromServerForm.EDITABLE_UNDELETABLE );
	}

	private Widget doString () {
		StringLabel s;

		s = new StringLabel ();
		s.setStyleName ( "static-value" );
		return s;
	}
}
