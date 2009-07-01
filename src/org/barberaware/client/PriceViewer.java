/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class PriceViewer extends Label {
	private float		total;

	public PriceViewer () {
		total = 0;
		setText ( "0 €" );
	}

	public void setValue ( float price ) {
		price = ( ( float ) ( ( int ) ( price * 100 ) ) ) / 100;
		setText ( price  + " €" );
		total = price;
	}

	public void sumValue ( float price ) {
		setValue ( total + price );
	}
}
