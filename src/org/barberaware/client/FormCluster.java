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

public abstract class FormCluster extends FormGroup {
	private boolean				automatic;
	private boolean				addable;

	private void buildCommon ( String type, boolean auto ) {
		automatic = auto;

		Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( automatic == true ) {
					int add;

					add = addElement ( object );
					if ( add == 2 )
						return;

					customNew ( object, add == 1 );
				}
			}

			public void onModify ( FromServer object ) {
				FromServerForm iter;

				iter = refreshElement ( object );
				customModify ( iter );
			}

			public void onDestroy ( FromServer object ) {
				deleteElement ( object );
			}
		} );
	}

	public FormCluster ( String type, String icon_path, boolean auto ) {
		super ( icon_path );
		buildCommon ( type, auto );
	}

	public FormCluster ( String type, String icon_path ) {
		super ( icon_path );
		buildCommon ( type, true );
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento in
		caso di modifica di uno degli elementi della lista
	*/
	protected void customModify ( FromServerForm form ) {
		/* dummy */
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento sul
		salvataggio di uno degli elementi della lista. Il parametro true_new e' true
		quando si tratta di un nuovo oggetto proveniente dal server, false se e' stato
		salvato un oggetto editato appunto all'interno del FormCluster e di cui magari si
		vuole cambiare il contenuto del form costruito con doNewEditableRow()
	*/
	protected void customNew ( FromServer object, boolean true_new ) {
		/* dummy */
	}
}
