/*  ButtonsBar
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.google.gwt.user.client.ui.*;

public class ButtonsBar extends Composite {
	private FlexTable	main;

	public ButtonsBar () {
		FocusPanel container;
		HTMLTable.RowFormatter formatter;

		container = new FocusPanel ();
		container.setStyleName ( "buttons-bar" );
		initWidget ( container );

		container.addMouseListener ( new MouseListener () {
			public void onMouseEnter ( Widget sender ) {
				showCells ( true );
			}

			public void onMouseLeave ( Widget sender ) {
				showCells ( false );
			}

			public void onMouseMove ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseDown ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseUp ( Widget sender, int x, int y ) {
				/* dummy */
			}
		} );

		main = new FlexTable ();
		container.add ( main );

		main.insertRow ( 0 );
		main.insertRow ( 0 );

		/*
			Nel CSS le due righe della tabella sono identificate dalle classi icons
			(dove si trovano i pulsanti) e help (dove si trovano le stringhe di
			descrizione)

			.buttons-bar .icons {
				margin-left:		5px;
				margin-right:		5px;
				text-align:		center;
			}

			.buttons-bar .help {
				margin-left:		5px;
				margin-right:		5px;
				text-align:		center;
				font-size:		10px;
			}
		*/

		formatter = main.getRowFormatter ();
		formatter.addStyleName ( 0, "icons" );
		formatter.addStyleName ( 1, "help" );
		formatter.setVisible ( 1, false );
	}

	public void add ( Widget button, String help ) {
		int col;
		Label label;

		col = main.getCellCount ( 0 );
		main.setWidget ( 0, col, button );

		if ( help != null ) {
			label = new HTML ( help );
			main.setWidget ( 1, col, label );
		}
	}

	public Widget getWidget ( int index ) {
		return main.getWidget ( 0, index );
	}

	public int getWidgetCount () {
		return main.getCellCount ( 0 );
	}

	private void showCells ( boolean show ) {
		/*
		int tot;
		Widget wid;

		tot = main.getCellCount ( 1 );

		for ( int i = 0; i < tot; i++ ) {
			wid = main.getWidget ( 1, i );
			wid.setVisible ( show );
		}
		*/

		HTMLTable.RowFormatter formatter;

		formatter = main.getRowFormatter ();
		formatter.setVisible ( 1, show );
	}
}
