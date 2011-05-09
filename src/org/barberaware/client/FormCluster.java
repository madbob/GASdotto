/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public abstract class FormCluster extends FormGroup implements Lockable {
	private String				objType;
	private boolean				automatic		= true;
	private boolean				locked			= false;
	private FromServerValidateCallback	filterCallback		= null;

	private void registerCallbacks () {
		Utils.getServer ().onObjectEvent ( objType, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int add;

				if ( filterCallback != null ) {
					if ( filterCallback.checkObject ( object ) == false )
						return;
				}

				add = 0;

				if ( automatic == true ) {
					add = addElement ( object );
					if ( add == 2 )
						return;
				}

				customNew ( object, add == 1 );
			}

			public void onModify ( FromServer object ) {
				FromServerRappresentation iter;

				iter = refreshElement ( object );
				customModify ( object, iter );
			}

			public void onDestroy ( FromServer object ) {
				deleteElement ( object );
				customDelete ( object );
			}

			protected String debugName () {
				return "FormCluster per " + objType;
			}
		} );
	}

	/*
		Il parametro "lock" permette di posticipare la registrazione delle callbacks di
		gestione degli elementi, se == true occorre invocare unlock() nel momento in cui
		si entra nel pannello
	*/
	public FormCluster ( String type, String adding_text, boolean auto, boolean lock ) {
		super ( adding_text );
		automatic = auto;
		objType = type;
		locked = lock;

		if ( lock == false )
			registerCallbacks ();
	}

	public FormCluster ( String type, String adding_text, FromServerValidateCallback filter, boolean lock ) {
		super ( adding_text );
		objType = type;
		locked = lock;
		filterCallback = filter;

		if ( lock == false )
			registerCallbacks ();
	}

	/*
		Il parametro "auto" permette di definire l'automatismo di popolamento del
		FormCluster: se == true esso viene automaticamente caricato con tutti gli oggetti
		del tipo specificato in arrivo, altrimenti e' necessario invocare manualmente
		addElement per ogni elemento da mostrare e gestire
	*/
	public FormCluster ( String type, String adding_text, boolean auto ) {
		super ( adding_text );
		automatic = auto;
		objType = type;
		registerCallbacks ();
	}

	public FormCluster ( String type, String adding_text ) {
		super ( adding_text );
		objType = type;
		registerCallbacks ();
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento in
		caso di modifica di uno degli elementi che dovrebbero stare nella lista. Viene
		invocata anche quando l'elemento stesso non e' contemplato: in tal caso il
		parametro form e' null
	*/
	protected void customModify ( FromServer object, FromServerRappresentation form ) {
		/* dummy */
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento sul
		salvataggio di uno degli elementi della lista. Il parametro true_new e' true
		quando si tratta di un nuovo oggetto proveniente dal server, false se e' stato
		salvato un oggetto editato appunto all'interno del FormCluster e di cui magari si
		vuole cambiare il contenuto del form costruito con doNewEditableRow().
		Attenzione: viene chiamata anche per i FormCluster non "automatici".
	*/
	protected void customNew ( FromServer object, boolean true_new ) {
		/* dummy */
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento in
		caso di distruzione di uno degli elementi della lista
	*/
	protected void customDelete ( FromServer object ) {
		/* dummy */
	}

	/****************************************************************** Lockable */

	public void unlock () {
		if ( locked ) {
			registerCallbacks ();
			locked = false;
		}
	}
}
