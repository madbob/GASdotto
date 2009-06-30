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

public class NotificationPanel extends GenericPanel {
	private FormCluster		main;

	public NotificationPanel () {
		super ();

		main = new FormCluster ( "Notification", "images/new_notification.png" ) {
				protected FromServerForm doEditableRow ( FromServer n ) {
					FromServerForm ver;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					Notification notify;
					EnumSelector type_sel;
					FromServerSelector users;

					notify = ( Notification ) n;
					ver = new FromServerForm ( notify );

					frame = new CustomCaptionPanel ( "Attributi" );
					ver.add ( frame );

					users = new FromServerIDSelector ( "User", true, true );
					users.addAllSelector ();
					frame.addPair ( "Destinatario", ver.getPersonalizedWidget ( "recipent", users ) );

					frame.addPair ( "Data Inizio", ver.getWidget ( "startdate" ) );
					frame.addPair ( "Date Fine", ver.getWidget ( "enddate" ) );

					type_sel = new EnumSelector ();
					type_sel.addItem ( "Informazione" );
					type_sel.addItem ( "Avvertimento" );
					frame.addPair ( "Tipo", ver.getPersonalizedWidget ( "alert_type", type_sel ) );

					sframe = new CaptionPanel ( "Testo" );
					sframe.add ( ver.getWidget ( "description" ) );
					ver.add ( sframe );

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					return doEditableRow ( new Notification () );
				}
		};

		addTop ( main );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Notifiche";
	}

	public String getSystemID () {
		return "notifications";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_notify.png" );
	}

	public void initView () {
		ServerRequest params;

		Utils.getServer ().testObjectReceive ( "User" );

		params = new ServerRequest ( "Notification" );
		params.add ( "all", 1 );
		Utils.getServer ().testObjectReceive ( params );
	}
}
