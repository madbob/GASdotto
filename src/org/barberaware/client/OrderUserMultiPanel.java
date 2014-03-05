/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderUserMultiPanel extends OrderUserManagerMode implements Lockable {
	private VerticalPanel		main;
	private OrderUserManagerMode	selection;
	private UserSelector		user;
	private FromServer		baseOrder;

	private boolean			editable;
	private boolean			freeEditable;
	private FromServer		currentValue;

	public OrderUserMultiPanel ( FromServer order, boolean edit, boolean freedit ) {
		Label header;
		HorizontalPanel pan;

		main = new VerticalPanel ();
		main.setWidth ( "100%" );
		initWidget ( main );

		editable = edit;
		freeEditable = freedit;
		baseOrder = order;

		pan = new HorizontalPanel ();
		pan.setStyleName ( "highlight-part" );
		main.add ( pan );

		header = new Label ( "Ordine eseguito a nome di " );
		pan.add ( header );
		pan.setCellVerticalAlignment ( header, HasVerticalAlignment.ALIGN_MIDDLE );

		user = new UserSelector ();
		pan.add ( user );

		user.addFilter ( new FromServerValidateCallback () {
			public boolean checkObject ( FromServer object ) {
				if ( object.getInt ( "privileges" ) == User.USER_LEAVED )
					return false;
				else
					return true;
			}
		} );

		user.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				retrieveCurrentOrderByUser ();
			}
		}, ChangeEvent.getType () );

		selection = new OrderUserPlainPanel ( baseOrder, edit, freedit );
		selection.setWidth ( "100%" );
		main.add ( selection );
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
		OrderUserManagerMode new_selection;

		currentValue = element;
		user.setValue ( currentValue.getObject ( "baseuser" ) );

		new_selection = null;

		friends = element.getArray ( "friends" );
		if ( ( friends == null || friends.size () == 0 ) && selection instanceof OrderUserFriendPanel )
			new_selection = new OrderUserPlainPanel ( baseOrder, editable, freeEditable );
		else if ( friends != null && friends.size () != 0 && selection instanceof OrderUserPlainPanel )
			new_selection = new OrderUserFriendPanel ( baseOrder, editable, freeEditable, false );

		if ( new_selection != null ) {
			main.remove ( selection );
			selection = new_selection;
			main.add ( selection );
			selection.setWidth ( "100%" );
		}

		selection.setValue ( currentValue );

		triggerOrderChange ( element );
	}

	public FromServer getValue () {
		return selection.getValue ();
	}

	/****************************************************************** OrderUserManagerMode */

	public void upgradeOrder ( FromServer order ) {
		selection.upgradeOrder ( order );
	}

	/****************************************************************** Lockable */

	public void unlock () {
		user.unlock ();
	}
}
