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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class IconsBar extends Composite {
	private HorizontalPanel		main;

	public IconsBar () {
		main = new HorizontalPanel ();
		initWidget ( main );
	}

	public void addImage ( String path ) {
		int num;
		Image iter;

		num = main.getWidgetCount ();

		for ( int i = 0; i < num; i++ ) {
			iter = ( Image ) main.getWidget ( i );
			if ( iter.getUrl ().equals ( path ) )
				return;
		}

		main.add ( new Image ( path ) );
	}

	public void delImage ( String path ) {
		int num;
		Image iter;

		num = main.getWidgetCount ();

		for ( int i = 0; i < num; i++ ) {
			iter = ( Image ) main.getWidget ( i );

			if ( iter.getUrl ().equals ( path ) ) {
				main.remove ( i );
				break;
			}
		}
	}
}
