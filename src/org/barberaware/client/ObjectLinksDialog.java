/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ObjectLinksDialog extends LinksDialog implements ObjectWidget {
	private ArrayList		names;
	private ArrayList		templates;
	private FromServer		currentObject;

	public ObjectLinksDialog ( String text ) {
		super ( text );

		names = new ArrayList ();
		templates = new ArrayList ();

		addHeader ( "Nessun file scaricabile" );
	}

	public void addLinkTemplate ( String name, String link ) {
		names.add ( name );
		templates.add ( link );
	}

	public void setValues ( FromServer element1, FromServer element2 ) {
		String name;
		String template;
		String id1;
		String id2;

		currentObject = element1;
		emptyBox ();

		if ( element1 == null || element1.isValid () == false || element2 == null || element2.isValid () == false ) {
			addHeader ( "Nessun file scaricabile" );
		}
		else {
			id1 = Integer.toString ( element1.getLocalID () );
			id2 = Integer.toString ( element2.getLocalID () );

			for ( int i = 0; i < names.size (); i++ ) {
				name = ( String ) names.get ( i );
				template = ( String ) templates.get ( i );
				template = template.replaceFirst ( "#", id1 );
				template = template.replaceFirst ( "#", id2 );
				addLink ( name, template );
			}
		}
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		String name;
		String template;
		String id;

		currentObject = element;
		emptyBox ();

		/*
			Il controllo esplicito su ID = 0 lo metto perche' gli OrderUser (per i quali codesto widget
			e' stato in primis introdotto) risultano sempre "validi". Cfr. OrderUser.isValid()
		*/
		if ( element == null || element.isValid () == false || element.getLocalID () == -1 ) {
			addHeader ( "Nessun file scaricabile" );
		}
		else {
			id = Integer.toString ( element.getLocalID () );

			for ( int i = 0; i < names.size (); i++ ) {
				name = ( String ) names.get ( i );
				template = ( String ) templates.get ( i );
				template = template.replaceFirst ( "#", id );
				addLink ( name, template );
			}
		}
	}

	public FromServer getValue () {
		return currentObject;
	}
}
