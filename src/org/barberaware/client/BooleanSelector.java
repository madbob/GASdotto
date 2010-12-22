/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;

public class BooleanSelector extends ToggleButton implements SourcesChangeEvents, BooleanWidget {
	private boolean				currentState;
	private ChangeListenerCollection	changeCallbacks;

	public BooleanSelector () {
		super ( "NO", "SI" );
		setStyleName ( "boolean-selector" );

		currentState = false;

		addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				BooleanSelector myself;

				myself = ( BooleanSelector ) sender;
				checkChange ( myself );
			}
		} );

		addKeyboardListener ( new KeyboardListener () {
			public void onKeyDown ( Widget sender, char keyCode, int modifiers ) {
				/* dummy */
			}

			public void onKeyPress ( Widget sender, char keyCode, int modifiers ) {
				/* dummy */
			}

			public void onKeyUp ( Widget sender, char keyCode, int modifiers ) {
				BooleanSelector myself;

				myself = ( BooleanSelector ) sender;
				checkChange ( myself );
			}
		} );
	}

	private void checkChange ( BooleanSelector myself ) {
		if ( currentState != myself.isDown () ) {
			currentState = myself.isDown ();

			if ( changeCallbacks != null )
				changeCallbacks.fireChange ( this );
		}
	}

	/****************************************************************** BooleanWidget */

	public void setValue ( boolean value ) {
		currentState = value;
		super.setDown ( value );
	}

	public boolean getValue () {
		return isDown ();
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
