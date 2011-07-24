/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.google.gwt.user.client.ui.*;

public abstract class OrderUserManagerMode extends FromServerRappresentation implements ObjectWidget, SourcesChangeEvents, Lockable {
	private ArrayList		ordersCallbacks		= null;

	protected void triggerOrderChange ( FromServer object ) {
		FromServerCallback listener;

		if ( ordersCallbacks != null ) {
			for ( int i = 0; i < ordersCallbacks.size (); i++ ) {
				listener = ( FromServerCallback ) ordersCallbacks.get ( i );
				listener.execute ( object );
			}
		}
	}

	public void addOrderListener ( FromServerCallback listener ) {
		if ( ordersCallbacks == null )
			ordersCallbacks = new ArrayList ();
		ordersCallbacks.add ( listener );
	}

	public void removeOrderListener ( FromServerCallback listener ) {
		if ( ordersCallbacks != null )
			ordersCallbacks.remove ( listener );
	}

	public abstract void upgradeOrder ( FromServer order );
	public abstract void unlock ();
}
