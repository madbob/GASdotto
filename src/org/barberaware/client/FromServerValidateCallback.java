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

public abstract class FromServerValidateCallback {
	public boolean check ( FromServer object, String attribute, Widget widget ) {
		/* dummy */
		return true;
	}

	public boolean checkObject ( FromServer object ) {
		/* dummy */
		return true;
	}

	/*
		Copiato da http://groups.google.com/group/Google-Web-Toolkit/browse_frm/thread/3ed2c77c45e784d7/ed59614bee075350
		Thanks to Menno van Gangelen
	*/
	private static native boolean isValidEmail ( String email ) /*-{
		var reg1 = /(@.*@)|(\.\.)|(@\.)|(\.@)|(^\.)/; // not valid
		var reg2 = /^.+\@(\[?)[a-zA-Z0-9\-\.]+\.([a-zA-Z]{2,3}|[0-9]{1,3})(\]?)$/; // valid
		return !reg1.test(email) && reg2.test(email);
	}-*/;

	public static FromServerValidateCallback defaultMailValidationCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					String text;
					boolean ret;

					text = ( ( TextBox ) widget ).getText ();
					if ( text.equals ( "" ) )
						return true;

					ret = isValidEmail ( text );
					if ( ret == false )
						Utils.showNotification ( "Mail non valida" );

					return ret;
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
					if ( Character.isDigit ( c ) == false && c != '+' ) {
						Utils.showNotification ( "Numero telefonico non valido" );
						return false;
					}

					/*
						Qui vengono validate anche stringhe che
						presentano spazi, che non sono numeri validi ma
						vengono comunque tollerate per venire incontro
						alla diffusa abitudine di separare i blocchi ci
						cifre
					*/
					for ( int i = 1; i < text.length (); i++ ) {
						c = text.charAt ( i );

						if ( Character.isDigit ( c ) == false && c != ' ' ) {
							Utils.showNotification ( "Numero telefonico non valido" );
							return false;
						}
					}

					return true;
				}
			};
	}

	public static FromServerValidateCallback defaultUniqueStringValidationCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					String text;
					boolean ret;
					FromServer iter;
					ArrayList list;

					text = ( ( StringWidget ) widget ).getValue ();

					/*
						Se la stringa e' vuota, viene considerata valida
					*/
					if ( text.equals ( "" ) )
						return true;

					/*
						Attenzione: il valore viene confrontato solo con
						quelli gia' presenti in cache
					*/

					list = Utils.getServer ().getObjectsFromCache ( object.getType () );
					ret = true;

					for ( int i = 0; i < list.size (); i++ ) {
						iter = ( FromServer ) list.get ( i );

						if ( ( iter.equals ( object ) == false ) &&
								( iter.getString ( attribute ).equals ( text ) ) ) {

							Utils.showNotification ( "Valore non univoco" );
							ret = false;
							break;
						}
					}

					return ret;
				}
			};
	}

	public static FromServerValidateCallback defaultObjectValidationCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					ObjectWidget selector;

					selector = ( ObjectWidget ) widget;

					if ( selector.getValue () == null ) {
						Utils.showNotification ( "Nessun valore selezionato" );
						return false;
					}
					else
						return true;
				}
			};
	}

	public static FromServerValidateCallback defaultDateValidationCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					DateWidget selector;

					selector = ( DateWidget ) widget;

					if ( selector.getValue () == null ) {
						Utils.showNotification ( "Devi settare la data" );
						return false;
					}
					else
						return true;
				}
			};
	}
}
