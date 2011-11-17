/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class PlainOrdersBox extends PlainFillBox {
	private class PlainOrderRow {
		private int		expiryMode;
		private FromServer	order;
		private ArrayList	users;
		private ComplexLabel	text;
		private Hidden		id;
		private Label		total;

		public PlainOrderRow ( FromServer o, int em ) {
			order = o;
			expiryMode = em;
			users = new ArrayList ();
		}

		public ArrayList getData () {
			int index;
			String name;
			String id_str;
			ArrayList sub_orders;
			ArrayList data;
			FromServer ord;

			data = new ArrayList ();

			text = new ComplexLabel ();
			text.setContent ( order.getString ( "name" ) );
			checkOrderExpiry ( order, text );
			data.add ( text );

			id_str = null;

			/*
				Nella riga HTML viene immesso un elemento nascosto che riporta l'ID dell'ordine di
				riferimento, o l'elenco di ID degli ordini coinvolti nel caso di un aggregato. In
				questo caso, viene anteposta una "A" per evitare collisioni di ID tra ordini ed
				aggregati.
				Si, lo so, e' un hack bruttissimo: prima o dopo dovro' provvedere ad una soluzione
				piu' seria
			*/

			if ( order.getType () == "Order" ) {
				id_str = Integer.toString ( order.getLocalID () );
			}
			else if ( order.getType () == "OrderAggregate" ) {
				id_str = "A" + Integer.toString ( order.getLocalID () );
				sub_orders = order.getArray ( "orders" );

				for ( int i = 0; i < sub_orders.size (); i++ ) {
					ord = ( FromServer ) sub_orders.get ( i );
					id_str += ":" + Integer.toString ( ord.getLocalID () );

					index = retrieveOrderIndex ( ord );
					if ( index != -1 )
						removeRow ( index );
				}
			}

			id = new Hidden ( "id", id_str );
			data.add ( id );

			total = new Label ();
			total.setStyleName ( "highlight-text" );
			total.addStyleName ( "small-text" );
			data.add ( total );

			return data;
		}

		public void addOrderUser ( FromServer user ) {
			FromServer order;
			FromServer tmp_ou;

			order = user.getObject ( "baseorder" );

			for ( int i = 0; i < users.size (); i++ ) {
				tmp_ou = ( FromServer ) users.get ( i );
				if ( tmp_ou.getObject ( "baseorder" ).equals ( order ) ) {
					users.remove ( tmp_ou );
					break;
				}
			}

			users.add ( user );
			doTotal ();
		}

		public void removeOrderUser ( FromServer user ) {
			if ( users.remove ( user ) == true )
				doTotal ();
		}

		public boolean removeMyself ( FromServer o ) {
			if ( order.equals ( o ) )
				return true;
			else
				return false;
		}

		public boolean hasOrder ( FromServer o ) {
			ArrayList sub_orders;
			FromServer ord;

			if ( order.getType () == "Order" ) {
				return order.equals ( o );
			}
			else {
				if ( o.getType () == "OrderAggregate" )
					return order.equals ( o );

				sub_orders = order.getArray ( "orders" );

				for ( int i = 0; i < sub_orders.size (); i++ ) {
					ord = ( FromServer ) sub_orders.get ( i );
					if ( ord.equals ( o ) )
						return true;
				}

				return false;
			}
		}

		private void checkOrderExpiry ( FromServer order, ComplexLabel text ) {
			long now;
			long d;
			Date date;

			now = System.currentTimeMillis ();
			d = -1;

			if ( expiryMode == 0 ) {
				d = order.getDate ( "enddate" ).getTime ();
			}
			else if ( expiryMode == 1 ) {
				date = order.getDate ( "shippingdate" );
				if ( date != null )
					d = date.getTime ();
			}

			if ( d != -1 && d - now < ( 1000 * 60 * 60 * 24 * 2 ) )
				text.addStyleName ( "highlight-text" );
			else
				text.removeStyleName ( "highlight-text" );
		}

		private void doTotal () {
			float tot;
			OrderUser tmp_ou;

			tot = 0;

			for ( int i = 0; i < users.size (); i++ ) {
				tmp_ou = ( OrderUser ) users.get ( i );
				tot += tmp_ou.getTotalPriceWithFriends ();
			}

			if ( tot == 0 )
				total.setText ( "" );
			else
				total.setText ( " (hai ordinato " + Utils.priceToString ( tot ) + ")" );
		}
	}

	private int		expiryMode;
	private ArrayList	orders;

	public PlainOrdersBox ( int em ) {
		super ();

		orders = new ArrayList ();
		expiryMode = em;
	}

	public int addRow ( FromServer order ) {
		int ret;
		ArrayList sub_orders;
		FromServer ord;
		PlainOrderRow row;

		if ( retrieveOrderRow ( order ) != null )
			return -1;

		if ( order.getType () == "OrderAggregate" ) {
			sub_orders = order.getArray ( "orders" );

			for ( int i = 0; i < sub_orders.size (); i++ ) {
				ord = ( FromServer ) sub_orders.get ( i );
				removeOrder ( ord );
			}
		}

		row = new PlainOrderRow ( order, expiryMode );
		ret = super.addRow ( row.getData () );
		orders.add ( row );
		return ret;
	}

	public void removeOrder ( FromServer order ) {
		PlainOrderRow row;

		for ( int i = 0; i < orders.size (); i++ ) {
			row = ( PlainOrderRow ) orders.get ( i );
			if ( row.hasOrder ( order ) ) {
				if ( row.removeMyself ( order ) == true ) {
					orders.remove ( row );
					removeRow ( i );
				}

				break;
			}
		}
	}

	public void addOrderUser ( FromServer orderuser ) {
		boolean found;
		FromServer order;
		FromServer parent_aggregate;
		FromServerAggregate aggregate;
		ArrayList aggregates;
		PlainOrderRow row;

		order = orderuser.getObject ( "baseorder" );

		row = retrieveOrderRow ( order );
		if ( row == null ) {
			if ( order.getBool ( "parent_aggregate" ) == true ) {
				parent_aggregate = OrderAggregate.retrieveAggregate ( order );

				if ( parent_aggregate != null ) {
					order = parent_aggregate;
				}
				else {
					/*
						Se un Order e' marcato come contenuto in un aggregato, ma l'aggregato
						di riferimento non viene trovato, cambio il suo stato e faccio finta
						di niente. Non e' bello, ma almeno ho una soluzione di fallback per
						le inconsistenze
					*/
					order.setBool ( "parent_aggregate", false );
				}
			}

			row = new PlainOrderRow ( order, expiryMode );

			/*
				Prima si mette nel widget e poi si salva nell'indice locale, onde
				evitare sovrapposizioni
			*/
			super.addRow ( row.getData () );
			orders.add ( row );
		}

		row.addOrderUser ( orderuser );
	}

	public void removeOrderUser ( FromServer orderuser ) {
		FromServer order;
		PlainOrderRow row;

		order = orderuser.getObject ( "baseorder" );

		row = retrieveOrderRow ( order );
		if ( row != null )
			row.removeOrderUser ( orderuser );
	}

	private int retrieveOrderIndex ( FromServer target ) {
		PlainOrderRow row;

		for ( int i = 0; i < orders.size (); i++ ) {
			row = ( PlainOrderRow ) orders.get ( i );
			if ( row.hasOrder ( target ) )
				return i;
		}

		return -1;
	}

	private PlainOrderRow retrieveOrderRow ( FromServer target ) {
		int index;

		index = retrieveOrderIndex ( target );
		if ( index != -1 )
			return ( PlainOrderRow ) orders.get ( index );
		else
			return null;
	}
}
