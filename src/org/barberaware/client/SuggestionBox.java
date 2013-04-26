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

public class SuggestionBox extends Composite {
	public static int	ALIGN_RIGHT			= 0;
	public static int	ALIGN_BOTTOM			= 1;

	private DockPanel	main;
	private HTML		contents;
	private Widget		layoutReference			= null;
	private int		alignment			= ALIGN_RIGHT;

	public SuggestionBox () {
		main = new DockPanel ();
		main.setStyleName ( "suggestion-box" );
		DOM.setStyleAttribute ( main.getElement (), "position", "absolute" );
		initWidget ( main );

		contents = new HTML ();
		main.add ( contents, DockPanel.CENTER );

		main.setVisible ( false );
	}

	public void relativeTo ( Widget reference, int align ) {
		Image image;

		layoutReference = reference;
		alignment = align;
		image = null;

		if ( alignment == ALIGN_RIGHT ) {
			image = new Image ( "images/arrow-to-left.png" );
			main.add ( image, DockPanel.WEST );
		}
		else if ( alignment == ALIGN_BOTTOM ) {
			image = new Image ( "images/arrow-to-up.png" );
			main.add ( image, DockPanel.NORTH );
		}

		main.setCellVerticalAlignment ( image, HasVerticalAlignment.ALIGN_TOP );
		main.setCellHorizontalAlignment ( image, HasHorizontalAlignment.ALIGN_LEFT );
	}

	public void setHTML ( String html ) {
		contents.setHTML ( html );
	}

	public void show () {
		int ref_left;
		int ref_top;
		int ref_width;
		int ref_height;
		int new_x;
		int new_y;

		ref_left = layoutReference.getAbsoluteLeft ();
		ref_top = layoutReference.getAbsoluteTop ();
		ref_width = layoutReference.getOffsetWidth ();
		ref_height = layoutReference.getOffsetHeight ();
		new_x = ref_left;
		new_y = ref_top;

		if ( alignment == ALIGN_RIGHT ) {
			new_x = ref_left + ref_width;
			new_y = ref_top;
		}
		else if ( alignment == ALIGN_BOTTOM ) {
			new_x = ref_top + ref_height;
			new_y = ref_left;
		}

		DOM.setStyleAttribute ( main.getElement (), "top", new_y + "px" );
		DOM.setStyleAttribute ( main.getElement (), "left", new_x + "px" );
		main.setVisible ( true );
	}

	public void hide () {
		main.setVisible ( false );
	}
}
