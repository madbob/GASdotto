/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class UserSelector extends DeckPanel implements ObjectWidget, Lockable {
	private FromServerSelector			select;

	public UserSelector () {
		User user;
		Label first;

		setStyleName ( "user-selector" );

		user = Session.getUser ();

		first = new Label ( user.getString ( "name" ) + " (clicca qui per modificare)" );
		first.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				showWidget ( 1 );
				fire ();
			}
		} );
		add ( first );

		select = new FromServerSelector ( "User", true, true, true );
		add ( select );

		showWidget ( 0 );
	}

	private void fire () {
		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
	}

	public void addFilter ( FromServerValidateCallback filter ) {
		select.addFilter ( filter );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		if ( selected != null ) {
			showWidget ( 1 );
			select.setValue ( selected );
		}
		else
			showWidget ( 0 );
	}

	public FromServer getValue () {
		if ( getVisibleWidget () == 0 )
			return Session.getUser ();
		else
			return select.getValue ();
	}

	/****************************************************************** Lockable */

	public void unlock () {
		select.unlock ();
	}
}
