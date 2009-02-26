/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class DateSelector extends Composite {
	private TextBox			main;
	private CalendarWidget		cal;
	private DialogBox		dialog;
	private boolean			opened;

	public DateSelector () {
		opened = false;

		cal = new CalendarWidget ();
		cal.addChangeListener (
			new ChangeListener () {
				public void onChange ( Widget sender ) {
					dialog.hide ();
					opened = false;
					syncDate ( cal.getDate () );
				}
			}
		);

		dialog = new DialogBox ( false );
		dialog.setWidget ( cal );

		main = new TextBox ();
		main.setStyleName ( "date-selector" );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					dialog.center ();
					dialog.show ();
				}
			}

			public void onLostFocus ( Widget sender ) {
				/* dummy */
			}
		} );

		initWidget ( main );
		clean ();
	}

	private void syncDate ( Date date ) {
		cal.setDate ( date.getYear () + 1900, date.getMonth (), date.getDate () );
		main.setText ( Utils.printableDate ( date ) );
	}

	public Date getValue () {
		Date ret;

		/*
			Qui per sapere se una data e' stata davvero selezionata o meno si
			confronta la stringa all'interno del pulsante: se e' uguale a quella
			settata quando si ripulisce, non torna null. Metodo bruttissimo e poco
			mantenibile
		*/
		if ( main.getText ().equals ( "Scegli una data" ) )
			ret = null;
		else
			ret = cal.getDate ();

		return ret;
	}

	public void setValue ( Date date ) {
		if ( date == null )
			clean ();
		else
			syncDate ( date );
	}

	public void setEnabled ( boolean enabled ) {
		main.setEnabled ( enabled );
	}

	public void clean () {
		main.setText ( "Scegli una data" );
	}
}
