/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.log.client.Log;

public class User extends FromServer {
	public static int	USER_COMMON		= 0;
	public static int	USER_RESPONSABLE	= 1;
	public static int	USER_ADMIN		= 2;
	public static int	USER_LEAVED		= 3;
	public static int	USER_MASTER		= 4;

	public User () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				String name;
				String surname;

				name = obj.getString ( "firstname" );
				surname = obj.getString ( "surname" );

				if ( name.equals ( "" ) && surname.equals ( "" ) ) {
					return "Nuovo Utente";
				}
				else {
					if ( name.equals ( "" ) )
						return surname;
					else if ( surname.equals ( "" ) )
						return name;
					else
						return surname + " " + name;
				}
			}
		} );

		addAttribute ( "login", FromServer.STRING );
		addAttribute ( "password", FromServer.STRING );
		addAttribute ( "firstname", FromServer.STRING );
		addAttribute ( "surname", FromServer.STRING );
		addAttribute ( "birthday", FromServer.DATE );
		addAttribute ( "join_date", FromServer.DATE );
		addAttribute ( "card_number", FromServer.STRING );
		addAttribute ( "phone", FromServer.STRING );
		addAttribute ( "mobile", FromServer.STRING );
		addAttribute ( "mail", FromServer.STRING );
		addAttribute ( "mail2", FromServer.STRING );
		addAttribute ( "address", FromServer.ADDRESS );
		addAttribute ( "paying", FromServer.OBJECT, BankMovement.class );
		addAttribute ( "privileges", FromServer.INTEGER );
		addAttribute ( "family", FromServer.INTEGER );
		addAttribute ( "photo", FromServer.STRING );
		addAttribute ( "codfisc", FromServer.STRING );
		addAttribute ( "lastlogin", FromServer.DATE );
		addAttribute ( "leaving_date", FromServer.DATE );

		/*
			"bank_account" fa riferimento alle coordinate bancarie
			da usare nei RID (quando attivati)
			"current_balance" fa riferimento al "conto" gestito
			dalla cassa interna
		*/
		addAttribute ( "bank_account", FromServer.STRING );
		addAttribute ( "current_balance", FromServer.FLOAT );
		addAttribute ( "deposit", FromServer.OBJECT, BankMovement.class );

		/*
			Data di autorizzazione per l'addebito sul conto corrente
			e data del primo addebito (se i RID sono attivi)
		*/
		addAttribute ( "sepa_subscribe", FromServer.DATE );
		addAttribute ( "first_sepa", FromServer.DATE );

		addAttribute ( "shipping", FromServer.OBJECT, ShippingPlace.class );
		addAttribute ( "suppliers_notification", FromServer.ARRAY, Supplier.class );

		setDate ( "join_date", new Date ( System.currentTimeMillis () ) );

		alwaysReload ( true );
		alwaysSendObject ( "paying", true );
		alwaysSendObject ( "deposit", true );
	}

	public void checkUserPaying ( FromServerForm form ) {
		FromServer last_pay;
		Date gas_date;
		Date now;
		Date pay_date;
		EmblemsBar bar;

		bar = form.emblems ();

		gas_date = Session.getGAS ().getDate ( "payment_date" );
		if ( gas_date == null ) {
			bar.deactivate ( "paying" );
			return;
		}

		last_pay = this.getObject ( "paying" );
		if ( last_pay == null || last_pay.getFloat ( "amount" ) == 0 ) {
			bar.activate ( "paying" );
			return;
		}

		pay_date = last_pay.getDate ( "date" );
		if ( pay_date == null ) {
			bar.activate ( "paying" );
			return;
		}

		now = new Date ( System.currentTimeMillis () );

		/*
			Algoritmone scritto da Pier!
		*/

		if ( now.getMonth () >= gas_date.getMonth () )
			gas_date.setYear ( now.getYear () );
		else
			gas_date.setYear ( now.getYear () - 1 );

		if ( pay_date.before ( gas_date ) )
			bar.activate ( "paying" );
		else
			bar.deactivate ( "paying" );
	}
}
