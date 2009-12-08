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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class MainApp extends Composite {
	private MainStack		stack;
	static private Header		header;

	public MainApp () {
		VerticalPanel main;
		HorizontalPanel hor;
		Menu menu;

		main = new VerticalPanel ();
		initWidget ( main );
		main.setStyleName ( "main" );

		header = new Header ();
		main.add ( header );
		main.setCellHeight ( header, "40px" );

		hor = new HorizontalPanel ();
		hor.setSize ( "100%", "100%" );
		main.add ( hor );

		stack = new MainStack ();
		menu = new Menu ( stack );
		stack.wireMenu ( menu );
		hor.add ( menu );
		hor.setCellWidth ( menu, "15%" );
		hor.add ( stack );
		hor.setCellWidth ( stack, "85%" );
		header.wireMainStack ( stack );

		main.add ( new Footer () );

		main.add ( Utils.getNotificationsArea () );

		stack.showPanelAtPos ( 0 );
		openLink ();
	}

	private void openLink () {
		String link;

		/*
			Il cookie qui consultato viene settato in GASdotto.php, sul server
		*/
		link = Cookies.getCookie ( "initial_url" );
		if ( link != null ) {
			stack.goTo ( link );
			Cookies.removeCookie ( "initial_url" );
		}
	}

	public static void jumpTop () {
		DOM.scrollIntoView ( header.getElement () );
	}
}
