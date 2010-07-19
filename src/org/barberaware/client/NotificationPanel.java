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

public class NotificationPanel extends GenericPanel {
	private FormCluster		main;

	public NotificationPanel () {
		super ();

		main = new FormCluster ( "Notification", "Nuova Notifica" ) {
				private MultiSelector destinationSelect () {
					MultiSelector users;

					users = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_ALL, null );

					users.addSelectionCallbacks ( "Seleziona solo Referenti",
						new FilterCallback () {
							public boolean check ( FromServer obj, String text ) {
								if ( obj.getInt ( "privileges" ) == User.USER_RESPONSABLE ||
										obj.getInt ( "privileges" ) == User.USER_ADMIN )
									return true;
								else
									return false;
							}
						}
					);

					return users;
				}

				protected FromServerForm doEditableRow ( FromServer n ) {
					FromServerForm ver;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					EnumSelector type_sel;
					MultiSelector users;

					ver = new FromServerForm ( n );

					frame = new CustomCaptionPanel ( "Attributi" );
					ver.add ( frame );

					frame.addPair ( "Destinatario", ver.getPersonalizedWidget ( "recipent", destinationSelect () ) );

					frame.addPair ( "Data Inizio", ver.getWidget ( "startdate" ) );
					frame.addPair ( "Date Fine", ver.getWidget ( "enddate" ) );

					type_sel = new EnumSelector ();
					type_sel.addItem ( "Informazione" );
					type_sel.addItem ( "Avvertimento" );
					frame.addPair ( "Tipo", ver.getPersonalizedWidget ( "alert_type", type_sel ) );

					if ( Session.getGAS ().getBool ( "use_mail" ) == true )
						frame.addPair ( "Invia anche via Mail", ver.getWidget ( "send_mail" ) );

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
		ObjectRequest params;

		Utils.getServer ().testObjectReceive ( "User" );

		params = new ObjectRequest ( "Notification" );
		params.add ( "all", 1 );
		Utils.getServer ().testObjectReceive ( params );
	}
}
