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

public class CustomFile extends FromServer {
	public CustomFile () {
		super ();

		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "server_path", FromServer.STRING );
		addAttribute ( "by_user", FromServer.OBJECT, User.class );
		addAttribute ( "upload_date", FromServer.DATE );

		setString ( "name", "Nuovo Documento" );
		setObject ( "by_user", Session.getUser () );
		setDate ( "upload_date", new Date ( System.currentTimeMillis () ) );
	}
}
