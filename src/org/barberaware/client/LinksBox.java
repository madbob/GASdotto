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
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class LinksBox extends CaptionPanel {
	private FlowPanel	main;
	private boolean		empty;

	public LinksBox () {
		super ( "Link Utili" );

		main = new FlowPanel ();
		add ( main );

		empty = true;

		Utils.getServer ().onObjectEvent ( "Link", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				Anchor button;

				button = new Anchor ( object.getString ( "name" ), false, getRightURL ( object ), "_blank" );
				button.addStyleName ( "external-link" );
				main.add ( button );

				checkEmpty ();
			}

			public void onModify ( FromServer object ) {
				Anchor button;

				button = findLink ( object.getString ( "url" ) );
				if ( button != null ) {
					button.setText ( object.getString ( "name" ) );
					button.setHref ( getRightURL ( object ) );
				}
			}

			public void onDestroy ( FromServer object ) {
				Anchor button;

				button = findLink ( object.getString ( "url" ) );
				if ( button != null ) {
					button.removeFromParent ();
					checkEmpty ();
				}
			}
		} );

		Utils.getServer ().testObjectReceive ( "Link" );
		checkEmpty ();
	}

	private Anchor findLink ( String url ) {
		Anchor button;
		Iterator<Widget> iter;

		iter = main.iterator ();

		while ( iter.hasNext () ) {
			button = ( Anchor ) iter.next ();
			if ( button.getHref () == url )
				return button;
		}

		return null;
	}

	private String getRightURL ( FromServer object ) {
		String url;

		url = object.getString ( "url" );
		if ( url.startsWith ( "http" ) == false )
			url = "http://" + url;

		return url;
	}

	private void checkEmpty () {
		if ( main.getWidgetCount () == 0 ) {
			empty = true;
			main.add ( new Label ( "Non sono stati definiti links" ) );
		}
		else {
			if ( empty == true )
				main.getWidget ( 0 ).removeFromParent ();

			empty = false;
		}
	}
}
