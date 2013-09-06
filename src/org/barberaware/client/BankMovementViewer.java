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

public class BankMovementViewer extends Composite implements ObjectWidget {
	private FromServer		object;

	private NameLabelWidget		user;
	private NameLabelWidget		supplier;
	private DateViewer		date;
	private PriceViewer		amount;
	private Label			notes;

	public BankMovementViewer () {
		FlexTable main;

		main = new FlexTable ();
		main.addStyleName ( "bankmovement-viewer" );
		main.setWidth ( "100%" );
		initWidget ( main );

		user = new NameLabelWidget ();
		main.setWidget ( 0, 0, new Label ( "Utente" ) );
		main.setWidget ( 0, 1, user );

		supplier = new NameLabelWidget ();
		main.setWidget ( 1, 0, new Label ( "Fornitore" ) );
		main.setWidget ( 1, 1, supplier );

		date = new DateViewer ();
		main.setWidget ( 2, 0, new Label ( "Data" ) );
		main.setWidget ( 2, 1, date );

		amount = new PriceViewer ();
		main.setWidget ( 3, 0, new Label ( "Importo" ) );
		main.setWidget ( 3, 1, amount );

		notes = new Label ();
		main.setWidget ( 4, 0, new Label ( "Descrizione" ) );
		main.setWidget ( 4, 1, notes );
	}

	/****************************************************************** FromServerRappresentation */

	public void setValue ( FromServer obj ) {
		object = obj;

		user.setValue ( Utils.getServer ().getObjectFromCache ( "User", object.getInt ( "payuser" ) ) );
		supplier.setValue ( Utils.getServer ().getObjectFromCache ( "Supplier", object.getInt ( "paysupplier" ) ) );
		date.setValue ( object.getDate ( "date" ) );
		amount.setVal ( object.getFloat ( "amount" ) );
		notes.setText ( object.getString ( "notes" ) );
	}

	public FromServer getValue () {
		return object;
	}
}

