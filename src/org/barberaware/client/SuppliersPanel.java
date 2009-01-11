/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;

public class SuppliersPanel extends GenericPanel {
	public SuppliersPanel () {
		super ();

		Utils.getServer ().onObjectReceive ( "Supplier", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				insert ( doRow ( ( Supplier ) object ), 0 );
			}
		} );

		Utils.getServer ().onObjectReceive ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				/**
					TODO	Andare a piazzare la notifica di ordine
						disponibile, e magari anche il pulsante per
						accedervi
				*/
			}
		} );

		addFirstTempRow ( new Label ( "Non ci sono fornitori registrati." ) );
	}

	private Widget doRow ( Supplier supplier ) {
		FromServerForm ver;
		HorizontalPanel hor;
		FlexTable fields;

		ver = new FromServerForm ( supplier );

		ver.setAdditionalIconsCallback ( new FromServerFormIcons () {
			public Panel retrive ( FromServer obj ) {
				HorizontalPanel hor;

				hor = new HorizontalPanel ();

				/**
					TODO	Completare creazione icone notifica
				*/

				return hor;
			}
		} );

		hor = new HorizontalPanel ();
		ver.add ( hor );

		fields = new FlexTable ();
		hor.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Nome" ) );
		fields.setWidget ( 0, 1, new Label ( supplier.getString ( "name" ) ) );

		fields = new FlexTable ();
		hor.add ( fields );

		fields.setWidget ( 1, 0, new Label ( "Indirizzo" ) );
		fields.setWidget ( 1, 1, new Label ( supplier.getString ( "address" ) ) );

		ver.add ( new Label ( "Descrizione" ) );
		ver.add ( new Label ( supplier.getString ( "description" ) ) );

		return ver;
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Fornitori";
	}

	public Image getIcon () {
		return new Image ( "images/path_suppliers.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Supplier" );
		Utils.getServer ().testObjectReceive ( "Order" );
	}
}
