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

import com.google.gwt.user.client.ui.*;

public abstract class GenericPanel extends VerticalPanel {
	private boolean		showingFirstTemp;

	protected class GenericPanelHead extends Composite {
		public GenericPanelHead ( GenericPanel panel ) {
			HorizontalPanel head;

			head = new HorizontalPanel ();
			head.setStyleName ( "genericpanel-header" );
			initWidget ( head );

			head.add ( new Label ( panel.getName () ) );
			head.add ( panel.getIcon () );
		}
	}

	public GenericPanel () {
		GenericPanelHead head;

		showingFirstTemp = false;

		setStyleName ( "genericpanel" );
		setSize ( "100%", "100%" );

		head = new GenericPanelHead ( this );
		add ( head );
		setCellWidth ( head, "100%" );
		setCellHorizontalAlignment ( head, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	public void addFirstTempRow ( Widget widget ) {
		if ( showingFirstTemp == true )
			return;

		showingFirstTemp = true;
		widget.setStyleName ( "empty-list-placeholder" );
		add ( widget );
	}

	/*
		Viene wrappata la funzione insert() originale per il trattamento della eventuale
		prima riga speciale aggiunta con addFirstTempRow()
	*/
	public void insert ( Widget widget, int index ) {
		if ( showingFirstTemp == true ) {
			showingFirstTemp = false;
			remove ( 1 );
		}

		super.insert ( widget, index );
	}

	protected void addTop ( Widget to_add ) {
		insert ( to_add, 1 );
	}

	protected void addBottom ( Widget to_add ) {
		insert ( to_add, getWidgetCount () - 1 );
	}

	public abstract String getName ();
	public abstract Image getIcon ();
	public abstract void initView ();
}
