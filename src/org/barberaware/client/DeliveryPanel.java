/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
			public void onReceive ( FromServer object ) {
				int index;
				FromServerForm form;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					syncUserOrder ( form, ( OrderUser ) object, 0 );
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				FromServerForm form;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					syncUserOrder ( form, ( OrderUser ) object, 1 );
				}
			}

			public void onDestroy ( FromServer object ) {
				int index;
				FromServerForm form;

				index = retrieveOrderForm ( ( Order ) object.getObject ( "baseorder" ) );
				if ( index != -1 ) {
					form = ( FromServerForm ) getWidget ( index );
					syncUserOrder ( form, ( OrderUser ) object, 2 );
				}
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Order ord;

				ord = ( Order ) object;

				/* RI-CORREGGERE QUESTO */
				/*
				if ( ord.getInt ( "status" ) == Order.CLOSED )
					addTop ( doOrderRow ( ord ) );
				*/
				addTop ( doOrderRow ( ord ) );
			}

			public void onModify ( FromServer object ) {
				int status;
				Order ord;
				int index;

				ord = ( Order ) object;
				status = ord.getInt ( "status" );
				index = retrieveOrderForm ( ord );

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

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveOrderForm ( ( Order ) object );
				if ( index != -1 )
					remove ( index );
			}
		} );
	}

	private Widget doOrderRow ( Order order ) {
		final FromServerForm ver;
		DeliverySummary summary;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		ver = new FromServerForm ( order, FromServerForm.NOT_EDITABLE );

		ver.add ( Utils.getServer ().fileLink ( "Scarica file CSV", "", "order_csv.php?id=" + order.getLocalID () ) );
		ver.add ( Utils.getServer ().fileLink ( "Scarica file PDF", "", "delivery_pdf.php?id=" + order.getLocalID () ) );

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

	private int retrieveOrderForm ( Order parent ) {
		FromServerForm form;
		Order tmp_order;

		for ( int i = ( hasOrders == true ? 1 : 2 ); i < getWidgetCount (); i++ ) {
			form = ( FromServerForm ) getWidget ( i );
			tmp_order = ( Order ) form.getObject ();

			if ( parent.getLocalID () == tmp_order.getLocalID () )
				return i;
		}

		return -1;
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

		for ( int i = ( hasOrders == true ? 1 : 2 ); i < getWidgetCount (); i++ ) {
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
		ServerRequest params;

		params = new ServerRequest ( "Order" );

		/* RI-CORREGGERE QUESTO */
		/*
			params.add ( "status", Order.CLOSED );
		*/

		Utils.getServer ().testObjectReceive ( params );

		Utils.getServer ().testObjectReceive ( "OrderUser" );
	}
}
