/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class Supplier extends FromServer {
	public Supplier () {
		super ();

		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "contact", FromServer.STRING );
		addAttribute ( "phone", FromServer.STRING );
		addAttribute ( "fax", FromServer.STRING );
		addAttribute ( "mail", FromServer.STRING );
		addAttribute ( "website", FromServer.STRING );
		addAttribute ( "address", FromServer.ADDRESS );
		addAttribute ( "order_mode", FromServer.LONGSTRING );
		addAttribute ( "paying_mode", FromServer.LONGSTRING );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "references", FromServer.ARRAY, User.class );
		addAttribute ( "carriers", FromServer.ARRAY, User.class );
		addAttribute ( "files", FromServer.ARRAY, CustomFile.class );
		addAttribute ( "orders_months", FromServer.STRING );

		setString ( "name", "Nuovo Fornitore" );
		isSharable ( true );
	}

	public boolean iAmReference () {
		User myself;
		int privileges;
		ArrayList references;
		FromServer ref;

		myself = Session.getUser ();
		privileges = myself.getInt ( "privileges" );

		if ( privileges == User.USER_RESPONSABLE || privileges == User.USER_ADMIN ) {
			references = getArray ( "references" );
			if ( references == null )
				return false;

			for ( int i = 0; i < references.size (); i++ ) {
				ref = ( FromServer ) references.get ( i );
				if ( ref.equals ( myself ) )
					return true;
			}
		}

		return false;
	}

	public boolean iAmCarrier () {
		User myself;
		int privileges;
		ArrayList references;
		FromServer ref;

		myself = Session.getUser ();
		privileges = myself.getInt ( "privileges" );

		if ( privileges == User.USER_RESPONSABLE || privileges == User.USER_ADMIN ) {
			references = getArray ( "carriers" );
			if ( references == null )
				return false;

			for ( int i = 0; i < references.size (); i++ ) {
				ref = ( FromServer ) references.get ( i );
				if ( ref.equals ( myself ) )
					return true;
			}
		}

		return false;
	}
}
