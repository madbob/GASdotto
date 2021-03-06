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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class GASBankSummary extends FromServerRappresentationActive {
	public GASBankSummary () {
		CustomCaptionPanel frame;
		PriceViewer price;

		setValue ( Session.getGAS () );

		frame = new CustomCaptionPanel ( "Saldo" );
		initWidget ( frame );

		price = new PriceViewer ();
		frame.addPair ( "Saldo", getPersonalizedWidget ( "current_balance", price ) );

		price = new PriceViewer ();
		frame.addPair ( "Saldo C/C", getPersonalizedWidget ( "current_bank_balance", price ) );

		price = new PriceViewer ();
		frame.addPair ( "Saldo Cassa", getPersonalizedWidget ( "current_cash_balance", price ) );

		price = new PriceViewer ();
		frame.addPair ( "Saldo Ordini", getPersonalizedWidget ( "current_orders_balance", price ) );

		price = new PriceViewer ();
		frame.addPair ( "Saldo Cauzioni", getPersonalizedWidget ( "current_deposit_balance", price ) );

		/*
			Per ogni aggiornamento che avviene ad un BankMovement
			ricarico tutta l'istanza del GAS con i saldi corretti
		*/
		Utils.getServer ().onObjectEvent ( "BankMovement", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				/* dummy */
			}

			public void onModify ( FromServer object ) {
				// Utils.getServer ().forceObjectReload ( Session.getGAS () );
			}

			public void onDestroy ( FromServer object ) {
				Utils.getServer ().forceObjectReload ( Session.getGAS () );
			}
		} );
	}
}
