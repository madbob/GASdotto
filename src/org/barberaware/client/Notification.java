/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class Notification extends FromServer {
	public static int	INFO		= 1;
	public static int	WARNING		= 2;
	public static int	ERROR		= 3;

	public Notification () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new StringFromObjectClosure () {
			public String retrive ( FromServer obj ) {
				int recipent;

				recipent = obj.getInt ( "recipent" );

				if ( recipent == -1 ) {
					return "A Tutti gli Utente";
				}
				else {
					User user;

					user = ( User ) Utils.getServer ().getObjectFromCache ( "User", recipent );
					if ( user != null )
						return "A: " + user.getString ( "name" );
					else
						return "A: Utente Sconosciuto";
				}
			}
		} );

		addAttribute ( "alert_type", FromServer.INTEGER );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "startdate", FromServer.DATE );
		addAttribute ( "enddate", FromServer.DATE );
		addAttribute ( "recipent", FromServer.INTEGER );
	}

	public Image getIcon () {
		int type;

		type = getInt ( "alert_type" );

		switch ( type ) {
			case 1:
				return new Image ( "images/notify-info.png" );
			case 2:
				return new Image ( "images/notify-warning.png" );
			case 3:
			default:
				return new Image ( "images/notify-error.png" );
		}
	}
}
