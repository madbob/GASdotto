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

import com.allen_sauer.gwt.log.client.Log;

public class RequestDesc {
	private String	params;
	private long	date;

	public RequestDesc () {
		params = "";
		date = 0;
	}

	public boolean testAndSet ( ObjectRequest testable ) {
		long now;
		boolean ret;
		String params_str;

		ret = true;
		now = System.currentTimeMillis ();
		params_str = testable.toString ();

		if ( params == params_str && now - date < 500 )
			ret = false;

		params = params_str;
		date = now;

		return ret;
	}
}
