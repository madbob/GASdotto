/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		ProductsPresentationList products;
		OpenedOrdersList orders;
		PastOrdersList past_orders;
		FromServerTable references;

		emblemsAttach ( Utils.getEmblemsCache ( "supplier" ) );

		desc = supplier.getString ( "description" );
		if ( desc == "" )
			desc = "Nessuna descrizione disponibile per questo fornitore";
		frame = new CaptionPanel ( "Descrizione" );
		add ( frame );
		frame.add ( new Label ( desc ) );

		desc = supplier.getString ( "orders_months" );
		if ( desc != "" ) {
			frame = new CaptionPanel ( "Mesi Consigliati" );
			add ( frame );
			frame.add ( getPersonalizedWidget ( "orders_months", new MonthsSelector ( false ) ) );
		}

		orders = new OpenedOrdersList ( supplier, this );
		setExtraWidget ( "orders", orders );
		frame = new CaptionPanel ( "Storico ultimi 10 ordini" );
		add ( frame );
		frame.add ( orders );

		past_orders = new PastOrdersList ( supplier, this );
		setExtraWidget ( "past_orders", past_orders );
		frame = new CaptionPanel ( "Ultimi 10 ordini effettuati da me" );
		add ( frame );
		frame.add ( past_orders );

		frame = new CaptionPanel ( "Referenti" );
		add ( frame );
		references = new FromServerTable ();
		references.addColumn ( "Nome", "name", false );
		references.addColumn ( "Telefono", "phone", false );
		references.addColumn ( "Mail", "mail", false );
		frame.add ( getPersonalizedWidget ( "references", references ) );

		if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
			FilesStaticList files;

			files = new FilesStaticList ();
			frame = new CaptionPanel ( "Files" );
			add ( frame );
			frame.add ( getPersonalizedWidget ( "files", files ) );
		}

		products = new ProductsPresentationList ( supplier );
		setExtraWidget ( "products", products );
		frame = new CaptionPanel ( "Prodotti" );
		add ( frame );
		frame.add ( products );
	}
}
