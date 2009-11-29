/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class DateSelector extends Composite implements DateWidget {
	private TextBox			main;
	private CalendarWidget		cal;
	private Date			currentDate;
	private DialogBox		dialog;
	private boolean			opened;

	public DateSelector () {
		opened = false;
		currentDate = null;

		cal = new CalendarWidget ();
		cal.addCallback (
			new SavingDialogCallback () {
				private void commonClose () {
					dialog.hide ();
					opened = false;
				}

				public void onSave ( SavingDialog d ) {
					commonClose ();
					syncDate ( cal.getDate () );
				}

				public void onCancel ( SavingDialog d ) {
					commonClose ();
				}
			}
		);

		dialog = new DialogBox ( false );
		dialog.setWidget ( cal );

		main = new TextBox ();
		main.setStyleName ( "date-selector" );
		main.setVisibleLength ( 17 );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					cal.setDate ( currentDate );
					dialog.center ();
					dialog.show ();
				}
			}

			public void onLostFocus ( Widget sender ) {
				/* dummy */
			}
		} );

		initWidget ( main );
		clean ();
	}

	private void syncDate ( Date date ) {
		currentDate = date;
		main.setText ( Utils.printableDate ( date ) );
	}

	public void setEnabled ( boolean enabled ) {
		main.setEnabled ( enabled );
	}

	public void clean () {
		main.setText ( "Scegli una data" );
		currentDate = null;
	}

	/****************************************************************** DateWidget */

	public void setValue ( Date date ) {
		if ( date == null )
			clean ();
		else
			syncDate ( date );
	}

	public Date getValue () {
		return currentDate;
	}
}
