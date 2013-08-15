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

public class UserMovementsSummary extends Composite {
	private MovementsSummary	mainTable;
	private DateRange		dates;
	private FromServer		target;

	public UserMovementsSummary ( FromServer user ) {
		VerticalPanel main;
		HorizontalPanel hor;

		target = user;

		main = new VerticalPanel ();
		initWidget ( main );

		dates = new DateRange ();
		dates.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				refresh ();
			}
		} );

		main.add ( dates );

		mainTable = new MovementsSummary ( false );
		main.add ( mainTable );
	}

	public void refresh () {
		ObjectRequest params;

		params = new ObjectRequest ( "BankMovement" );
		params.add ( "startdate", Utils.encodeDate ( dates.getStartDate () ) );
		params.add ( "enddate", Utils.encodeDate ( dates.getEndDate () ) );
		params.add ( "payuser", Integer.toString ( target.getLocalID () ) );

		mainTable.refresh ( params );
	}
}

