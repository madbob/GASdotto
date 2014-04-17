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

public class ProductUserVariant extends FromServer {
	public ProductUserVariant () {
		super ();

		addAttribute ( "delivered", FromServer.BOOLEAN );
		addAttribute ( "components", FromServer.ARRAY, ProductUserVariantComponent.class );

		alwaysSendObject ( "components", true );
	}

	public int compare ( Object first, Object second ) {
		int ret;
		FromServer f;
		FromServer s;

		ret = super.compare ( first, second );
		if ( ret != 0 )
			return ret;

		f = ( FromServer ) first;
		s = ( FromServer ) second;

		if ( Utils.compareFromServerArray ( f.getArray ( "components" ), s.getArray ( "components" ) ) == false )
			return 1;

		return 0;
	}

	public String getTextSummary () {
		int num;
		String ret;
		ArrayList components;
		FromServer component;

		components = getArray ( "components" );
		num = components.size ();
		ret = "";

		for ( int i = 0; i < num; i++ ) {
			component = ( FromServer ) components.get ( i );
			ret = ret + component.getObject ( "variant" ).getString ( "name" ) + ": " + component.getObject ( "value" ).getString ( "name" );

			if ( i != num - 1 )
				ret += ", ";
		}

		return ret;
	}
}
