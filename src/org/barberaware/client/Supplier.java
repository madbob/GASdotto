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
import com.google.gwt.json.client.*;
import com.google.gwt.event.logical.shared.*;

import com.allen_sauer.gwt.log.client.Log;

public class Supplier extends FromServer {
	public static int		SHIPPING_ONCE = 0;
	public static int		SHIPPING_TO_PLACE = 1;

	public Supplier () {
		super ();

		addAttribute ( "name", FromServer.STRING );
		addAttribute ( "tax_code", FromServer.STRING );
		addAttribute ( "vat_number", FromServer.STRING );
		addAttribute ( "contact", FromServer.STRING );
		addAttribute ( "phone", FromServer.STRING );
		addAttribute ( "fax", FromServer.STRING );
		addAttribute ( "mail", FromServer.STRING );
		addAttribute ( "website", FromServer.STRING );
		addAttribute ( "address", FromServer.ADDRESS );
		addAttribute ( "order_mode", FromServer.LONGSTRING );
		addAttribute ( "paying_mode", FromServer.LONGSTRING );
		addAttribute ( "paying_by_bank", FromServer.BOOLEAN );
		addAttribute ( "description", FromServer.LONGSTRING );
		addAttribute ( "references", FromServer.ARRAY, User.class );
		addAttribute ( "carriers", FromServer.ARRAY, User.class );
		addAttribute ( "files", FromServer.ARRAY, CustomFile.class );
		addAttribute ( "orders_months", FromServer.STRING );
		addAttribute ( "shipping_manage", FromServer.INTEGER );
		addAttribute ( "hidden", FromServer.BOOLEAN );
		addAttribute ( "current_balance", FromServer.FLOAT );

		setString ( "name", "Nuovo Fornitore" );
		isSharable ( true );
	}

	public boolean iAmReference () {
		User myself;
		int privileges;
		ArrayList<FromServer> references;

		myself = Session.getUser ();
		privileges = myself.getInt ( "privileges" );

		if ( privileges == User.USER_RESPONSABLE || privileges == User.USER_ADMIN ) {
			references = getArray ( "references" );
			if ( references == null )
				return false;

			for ( FromServer ref : references )
				if ( ref.equals ( myself ) )
					return true;
		}

		return false;
	}

	public boolean iAmCarrier () {
		User myself;
		int privileges;
		ArrayList<FromServer> references;

		myself = Session.getUser ();
		privileges = myself.getInt ( "privileges" );

		if ( privileges == User.USER_RESPONSABLE || privileges == User.USER_ADMIN ) {
			references = getArray ( "carriers" );
			if ( references == null )
				return false;

			for ( FromServer ref : references )
				if ( ref.equals ( myself ) )
					return true;
		}

		return false;
	}

	public static CyclicToggle doSupplierShippingSelector ( boolean active ) {
		CyclicToggle shipping;

		shipping = new CyclicToggle ( active );
		shipping.addState ( "images/shipping_once.png" );
		shipping.addState ( "images/shipping_to_place.png" );
		shipping.setDefaultSelection ( 0 );
		return shipping;
	}

	public BooleanSelector doSupplierNotificationsSelector ( final User for_user ) {
		ArrayList<FromServer> preferred_suppliers;
		final Supplier supplier;
		BooleanSelector notify;

		supplier = this;

		notify = new BooleanSelector ();
		notify.setVal ( false );

		preferred_suppliers = for_user.getArray ( "suppliers_notification" );
		for ( FromServer preferred_supplier : preferred_suppliers ) {
			if ( preferred_supplier.equals ( this ) == true ) {
				notify.setVal ( true );
				break;
			}
		}

		notify.addValueChangeHandler ( new ValueChangeHandler<Boolean> () {
			public void onValueChange ( ValueChangeEvent<Boolean> event ) {
				if ( event.getValue () == true )
					for_user.addToArray ( "suppliers_notification", supplier );
				else
					for_user.removeFromArray ( "suppliers_notification", supplier );

				for_user.save ( new ServerResponse () {
					public void onComplete ( JSONValue response ) {
						Utils.showNotification ( "Configurazione salvata", Notification.INFO );
					}
				} );
			}
		} );

		return notify;
	}
}
