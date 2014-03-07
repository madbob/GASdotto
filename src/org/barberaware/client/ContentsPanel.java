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

import com.allen_sauer.gwt.log.client.Log;

public class ContentsPanel extends GenericPanel {
	private FormCluster		links;
	private FilesGroup		files;

	public ContentsPanel () {
		super ();

		if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
			files = new FilesGroup ();
			files.addExtraCallback ( new FromServerFormCallbacks () {
				public void onSaved ( FromServerRappresentationFull form ) {
					FromServer gas;
					FromServer new_file;
					ArrayList<FromServer> existing;

					gas = Session.getGAS ();
					new_file = form.getValue ();
					existing = gas.getArray ( "files" );

					for ( FromServer test : existing )
						if ( test.equals ( new_file ) == true )
							return;

					gas.addToArray ( "files", new_file );
					gas.save ( null );
				}
			} );

			files.setElements ( Session.getGAS ().getArray ( "files" ) );
			addTop ( files );
		}

		links = new FormCluster ( "Link", "Nuovo Link" ) {
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

		addTop ( links );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Contenuti";
	}

	public String getSystemID () {
		return "contents";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( links.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_links.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Link" );
	}
}
