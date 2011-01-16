/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProductUserVariantComponent extends FromServer {
	public ProductUserVariantComponent () {
		super ();

		addAttribute ( "variant", FromServer.OBJECT, ProductVariant.class );
		addAttribute ( "value", FromServer.OBJECT, ProductVariantValue.class );
	}

	public int compare ( Object first, Object second ) {
		int ret;
		ProductUserVariantComponent f;
		ProductUserVariantComponent s;
		ProductVariantValue value;

		f = ( ProductUserVariantComponent ) first;
		s = ( ProductUserVariantComponent ) second;

		ret = super.compare ( f, s );

		if ( ret == 0 ) {
			value = ( ProductVariantValue ) f.getObject ( "value" );
			ret = value.compare ( value, s.getObject ( "value" ) );
		}

		return ret;
	}
}
