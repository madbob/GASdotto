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
import com.google.gwt.user.client.ui.*;

public class GAS extends FromServer {
	public GAS () {
		super ();
		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "is_master", FromServer.BOOLEAN );
		addAttribute ( "mail", FromServer.STRING );
		addAttribute ( "image", FromServer.STRING );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "use_mail", FromServer.BOOLEAN );
		addAttribute ( "mail_conf", FromServer.STRING );
		addAttribute ( "mailinglist", FromServer.STRING );
		addAttribute ( "payments", FromServer.BOOLEAN );
		addAttribute ( "payment_date", FromServer.DATE );
		addAttribute ( "default_fee", FromServer.FLOAT );
		addAttribute ( "default_deposit", FromServer.FLOAT );
		addAttribute ( "use_rid", FromServer.BOOLEAN );
		addAttribute ( "rid_conf", FromServer.STRING );
		addAttribute ( "use_shipping", FromServer.BOOLEAN );
		addAttribute ( "use_fullusers", FromServer.BOOLEAN );
		addAttribute ( "use_bank", FromServer.BOOLEAN );
		addAttribute ( "current_balance", FromServer.FLOAT );
		addAttribute ( "current_bank_balance", FromServer.FLOAT );
		addAttribute ( "current_cash_balance", FromServer.FLOAT );
		addAttribute ( "current_orders_balance", FromServer.FLOAT );
		addAttribute ( "current_deposit_balance", FromServer.FLOAT );
		addAttribute ( "last_balance_date", FromServer.DATE );
		addAttribute ( "last_balance", FromServer.FLOAT );
		addAttribute ( "last_bank_balance", FromServer.FLOAT );
		addAttribute ( "last_cash_balance", FromServer.FLOAT );
		addAttribute ( "last_orders_balance", FromServer.FLOAT );
		addAttribute ( "last_deposit_balance", FromServer.FLOAT );
		addAttribute ( "files", FromServer.ARRAY, CustomFile.class );
		addAttribute ( "emergency_access", FromServer.BOOLEAN );

		setString ( "name", "Senza Nome" );
	}
}
