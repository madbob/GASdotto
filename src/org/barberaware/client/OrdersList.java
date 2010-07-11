/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public abstract class OrdersList extends Composite {
	private FlexTable	main;
	private FromServerForm	mainForm;
	private ArrayList	orders;

	protected void buildMe ( FromServer supplier, FromServerForm reference ) {
		main = new FlexTable ();
		initWidget ( main );

		mainForm = reference;
		orders = new ArrayList ();

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
		EmblemsBar icons;

		main.setWidget ( 0, 0, new Label ( getEmptyNotification () ) );
		orders.clear ();

		icons = mainForm.emblems ();
		icons.deactivate ( getMainIcon () );
	}

	private void doRow ( int row, Order order ) {
		main.setWidget ( row, 0, new Hidden ( "id", Integer.toString ( order.getLocalID () ) ) );
		main.setWidget ( row, 1, new Label ( order.getString ( "name" ) ) );
	}

	public void addOrder ( Order order ) {
		boolean positioned;
		int i;
		Date start;
		Order cmp;
		EmblemsBar icons;

		if ( orders.size () == 0 ) {
			main.removeRow ( 0 );
			icons = mainForm.emblems ();
			icons.activate ( getMainIcon () );

			doRow ( 0, order );
			orders.add ( order );
			return;
		}
		else {
			if ( retrieveOrder ( order ) != -1 )
				return;
		}

		start = order.getDate ( "startdate" );
		positioned = false;

		for ( i = 0; i < orders.size () && i < 10; i++ ) {
			cmp = ( Order ) orders.get ( i );

			if ( cmp.getDate ( "startdate" ).before ( start ) ) {
				main.insertRow ( i );
				doRow ( i, order );
				orders.add ( order );

				if ( orders.size () == 11 ) {
					orders.remove ( 10 );
					main.removeRow ( 10 );
				}

				positioned = true;
				break;
			}
		}

		if ( positioned == false && i < 10 ) {
			doRow ( i, order );
			orders.add ( order );
		}
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
			orders.remove ( index );

			if ( orders.size () == 0 )
				clean ();
		}
	}

	private int retrieveOrder ( Order order ) {
		Date start;
		Order cmp;

		start = order.getDate ( "startdate" );

		for ( int i = 0; i < orders.size (); i++ ) {
			cmp = ( Order ) orders.get ( i );

			if ( cmp.equals ( order ) )
				return i;

			if ( cmp.getDate ( "startdate" ).before ( start ) )
				return -1;
		}

		return -1;
	}

	protected abstract String getEmptyNotification ();
	protected abstract String getMainIcon ();
	protected abstract void checkExistingOrders ( FromServer supplier );

	protected static void configEmblem ( EmblemsInfo info ) {
		/* Non so perche' non posso dichiarare un metodo abstract static... */
	}
}
