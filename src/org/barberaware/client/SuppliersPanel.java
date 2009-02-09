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
import com.google.gwt.user.client.ui.*;

public class SuppliersPanel extends GenericPanel {
	private FormCluster		main;

	public SuppliersPanel () {
		super ();

		main = new FormCluster ( "Supplier", null ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				FromServerForm ver;
				HorizontalPanel hor;
				FlexTable fields;
				Supplier supplier;

				supplier = ( Supplier ) supp;
				ver = new FromServerForm ( supplier, false );

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

				ver.add ( new Label ( supplier.getString ( "description" ) ) );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				/* dummy */
				return null;
			}
		};

		addTop ( main );

		/**
			TODO	Aggiungere lista ordini aperti per ogni fornitore
		*/
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
