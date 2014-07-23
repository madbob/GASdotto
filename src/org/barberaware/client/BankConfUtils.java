/*  GASdotto
 *  Copyright (C) 2014 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankConfUtils extends FromServerRappresentation {
	private DeckPanel		container;

	public BankConfUtils ( FromServer GAS ) {
		HTML message;
		CustomFormTable main;
		FlowPanel hor;
		Button button;

		container = new DeckPanel ();
		initWidget ( container );

		message = new HTML ( "Se abiliti la gestione cassa qui avrai altre opzioni avanzate a disposizione.<br />" );

		message.setStyleName ( "smaller-text" );
		container.add ( message );

		main = new CustomFormTable ();
		container.add ( main );

		button = new Button ( "Revisiona" );
		button.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				BankRevisionDialog dialog;

				dialog = new BankRevisionDialog ();
				dialog.center ();
				dialog.show ();
			}
		} );
		main.addPair ( "Revisiona Saldi", "Con questa funzione è possibile ricalcolare tutti i saldi salvati nell'applicazione, a partire dall'ultima chiusura di bilancio. Da usare qualora i conti non tornassero o per operazioni di verifica periodica.", button );

		hor = new FlowPanel ();
		button = new Button ( "Chiudi Ora" );
		button.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				BankCloseBalanceDialog dialog;

				dialog = new BankCloseBalanceDialog ();
				dialog.center ();
				dialog.show ();
			}
		} );
		hor.add ( button );
		hor.add ( new Label ( "Ultima chiusura: " + Utils.printableDate ( GAS.getDate ( "last_balance_date" ) ) ) );
		main.addPair ( "Chiudi Anno Sociale", "Pigiando questo tasto i saldi attuali del GAS, degli utenti e dei fornitori vengono duplicati dal loro stato attuale. Tutte le successive revisioni partiranno da tali valori, contemplando solo i movimenti successivi a tale evento. I movimenti antecedenti l'ultima chiusura di bilancio non possono più essere modificati.", hor );

		container.showWidget ( 0 );
	}

	public void setEnabled ( boolean enabled ) {
		container.showWidget ( enabled == true ? 1 : 0 );
	}
}
