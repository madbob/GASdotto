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
import com.google.gwt.user.client.ui.*;

public abstract class FromServerValidateCallback {
	public boolean checkAttribute ( FromServer object, String attribute, Widget widget ) {
		/* dummy */
		return true;
	}

	public boolean checkObject ( FromServer object ) {
		/* dummy */
		return true;
	}

	public static FromServerValidateCallback defaultMailValidationCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					String text;

					text = ( ( TextBox ) widget ).getText ();
					if ( text.equals ( "" ) )
						return true;

					/**
						TODO	Finire callback validazione mail
					*/

					return true;
				}
			};
	}

	public static FromServerValidateCallback defaultPhoneValidationCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					String text;
					char c;

					text = ( ( TextBox ) widget ).getText ();
					if ( text.equals ( "" ) )
						return true;

					/*
						Per tollerare i numeri con prefisso
						internazionale (magari copiati ed icollati da
						qualche parte) il primo carattere della stringa
						puo' essere '+'
					*/
					c = text.charAt ( 0 );
					if ( Character.isDigit ( c ) == false && c != '+' )
						return false;

					/*
						Qui vengono validate anche stringhe che
						presentano spazi, che non sono numeri validi ma
						vengono comunque tollerate per venire incontro
						alla diffusa abitudine di separare i blocchi ci
						cifre
					*/
					for ( int i = 1; i < text.length (); i++ ) {
						c = text.charAt ( i );

						if ( Character.isDigit ( c ) == false && c != ' ' )
							return false;
					}

					return true;
				}
			};
	}
}
