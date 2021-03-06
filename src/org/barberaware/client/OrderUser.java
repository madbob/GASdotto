/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderUser extends FromServer implements OrderUserInterface {
	public static int	TO_DELIVER		= 0;
	public static int	PARTIAL_DELIVERY	= 1;
	public static int	COMPLETE_DELIVERY	= 2;
	public static int	SAVED			= 3;

	public OrderUser () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				return obj.getObject ( "baseorder" ).getString ( "name" );
			}
		} );

		/*
			L'array "allproducts" e' da usare con estrema cautela, in quanto non si
			mantiene in riferimento tra i prodotti dell'ordine principale e quelli
			dei sotto-ordini degli amici.
			Buono per modificare i prodotti all'interno degli ordini (ad esempio, per
			marcare quelli consegnati) ma non per modificare le liste stesse
		*/
		addFakeAttribute ( "allproducts", FromServer.ARRAY, new ValueFromObjectClosure () {
			public ArrayList<FromServer> retriveArray ( FromServer obj ) {
				ArrayList<FromServer> ret;
				ArrayList<FromServer> friends;

				ret = new ArrayList<FromServer> ();

				friends = obj.getArray ( "products" );
				if ( friends != null )
					ret.addAll ( friends );

				friends = obj.getArray ( "friends" );
				if ( friends != null ) {
					for ( FromServer friend : friends )
						ret.addAll ( friend.getArray ( "products" ) );
				}

				return ret;
			}
		} );

		addAttribute ( "baseuser", FromServer.OBJECT, User.class );
		addAttribute ( "baseorder", FromServer.OBJECT, Order.class );
		addAttribute ( "products", FromServer.ARRAY, ProductUser.class );
		addAttribute ( "friends", FromServer.ARRAY, OrderUserFriend.class );
		addAttribute ( "deliverydate", FromServer.DATE );
		addAttribute ( "deliveryperson", FromServer.OBJECT, User.class );
		addAttribute ( "status", FromServer.INTEGER );
		addAttribute ( "payment_event", FromServer.OBJECT, BankMovement.class );
		addAttribute ( "notes", FromServer.STRING );

		setInt ( "status", TO_DELIVER );

		alwaysReload ( true );
		alwaysSendObject ( "products", true );
		alwaysSendObject ( "friends", true );
		alwaysSendObject ( "payment_event", true );
	}

	/*
		Un OrderUser e' sempre valido, o meglio esiste anche se non e' salvato sul
		server. Questo perche' anche se non viene avanzata nessuna richiesta per un dato
		Order di riferimento comunque l'oggetto e' valido in quanto puo' essere editato
		in un qualsiasi altro momento
	*/
	public boolean isValid () {
		return true;
	}

	public static FromServer findMine ( FromServer base ) {
		ArrayList orders;
		FromServer order;
		FromServer me;
		FromServer ret;
		FromServer parent;

		orders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		me = Session.getUser ();
		ret = null;

		for ( int i = 0; i < orders.size (); i++ ) {
			order = ( FromServer ) orders.get ( i );
			parent = order.getObject ( "baseorder" );

			if ( parent != null && parent.equals ( base ) && order.getObject ( "baseuser" ).equals ( me ) ) {
				ret = order;
				break;
			}
		}

		return ret;
	}

	public float getTotalPrice () {
		ArrayList products;

		products = getArray ( "products" );

		if ( products == null )
			return 0;
		else
			return ProductUser.sumProductUserArray ( products, "quantity" );
	}

	public void setObject ( String name, FromServer value ) {
		if ( name == "payment_event" && value != null ) {
			if ( value.getInt ( "movementtype" ) != BankMovement.ORDER_USER_PAYMENT )
				value.setInt ( "movementtype", BankMovement.ORDER_USER_PAYMENT );
		}

		super.setObject ( name, value );
	}

	/****************************************************************** OrderUserInterface */

	public boolean hasFriends () {
		ArrayList friends;

		friends = getArray ( "friends" );
		return ( friends != null && friends.size () != 0 );
	}

	public float getTotalPriceWithFriends () {
		float total;
		ArrayList friends;
		OrderUserFriend order;

		total = getTotalPrice ();
		friends = getArray ( "friends" );

		for ( int i = 0; i < friends.size (); i++ ) {
			order = ( OrderUserFriend ) friends.get ( i );
			total += order.getTotalPrice ();
		}

		return total;
	}

	public float getDeliveredPriceWithFriends ( boolean today ) {
		float total;

		total = 0;

		/*
			Se sto usando la Gestione Cassa prendo per buono il prezzo pagato indicato
			li', altrimenti faccio il calcolo sui prodotti consegnati
		*/
		if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
			FromServer event;

			event = getObject ( "payment_event" );
			if ( event != null && ( today == false || Utils.dateIsToday ( event.getDate ( "date" ) ) ) )
				total = event.getFloat ( "amount" );
		}
		else {
			ArrayList products;

			products = getArray ( "allproducts" );
			if ( products != null )
				total = ProductUser.sumProductUserArray ( products, "delivered" );
		}

		return total;
	}
}
