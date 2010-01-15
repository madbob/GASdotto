/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class StatisticsPanel extends GenericPanel {
	private VerticalPanel		main;
	private LinksDialog		files;
	private DateSelector		startDate;
	private DateSelector		endDate;

	public StatisticsPanel () {
		super ();

		Date now;
		Date past;
		ChangeListener listener;
		CaptionPanel frame;
		FlexTable input;

		main = new VerticalPanel ();
		addTop ( main );

		frame = new CaptionPanel ( "Report per Utenti/Fornitori" );
		main.add ( frame );

		input = new FlexTable ();
		frame.setContentWidget ( input );

		listener = new ChangeListener () {
			public void onChange ( Widget sender ) {
				performUpdate ();
			}
		};

		now = new Date ( System.currentTimeMillis () );
		endDate = new DateSelector ();
		endDate.setValue ( now );

		now = ( Date ) now.clone ();
		now.setYear ( now.getYear () - 1 );
		startDate = new DateSelector ();
		startDate.setValue ( now );

		startDate.addChangeListener ( listener );
		input.setWidget ( 1, 0, new Label ( "Dal" ) );
		input.setWidget ( 1, 1, startDate );

		endDate.addChangeListener ( listener );
		input.setWidget ( 2, 0, new Label ( "Al" ) );
		input.setWidget ( 2, 1, endDate );

		files = new LinksDialog ( "Scarica Statistiche" );
		input.setWidget ( 3, 0, files );
		input.getFlexCellFormatter ().setColSpan ( 3, 0, 2 );
	}

	private void updateLinks () {
		files.emptyBox ();
		files.addLink ( "CVS", "graph_data.php?document=csv&amp;graph=0&amp;startdate=" + Utils.encodeDate ( startDate.getValue () ) + "&amp;enddate=" + Utils.encodeDate ( endDate.getValue () ) );
		files.addLink ( "PDF", "graph_data.php?document=pdf&amp;graph=0&amp;startdate=" + Utils.encodeDate ( startDate.getValue () ) + "&amp;enddate=" + Utils.encodeDate ( endDate.getValue () ) );
	}

	private void performUpdate () {
		updateLinks ();
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Statistiche";
	}

	public String getSystemID () {
		return "statistics";
	}

	public Image getIcon () {
		return new Image ( "images/path_stats.png" );
	}

	public void initView () {
		updateLinks ();
	}
}
