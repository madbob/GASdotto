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

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.*;

public class CustomCaptionPanel extends CaptionPanel {
	private class CustomComplexPanel extends VerticalPanel {
		public CustomComplexPanel () {
			super ();
		}

		public void adoptExt ( Widget w ) {
			w.removeFromParent ();
			getChildren ().add ( w );
			adopt ( w );
		}
	}

	private CustomComplexPanel	content;

	public CustomCaptionPanel ( String title ) {
		super ( title );

		content = new CustomComplexPanel ();
		setContentWidget ( content );

		setStyleName ( "custom-caption-panel" );
	}

	public void addPair ( String name, Widget element ) {
		LabelElement lab;
		Element myself;

		/*
			Ideale per essere usato con il CSS descritto qui:
			http://www.bitrepository.com/how-to-create-a-tableless-form.html
		*/

		lab = Document.get ().createLabelElement ();
		lab.setInnerText ( name );

		myself = content.getElement ();
		myself.appendChild ( lab );
		myself.appendChild ( element.getElement () );
		myself.appendChild ( Document.get ().createBRElement () );
		content.adoptExt ( element );
	}
}
