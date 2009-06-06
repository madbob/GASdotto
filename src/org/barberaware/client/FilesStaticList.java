/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class FilesStaticList extends FromServerArray {
	private VerticalPanel	main;
	private ArrayList	currentFiles;

	public FilesStaticList () {
		main = new VerticalPanel ();
		initWidget ( main );

		currentFiles = null;
	}

	private Widget doCell ( CustomFile file ) {
		/*
			I files vengono aperti in una finestra separata per evitare di perdere lo
			stato interno dell'applicazione AJAX
		*/
		return new HTML ( "<a target=\"_blank\" href=\"" + ( Utils.getServer ().getDomain () + file.getString ( "server_path" ) ) + "\">" + file.getString ( "name" ) + "</a>" );
	}

	/****************************************************************** FromServerArray */

	/*
		Funzione non implementata a causa del suo non utilizzo
	*/
	public void addElement ( FromServer element ) {
		/* dummy */
	}

	public void setElements ( ArrayList elements ) {
		int tot;
		Widget cell;

		if ( elements == null )
			return;

		currentFiles = elements;

		tot = elements.size ();
		for ( int i = 0; i < tot; i++ ) {
			cell = doCell ( ( CustomFile ) elements.get ( i ) );
			main.add ( cell );
		}
	}

	/*
		Funzione non implementata a causa del suo non utilizzo
	*/
	public void removeElement ( FromServer element ) {
		/* dummy */
	}

	public ArrayList getElements () {
		return currentFiles;
	}
}
