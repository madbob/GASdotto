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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankCloseBalanceDialog extends PasswordValidateDialog {
	private DateSelector	date;

	public BankCloseBalanceDialog () {
		CustomFormTable form;

		this.setText ( "Conferma Chiusura Bilancio" );

		form = new CustomFormTable ();
		date = new DateSelector ();
		form.addPair ( "Data Chiusura", date );
		this.add ( form );
	}

	protected void confirmedExecution () {
		final String d;
		Date dat;
		RequestCallback myCallback;

		dat = date.getValue ();
		if ( dat != null )
			d = Utils.encodeDate ( dat );
		else
			d = Utils.encodeDate ( new Date ( System.currentTimeMillis () ) );

		myCallback = new RequestCallback () {
			public void onError ( Request request, Throwable exception ) {
				Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );
			}

			public void onResponseReceived ( Request request, Response response ) {
				String res;

				res = response.getText ();
				if ( res == "done" )
					Window.Location.reload ();
				else
					Utils.getServer ().rawGet ( "bank_op.php?type=close&date=" + d + "&offset=" + res, this );
			}
		};

		Utils.getServer ().rawGet ( "bank_op.php?type=close&date=" + d, myCallback );
	}
}
