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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

public class SmoothingNotify extends Composite {
	private class SingleNotification {
		public String	text;
		public int	type;

		public SingleNotification ( String te, int ty ) {
			text = te;
			type = ty;
		}
	}

	HorizontalPanel		main;
	Label			notification;
	int			staticTime;
	int			fadeFrequency;
	boolean			running;
	CircularArray		requests;

	public static int	NOTIFY_ERROR		= 0;
	public static int	NOTIFY_INFO		= 1;

	public SmoothingNotify () {
		staticTime = 4000;
		fadeFrequency = 100;

		requests = new CircularArray ();

		main = new HorizontalPanel ();
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setVisible ( false );
		main.setStyleName ( "smoothing-notify" );
		initWidget ( main );

		notification = new Label ();
		main.add ( notification );
		running = false;
	}

	private void popup () {
		SingleNotification not;

		not = ( SingleNotification ) this.requests.remove ();

		if ( not != null && not.text != null ) {
			Timer timer;

			main.setVisible ( true );
			notification.setText ( not.text );

			if ( not.type == NOTIFY_ERROR )
				main.addStyleName ( "smoothing-notify-error" );
			else if ( not.type == NOTIFY_INFO )
				main.addStyleName ( "smoothing-notify-info" );

			DOM.setStyleAttribute ( main.getElement (), "opacity", "1.0" );
			DOM.setStyleAttribute ( main.getElement (), "filter", "alpha(opacity:1.0)" );

			timer = new Timer () {
				public void run () {
					Timer fading;

					fading = new Timer () {
						public void run () {
							double opacity;
							Element tmp;

							tmp = main.getElement ();
							opacity = Double.parseDouble ( DOM.getStyleAttribute ( tmp, "opacity" ) );

							if ( opacity > 0.0 ) {
								String newvalue;
								newvalue = Double.toString ( opacity - 0.1 );
								DOM.setStyleAttribute ( tmp, "opacity", newvalue );
								DOM.setStyleAttribute ( tmp, "filter", "alpha(opacity:" + newvalue + ")" );
							}

							else {
								cancel ();
								popup ();
							}
						}
					};

					fading.scheduleRepeating ( fadeFrequency );
				}
			};

			timer.schedule ( staticTime );
			running = true;
		}
		else {
			main.setVisible ( false );
			running = false;
		}
	}

	public void show ( String text, int type ) {
		requests.add ( new SingleNotification ( text, type ) );

		if ( running == false )
			popup ();
	}
}
