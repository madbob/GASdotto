/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

/**
	TODO	Per motivi di semplicita' e' stato deciso di trattare in questo pannello sia gli ordini chiusi che
		quelli aperti, per mettere sempre a disposizione strumenti di analisi globale (CSV e PDF) per
		compiere operazioni di revisione. Presto o tardi tali funzionalita' dovrebbero essere integrate
		nell'applicazione vera e propria, ma fino a tal momento ci si accontenta di questa soluzione
		provvisoria. Quando il tutto sara' sistemato, correggere le parti sotto marcate come "RI-CORREGGERE
		QUESTO" decommentando i punti critici ed eliminando il codice temporaneo
*/

public class DeliveryPanel extends GenericPanel {
	private boolean		hasOrders;

	public DeliveryPanel () {
		super ();

		hasOrders = false;
		addTop ( new Label ( "Non ci sono ordini chiusi di cui effettuare consegne" ) );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private void findAndDo ( Order ord, OrderUser uord, int action ) {
				FromServerForm form;

				if ( ord == null )
					return;

				form = ( FromServerForm ) ord.getRelatedInfo ( "DeliveryPanel" );
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
				int index;
				Order ord;
				FromServerForm form;

				ord = ( Order ) object;

				/* RI-CORREGGERE QUESTO */
				/*
				if ( ord.getInt ( "status" ) == Order.CLOSED )
					addTop ( doOrderRow ( ord ) );
				*/

				form = ( FromServerForm ) ord.getRelatedInfo ( "DeliveryPanel" );
				if ( form == null ) {
					index = getSortedPosition ( object );
					insert ( doOrderRow ( ord ), index );
				}
			}

			public void onModify ( FromServer object ) {
				int status;
				int index;
				Order ord;
				FromServerForm form;

				ord = ( Order ) object;

				status = ord.getInt ( "status" );
				form = ( FromServerForm ) ord.getRelatedInfo ( "DeliveryPanel" );

				/* RI-CORREGGERE QUESTO */
				/*
				if ( index != -1 && status == Order.OPENED )
					remove ( index );

				else if ( index == -1 && status == Order.CLOSED ) {
					FromServerForm form;
					ArrayList uorders;
					OrderUser uord;

					form = ( FromServerForm ) doOrderRow ( ord );
					addTop ( form );
					uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );

					for ( int i = 0; i < uorders.size (); i++ ) {
						uord = ( OrderUser ) uorders.get ( i );
						if ( uord.getObject ( "baseorder" ).equals ( object ) )
							syncUserOrder ( form, uord, 0 );
					}
				}
				*/

				ArrayList uorders;
				OrderUser uord;

				if ( form == null ) {
					form = ( FromServerForm ) doOrderRow ( ord );
					addTop ( form );
				}
				else {
					form.refreshContents ( null );
				}

				uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );

				for ( int i = 0; i < uorders.size (); i++ ) {
					uord = ( OrderUser ) uorders.get ( i );
					if ( uord.getObject ( "baseorder" ).equals ( object ) )
						syncUserOrder ( form, uord, 0 );
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerForm form;

				form = ( FromServerForm ) object.getRelatedInfo ( "DeliveryPanel" );

				if ( form != null ) {
					form.invalidate ();
					object.delRelatedInfo ( "DeliveryPanel" );
				}
			}

			protected String debugName () {
				return "DeliveryPanel";
			}
		} );
	}

	private int getSortedPosition ( FromServer object ) {
		int i;
		int num;
		Date cdate;
		Date tdate;
		FromServer object_2;
		FromServerForm iter;

		if ( hasOrders == false )
			return 0;

		tdate = object.getDate ( "shippingdate" );
		if ( tdate == null )
			return 0;

		num = getWidgetCount ();
		if ( object == null )
			return num;

		for ( i = 0; i < num; i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			object_2 = iter.getObject ();

			if ( object_2 != null ) {
				cdate = object_2.getDate ( "shippingdate" );
				if ( cdate != null && cdate.compareTo ( tdate ) > 0 )
					break;
			}
		}

		return i;
	}

	private Widget doOrderRow ( Order order ) {
		HorizontalPanel downloads;
		final FromServerForm ver;
		DeliverySummary summary;
		LinksDialog files;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 0 );
		}

		ver = new FromServerForm ( order, FromServerForm.NOT_EDITABLE );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerForm form ) {
				Order ord;

				ord = ( Order ) form.getObject ();
				ord.asyncLoadUsersOrders ();
			}
		} );

		downloads = new HorizontalPanel ();
		downloads.setStyleName ( "bottom-buttons" );

		files = new LinksDialog ( "Ordinati e Consegnati" );
		files.addLink ( "CSV", "order_csv.php?id=" + order.getLocalID () + "?type=shipped" );
		files.addLink ( "PDF", "delivery_pdf.php?id=" + order.getLocalID () + "?type=shipped" );
		downloads.add ( files );

		files = new LinksDialog ( "Prodotti Prezzati" );
		files.addLink ( "CSV", "order_csv.php?id=" + order.getLocalID () + "?type=saved" );
		files.addLink ( "PDF", "delivery_pdf.php?id=" + order.getLocalID () + "?type=saved" );
		downloads.add ( files );

		files = new LinksDialog ( "Riassunto Prodotti" );
		files.addLink ( "CSV", "products_cvs.php?id=" + order.getLocalID () );
		files.addLink ( "PDF", "products_pdf.php?id=" + order.getLocalID () );
		downloads.add ( files );

		ver.add ( downloads );

		ver.add ( new HTML ( "<hr>" ) );

		summary = new DeliverySummary ();
		ver.setExtraWidget ( "list", summary );
		ver.add ( summary );

		order.addRelatedInfo ( "DeliveryPanel", ver );
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
		int index;
		FromServerForm iter;

		index = -1;

		for ( int i = ( hasOrders == true ? 0 : 1 ); i < getWidgetCount (); i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			if ( iter.isOpen () == true ) {
				index = iter.getObject ().getLocalID ();
				break;
			}
		}

		return Integer.toString ( index );
	}

	public Image getIcon () {
		return new Image ( "images/path_delivery.png" );
	}

	public void initView () {
		ObjectRequest params;

		params = new ObjectRequest ( "Order" );

		/* RI-CORREGGERE QUESTO */
		/*
			params.add ( "status", Order.CLOSED );
		*/

		Utils.getServer ().testObjectReceive ( params );
	}
}
