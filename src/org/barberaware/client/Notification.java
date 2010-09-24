/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	public static int	INFO		= 0;
	public static int	WARNING		= 1;
	public static int	ERROR		= 2;

	public Notification () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new StringFromObjectClosure () {
			public String retrive ( FromServer obj ) {
				int len;
				ArrayList recipents;
				String ret;

				recipents = obj.getArray ( "recipent" );
				if ( recipents == null )
					return "Nuova Notifica";

				len = recipents.size ();

				if ( len == 0 ) {
					ret = "A: Tutti";
				}
				else {
					int i;
					int str_len;
					boolean closed;
					FromServer iter;
					String name;

					ret = "A: ";
					str_len = 2;
					len = len - 1;
					closed = false;

					for ( i = 0; i < len; i++ ) {
						iter = ( FromServer ) recipents.get ( i );
						name = iter.getString ( "name" );
						ret += name + ", ";

						str_len = str_len + name.length ();
						if ( str_len >= 40 ) {
							ret += "e altri";
							closed = true;
							break;
						}
					}

					if ( closed == false ) {
						iter = ( FromServer ) recipents.get ( i );
						name = iter.getString ( "name" );
						ret += name;
					}
				}

				return ret;
			}
		} );

		addAttribute ( "alert_type", FromServer.INTEGER );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "startdate", FromServer.DATE );
		addAttribute ( "enddate", FromServer.DATE );
		addAttribute ( "recipent", FromServer.ARRAY, User.class );
		addAttribute ( "send_mail", FromServer.BOOLEAN );
	}

	public Image getIcon () {
		int type;

		type = getInt ( "alert_type" );

		if ( type == INFO )
			return new Image ( "images/notify-info.png" );
		else if ( type == WARNING )
			return new Image ( "images/notify-warning.png" );
		else
			return new Image ( "images/notify-error.png" );
	}

	/*
		Questa serve solo per creare notifiche ad uso interno e temporaneo, ad esempio
		quelle esposte dal "log di sessione"
	*/
	public static Notification instanceInternalNotification ( String message ) {
		Notification ret;

		ret = new Notification ();
		ret.setString ( "description", message );
		ret.setDate ( "startdate", new Date ( System.currentTimeMillis () ) );
		return ret;
	}
}
