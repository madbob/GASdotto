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
		addAttribute ( "type", FromServer.INTEGER );
		addAttribute ( "description", FromServer.LONGSTRING );
	}

	public Image getIcon () {
		int type;

		type = getInt ( "type" );

		switch ( type ) {
			case 1:
				return new Image ( "images/notify-info.png" );
			case 2:
				return new Image ( "images/notify-warning.png" );
			case 3:
				return new Image ( "images/notify-error.png" );
			default:
				return null;
		}
	}

	public Widget show () {
		HorizontalPanel main;

		main = new HorizontalPanel ();
		main.add ( getIcon () );
		main.add ( new Label ( getString ( "description" ) ) );
		return main;
	}
}
