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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class BankAccountBox extends Composite implements StringWidget {
	private TextBox			abi;
	private TextBox			cab;
	private TextBox			account;

	public BankAccountBox () {
		HorizontalPanel container;

		container = new HorizontalPanel ();
		initWidget ( container );

		abi = doCell ( container, "ABI", 5 );
		cab = doCell ( container, "CAB", 5 );
		account = doCell ( container, "Conto", 12 );
	}

	private TextBox doCell ( Panel main, String name, int length ) {
		VerticalPanel cell;
		TextBox text;
		Label label;

		cell = new VerticalPanel ();
		main.add ( cell );

		label = new Label ( name );
		label.setStyleName ( "smaller-text" );
		cell.add ( label );
		cell.setCellHorizontalAlignment ( label, HasHorizontalAlignment.ALIGN_CENTER );

		text = new TextBox ();
		text.setVisibleLength ( length );
		cell.add ( text );

		return text;
	}

	/****************************************************************** StringWidget */

	public void setValue ( String value ) {
		String [] tokens;

		tokens = value.split ( " " );
		abi.setText ( tokens [ 0 ] );
		cab.setText ( tokens [ 1 ] );
		account.setText ( tokens [ 2 ] );
	}

	public String getValue () {
		return abi.getText () + " " + cab.getText () + " " + account.getText ();
	}
}
