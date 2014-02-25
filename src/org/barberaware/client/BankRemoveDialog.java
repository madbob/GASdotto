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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankRemoveDialog extends PasswordValidateDialog implements ObjectWidget {
	private FromServer			object;
	private BankMovementViewer		info;

	public BankRemoveDialog () {
		this.setText ( "Rimuovi Movimento" );

		this.add ( new HTML ( "Sicuro di voler rimuovere questo movimento?" ) );

		info = new BankMovementViewer ();
		this.add ( info );
	}

	protected void confirmedExecution () {
		object.destroy ( new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				int type;
				ServerHook hook;

				type = object.getInt ( "movementtype" );
				hook = Utils.getServer ();

				if ( type == BankMovement.ANNUAL_PAYMENT || type == BankMovement.ORDER_USER_PAYMENT || type == BankMovement.USER_CREDIT )
					hook.forceObjectReload ( "User", object.getInt ( "payuser" ) );
				if ( type == BankMovement.ORDER_USER_PAYMENT || type == BankMovement.ORDER_PAYMENT )
					hook.forceObjectReload ( "Supplier", object.getInt ( "paysupplier" ) );
			}
		} );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer selected ) {
		if ( selected == null )
			return;

		object = selected;
		info.setValue ( selected );
	}

	public FromServer getValue () {
		return info.getValue ();
	}
}

