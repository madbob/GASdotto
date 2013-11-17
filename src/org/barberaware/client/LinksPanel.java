/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class LinksPanel extends GenericPanel {
	private FormCluster		main;

	public LinksPanel () {
		super ();

		main = new FormCluster ( "Link", "Nuovo Link" ) {
			protected FromServerForm doEditableRow ( FromServer n ) {
				FromServerForm ver;
				DateSelector date;
				CustomCaptionPanel frame;
				CaptionPanel sframe;
				EnumSelector type_sel;
				MultiSelector users;

				ver = new FromServerForm ( n );

				frame = new CustomCaptionPanel ( "Attributi" );
				ver.add ( frame );

				frame.addPair ( "Titolo", ver.getWidget ( "name" ) );
				frame.addPair ( "URL", ver.getWidget ( "url" ) );

				return ver;
			}

			protected FromServerForm doNewEditableRow () {
				Link link;

				link = new Link ();
				return doEditableRow ( link );
			}
		};

		addTop ( main );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Links";
	}

	public String getSystemID () {
		return "links";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_links.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Link" );
	}
}
