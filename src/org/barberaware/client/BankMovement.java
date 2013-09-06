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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankMovement extends FromServer {
	public static int	DEPOSIT_PAYMENT		= 0;
	public static int	DEPOSIT_RETURN		= 1;
	public static int	ANNUAL_PAYMENT		= 2;
	public static int	ORDER_USER_PAYMENT	= 3;
	public static int	ORDER_PAYMENT		= 4;
	public static int	USER_CREDIT		= 5;
	public static int	GAS_BUYING		= 6;
	public static int	INTERNAL_TRANSFER	= 7;

	public static int	BY_BANK			= 0;
	public static int	BY_CASH			= 1;

	public BankMovement () {
		super ();

		addFakeAttribute ( "name", FromServer.STRING, new ValueFromObjectClosure () {
			public String retriveString ( FromServer obj ) {
				Date d = obj.getDate ( "date" );

				if ( d != null && obj.getFloat ( "amount" ) != 0 )
					return Utils.printableDate ( d );
				else
					return "Mai";
			}
		} );

		addFakeAttribute ( "payreference", FromServer.OBJECT, new ValueFromObjectClosure () {
			public FromServer retriveObject ( FromServer obj ) {
				int ref;

				ref = obj.getInt ( "payuser" );

				if ( ref < 1 ) {
					ref = obj.getInt ( "paysupplier" );

					if ( ref < 1 )
						return Session.getGAS ();
					else
						return Utils.getServer ().getObjectFromCache ( "Supplier", ref );
				}
				else {
					return Utils.getServer ().getObjectFromCache ( "User", ref );
				}
			}
		} );

		addAttribute ( "payuser", FromServer.INTEGER );
		addAttribute ( "paysupplier", FromServer.INTEGER );
		addAttribute ( "date", FromServer.DATE );
		addAttribute ( "registrationdate", FromServer.DATE );
		addAttribute ( "registrationperson", FromServer.OBJECT, User.class );
		addAttribute ( "amount", FromServer.PRICE );
		addAttribute ( "movementtype", FromServer.INTEGER );
		addAttribute ( "method", FromServer.INTEGER );
		addAttribute ( "cro", FromServer.STRING );
		addAttribute ( "notes", FromServer.LONGSTRING );

		setDate ( "registrationdate", new Date ( System.currentTimeMillis () ) );
		setObject ( "registrationperson", Session.getUser () );

		alwaysReload ( true );
	}

	public int compare ( Object first, Object second ) {
		int ret;
		float famount;
		float samount;
		Date fdate;
		Date sdate;
		FromServer f;
		FromServer s;

		ret = super.compare ( first, second );

		if ( ret == 0 ) {
			f = ( FromServer ) first;
			s = ( FromServer ) second;

			fdate = f.getDate ( "date" );
			sdate = s.getDate ( "date" );

			if ( fdate == null || sdate == null ) {
				ret = -1;
			}
			else if ( fdate.before ( sdate ) ) {
				ret = -1;
			}
			else if ( fdate.after ( sdate ) ) {
				ret = 1;
			}
			else {
				famount = f.getFloat ( "amount" );
				samount = s.getFloat ( "amount" );
				ret = ( int ) ( famount - samount );
			}
		}

		return ret;
	}

	public boolean testAmounts () {
		FromServer user;

		user = Utils.getServer ().getObjectFromCache ( "User", getInt ( "payuser" ) );

		if ( user != null ) {
			if ( user.getFloat ( "current_balance" ) < getFloat ( "amount" ) && getInt ( "method" ) == BY_BANK ) {
				Utils.infoDialog ( "Credito non Sufficiente", "Il credito disponibile per questo utente non è sufficiente! Si accettano solo pagamenti in contanti." );
				return false;
			}
		}

		return true;
	}
}

