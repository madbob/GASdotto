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

import com.allen_sauer.gwt.log.client.Log;

public class SuppliersPanel extends GenericPanel {
	private abstract class OrdersList extends Composite {
		private FlexTable	main;
		private FromServerForm	mainForm;
		private int		num;

		protected void buildMe ( FromServer supplier, FromServerForm reference, boolean link_to_order ) {
			main = new FlexTable ();
			initWidget ( main );

			if ( link_to_order ) {
				main.addTableListener ( new TableListener () {
					public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
						Hidden id;

						if ( row == 0 )
							return;

						id = ( Hidden ) main.getWidget ( row, 0 );
						goTo ( "orders::" + id.getValue () );
					}
				} );
			}

			mainForm = reference;

			clean ();

			/*
				Problema: il monitor registrato sugli Order non basta, in quanto
				viene eseguito prima che vengano creati i form dei fornitori e
				gli ordini non vengono dunque assegnati correttamente. Pertanto
				qui rieseguo il controllo su tutti gli ordini in cache e popolo
				il OrdersList
			*/
			checkExistingOrders ( supplier );
		}

		private void clean () {
			IconsBar icons;

			main.setWidget ( 0, 0, new Label ( getEmptyNotification () ) );
			num = 0;

			icons = mainForm.getIconsBar ();
			icons.delImage ( getMainIcon () );
		}

		public void addOrder ( Order order ) {
			if ( num == 0 ) {
				IconsBar icons;

				main.removeRow ( 0 );
				icons = mainForm.getIconsBar ();
				icons.addImage ( getMainIcon () );
			}
			else {
				if ( retrieveOrder ( order ) != -1 )
					return;
			}

			main.setWidget ( num, 0, new Hidden ( "id", Integer.toString ( order.getLocalID () ) ) );
			main.setWidget ( num, 1, new Label ( order.getString ( "name" ) ) );
			num++;
		}

		private int retrieveOrder ( Order order ) {
			int rows;
			String id;
			Hidden existing_id;

			if ( num == 0 )
				return -1;

			rows = main.getRowCount ();
			id = Integer.toString ( order.getLocalID () );

			for ( int i = 0; i < rows; i++ ) {
				existing_id = ( Hidden ) main.getWidget ( i, 0 );
				if ( existing_id.getValue ().equals ( id ) )
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
			else
				addOrder ( order );
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

		protected abstract String getEmptyNotification ();
		protected abstract String getMainIcon ();
		protected abstract void checkExistingOrders ( FromServer supplier );
	}

	private class OpenedOrdersList extends OrdersList {
		public OpenedOrdersList ( FromServer supplier, FromServerForm reference ) {
			buildMe ( supplier, reference, true );
		}

		protected String getEmptyNotification () {
			return "Non ci sono ordini aperti per questo fornitore";
		}

		protected String getMainIcon () {
			return "images/notifications/supplier_having_orders.png";
		}

		protected void checkExistingOrders ( FromServer supplier ) {
			ArrayList list;
			FromServer ord;
			int supp_id;

			list = Utils.getServer ().getObjectsFromCache ( "Order" );
			supp_id = supplier.getLocalID ();

			for ( int i = 0; i < list.size (); i++ ) {
				ord = ( FromServer ) list.get ( i );

				if ( ord.getInt ( "status" ) == Order.OPENED &&
						ord.getObject ( "supplier" ).getLocalID () == supp_id )
					addOrder ( ( Order ) ord );
			}
		}
	}

	private class PastOrdersList extends OrdersList {
		public PastOrdersList ( FromServer supplier, FromServerForm reference ) {
			buildMe ( supplier, reference, false );
		}

		protected String getEmptyNotification () {
			return "Non sono mai stati eseguiti ordini per questo fornitore";
		}

		protected String getMainIcon () {
			return "images/notifications/supplier_having_past_orders.png";
		}

		protected void checkExistingOrders ( FromServer supplier ) {
			ArrayList list;
			FromServer ord;
			Order base_ord;
			int supp_id;

			list = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
			supp_id = supplier.getLocalID ();

			for ( int i = 0; i < list.size (); i++ ) {
				ord = ( FromServer ) list.get ( i );
				base_ord = ( Order ) ord.getObject ( "baseorder" );

				if ( base_ord.getInt ( "status" ) == Order.CLOSED &&
						base_ord.getObject ( "supplier" ).getLocalID () == supp_id )
					addOrder ( base_ord );
			}
		}
	}

	private FormCluster		main;
	private ArrayList		scheduledProducts;

	public SuppliersPanel () {
		super ();

		scheduledProducts = new ArrayList ();

		main = new FormCluster ( "Supplier", null ) {
			protected FromServerForm doEditableRow ( FromServer supp ) {
				String desc;
				CaptionPanel frame;
				FromServerForm ver;
				Supplier supplier;
				ProductsPresentationList products;
				OpenedOrdersList orders;
				PastOrdersList past_orders;

				supplier = ( Supplier ) supp;
				ver = new FromServerForm ( supplier, FromServerForm.NOT_EDITABLE );

				desc = supplier.getString ( "description" );
				if ( desc == "" )
					desc = "Nessuna descrizione disponibile per questo fornitore";
				frame = new CaptionPanel ( "Descrizione" );
				ver.add ( frame );
				frame.add ( new Label ( desc ) );

				products = new ProductsPresentationList ( supplier );
				ver.setExtraWidget ( "products", products );
				frame = new CaptionPanel ( "Prodotti" );
				ver.add ( frame );
				frame.add ( products );

				orders = new OpenedOrdersList ( supp, ver );
				ver.setExtraWidget ( "orders", orders );
				frame = new CaptionPanel ( "Ordini correntemente aperti" );
				ver.add ( frame );
				frame.add ( orders );

				past_orders = new PastOrdersList ( supp, ver );
				ver.setExtraWidget ( "past_orders", past_orders );
				frame = new CaptionPanel ( "Ordini effettuati" );
				ver.add ( frame );
				frame.add ( past_orders );

				if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
					FilesStaticList files;

					files = new FilesStaticList ();
					frame = new CaptionPanel ( "Files" );
					ver.add ( frame );
					frame.add ( ver.getPersonalizedWidget ( "files", files ) );
				}

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				/*
					Il pannello qui descritto serve solo per mostrare la
					lista di fornitori, dunque nessun form di creazione e'
					previsto
				*/
				return null;
			}

			protected void customNew ( FromServer object, boolean true_new ) {
				checkProductsSchedule ();
			}
		};

		addTop ( main );

		/*
			Questo viene eseguito appunto quando arriva qualche dato nuovo dal
			server, non per inizializzare pannelli esistenti.
			Cfr. OpenedOrdersList::OpenedOrdersList()
		*/
		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm form;

				if ( object.getInt ( "status" ) != Order.OPENED )
					return;

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

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				FromServerForm form;
				Order ord;

				if ( object.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					return;

				ord = ( Order ) object.getObject ( "baseorder" );
				form = main.retrieveForm ( ord.getObject ( "supplier" ) );

				if ( form != null ) {
					PastOrdersList list;

					list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );
					list.addOrder ( ord );
				}
			}

			public void onModify ( FromServer object ) {
				FromServerForm form;
				Order ord;

				ord = ( Order ) object.getObject ( "baseorder" );
				form = main.retrieveForm ( ord.getObject ( "supplier" ) );

				if ( form != null ) {
					PastOrdersList list;
					list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );
					list.modOrder ( ord );
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerForm form;
				Order ord;

				ord = ( Order ) object.getObject ( "baseorder" );
				form = main.retrieveForm ( ord.getObject ( "supplier" ) );

				if ( form != null ) {
					PastOrdersList list;
					list = ( PastOrdersList ) form.retriveInternalWidget ( "past_orders" );
					list.delOrder ( ord );
				}
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Product prod;

				prod = ( Product ) object;
				if ( insertProduct ( prod ) == false )
					scheduledProducts.add ( prod );
			}

			public void onModify ( FromServer object ) {
				Product prod;
				ProductsPresentationList panel;

				prod = ( Product ) object;
				panel = retrieveProductsPanel ( prod );
				if ( panel == null )
					panel.refreshProduct ( prod );
			}

			public void onDestroy ( FromServer object ) {
				Product prod;
				ProductsPresentationList panel;

				prod = ( Product ) object;
				panel = retrieveProductsPanel ( prod );
				if ( panel == null )
					panel.deleteProduct ( prod );
			}

			protected String debugName () {
				return "SuppliersPanel";
			}
		} );

		Utils.getServer ().testObjectReceive ( "Supplier" );
	}

	private ProductsPresentationList retrieveProductsPanel ( Product product ) {
		Supplier supplier;
		FromServerForm supplier_form;

		supplier = ( Supplier ) product.getObject ( "supplier" );
		supplier_form = main.retrieveForm ( supplier );

		if ( supplier_form != null )
			return ( ProductsPresentationList ) supplier_form.retriveInternalWidget ( "products" );
		else
			return null;
	}

	private boolean insertProduct ( Product product ) {
		ProductsPresentationList panel;

		panel = retrieveProductsPanel ( product );
		if ( panel != null ) {
			panel.addProduct ( product );
			return true;
		}
		else
			return false;
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

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Fornitori";
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
		Utils.getServer ().testObjectReceive ( "Order" );
		Utils.getServer ().testObjectReceive ( "OrderUser" );
	}
}
