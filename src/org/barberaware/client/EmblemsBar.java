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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class EmblemsBar extends Composite {
	private HorizontalPanel		main;
	private EmblemsInfo		infos;

	public EmblemsBar ( EmblemsInfo info ) {
		Image placeholder;

		main = new HorizontalPanel ();
		main.setStyleName ( "icons-bar" );
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_RIGHT );
		initWidget ( main );

		infos = info;

		for ( int i = 0; i < infos.numSymbols (); i++ ) {
			placeholder = new Image ();
			main.add ( placeholder );
			main.setCellWidth ( placeholder, "22px" );
			placeholder.setVisible ( false );
		}
	}

	public void activate ( String name, int index ) {
		Image img;
		String path;

		img = ( Image ) main.getWidget ( infos.getIndex ( name ) );
		path = infos.getSymbol ( name, index );

		if ( path == "" ) {
			img.setVisible ( false );
		}
		else {
			img.setUrl ( path );
			img.setVisible ( true );
		}
	}

	public void activate ( String name ) {
		if ( name == null )
			return;

		activate ( name, 0 );
	}

	public void deactivate ( String name ) {
		Image img;

		if ( name == null )
			return;

		img = ( Image ) main.getWidget ( infos.getIndex ( name ) );
		img.setVisible ( false );
	}
}
