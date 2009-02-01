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
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

public class FromServerResponse extends ServerResponse {
	public static int		ACTION_CREATE	= 0;
	public static int		ACTION_MODIFY	= 1;
	public static int		ACTION_DELETE	= 2;

	private int			type;
	private FromServer		reference;
	private ServerResponse		callback;

	public FromServerResponse ( int t, FromServer ref, ServerResponse resp ) {
		type = t;
		reference = ref;
		callback = resp;
	}

	public void onComplete ( JSONValue response ) {
		int localID;

		localID = Integer.parseInt ( response.isString ().stringValue () );

		if ( localID < 0 )
			Utils.showNotification ( "Errore nel salvataggio sul database" );

		else {
			reference.setLocalID ( localID );

			if ( type == ACTION_CREATE )
				Utils.getServer ().triggerObjectCreation ( reference );
			else if ( type == ACTION_MODIFY )
				Utils.getServer ().triggerObjectModification ( reference );
			else if ( type == ACTION_DELETE )
				Utils.getServer ().triggerObjectDeletion ( reference );

			if ( callback != null )
				callback.onComplete ( response );
		}
	}
}
