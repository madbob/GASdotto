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

import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class FeeConfigurator extends FromServerRappresentation {
	private DeckPanel		container;

	public FeeConfigurator ( FromServer GAS ) {
		HTML message;
		CustomFormTable main;
		DateSelector paydate;
		
		setValue ( GAS );

		container = new DeckPanel ();
		initWidget ( container );

		message = new HTML ( "Se abiliti le quote dei soci qui potrai specificarne i dettagli.<br />" );

		message.setStyleName ( "smaller-text" );
		container.add ( message );

		main = new CustomFormTable ();
		container.add ( main );

		paydate = new DateSelector ();
		paydate.ignoreYear ( true );
		main.addPair ( "Inizio Anno Sociale", getPersonalizedWidget ( "payment_date", paydate ) );
		
		main.addPair ( "Quota Annuale", getWidget ( "default_fee" ) );
		main.addPair ( "Deposito", getWidget ( "default_deposit" ) );

		container.showWidget ( 0 );
	}

	public void setEnabled ( boolean enabled ) {
		container.showWidget ( enabled == true ? 1 : 0 );
	}
}
