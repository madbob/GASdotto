/*  GASdotto
 *  Copyright (C) 2013/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import java.lang.*;
import java.util.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class FromServerAggregateVirtual extends FromServerAggregate {
	public FromServerAggregateVirtual ( String attribute ) {
		super ( attribute );

		/*
			La classe FromServerAggregateVirtual serve per gli ogggetti che non verranno mai mappati sul
			database, ne' tantomeno spediti al server. Pertanto non hanno mai un ID valido, ma gliene
			forzo uno per evitare che da qualche parte nel codice questi vengano interpretati come
			oggetti non validi
		*/
		setLocalID ( ( int ) Math.round ( Math.random () * 1000 ) );
	}

	public void destroy ( ServerResponse callback ) {
		ArrayList objects;
		FromServer tmp;

		objects = getObjects ();

		for ( int i = 0; i < objects.size (); i++ ) {
			tmp = ( FromServer ) objects.get ( i );

			/**
				TODO	Qui ignoro deliberatamente la
					callback fornita, non essendo
					ancora gestite funzioni di
					accumulazione
			*/
			tmp.destroy ( null );
		}
	}
}
