/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

/*
	Questo widget viene usato in sostituzione di FocusPanel laddove si
	devono catturare solo gli spostamenti del mouse.
	GWT ha un bug noto percui in WebKit gli eventi di focus e blur vanno in
	conflitto tra il FocusPanel ed eventuali altri FocusWidget contenuti.
	http://code.google.com/p/google-web-toolkit/issues/detail?id=1471
*/

public class MousePanel extends SimplePanel implements SourcesMouseEvents {
	private MouseListenerCollection mouseListeners;

	public MousePanel () {
		sinkEvents ( Event.MOUSEEVENTS );
	}

	public void onBrowserEvent ( Event event ) {
		switch ( DOM.eventGetType ( event ) ) {
			case Event.ONMOUSEDOWN:
			case Event.ONMOUSEUP:
			case Event.ONMOUSEMOVE:
			case Event.ONMOUSEOVER:
			case Event.ONMOUSEOUT:
				if ( mouseListeners != null )
					mouseListeners.fireMouseEvent ( this, event );
				break;
		}
	}

	public void addMouseListener ( MouseListener listener ) {
		if ( mouseListeners == null )
			mouseListeners = new MouseListenerCollection ();
		mouseListeners.add ( listener );
	}

	public void removeMouseListener ( MouseListener listener ) {
		if ( mouseListeners != null )
			mouseListeners.remove ( listener );
	}
}

