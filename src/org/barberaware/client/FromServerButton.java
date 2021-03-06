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

import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

public class FromServerButton extends Button {
	private FromServer reference;
	private FromServerCallback callback;

	public FromServerButton ( FromServer ref, String text, FromServerCallback call ) {
		super ( text );

		reference = ref;
		callback = call;

		addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				callback.execute ( reference );
			}
		} );
	}
}
