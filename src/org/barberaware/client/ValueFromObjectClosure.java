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

public abstract class ValueFromObjectClosure {
	public int retriveInteger ( FromServer obj ) {
		/* dummy */
		return -1;
	}

	public String retriveString ( FromServer obj ) {
		/* dummy */
		return "";
	}

	public Date retriveDate ( FromServer obj ) {
		/* dummy */
		return null;
	}

	public FromServer retriveObject ( FromServer obj ) {
		/* dummy */
		return null;
	}

	public ArrayList retriveArray ( FromServer obj ) {
		/* dummy */
		return new ArrayList ();
	}
}
