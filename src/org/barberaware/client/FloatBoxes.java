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
import java.lang.*;
import com.google.gwt.user.client.ui.*;

public class FloatBoxes extends VerticalPanel implements FloatWidget {
	public FloatBoxes () {
	}

	public FloatBox addBox () {
		FloatBox box;

		box = new FloatBox ();
		add ( box );
		return box;
	}

	/****************************************************************** FloatWidget */

	public void setVal ( float value ) {
		/* dummy */
	}

	public float getVal () {
		float sum;
		FloatBox box;

		sum = 0;

		for ( int i = 0; i < getWidgetCount (); i++ ) {
			box = ( FloatBox ) getWidget ( i );
			sum += box.getVal ();
		}

		return sum;
	}
}
