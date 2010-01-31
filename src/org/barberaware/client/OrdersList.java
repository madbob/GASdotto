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
	private int		num;

	protected void buildMe ( FromServer supplier, FromServerForm reference ) {
		main = new FlexTable ();
		initWidget ( main );

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
