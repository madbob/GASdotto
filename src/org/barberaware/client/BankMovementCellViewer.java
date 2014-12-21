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
import com.google.gwt.event.logical.shared.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankMovementCellViewer extends HorizontalPanel implements ObjectWidget {
	private Label			label;
	private Image			icon;
	private FromServer		object;

	public BankMovementCellViewer () {
		setWidth ( "auto" );
		setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );

		label = new Label ();
		label.setText ( "Mai" );
		icon = new Image ();
		icon.setVisible ( false );

		this.add ( label );
		this.add ( icon );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer obj ) {
		int method;

		object = obj;

		if ( obj == null ) {
			label.setText ( "Mai" );
			icon.setVisible ( false );
		}
		else {
			label.setText ( obj.getString ( "name" ) );

			method = obj.getInt ( "method" );
			if ( method == BankMovement.BY_CASH )
				icon.setUrl ( "images/notifications/pay_by_cash.png" );
			else
				icon.setUrl ( "images/notifications/pay_by_bank.png" );

			icon.setVisible ( true );
		}
	}

	public FromServer getValue () {
		return object;
	}
}
