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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class RadioButtons extends Composite implements SourcesChangeEvents {
	private HorizontalPanel		main;
	private ArrayList		callbacks		= null;

	public RadioButtons () {
		main = new HorizontalPanel ();
		main.addStyleName ( "radio-buttons" );
		initWidget ( main );
	}

	public void add ( Image up ) {
		ToggleButton toggle;

		toggle = new ToggleButton ( up );
		main.add ( toggle );

		toggle.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				ToggleButton t;
				ToggleButton iter;

				t = ( ToggleButton ) sender;

				if ( t.isDown () ) {
					DOM.eventCancelBubble ( DOM.eventGetCurrentEvent (), true );
				}
				else {
					for ( int i = 0; i < main.getWidgetCount (); i++ ) {
						iter = ( ToggleButton ) main.getWidget ( i );
						buttonUpDown ( iter, false );
					}

					buttonUpDown ( t, true );
					triggerChange ();
				}
			}
		} );
	}

	public int getToggled () {
		ToggleButton iter;

		for ( int i = 0; i < main.getWidgetCount (); i++ ) {
			iter = ( ToggleButton ) main.getWidget ( i );
			if ( iter.isDown () == true )
				return i;
		}

		return -1;
	}

	public void setToggled ( int index ) {
		int i;
		ToggleButton iter;

		for ( i = 0; i < main.getWidgetCount (); i++ ) {
			iter = ( ToggleButton ) main.getWidget ( i );
			buttonUpDown ( iter, false );
		}

		iter = ( ToggleButton ) main.getWidget ( index );
		buttonUpDown ( iter, true );
	}

	private void buttonUpDown ( ToggleButton button, boolean down ) {
		button.setDown ( down );

		if ( down == true )
			button.addStyleName ( "radio-buttons-selected" );
		else
			button.removeStyleName ( "radio-buttons-selected" );
	}

	private void triggerChange () {
		Utils.triggerChangesCallbacks ( callbacks, this );
	}

	/****************************************************************** SourcesChangeEvents */

	public void addChangeListener ( ChangeListener listener ) {
		if ( callbacks == null )
			callbacks = new ArrayList ();
		callbacks.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( callbacks != null )
			callbacks.remove ( listener );
	}
}
