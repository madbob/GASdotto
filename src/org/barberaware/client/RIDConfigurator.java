/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public class RIDConfigurator extends Composite implements StringWidget {
	private DeckPanel		container;
	private TextBox			denomination;
	private BankAccountBox		account;
	private TextBox			code;

	public RIDConfigurator () {
		HTML message;
		FlexTable main;

		container = new DeckPanel ();
		initWidget ( container );

		message = new HTML ( "Se abiliti i pagamenti RID qui dovrai immettere i dati relativi al conto corrente del GAS.<br />" +
					"Per maggiori informazioni su tale modalità di pagamento chiedi consulenza alla tua banca." );

		message.setStyleName ( "smaller-text" );
		container.add ( message );

		main = new FlexTable ();
		container.add ( main );

		denomination = new TextBox ();
		main.setWidget ( 0, 0, new Label ( "Denominazione" ) );
		main.setWidget ( 0, 1, denomination );

		account = new BankAccountBox ();
		main.setWidget ( 1, 0, new Label ( "Conto Corrente" ) );
		main.setWidget ( 1, 1, account );

		code = new TextBox ();
		main.setWidget ( 2, 0, new Label ( "Codice Azienda" ) );
		main.setWidget ( 2, 1, code );

		container.showWidget ( 0 );
	}

	public void setEnabled ( boolean enabled ) {
		container.showWidget ( enabled == true ? 1 : 0 );
	}

	/****************************************************************** StringWidget */

	public void setValue ( String value ) {
		String [] tokens;

		if ( value == null || value == "" ) {
			setEnabled ( false );
		}
		else {
			setEnabled ( true );
			tokens = value.split ( "::" );
			denomination.setText ( tokens [ 0 ] );
			account.setValue ( tokens [ 1 ] );
			code.setText ( tokens [ 2 ] );
		}
	}

	public String getValue () {
		if ( container.getVisibleWidget () == 0 ) {
			return "";
		}
		else {
			return denomination.getText () + "::" + account.getValue () + "::" + code.getText ();
		}
	}
}
