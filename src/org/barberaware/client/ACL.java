/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ACL extends FromServer {
	public static int		ACL_OWNER	= 0;
	public static int		ACL_READWRITE	= 1;
	public static int		ACL_READONLY	= 2;
	public static int		ACL_NONE	= 3;

	public ACL () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				return obj.getObject ( "gas" ).getString ( "name" );
			}
		} );

		addAttribute ( "gas", FromServer.OBJECT, GAS.class );

		/*
			Qui potrei usare un singolo attributo FromServer.OBJECT,
			ma non so il tipo a priori dunque per non complicarmi la
			vita scompongo l'informazione al minimo indispensabile
		*/
		addAttribute ( "target_type", FromServer.STRING );
		addAttribute ( "target_id", FromServer.INTEGER );

		addAttribute ( "privileges", FromServer.INTEGER );
	}
}
