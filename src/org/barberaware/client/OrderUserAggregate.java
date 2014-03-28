/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class OrderUserAggregate extends FromServerAggregateVirtual implements OrderUserInterface {
	public OrderUserAggregate () {
		super ( "orders" );

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				FromServer order;

				order = obj.getObject ( "baseorder" );
				if ( order == null )
					return "Nuovo Aggregato";
				else
					return order.getString ( "name" );
			}
		} );

		addWritebackFakeAttribute ( "baseuser", FromServer.OBJECT, User.class, new WritebackInOutClosure () {
			public FromServer retriveObject ( FromServer obj ) {
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null || orders.size () == 0 )
					return null;

				order = ( FromServer ) orders.get ( 0 );
				return order.getObject ( "baseuser" );
			}
		} );

		addWritebackFakeAttribute ( "status", FromServer.INTEGER, new WritebackInOutClosure () {
			public int retriveInteger ( FromServer obj ) {
				int ret;
				int check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return OrderUser.TO_DELIVER;

				ret = 1000;

				for ( int i = 0; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getInt ( "status" );
					if ( check < ret )
						ret = check;
				}

				return ret;
			}
		} );

		addFakeAttribute ( "allproducts", FromServer.ARRAY, new ValueFromObjectClosure () {
			public ArrayList<FromServer> retriveArray ( FromServer obj ) {
				ArrayList<FromServer> ret;
				ArrayList<FromServer> orders;

				ret = new ArrayList<FromServer> ();

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return ret;

				for ( FromServer order : orders )
					ret.addAll ( order.getArray ( "allproducts" ) );

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "deliverydate", FromServer.DATE, new WritebackInOutClosure () {
			public Date retriveDate ( FromServer obj ) {
				int i;
				Date ret;
				Date check;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				i = 0;
				ret = null;

				do {
					order = ( FromServer ) orders.get ( i );
					ret = order.getDate ( "deliverydate" );
					i++;
				} while ( ret == null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "deliverydate" );
					if ( check != null && check.after ( ret ) )
						ret = check;
				}

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "deliveryperson", FromServer.OBJECT, User.class, new WritebackInOutClosure () {
			public FromServer retriveObject ( FromServer obj ) {
				int i;
				Date greater;
				Date check;
				ArrayList orders;
				FromServer order;
				FromServer ret;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				i = 0;
				ret = null;
				greater = null;

				do {
					order = ( FromServer ) orders.get ( i );
					ret = order.getObject ( "deliveryperson" );
					greater = order.getDate ( "deliverydate" );
					i++;
				} while ( greater == null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "deliverydate" );
					if ( check != null && check.after ( greater ) ) {
						greater = check;
						ret = order.getObject ( "deliveryperson" );
					}
				}

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "notes", FromServer.STRING, new WritebackInOutClosure () {
			public String retrieveString ( FromServer obj ) {
				String prev_note;
				String ret;
				String note;
				ArrayList orders;
				FromServer order;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return "";

				ret = "";
				prev_note = "";

				for (int i = 0; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					note = order.getString ( "notes" );

					/*
						Essendo questo un attributo "writeback" alla fine tutti gli OrderUser
						inclusi hanno le stesse note, dunque non posso limitarmi a concatenare
						le stringhe da ciascuno altrimenti mi trovo un test lunghissimo in
						cui si ripete sempre la stessa cosa. Dunque concateno solo le
						stringhe che sono diverse tra loro
					*/

					if ( note.equals ( prev_note ) == false && note != null && note != "" ) {
						ret = ret + "\n" + note;
						prev_note = note;
					}
				}

				return ret;
			}
		} );

		addWritebackFakeAttribute ( "payment_event", FromServer.OBJECT, User.class, new WritebackInOutClosure () {
			public FromServer retriveObject ( FromServer obj ) {
				int i;
				float tot_amount;
				Date greater;
				Date check;
				ArrayList orders;
				FromServer order;
				FromServer greater_bm;
				FromServer ret;

				orders = obj.getArray ( "orders" );
				if ( orders == null )
					return null;

				i = 0;
				tot_amount = 0;
				greater = null;

				do {
					order = ( FromServer ) orders.get ( i );

					greater_bm = order.getObject ( "payment_event" );
					if ( greater_bm != null ) {
						greater = greater_bm.getDate ( "date" );
						tot_amount += greater_bm.getFloat ( "amount" );
					}

					i++;
				} while ( greater == null && i < orders.size () );

				for ( ; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					check = order.getDate ( "deliverydate" );
					if ( check != null && check.after ( greater ) ) {
						greater = check;
						greater_bm = order.getObject ( "payment_event" );
					}

					tot_amount += order.getObject ( "payment_event" ).getFloat ( "amount" );
				}

				ret = new BankMovement ();

				if ( tot_amount > 0 ) {
					ret.setFloat ( "amount", tot_amount );
					ret.setDate ( "date", greater_bm.getDate ( "date" ) );
					ret.setDate ( "registrationdate", greater_bm.getDate ( "registrationdate" ) );
					ret.setObject ( "registrationperson", greater_bm.getObject ( "registrationperson" ) );
					ret.setInt ( "movementtype", greater_bm.getInt ( "movementtype" ) );
					ret.setInt ( "method", greater_bm.getInt ( "method" ) );
				}

				return ret;
			}

			public void setAttribute ( FromServerAggregate parent, String name, Object value ) {
				String notes;
				ArrayList children;
				FromServer child;
				FromServer bm;
				FromServer subbm;

				bm = ( FromServer ) value;

				children = parent.getObjects ();

				if ( children != null ) {
					for ( int i = 0; i < children.size (); i++ ) {
						child = ( FromServer ) children.get ( i );

						subbm = child.getObject ( "payment_event" );
						if ( subbm == null )
							subbm = new BankMovement ();

						subbm.setInt ( "payuser", child.getObject ( "baseuser" ).getLocalID () );
						subbm.setInt ( "paysupplier", child.getObject ( "baseorder" ).getObject ( "supplier" ).getLocalID () );
						subbm.setDate ( "date", bm.getDate ( "date" ) );
						subbm.setDate ( "registrationdate", bm.getDate ( "registrationdate" ) );
						subbm.setObject ( "registrationperson", bm.getObject ( "registrationperson" ) );

						/*
							Qui si assume che tutti i prodotti consegnati siano stati pagati
						*/
						subbm.setFloat ( "amount", ProductUser.sumProductUserArray ( child.getArray ( "allproducts" ), "delivered" ) );

						subbm.setInt ( "movementtype", bm.getInt ( "movementtype" ) );
						subbm.setInt ( "method", bm.getInt ( "method" ) );
						subbm.setString ( "cro", "" );

						notes = bm.getString ( "notes" );
						if ( notes == "" )
							notes = "Pagamento ordine a " + child.getObject ( "baseorder" ).getObject ( "supplier" ).getString ( "name" );

						subbm.setString ( "notes", notes );

						child.setObject ( "payment_event", subbm );
					}
				}
			}
		} );

		addAttribute ( "baseorder", FromServer.OBJECT, OrderAggregate.class );
		addAttribute ( "orders", FromServer.ARRAY, OrderUser.class );
	}

	/*
		Trattandosi questo di un oggetto totalmente virtuale, senza un corrispettivo sul
		database e costruito localmente alla bisogna, viene considerato sempre valido
	*/
	public boolean isValid () {
		return true;
	}

	public void save ( ServerResponse callback ) {
		ArrayList<FromServer> orders;
		ArrayList products;
		FromServer user;

		orders = getArray ( "orders" );
		user = getObject ( "baseuser" );

		for ( FromServer order : orders ) {
			order.setObject ( "baseuser", user );
			products = order.getArray ( "products" );

			if ( products != null && products.size () > 0 )
				order.save ( callback );
			else
				order.destroy ( callback );
		}
	}

	public static OrderUserAggregate retrieveAggregate ( FromServer order, FromServer user ) {
		ArrayList aggregates;
		OrderUserAggregate ret;

		aggregates = Utils.getServer ().getObjectsFromCache ( "OrderUserAggregate" );

		for ( int i = 0; i < aggregates.size (); i++ ) {
			ret = ( OrderUserAggregate ) aggregates.get ( i );
			if ( ret.getObject ( "baseorder" ).equals ( order ) && ret.getObject ( "baseuser" ).equals ( user ) )
				return ret;
		}

		return null;
	}

	/****************************************************************** FromServerAggregate */

	public boolean validateNewChild ( FromServer child ) {
		ArrayList products;

		products = child.getArray ( "products" );

		/*
			Il sotto-ordine non e' valido solo se nuovo e vuoto.
			Se e' esistente e vuoto deve comunque essere inviato
			al backend, per essere eliminato
		*/
		return ( child.getLocalID () != -1 || ( products != null && products.size () > 0 ) );
	}

	/****************************************************************** OrderUserInterface */

	public boolean hasFriends () {
		ArrayList orders;
		OrderUser ord;

		orders = getObjects ();

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( OrderUser ) orders.get ( i );
			if ( ord.hasFriends () == true )
				return true;
		}

		return false;
	}

	public float getTotalPriceWithFriends () {
		float total;
		ArrayList orders;
		OrderUser ord;

		orders = getObjects ();
		total = 0;

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( OrderUser ) orders.get ( i );
			total += ord.getTotalPriceWithFriends ();
		}

		return total;
	}

	public float getDeliveredPriceWithFriends ( boolean today ) {
		float total;
		ArrayList orders;
		OrderUser ord;

		orders = getObjects ();
		total = 0;

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( OrderUser ) orders.get ( i );
			total += ord.getDeliveredPriceWithFriends ( today );
		}

		return total;
	}
}
