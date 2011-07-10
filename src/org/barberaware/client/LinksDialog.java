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

public class LinksDialog extends Composite {
	private String		mainText;
	private DialogBox	dialog;
	private VerticalPanel	links;

	public LinksDialog ( String text ) {
		Label main;
		Button but;
		HorizontalPanel buttons;

		mainText = text;

		main = new Label ( text );
		main.setStyleName ( "file-link" );
		main.addMouseListener ( new MouseListener () {
			private int initial_x;
			private int initial_y;

			public void onMouseDown ( Widget sender, int x, int y ) {
				initial_x = x;
				initial_y = y;
			}

			public void onMouseEnter ( Widget sender ) {
				/* dummy */
			}

			public void onMouseLeave ( Widget sender ) {
				/* dummy */
			}

			public void onMouseMove ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseUp ( Widget sender, int x, int y ) {
				if ( initial_x == x && initial_y == y ) {
					dialog.setPopupPosition ( sender.getAbsoluteLeft () + x - 20,
									sender.getAbsoluteTop () + y - 20 );
					dialog.show ();
				}
			}
		} );

		links = new VerticalPanel ();

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		links.add ( buttons );

		dialog = new DialogBox ();
		dialog.setText ( "Scarica File" );
		dialog.setWidget ( links );

		initWidget ( main );
	}

	public void addLink ( String name, String link ) {
		HTML button;

		button = Utils.getServer ().fileLink ( mainText + " - " + name, "", link );
		button.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.hide ();
			}
		} );

		links.insert ( button, links.getWidgetCount () - 1 );
	}

	public void addHeader ( String text ) {
		links.insert ( new Label ( text ), links.getWidgetCount () - 1 );
	}

	public void emptyBox () {
		int count;

		count = links.getWidgetCount ();

		for ( int i = 0; i < count - 1; i++ )
			links.remove ( 0 );
	}
}
