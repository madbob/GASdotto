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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class FormClusterFilter extends TextBox {
	private FormCluster	reference;
	private TextBox		searchBar;
	private FilterCallback	callback;

	public FormClusterFilter ( FormCluster ref, FilterCallback call ) {
		searchBar = this;
		searchBar.setStyleName ( "search-bar" );

		searchBar.addKeyUpHandler ( new KeyUpHandler () {
			public void onKeyUp ( KeyUpEvent event ) {
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

		for ( int i = reference.firstIterableIndex (); i < reference.latestIterableIndex (); i++ ) {
			iter = reference.retrieveForm ( i );
			if ( iter.getValue () != null )
				iter.setVisible ( true );
		}
	}

	private void executeSearch () {
		String text;
		FromServerForm iter;

		text = this.getText ();

		for ( int i = reference.firstIterableIndex (); i < reference.latestIterableIndex (); i++ ) {
			iter = reference.retrieveForm ( i );

			if ( callback.check ( iter.getValue (), text ) == false )
				iter.setVisible ( false );
			else
				iter.setVisible ( true );
		}
	}
}
