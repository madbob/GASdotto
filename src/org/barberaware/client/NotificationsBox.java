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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

public class NotificationsBox extends Composite {
	private CaptionPanel		ubermain;
	private FormGroup		main;

	public NotificationsBox () {
		ubermain = new CaptionPanel ( "Hai nuove notifiche..." );
		ubermain.setVisible ( false );
		initWidget ( ubermain );

		main = new FormGroup ( null ) {
			protected FromServerForm doEditableRow ( FromServer n ) {
				FromServerForm ver;

				ver = new FromServerForm ( n, FromServerForm.NOT_EDITABLE );
				ver.emblemsAttach ( Utils.getEmblemsCache ( "notifications" ) );

				ver.emblems ().activate ( "type", n.getInt ( "alert_type" ) );
				ver.add ( ver.getPersonalizedWidget ( "description", new LongStringLabel () ) );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				return null;
			}

			protected int sorting ( FromServer first, FromServer second ) {
				if ( first == null )
					return 1;
				else if ( second == null )
					return -1;

				return -1 * ( first.getDate ( "enddate" ).compareTo ( second.getDate ( "enddate" ) ) );
			}
		};

		ubermain.add ( main );

		Utils.getServer ().onObjectEvent ( "Notification", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Notification tmp;

				tmp = ( Notification ) object;
				if ( tmp.isForMe () ) {
					main.addElement ( object );
					ubermain.setVisible ( true );
				}
			}

			public void onModify ( FromServer object ) {
				main.refreshElement ( object );
			}

			public void onDestroy ( FromServer object ) {
				main.deleteElement ( object );
				if ( main.getElementsNum () == 0 )
					ubermain.setVisible ( false );
			}
		} );
	}

	public void syncList () {
		Utils.getServer ().testObjectReceive ( "Notification" );
	}
}
