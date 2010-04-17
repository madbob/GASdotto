/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.json.client.*;

public class NotificationsBox extends Composite {
	private CaptionPanel		ubermain;
	private FlexTable		main;

	public NotificationsBox () {
		ubermain = new CaptionPanel ( "Hai nuove notifiche..." );
		ubermain.setStyleName ( "notifications-box" );
		ubermain.setVisible ( false );
		initWidget ( ubermain );

		main = new FlexTable ();
		ubermain.add ( main );

		Utils.getServer ().onObjectEvent ( "Notification", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int index;
				Notification tmp;

				tmp = ( Notification ) object;

				if ( isForMe ( tmp ) ) {
					if ( retrieveExisting ( tmp ) == -1 ) {
						index = main.getRowCount ();
						setNotification ( tmp, index );
						ubermain.setVisible ( true );
					}
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				Notification notify;

				notify = ( Notification ) object;

				index = retrieveExisting ( notify );
				if ( index != -1 )
					setNotification ( notify, index );
			}

			public void onDestroy ( FromServer object ) {
				int index;

				index = retrieveExisting ( ( Notification ) object );
				if ( index != -1 ) {
					main.removeRow ( index );

					if ( main.getRowCount () == 0 )
						ubermain.setVisible ( false );
				}
			}
		} );
	}

	private boolean isForMe ( Notification notify ) {
		ArrayList dests;
		User myself;
		FromServer iter;

		dests = notify.getArray ( "recipent" );
		myself = Session.getUser ();

		for ( int i = 0; i < dests.size (); i++ ) {
			iter = ( FromServer ) dests.get ( i );
			if ( myself.equals ( iter ) )
				return true;
		}

		return false;
	}

	private void setNotification ( Notification notify, int index ) {
		main.setWidget ( index, 0, new Hidden ( "id", Integer.toString ( notify.getLocalID () ) ) );
		main.setWidget ( index, 1, notify.getIcon () );
		main.setWidget ( index, 2, new Label ( notify.getString ( "description" ) ) );
	}

	private int retrieveExisting ( Notification notify ) {
		int tot;
		String iter_id;
		String search_id;

		tot = main.getRowCount ();
		search_id = Integer.toString ( notify.getLocalID () );

		for ( int i = 0; i < tot; i++ ) {
			iter_id = ( ( Hidden ) main.getWidget ( i, 0 ) ).getValue ();
			if ( iter_id.equals ( search_id ) )
				return i;
		}

		return -1;
	}

	public void syncList () {
		Utils.getServer ().testObjectReceive ( "Notification" );
	}
}
