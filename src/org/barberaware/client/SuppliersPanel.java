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
	private class OpenedOrdersList extends Composite {
		private FlexTable	main;
		private FromServerForm	mainForm;
		private int		num;

		public OpenedOrdersList ( FromServerForm reference ) {
			main = new FlexTable ();
			initWidget ( main );

			mainForm = reference;

			clean ();
		}

		private void clean () {
			IconsBar icons;

			main.setWidget ( 0, 0, new Label ( "Non ci sono ordini aperti per questo fornitore" ) );
			num = 0;

			icons = mainForm.getIconsBar ();
			icons.delImage ( "images/notifications/supplier_having_orders.png" );
		}

		public void addOrder ( Order order ) {
			if ( num == 0 ) {
				IconsBar icons;

				main.removeRow ( 0 );
				main.setWidget ( 0, 0, new Label ( "Ordini aperti:" ) );

				icons = mainForm.getIconsBar ();
				icons.addImage ( "images/notifications/supplier_having_orders.png" );
			}

			num++;

			/**
				TODO	Rendere gli ordini cliccabili
			*/
			main.setWidget ( num, 0, new Label ( order.getString ( "name" ) ) );
		}

		private int retrieveOrder ( Order order ) {
			int rows;
			String name;
			Label text;

			rows = main.getRowCount ();
			name = order.getString ( "name" );

			for ( int i = 1; i < rows; i++ ) {
				text = ( Label ) main.getWidget ( i, 0 );
				if ( text.equals ( name ) )
					return i;
			}

			return -1;
		}

		public void modOrder ( Order order ) {
			int index;
			Label label;

			index = retrieveOrder ( order );

			if ( index != -1 ) {
				label = ( Label ) main.getWidget ( index, 0 );
				label.setText ( order.getString ( "name" ) );
			}
		}

		public void delOrder ( Order order ) {
			int index;

			index = retrieveOrder ( order );

			if ( index != -1 ) {
				main.removeRow ( index );
				num--;

				if ( num == 0 )
					clean ();
			}
		}
	}

	private FormCluster		main;

	public SuppliersPanel () {
		super ();

		main = new FormCluster ( "Supplier", null ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				FromServerForm ver;
				HorizontalPanel hor;
				FlexTable fields;
				Supplier supplier;
				OpenedOrdersList orders;

				supplier = ( Supplier ) supp;
				ver = new FromServerForm ( supplier, FromServerForm.NOT_EDITABLE );

				ver.add ( new Label ( supplier.getString ( "description" ) ) );

				orders = new OpenedOrdersList ( ver );

				ver.setExtraWidget ( "orders", orders );
				ver.add ( orders );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				/* dummy */
				return null;
			}
		};

		addTop ( main );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm form;

				form = main.retrieveForm ( object.getObject ( "supplier" ) );

				if ( form != null ) {
					OpenedOrdersList list;
					list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );
					list.addOrder ( ( Order ) object );
				}
			}

			public void onModify ( FromServer object ) {
				FromServerForm form;

				form = main.retrieveForm ( object.getObject ( "supplier" ) );

				if ( form != null ) {
					OpenedOrdersList list;
					list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );
					list.modOrder ( ( Order ) object );
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerForm form;

				form = main.retrieveForm ( object.getObject ( "supplier" ) );

				if ( form != null ) {
					OpenedOrdersList list;
					list = ( OpenedOrdersList ) form.retriveInternalWidget ( "orders" );
					list.delOrder ( ( Order ) object );
				}
			}
		} );

		/**
			TODO	Aggiungere informazione su stato ordini aperti in passato e nel
				presente dall'utente corrente
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
