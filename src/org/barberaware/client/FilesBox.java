/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class FilesBox extends CaptionPanel {
	private FlowPanel	main;

	public FilesBox () {
		super ( "Documenti Condivisi" );

		main = new FlowPanel ();
		add ( main );

		Utils.getServer ().onObjectEvent ( "GAS", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				populate ( object );
			}

			public void onModify ( FromServer object ) {
				populate ( object );
			}

			public void onDestroy ( FromServer object ) {
				/* dummy */
			}
		} );
	}

	private void populate ( FromServer gas ) {
		ArrayList<FromServer> files;
		DownloadButton button;

		main.clear ();

		files = gas.getArray ( "files" );

		if ( files.size () == 0 ) {
			main.add ( new Label ( "Non ci sono documenti condivisi" ) );
		}
		else {
			for ( FromServer f : files ) {
				button = new DownloadButton ();
				button.setValue ( f.getString ( "name" ), f.getString ( "server_path" ) );
				main.add ( button );
			}
		}
	}
}
