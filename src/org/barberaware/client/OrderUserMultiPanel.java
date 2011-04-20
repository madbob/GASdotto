/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class OrderUserMultiPanel extends Composite implements ObjectWidget, OrderUserManagerMode, Lockable {
	private ProductsUserSelection	selection;
	private DummyTextArea		notes;
	private UserSelector		user;
	private Label			noedit;
	private FromServer		baseOrder;

	private boolean			editable;
	private FromServer		currentValue;

	public OrderUserMultiPanel ( FromServer order, boolean edit, boolean freedit ) {
		Label header;
		VerticalPanel main;
		HorizontalPanel pan;

		main = new VerticalPanel ();
		initWidget ( main );

		editable = edit;
		baseOrder = order;

		pan = new HorizontalPanel ();
		pan.setStyleName ( "highlight-part" );
		main.add ( pan );

		header = new Label ( "Ordine eseguito a nome di " );
		pan.add ( header );
		pan.setCellVerticalAlignment ( header, HasVerticalAlignment.ALIGN_MIDDLE );

		user = new UserSelector ();
		pan.add ( user );

		noedit = new Label ( "Questo ordine non può essere modificato perché contiene sotto-ordini" );
		noedit.setStyleName ( "smaller-text" );
		noedit.setVisible ( false );
		pan.add ( noedit );
		pan.setCellVerticalAlignment ( noedit, HasVerticalAlignment.ALIGN_MIDDLE );

		user.addFilter ( new FromServerValidateCallback () {
			public boolean checkObject ( FromServer object ) {
				if ( object.getInt ( "privileges" ) == User.USER_LEAVED )
					return false;
				else
					return true;
			}
		} );

		user.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				retrieveCurrentOrderByUser ();
			}
		} );

		selection = new ProductsUserSelection ( baseOrder.getArray ( "products" ), edit, freedit );
		main.add ( selection );

		notes = new DummyTextArea ();
		main.add ( notes );
	}

	private void retrieveCurrentOrderByUser () {
		int num;
		ArrayList uorders;
		FromServer u;
		FromServer uorder;

		u = user.getValue ();

		uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		num = uorders.size ();

		for ( int i = 0; i < num; i++ ) {
			uorder = ( FromServer ) uorders.get ( i );

			if ( u.equals ( uorder.getObject ( "baseuser" ) ) && baseOrder.equals ( uorder.getObject ( "baseorder" ) ) ) {
				setValue ( uorder );
				return;
			}
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseuser", u );
		uorder.setObject ( "baseorder", baseOrder );
		setValue ( uorder );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		ArrayList friends;

		currentValue = element;
		user.setValue ( currentValue.getObject ( "baseuser" ) );
		selection.setElements ( currentValue.getArray ( "products" ) );
		notes.setValue ( element.getString ( "notes" ) );

		friends = element.getArray ( "friends" );
		if ( friends == null || friends.size () == 0 ) {
			noedit.setVisible ( false );
			selection.setEditable ( true );
			notes.setEnabled ( true );
		}
		else {
			noedit.setVisible ( true );
			selection.setEditable ( false );
			notes.setEnabled ( false );
		}
	}

	public FromServer getValue () {
		if ( editable == true ) {
			currentValue.setArray ( "products", selection.getElements () );
			currentValue.setString ( "notes", notes.getValue () );
		}

		return currentValue;
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeProductsList ( ArrayList products ) {
		selection.upgradeProductsList ( products );
	}

	/****************************************************************** Lockable */

	public void unlock () {
		user.unlock ();
	}
}
