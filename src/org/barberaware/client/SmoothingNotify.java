/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
	boolean			holdFade;
	ArrayList		pastNotifications;
	CircularArray		requests;

	public SmoothingNotify () {
		staticTime = 4000;
		fadeFrequency = 100;

		requests = new CircularArray ();
		pastNotifications = new ArrayList ();

		main = new HorizontalPanel ();
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setVisible ( false );
		main.setStyleName ( "smoothing-notify" );
		initWidget ( main );

		notification = new Label ();
		main.add ( notification );

		notification.addMouseListener ( new MouseListener () {
			public void onMouseDown ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseEnter ( Widget sender ) {
				DOM.setStyleAttribute ( main.getElement (), "opacity", "1.0" );
				DOM.setStyleAttribute ( main.getElement (), "filter", "alpha(opacity:1.0)" );
				holdFade = true;
			}

			public void onMouseLeave ( Widget sender ) {
				holdFade = false;
			}

			public void onMouseMove ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseUp ( Widget sender, int x, int y ) {
				/* dummy */
			}
		} );

		running = false;
		holdFade = false;
	}

	private void popup () {
		SingleNotification not;

		not = ( SingleNotification ) this.requests.remove ();

		if ( not != null && not.text != null ) {
			Timer timer;

			main.setVisible ( true );
			notification.setText ( not.text );

			pastNotifications.add ( Notification.instanceInternalNotification ( not.text ) );

			if ( not.type == Notification.ERROR )
				main.addStyleName ( "smoothing-notify-error" );
			else if ( not.type == Notification.WARNING )
				main.addStyleName ( "smoothing-notify-warning" );
			else if ( not.type == Notification.INFO )
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

							if ( holdFade == true )
								return;

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

	public ArrayList getNotificationsHistory () {
		return pastNotifications;
	}
}
