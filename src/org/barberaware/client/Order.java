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

public class Order extends FromServer implements OrderInterface {
	/*
		Attenzione che questi indici sono cablati anche nella componente server,
		modificare con cautela
	*/

	public static int	OPENED		= 0;
	public static int	CLOSED		= 1;
	public static int	SUSPENDED	= 2;
	public static int	SHIPPED		= 3;

	/*
		Lo stato di auto-sospensione deve essere ultimo, altrimenti la selezione in
		OrdersEditPanel da problemi
	*/
	public static int	AUTO_SUSPEND	= 4;

	public Order () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				Date ship;
				FromServer supplier;
				String sup;
				String start;
				String end;
				String shipping;

				supplier = obj.getObject ( "supplier" );
				if ( supplier == null )
					return "Nuovo Ordine";

				sup = supplier.getString ( "name" );

				start = Utils.printableDate ( obj.getDate ( "startdate" ) );
				end = Utils.printableDate ( obj.getDate ( "enddate" ) );

				ship = obj.getDate ( "shippingdate" );
				if ( ship != null )
					shipping = ", in consegna il " + Utils.printableDate ( ship );
				else
					shipping = "";

				return sup + " (dal " + start + " al " + end + shipping + ")";
			}
		} );

		addAttribute ( "supplier", FromServer.OBJECT, Supplier.class );
		addAttribute ( "products", FromServer.ARRAY, Product.class );
		addAttribute ( "startdate", FromServer.DATE );
		addAttribute ( "enddate", FromServer.DATE );
		addAttribute ( "status", FromServer.INTEGER );
		addAttribute ( "shippingdate", FromServer.DATE );
		addAttribute ( "nextdate", FromServer.STRING );
		addAttribute ( "anticipated", FromServer.PERCENTAGE );
		addAttribute ( "mail_summary_sent", FromServer.DATE );
		addAttribute ( "parent_aggregate", FromServer.BOOLEAN );

		setDate ( "startdate", new Date ( System.currentTimeMillis () ) );
		setInt ( "status", OPENED );

		alwaysReload ( true );
	}

	public static CyclicToggle doOrderStatusSelector ( boolean active ) {
		CyclicToggle status;

		/*
			Nella selezione non appare lo stato 3, usato per l'auto-sospensione
			(nel caso di un ordine con data di apertura nel futuro)
		*/

		status = new CyclicToggle ( active );
		status.addState ( "images/order_status_opened.png" );
		status.addState ( "images/order_status_closed.png" );
		status.addState ( "images/order_status_suspended.png" );
		status.addState ( "images/order_status_shipped.png" );
		status.setDefaultSelection ( 2 );
		return status;
	}

	/****************************************************************** OrderInterface */

	public void asyncLoadUsersOrders () {
		ObjectRequest params;

		params = new ObjectRequest ( "OrderUser" );
		params.add ( "baseorder", getLocalID () );
		Utils.getServer ().testObjectReceive ( params );
	}

	public boolean iAmReference () {
		Supplier supplier;

		supplier = ( Supplier ) getObject ( "supplier" );
		return supplier.iAmReference ();
	}

	/****************************************************************** Comparator */

	public int compare ( Object first, Object second ) {
		int ret;
		FromServer f;
		FromServer s;

		if ( first == null )
			return 1;
		else if ( second == null )
			return -1;

		f = ( FromServer ) first;
		s = ( FromServer ) second;

		ret = -1 * ( f.getDate ( "enddate" ).compareTo ( s.getDate ( "enddate" ) ) );

		if ( ret == 0 )
			return f.getString ( "name" ).compareTo ( s.getString ( "name" ) );
		else
			return ret;
	}
}
