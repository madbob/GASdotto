/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class DeliveryPanel extends GenericPanel {
	private boolean		hasOrders;
	private FormGroup	main;
	private OrdersHubWidget	filter;

	public DeliveryPanel () {
		super ();

		main = new FormGroup ( null ) {
			protected FromServerForm doEditableRow ( FromServer obj ) {
				return doOrderRow ( obj );
			}

			protected FromServerForm doNewEditableRow () {
				return null;
			}

			protected int sorting ( FromServer first, FromServer second ) {
				Date fdate;
				Date sdate;

				fdate = first.getDate ( "shippingdate" );
				if ( fdate == null )
					return 1;

				sdate = second.getDate ( "shippingdate" );
				if ( sdate == null )
					return -1;

				return sdate.compareTo ( fdate );
			}
		};

		Utils.getServer ().onObjectEvent ( "OrderAggregate", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				ArrayList orders;
				FromServer tmp;

				orders = object.getArray ( "orders" );
				for ( int i = 0; i < orders.size (); i++ ) {
					tmp = ( FromServer ) orders.get ( i );
					main.deleteElement ( tmp );
				}

				main.addElement ( object );
			}

			public void onModify ( FromServer object ) {
				main.refreshElement ( object );
			}

			public void onDestroy ( FromServer object ) {
				String identifier;
				ArrayList orders;
				FromServer order;

				identifier = main.getIdentifier ();
				orders = Utils.getServer ().getObjectsFromCache ( "Order" );

				for ( int i = 0; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					order.delRelatedInfo ( identifier );
				}

				main.deleteElement ( object );
			}

			protected String debugName () {
				return "DeliveryPanel per OrderAggregate";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Supplier supp;
				FromServerRappresentation form;

				if ( object.getBool ( "parent_aggregate" ) == true )
					return;

				supp = ( Supplier ) object.getObject ( "supplier" );
				if ( supp.iAmReference () == false && supp.iAmCarrier () == false )
					return;

				form = main.retrieveForm ( object );
				if ( form == null ) {
					if ( main.addElement ( object ) == 1 ) {
						if ( object.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false ) {
							form = main.retrieveForm ( object );
							form.setVisible ( false );
						}
					}
				}
			}

			public void onModify ( FromServer object ) {
				int status;
				int index;
				OrderUser uord;
				ArrayList uorders;
				FromServerForm form;
				FromServerForm f;

				if ( object.getBool ( "parent_aggregate" ) == true ) {
					onDestroy ( object );
				}
				else {
					status = object.getInt ( "status" );
					form = ( FromServerForm ) main.retrieveForm ( object );

					if ( form == null ) {
						main.addElement ( object );
						form = ( FromServerForm ) main.retrieveForm ( object );
					}

					form.emblems ().activate ( "status", object.getInt ( "status" ) );

					uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
					for ( int i = 0; i < uorders.size (); i++ ) {
						uord = ( OrderUser ) uorders.get ( i );
						if ( uord.getObject ( "baseorder" ).equals ( object ) )
							syncUserOrder ( form, uord, 0 );
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerRappresentation form;

				form = main.retrieveForm ( object );
				if ( form != null && form.getValue ().equals ( object ) )
					main.deleteElement ( object );
			}

			protected String debugName () {
				return "DeliveryPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private void findAndDo ( FromServer uord, int action ) {
				FromServer ord;
				FromServerRappresentation form;

				ord = uord.getObject ( "baseorder" );
				if ( ord == null )
					return;

				if ( action == 0 && ord.getBool ( "parent_aggregate" ) == true )
					return;

				form = main.retrieveForm ( ord );
				if ( form != null )
					syncUserOrder ( form, ( OrderUser ) uord, action );
			}

			public void onReceive ( FromServer object ) {
				findAndDo ( object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndDo ( object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndDo ( object, 2 );
			}

			protected String debugName () {
				return "DeliveryPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUserAggregate", new ServerObjectReceive () {
			private void findAndDo ( FromServer uord, int action ) {
				FromServer ord;
				FromServerRappresentation form;

				ord = uord.getObject ( "baseorder" );
				if ( ord == null )
					return;

				form = main.retrieveForm ( ord );
				if ( form != null )
					syncUserOrder ( form, ( OrderUserAggregate ) uord, action );
			}

			public void onReceive ( FromServer object ) {
				findAndDo ( object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndDo ( object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndDo ( object, 2 );
			}

			protected String debugName () {
				return "DeliveryPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "User", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				/* dummy */
			}

			public void onModify ( FromServer object ) {
				ArrayList forms;
				FromServerForm form;
				DeliverySummary summary;

				forms = main.collectForms ();

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					summary = ( DeliverySummary ) form.retriveInternalWidget ( "list" );
					summary.modUser ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				ArrayList forms;
				FromServerForm form;
				DeliverySummary summary;

				forms = main.collectForms ();

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					summary = ( DeliverySummary ) form.retriveInternalWidget ( "list" );
					summary.delUser ( object );
				}
			}

			protected String debugName () {
				return "DeliveryPanel";
			}
		} );

		addTop ( Utils.getEmblemsCache ( "orders" ).getLegend ( Utils.getEmblemsCache ( "delivery" ) ) );
		addTop ( main );

		hasOrders = false;
		addTop ( new Label ( "Non ci sono ordini chiusi di cui effettuare consegne" ) );

		doFilterOptions ();
	}

	private void doFilterOptions () {
		filter = new OrdersHubWidget () {
			public void doFilter ( boolean show, Date start, Date end, FromServer supplier ) {
				/*
					TODO	Questo e' identico all'omonima
						funzione in OrdersEditPanel
				*/
				boolean visible;
				ArrayList forms;
				FromServerForm form;
				FromServer ord;

				forms = main.collectForms ();

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					ord = form.getValue ();
					visible = false;

					if ( show == true ) {
						if ( ord.getDate ( "startdate" ).after ( start ) && ord.getDate ( "enddate" ).before ( end ) ) {
							if ( supplier == null ) {
								visible = true;
							}
							else {
								if ( ord instanceof Order && ord.getObject ( "supplier" ).getLocalID () == supplier.getLocalID () )
									visible = true;
								else if ( ord instanceof OrderAggregate && ( ( OrderAggregate ) ord ).hasSupplier ( supplier ) )
									visible = true;
							}
						}
					}
					else {
						if ( ord.getInt ( "status" ) != Order.SHIPPED )
							visible = true;
					}

					form.setVisible ( visible );
				}
			}
		};

		addTop ( filter );
	}

	private FromServerForm doOrderRow ( FromServer order ) {
		boolean is_aggregate;
		boolean has_shipping;
		String order_identifier;
		ArrayList orders;
		CaptionPanel frame;
		HorizontalPanel downloads;
		final FromServerForm ver;
		FromServer ord;
		DeliverySummary summary;
		CashCount cash;
		LinksDialog files;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		is_aggregate = ( order.getType () == "OrderAggregate" );
		has_shipping = ( ( Utils.getServer ().getObjectsFromCache ( "ShippingPlace" ).size () != 0 ) &&
					( is_aggregate == false && ( order.getObject ( "supplier" ).getInt ( "shipping_manage" ) == Supplier.SHIPPING_TO_PLACE ) ) );

		ver = new FromServerForm ( order, FromServerForm.NOT_EDITABLE );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );
		ver.emblems ().activate ( "status", order.getInt ( "status" ) );

		if ( is_aggregate == true ) {
			ver.emblems ().activate ( "aggregate" );

			orders = order.getArray ( "orders" );
			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( FromServer ) orders.get ( i );
				ord.addRelatedInfo ( main.getIdentifier (), ver );
			}
		}

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerRappresentationFull form ) {
				FromServer obj;
				Order ord;
				ArrayList orders;

				obj = form.getValue ();

				if ( obj.getType () == "Order" ) {
					ord = ( Order ) obj;
					ord.asyncLoadUsersOrders ();
				}
				else if ( obj.getType () == "OrderAggregate" ) {
					orders = obj.getArray ( "orders" );

					for ( int i = 0; i < orders.size (); i++ ) {
						ord = ( Order ) orders.get ( i );
						ord.asyncLoadUsersOrders ();
					}
				}
			}
		} );

		frame = new CaptionPanel ( "Esporta Report" );
		frame.addStyleName ( "print-reports-box" );
		ver.add ( frame );

		downloads = new HorizontalPanel ();

		order_identifier = Integer.toString ( order.getLocalID () );
		if ( is_aggregate == true )
			order_identifier += "&amp;aggregate=true";

		files = new LinksDialog ( "Ordinati e Consegnati" );
		if ( has_shipping == true )
			files.addHeader ( "Tutti i Luoghi" );
		files.addLink ( "CSV", "order_doc.php?id=" + order_identifier + "&amp;format=csv&amp;type=shipped" );
		files.addLink ( "PDF", "order_doc.php?id=" + order_identifier + "&amp;format=pdf&amp;type=shipped" );
		downloads.add ( files );
		ver.setExtraWidget ( "ordered_files", files );

		files = new LinksDialog ( "Prodotti Prezzati" );
		if ( has_shipping == true )
			files.addHeader ( "Tutti i Luoghi" );
		files.addLink ( "CSV", "order_doc.php?id=" + order_identifier + "&amp;format=csv&amp;type=saved" );
		files.addLink ( "PDF", "order_doc.php?id=" + order_identifier + "&amp;format=pdf&amp;type=saved" );
		downloads.add ( files );
		ver.setExtraWidget ( "priced_files", files );

		files = new LinksDialog ( "Dettaglio Consegne" );
		if ( has_shipping == true )
			files.addHeader ( "Tutti i Luoghi" );
		files.addLink ( "PDF", "delivery_document.php?id=" + order_identifier + "&amp;format=pdf&amp;type=delivery" );
		downloads.add ( files );
		ver.setExtraWidget ( "detailed_files", files );

		/*
			Il "riassunto prodotti", essendo destinato ai singoli fornitori, viene
			separato per singoli ordini se si tratta di un ordine aggregato
		*/

		files = new LinksDialog ( "Riassunto Prodotti" );

		if ( is_aggregate == true ) {
			orders = order.getArray ( "orders" );
			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( FromServer ) orders.get ( i );

				files.addHeader ( ord.getObject ( "supplier" ).getString ( "name" ) );
				files.addLink ( "CSV", "products_summary.php?format=csv&amp;id=" + ord.getLocalID () );
				files.addLink ( "PDF", "products_summary.php?format=pdf&amp;id=" + ord.getLocalID () );
			}
		}
		else {
			files.addLink ( "CSV", "products_summary.php?format=csv&amp;id=" + order_identifier );
			files.addLink ( "PDF", "products_summary.php?format=pdf&amp;id=" + order_identifier );
		}

		downloads.add ( files );

		if ( Session.getGAS ().getBool ( "use_rid" ) ) {
			files = new LinksDialog ( "Genera RID" );
			files.addLink ( "RID", "rid_generator.php?id=" + order_identifier );
			downloads.add ( files );
		}

		frame.add ( downloads );

		summary = new DeliverySummary ();
		ver.setExtraWidget ( "list", summary );
		ver.add ( summary );

		ver.add ( new HTML ( "<hr style=\"margin-top: 20px; margin-bottom: 20px;\" />" ) );

		cash = new CashCount ();
		ver.setExtraWidget ( "cash", cash );
		ver.add ( cash );

		return ver;
	}

	/*
		action:
			0 = ordine aggiunto
			1 = ordine modificato, aggiorna
			2 = ordine eliminato
	*/
	private void syncUserOrder ( FromServerRappresentation ver, OrderUserInterface uorder, int action ) {
		DeliverySummary summary;
		CashCount cash;
		FromServer order;

		summary = ( DeliverySummary ) ver.retriveInternalWidget ( "list" );
		cash = ( CashCount ) ver.retriveInternalWidget ( "cash" );

		switch ( action ) {
			case 0:
				summary.addOrder ( uorder );
				cash.addOrder ( uorder );

				if ( uorder instanceof OrderUser ) {
					order = ( ( OrderUser ) uorder ).getObject ( "baseorder" );

					if ( order.getBool ( "parent_aggregate" ) == false &&
							order.getObject ( "supplier" ).getInt ( "shipping_manage" ) == Supplier.SHIPPING_TO_PLACE ) {
						addShippingPlaceFiles ( ver, ( FromServer ) uorder );
					}
				}

				break;
			case 1:
				summary.modOrder ( uorder );
				cash.modOrder ( uorder );
				break;
			case 2:
				summary.delOrder ( uorder );
				cash.delOrder ( uorder );
				break;
			default:
				break;
		}
	}

	private void addShippingPlaceFiles ( FromServerRappresentation ver, FromServer uorder ) {
		boolean is_aggregate;
		String order_identifier;
		FromServer order;
		FromServer place;
		LinksDialog files;

		place = uorder.getObject ( "baseuser" ).getObject ( "shipping" );
		if ( place == null ) {
			place = ShippingPlace.getDefault ();
			if ( place == null )
				return;
		}

		files = ( LinksDialog ) ver.retriveInternalWidget ( "ordered_files" );
		if ( files.addUniqueHeader ( place.getString ( "name" ) ) == false )
			return;

		order = ver.getValue ();
		is_aggregate = ( order.getType () == "OrderAggregate" );

		order_identifier = Integer.toString ( order.getLocalID () );
		if ( is_aggregate == true )
			order_identifier += "&amp;aggregate=true";

		files.addLink ( "CSV", "order_doc.php?id=" + order_identifier + "&amp;format=csv&amp;type=shipped&amp;location=" + place.getLocalID () );
		files.addLink ( "PDF", "order_doc.php?id=" + order_identifier + "&amp;format=pdf&amp;type=shipped&amp;location=" + place.getLocalID () );

		files = ( LinksDialog ) ver.retriveInternalWidget ( "priced_files" );
		files.addUniqueHeader ( place.getString ( "name" ) );
		files.addLink ( "CSV", "order_doc.php?id=" + order_identifier + "&amp;format=csv&amp;type=saved&amp;location=" + place.getLocalID () );
		files.addLink ( "PDF", "order_doc.php?id=" + order_identifier + "&amp;format=pdf&amp;type=saved&amp;location=" + place.getLocalID () );

		files = ( LinksDialog ) ver.retriveInternalWidget ( "detailed_files" );
		files.addUniqueHeader ( place.getString ( "name" ) );
		files.addLink ( "PDF", "delivery_document.php?id=" + order_identifier + "&amp;format=pdf&amp;type=delivery&amp;location=" + place.getLocalID () );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Consegne";
	}

	public String getSystemID () {
		return "delivery";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_delivery.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "OrderAggregate" );
		Utils.getServer ().testObjectReceive ( "Order" );
		filter.doFilter ();
	}
}
