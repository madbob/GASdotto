/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class FilesStaticList extends Composite implements FromServerArray {
	private VerticalPanel	main;
	private boolean		empty;
	private ArrayList	currentFiles;

	public FilesStaticList () {
		main = new VerticalPanel ();
		initWidget ( main );

		currentFiles = null;

		empty = true;
		main.add ( new Label ( "Non ci sono files" ) );
	}

	private Widget doCell ( CustomFile file ) {
		/*
			I files vengono aperti in una finestra separata per evitare di perdere lo
			stato interno dell'applicazione AJAX
		*/
		return new HTML ( "<a target=\"_blank\" href=\"" + ( Utils.getServer ().getDomain () + file.getString ( "server_path" ) ) + "\">" + file.getString ( "name" ) + "</a>" );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		Widget cell;

		if ( empty == true ) {
			empty = false;
			main.clear ();
		}

		cell = doCell ( ( CustomFile ) element );
		main.add ( cell );
	}

	public void setElements ( ArrayList elements ) {
		int tot;

		main.clear ();

		if ( elements == null || elements.size () == 0 ) {
			main.add ( new Label ( "Non ci sono files" ) );
			return;
		}

		tot = elements.size ();
		currentFiles = elements;

		for ( int i = 0; i < tot; i++ )
			addElement ( ( CustomFile ) elements.get ( i ) );
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

	public void refreshElement ( FromServer element ) {
		/* dummy */
	}
}
