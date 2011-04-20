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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrderNotesDialog extends Composite {
	private FocusPanel		main;
	private Label			label;
	private DialogBox		dialog;
	private FlexTable		contents;

	public OrderNotesDialog () {
		HorizontalPanel container;

		main = new FocusPanel ();
		initWidget ( main );

		/*
			L'elemento parte nascosto, per poi visualizzarsi
			eventualmente quando serve
		*/
		main.addStyleName ( "hidden" );

		container = new HorizontalPanel ();
		main.add ( container );

		container.add ( new Image ( "images/has-notes.png" ) );

		label = new Label ();
		container.add ( label );

		doDialog ();

		main.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.center ();
				dialog.show ();
			}
		} );
	}

	private void doDialog () {
		VerticalPanel container;
		Button but;

		dialog = new DialogBox ();
		dialog.setText ( "Note degli Utenti" );

		container = new VerticalPanel ();
		dialog.setWidget ( container );

		contents = new FlexTable ();
		contents.setCellPadding ( 5 );
		container.add ( contents );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				dialog.hide ();
			}
		} );
		container.add ( but );
	}

	public void setOrders ( ArrayList orders ) {
		int row;
		String note;
		User user;
		OrderUser ord;

		if ( orders == null || orders.size () == 0 ) {
			main.addStyleName ( "hidden" );
		}
		else {
			contents.clear ();
			row = 0;

			for ( int i = 0; i < orders.size (); i++ ) {
				ord = ( OrderUser ) orders.get ( i );
				note = ord.getString ( "notes" );

				if ( note != null && note != "" ) {
					user = ( User ) ord.getObject ( "baseuser" );
					contents.setWidget ( row, 0, new Label ( user.getString ( "name" ) ) );
					contents.setWidget ( row, 1, new Label ( note, true ) );
					row++;
				}
			}

			if ( row != 0 ) {
				if ( row == 1 )
					label.setText ( row + " utente ha aggiunto una nota al suo ordine" );
				else
					label.setText ( row + " utenti hanno aggiunto una nota al loro ordine" );

				main.removeStyleName ( "hidden" );
			}
			else {
				main.addStyleName ( "hidden" );
			}
		}
	}
}
