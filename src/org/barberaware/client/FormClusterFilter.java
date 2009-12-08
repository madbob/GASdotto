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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class FormClusterFilter extends Composite {
	private FormCluster	reference;
	private TextBox		searchBar;
	private FilterCallback	callback;

	public FormClusterFilter ( FormCluster ref, FilterCallback call ) {
		HorizontalPanel main;

		main = new HorizontalPanel ();
		main.setStyleName ( "search-bar-wrap" );
		initWidget ( main );

		main.add ( new Label ( "Ricerca in Lista" ) );

		searchBar = new TextBox ();
		searchBar.setStyleName ( "search-bar" );
		main.add ( searchBar );

		searchBar.addKeyboardListener ( new KeyboardListenerAdapter () {
			public void onKeyPress ( Widget sender, char keyCode, int modifiers ) {
				String text;

				text = searchBar.getText ();

				if ( text.length () <= 1 )
					clearFilter ();
				else if ( text.length () >= 3 )
					executeSearch ();
			}
		} );

		reference = ref;
		callback = call;
	}

	private void clearFilter () {
		FromServerForm iter;

		for ( int i = 0; i < reference.latestIterableIndex (); i++ ) {
			iter = reference.retrieveForm ( i );
			if ( iter.getObject () != null )
				iter.setVisible ( true );
		}
	}

	private void executeSearch () {
		String text;
		FromServerForm iter;

		text = searchBar.getText ();

		for ( int i = 0; i < reference.latestIterableIndex (); i++ ) {
			iter = reference.retrieveForm ( i );

			if ( callback.check ( iter.getObject (), text ) == false )
				iter.setVisible ( false );
			else
				iter.setVisible ( true );
		}
	}
}
