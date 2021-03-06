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

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.*;

public class CustomCaptionPanel extends CaptionPanel {
	private CustomFormTable			content;

	public CustomCaptionPanel ( String title ) {
		super ( title );

		content = new CustomFormTable ();
		setContentWidget ( content );

		setStyleName ( "custom-caption-panel" );
	}

	public void addPair ( String name, Widget element, int row ) {
		content.addPair ( name, null, element, row, "custom-label" );
	}

	public void addPair ( String name, Widget element ) {
		content.addPair ( name, element );
	}

	public void addPair ( String name, String help, Widget element ) {
		content.addPair ( name, help, element );
	}

	public void addRight ( Widget element ) {
		content.addRight ( element );
	}
}

