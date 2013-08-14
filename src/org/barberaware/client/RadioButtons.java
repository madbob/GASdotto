/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class RadioButtons extends ButtonsBar implements SourcesChangeEvents {
	private int			selected		= -1;
	private ArrayList		callbacks		= null;

	public RadioButtons () {
		addStyleName ( "radio-buttons" );
	}

	public void add ( Image up, String name ) {
		ToggleButton toggle;

		toggle = new ToggleButton ( up, new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				boolean found;
				ToggleButton t;
				ToggleButton iter;

				t = ( ToggleButton ) event.getSource ();
				found = false;

				for ( int i = 0; i < getWidgetCount (); i++ ) {
					iter = ( ToggleButton ) getWidget ( i );

					if ( iter == t ) {
						if ( selected != i ) {
							selected = i;
							found = true;
							buttonUpDown ( iter, true );
						}
					}
					else {
						buttonUpDown ( iter, false );
					}
				}

				if ( found == true )
					triggerChange ();
			}
		} );

		toggle.setDown ( false );
		super.add ( toggle, name );
	}

	public int getToggled () {
		return selected;
	}

	public void setToggled ( int index ) {
		int i;
		ToggleButton iter;

		if ( index != selected ) {
			for ( i = 0; i < index; i++ ) {
				iter = ( ToggleButton ) getWidget ( i );
				buttonUpDown ( iter, false );
			}

			iter = ( ToggleButton ) getWidget ( index );
			buttonUpDown ( iter, true );

			for ( i++; i < getWidgetCount (); i++ ) {
				iter = ( ToggleButton ) getWidget ( i );
				buttonUpDown ( iter, false );
			}

			selected = index;
		}
	}

	public void propagateToggled ( int index ) {
		setToggled ( index );
		triggerChange ();
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
