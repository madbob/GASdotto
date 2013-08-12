/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class DateRange extends Composite implements SourcesChangeEvents {
	private DateSelector			startDate;
	private DateSelector			endDate;

	private ChangeListenerCollection	changeCallbacks;

	public DateRange () {
		Date now;
		FlexTable main;
		ChangeListener internal_listener;

		main = new FlexTable ();
		initWidget ( main );

		internal_listener = new ChangeListener () {
			public void onChange ( Widget sender ) {
				verifyAndFire ();
			}
		};

		now = new Date ( System.currentTimeMillis () );
		endDate = new DateSelector ();
		endDate.setValue ( now );

		now = ( Date ) now.clone ();
		now.setMonth ( now.getMonth () - 1 );
		startDate = new DateSelector ();
		startDate.setValue ( now );

		endDate.addChangeListener ( internal_listener );
		startDate.addChangeListener ( internal_listener );

		main.setWidget ( 0, 0, new Label ( "Dal" ) );
		main.setWidget ( 0, 1, startDate );

		main.setWidget ( 1, 0, new Label ( "Al" ) );
		main.setWidget ( 1, 1, endDate );
	}

	public Date getStartDate () {
		return startDate.getValue ();
	}

	public Date getEndDate () {
		return endDate.getValue ();
	}

	public Widget getStartDateWidget () {
		return startDate;
	}

	public Widget getEndDateWidget () {
		return endDate;
	}

	public boolean testBetween ( Date test ) {
		Date s;
		Date e;

		if ( test == null )
			return false;

		s = getStartDate ();
		e = getEndDate ();

		return ( ( test.after ( s ) && test.before ( e ) ) || ( test.equals ( s ) || test.equals ( e ) ) );
	}

	private void verifyAndFire () {
		Date s;
		Date e;

		s = getStartDate ();
		e = getEndDate ();

		if ( s.after ( e ) ) {
			Utils.showNotification ( "La data di partenza è posteriore alla data di fine selezione" );
			Utils.graphicPulseWidget ( startDate );
			Utils.graphicPulseWidget ( endDate );
			return;
		}

		if ( changeCallbacks != null )
			changeCallbacks.fireChange ( this );
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
}

