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

import com.allen_sauer.gwt.log.client.Log;

public class DateSelector extends Composite implements DateWidget, SourcesChangeEvents {
	private TextBox				main;
	private CalendarWidget			cal;
	private Date				currentDate;
	private boolean				opened;
	private boolean				showYear;
	private ChangeListenerCollection	changeCallbacks;

	public DateSelector () {
		opened = false;
		currentDate = null;
		showYear = true;

		cal = new CalendarWidget ();
		cal.addCallback (
			new SavingDialogCallback () {
				public void onSave ( SavingDialog d ) {
					opened = false;
					syncDate ( cal.getDate () );
				}

				public void onCancel ( SavingDialog d ) {
					opened = false;
				}
			}
		);

		main = new TextBox ();
		main.setStyleName ( "date-selector" );
		main.setVisibleLength ( 40 );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					cal.setDate ( currentDate );
					cal.center ();
					cal.show ();
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
		main.setText ( Utils.printableDate ( date, showYear ) );

		if ( changeCallbacks != null )
			changeCallbacks.fireChange ( this );
	}

	public void setEnabled ( boolean enabled ) {
		main.setEnabled ( enabled );
	}

	public void clean () {
		main.setText ( "Scegli una data" );
		currentDate = null;
	}

	public void yearSelectable ( boolean selectable ) {
		cal.yearSelectable ( selectable );
	}

	public void ignoreYear ( boolean ignore ) {
		showYear = !ignore;
		cal.ignoreYear ( ignore );
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks == null )
			changeCallbacks = new ChangeListenerCollection ();
		changeCallbacks.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks != null )
			changeCallbacks.remove ( listener );
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
