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

public class DeliveryPanel extends GenericPanel {
	private boolean		hasOrders;
	private FormGroup	main;
	private OrdersHubWidget	filter;

	public DeliveryPanel () {
		super ();

		main = new FormGroup ( null ) {
			protected FromServerForm doEditableRow ( FromServer obj ) {
				return doOrderRow ( ( Order ) obj );
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

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private void findAndDo ( Order ord, OrderUser uord, int action ) {
				FromServerForm form;

				if ( ord == null )
					return;

				form = main.retrieveForm ( ord );
				if ( form != null )
					syncUserOrder ( form, uord, action );
			}

			public void onReceive ( FromServer object ) {
				findAndDo ( ( Order ) object.getObject ( "baseorder" ), ( OrderUser ) object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndDo ( ( Order ) object.getObject ( "baseorder" ), ( OrderUser ) object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndDo ( ( Order ) object.getObject ( "baseorder" ), ( OrderUser ) object, 2 );
			}

			protected String debugName () {
				return "DeliveryPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Supplier supp;
				FromServerForm form;

				supp = ( Supplier ) object.getObject ( "supplier" );
				if ( supp.iAmReference () == false && supp.iAmCarrier () == false )
					return;

				if ( main.addElement ( object ) == 1 ) {
					if ( object.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false ) {
						form = main.retrieveForm ( object );
						form.setVisible ( false );
					}
				}
			}

			public void onModify ( FromServer object ) {
				int status;
				int index;
				OrderUser uord;
				ArrayList uorders;
				FromServerForm form;

				status = object.getInt ( "status" );
				form = main.retrieveForm ( object );

				if ( form == null ) {
					main.addElement ( object );
					form = main.retrieveForm ( object );
				}
				else {
					form.refreshContents ( null );
				}

				form.emblems ().activate ( "status", object.getInt ( "status" ) );

				uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
				for ( int i = 0; i < uorders.size (); i++ ) {
					uord = ( OrderUser ) uorders.get ( i );
					if ( uord.getObject ( "baseorder" ).equals ( object ) )
						syncUserOrder ( form, uord, 0 );
				}
			}

			public void onDestroy ( FromServer object ) {
				main.deleteElement ( object );
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
				ArrayList forms;
				FromServerForm form;
				FromServer ord;

				forms = main.collectForms ();

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					ord = form.getObject ();

					if ( show == true ) {
						if ( ord.getDate ( "startdate" ).after ( start ) &&
								ord.getDate ( "enddate" ).before ( end ) &&
								( supplier == null || ord.getObject ( "supplier" ).getLocalID () == supplier.getLocalID () ) ) {
							form.setVisible ( true );
						}
						else {
							form.setVisible ( false );
						}
					}
					else {
						if ( ord.getInt ( "status" ) == Order.SHIPPED )
							form.setVisible ( false );
						else
							form.setVisible ( true );
					}
				}
			}
		};

		addTop ( filter );
	}

	private FromServerForm doOrderRow ( Order order ) {
		CaptionPanel frame;
		HorizontalPanel downloads;
		final FromServerForm ver;
		DeliverySummary summary;
		LinksDialog files;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		ver = new FromServerForm ( order, FromServerForm.NOT_EDITABLE );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );
		ver.emblems ().activate ( "status", order.getInt ( "status" ) );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerForm form ) {
				Order ord;

				/**
					TODO	Magari si possono caricare solo gli ordini non ancora consegnati
						(gia' che quelli consegnati sono nascosti alla vista), badare a non
						stravolgere Order.asyncLoadUsersOrders() perche' altra roba dipende
						da li'
				*/
				ord = ( Order ) form.getObject ();
				ord.asyncLoadUsersOrders ();
			}
		} );

		frame = new CaptionPanel ( "Esporta Report" );
		frame.addStyleName ( "print-reports-box" );
		ver.add ( frame );

		downloads = new HorizontalPanel ();

		files = new LinksDialog ( "Ordinati e Consegnati" );
		files.addLink ( "CSV", "order_csv.php?id=" + order.getLocalID () + "&amp;type=shipped" );
		files.addLink ( "PDF", "delivery_pdf.php?id=" + order.getLocalID () + "&amp;type=shipped" );
		downloads.add ( files );

		files = new LinksDialog ( "Prodotti Prezzati" );
		files.addLink ( "CSV", "order_csv.php?id=" + order.getLocalID () + "&amp;type=saved" );
		files.addLink ( "PDF", "delivery_pdf.php?id=" + order.getLocalID () + "&amp;type=saved" );
		downloads.add ( files );

		files = new LinksDialog ( "Dettaglio Consegne" );
		// files.addLink ( "CSV", "delivery_document.php?id=" + order.getLocalID () + "&amp;format=csv&amp;type=delivery" );
		files.addLink ( "PDF", "delivery_document.php?id=" + order.getLocalID () + "&amp;format=pdf&amp;type=delivery" );
		downloads.add ( files );

		files = new LinksDialog ( "Riassunto Prodotti" );
		files.addLink ( "CSV", "products_summary.php?format=csv&amp;id=" + order.getLocalID () );
		files.addLink ( "PDF", "products_summary.php?format=pdf&amp;id=" + order.getLocalID () );
		downloads.add ( files );

		frame.add ( downloads );

		summary = new DeliverySummary ();
		ver.setExtraWidget ( "list", summary );
		ver.add ( summary );

		return ver;
	}

	/*
		action:
			0 = ordine aggiunto
			1 = ordine modificato, aggiorna
			2 = ordine eliminato
	*/
	private void syncUserOrder ( FromServerForm ver, OrderUser uorder, int action ) {
		DeliverySummary summary;

		summary = ( DeliverySummary ) ver.retriveInternalWidget ( "list" );

		switch ( action ) {
			case 0:
				summary.addOrder ( uorder );
				break;
			case 1:
				summary.modOrder ( uorder );
				break;
			case 2:
				summary.delOrder ( uorder );
				break;
			default:
				break;
		}
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
		ObjectRequest params;

		params = new ObjectRequest ( "Order" );
		Utils.getServer ().testObjectReceive ( params );

		filter.doFilter ();
	}
}
