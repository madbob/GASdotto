/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class Menu extends Composite {
	private FlexTable	main;
	private MainStack	parent;

	private void populateList () {
		int panels_num;
		ArrayList panels;
		GenericPanel tmp;
		HTMLTable.RowFormatter format;

		panels = parent.getPanels ();
		panels_num = panels.size ();
		format = main.getRowFormatter ();

		for ( int i = 0; i < panels_num; i++ ) {
			tmp = ( GenericPanel ) panels.get ( i );
			main.setWidget ( i, 0, tmp.getIcon () );
			main.setWidget ( i, 1, new Label ( tmp.getName () ) );
			format.setStyleName ( i, "main-menu-item" );
		}
	}

	public Menu ( MainStack stack ) {
		main = new FlexTable ();
		main.setStyleName ( "main-menu" );
		initWidget ( main );

		parent = stack;

		populateList ();

		main.addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				parent.showPanelAtPos ( row );
			}
		} );
	}

	/**
		TODO	Evidenziare in qualche modo la sezione correntemente aperta
	*/
}
