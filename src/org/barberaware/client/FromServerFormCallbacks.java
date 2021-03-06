/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class FromServerFormCallbacks {
	private String		identifier	= "";

	public FromServerFormCallbacks () {
	}

	public FromServerFormCallbacks ( String id ) {
		identifier = id;
	}

	public String getID () {
		return identifier;
	}

	public String getName ( FromServerRappresentationFull form ) {
		return form.getValue ().getString ( "name" );
	}

	/*
		Invocata quando viene richiesto di salvare il contenuto del form, *prima* che i
		valori editati nei campi siano effettivamente assegnati all'oggetto: in questo
		modo si possono verificare e modificare al volo
	*/
	public boolean onSave ( FromServerRappresentationFull form ) {
		/* dummy */
		return true;
	}

	/*
		Invocata alla conferma di salvataggio dell'oggetto gestito dal form
	*/
	public void onSaved ( FromServerRappresentationFull form ) {
		/* dummy */
	}

	public void onReset ( FromServerRappresentationFull form ) {
		/* dummy */
	}

	/*
		Invocata quando viene richiesto di eliminare l'oggetto rappresentato dal form, se
		ritorna false la procedura di eliminazione viene interrotta
	*/
	public boolean onDelete ( FromServerRappresentationFull form ) {
		/* dummy */
		return true;
	}

	public void onOpen ( FromServerRappresentationFull form ) {
		/* dummy */
	}

	/*
		Invocata quando viene richiesto di chiudere il pannello, permette di intervenire
		sui contenuti del form prima che questi vengano analizzati e sia eventualmente
		proposto il salvataggio
	*/
	public void onClosing ( FromServerRappresentationFull form ) {
		/* dummy */
	}

	/*
		Invocata quando il form e' stato completamente chiuso, particolarmente indicata
		per re-inizializzarlo
	*/
	public void onClose ( FromServerRappresentationFull form ) {
		/* dummy */
	}

	/*
		Invocata quando una operazione di salvataggio fallisce
	*/
	public void onError ( FromServerRappresentationFull form ) {
		/* dummy */
	}
}
