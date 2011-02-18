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

public class SystemConf extends FromServer {
	public SystemConf () {
		super ();

		/*
			Un attributo "name" non dovrebbe servire a nulla, se non a fare arrabbiare il FromServerForm
			che viene costruito in SystemPanel per riassumere le informazioni dell'applicazione
		*/
		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				return "Informazioni Generali";
			}
		} );

		addAttribute ( "gasdotto_main_version", FromServer.STRING );
		addAttribute ( "gasdotto_build_date", FromServer.DATE );
		addAttribute ( "has_file", FromServer.BOOLEAN );
	}
}
