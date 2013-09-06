/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class MovementsSummary extends FromServerTable {
	public MovementsSummary ( boolean show_ref ) {
		setEmptyWarning ( "Non ci sono movimenti registrati nell'intervallo di tempo selezionato." );

		addColumn ( "Causale", "movementtype", new WidgetFactoryCallback () {
			public Widget create () {
				CyclicToggle ret;

				ret = new CyclicToggle ( false );
				ret.addStateText ( "Versamento Deposito" );
				ret.addStateText ( "Restituzione Deposito" );
				ret.addStateText ( "Quota Annuale" );
				ret.addStateText ( "Pagamento Ordine Utente" );
				ret.addStateText ( "Pagamento Ordine a Fornitore" );
				ret.addStateText ( "Versamento Credito Utente" );
				ret.addStateText ( "Acquisto GAS" );
				ret.addStateText ( "Trasferimento Interno" );
				return ret;
			}
		} );

		addColumn ( "Descrizione", "notes", false );

		addColumn ( "Data", "date", new WidgetFactoryCallback () {
			public Widget create () {
				return new DateViewer ();
			}
		} );

		if ( show_ref == true )
			addColumn ( "Riferimento", "payreference", false );

		addColumn ( "Valore", "amount", new WidgetFactoryCallback () {
			public Widget create () {
				return new PriceViewer ();
			}
		} );

		clean ( true );
	}

	public void addEditingColumns () {
		addColumn ( "Edita", FromServerTable.TABLE_EDIT, new WidgetFactoryCallback () {
			public Widget create () {
				return new BankManualUpdate ();
			}
		} );

		addColumn ( "Rimuovi", FromServerTable.TABLE_REMOVE, new WidgetFactoryCallback () {
			public Widget create () {
				return new BankRemoveDialog ();
			}
		} );
	}

	public void refresh ( ObjectRequest filters ) {
		clean ( true );

		Utils.getServer ().serverGet ( filters, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				ArrayList<FromServer> movements;

				movements = Utils.getServer ().responseToObjects ( response, "BankMovement" );
				setElements ( movements );
			}
		} );
	}
}
