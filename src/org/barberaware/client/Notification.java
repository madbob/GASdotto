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

		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "alert_type", FromServer.INTEGER );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "startdate", FromServer.DATE );
		addAttribute ( "enddate", FromServer.DATE );
		addAttribute ( "sender", FromServer.OBJECT, User.class );
		addAttribute ( "recipent", FromServer.ARRAY, User.class );
		addAttribute ( "send_mail", FromServer.BOOLEAN );
		addAttribute ( "send_mailinglist", FromServer.BOOLEAN );

		setString ( "name", "Nuova Notifica" );
	}

	public boolean isForMe () {
		ArrayList dests;
		User myself;
		FromServer iter;

		dests = getArray ( "recipent" );
		myself = Session.getUser ();

		for ( int i = 0; i < dests.size (); i++ ) {
			iter = ( FromServer ) dests.get ( i );
			if ( myself.equals ( iter ) )
				return true;
		}

		return false;
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
