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
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class DateRange extends Composite {
	private DateSelector			startDate;
	private DateSelector			endDate;

	public DateRange () {
		Date now;
		FlexTable main;
		ChangeHandler internal_listener;

		main = new FlexTable ();
		initWidget ( main );

		internal_listener = new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
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

		endDate.addDomHandler ( internal_listener, ChangeEvent.getType () );
		startDate.addDomHandler ( internal_listener, ChangeEvent.getType () );

		main.setWidget ( 0, 0, new Label ( "Dal" ) );
		main.setWidget ( 0, 1, startDate );

		main.setWidget ( 1, 0, new Label ( "Al" ) );
		main.setWidget ( 1, 1, endDate );
	}

	public void lastWeek () {
		Date now;

		now = new Date ( System.currentTimeMillis () );
		endDate.setValue ( now );

		now = new Date ( now.getTime () - ( 7 * 24 * 3600 * 1000 ) );
		startDate.setValue ( now );
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

		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
	}
}

