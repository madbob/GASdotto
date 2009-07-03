/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.json.client.*;

public class User extends FromServer {
	public static int	USER_COMMON		= 0;
	public static int	USER_RESPONSABLE	= 1;
	public static int	USER_ADMIN		= 2;

	public User () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new StringFromObjectClosure () {
			public String retrive ( FromServer obj ) {
				String name;
				String surname;

				name = obj.getString ( "firstname" );
				surname = obj.getString ( "surname" );

				if ( name.equals ( "" ) && surname.equals ( "" ) )
					return "Nuovo Utente";
				else
					return surname + " " + name;
			}
		} );

		addAttribute ( "login", FromServer.STRING );
		addAttribute ( "password", FromServer.STRING );
		addAttribute ( "firstname", FromServer.STRING );
		addAttribute ( "surname", FromServer.STRING );
		addAttribute ( "join_date", FromServer.DATE );
		addAttribute ( "card_number", FromServer.STRING );
		addAttribute ( "phone", FromServer.STRING );
		addAttribute ( "mobile", FromServer.STRING );
		addAttribute ( "mail", FromServer.STRING );
		addAttribute ( "mail2", FromServer.STRING );
		addAttribute ( "address", FromServer.ADDRESS );
		addAttribute ( "paying", FromServer.DATE );
		addAttribute ( "privileges", FromServer.INTEGER );

		setDate ( "join_date", new Date ( System.currentTimeMillis () ) );
	}

	public void checkUserPaying ( FromServerForm form ) {
		Date last_pay;
		IconsBar bar;

		bar = form.getIconsBar ();
		last_pay = this.getDate ( "paying" );

		/**
			TODO	Aggiungere controllo su differenza data corrente e data pagamento quota
		*/
		if ( last_pay == null )
			bar.addImage ( "images/notifications/user_not_paying.png" );
		else
			bar.delImage ( "images/notifications/user_not_paying.png" );
	}
}
