/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class FromServerRappresentationCallbacks {
	private String		identifier	= "";

	public FromServerRappresentationCallbacks () {
	}

	public FromServerRappresentationCallbacks ( String id ) {
		identifier = id;
	}

	public String getID () {
		return identifier;
	}

	/*
		Invocata quando viene aggiunto un nuovo child, serve a
		costruire il relativo widget (che, eventualmente, deve essere
		ritornato)
	*/
	public FromServerRappresentation onAddChild ( FromServerRappresentation form, FromServer child ) {
		/* dummy */
		return null;
	}

	/*
		Invocata quando deve essere rimosso un child, torna TRUE se
		deve essere rimosso anche il relativo widget o meno.
		Un caso in cui un child viene rimosso, ma non il suo widget:
		quando nel pannello degli ordini viene rimosso un ordine
		dell'utente, ma deve restare il pezzo di pannello per poterlo
		ri-popolare nuovamente
	*/
	public boolean onRemoveChild ( FromServerRappresentation form ) {
		/* dummy */
		return true;
	}
}
