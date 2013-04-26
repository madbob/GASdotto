/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class Footer extends Composite {
	public Footer () {
		HTML label;
		VerticalPanel main;

		main = new VerticalPanel ();
		main.setStyleName ( "main-footer" );
		initWidget ( main );

		if ( getUserAgent ().contains ( "firefox" ) == false ) {
			label = new HTML ( "<p><img src=\"images/firefox.jpg\" alt=\"Firefox\">Il team GASdotto consiglia di usare <a href=\"http://getfirefox.com\">Firefox</a>.</p>" );
			main.add ( label );
			main.setCellVerticalAlignment ( label, HasVerticalAlignment.ALIGN_MIDDLE );
		}

		label = new HTML ( "<p><a href=\"http://gasdotto.net\">GASdotto</a> è software libero, rilasciato in licenza GPLv3.</p>" );
		main.add ( label );
		main.setCellVerticalAlignment ( label, HasVerticalAlignment.ALIGN_MIDDLE );
	}

	private static native String getUserAgent () /*-{
		return navigator.userAgent.toLowerCase();
	}-*/;
}
