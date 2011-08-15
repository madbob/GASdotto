/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class UserNameCell extends DummyTextBox {
	private FromServerForm		form;

	public UserNameCell ( FromServerForm parent_form ) {
		super ();

		addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				/* dummy */
			}

			public void onLostFocus ( Widget sender ) {
				String name;
				int id;

				name = getValue ();

				if ( form != null )
					id = form.getValue ().getLocalID ();
				else
					id = -1;

				Utils.getServer ().rawGet ( "data_shortcuts.php?type=unique_user&username=" + name + "&id=" + id, new RequestCallback () {
					public void onError ( Request request, Throwable exception ) {
						Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );
					}

					public void onResponseReceived ( Request request, Response response ) {
						JSONValue jsonObject;

						if ( response.getText () != "" ) {
							try {
								jsonObject = JSONParser.parse ( response.getText () );
								setValueInternal ( "" );
								Utils.showNotification ( jsonObject.isString ().stringValue () );
							}
							catch ( com.google.gwt.json.client.JSONException e ) {
								Utils.showNotification ( "Ricevuti dati invalidi dal server" );
							}
						}

						Utils.getServer ().dataArrived ();
					}
				} );
			}
		} );

		form = parent_form;
	}

	public static FromServerValidateCallback checkLoginNameCallback () {
		return
			new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					String text;
					boolean ret;
					FromServer iter;
					ArrayList list;

					text = ( ( StringWidget ) widget ).getValue ();
					if ( text.equals ( "" ) ) {
						Utils.showNotification ( "Il nome account non è stato definito" );
						return false;
					}

					/*
						Benche' l'univocita' dello username venga controllata da un'altra
						parte, mantengo comunque questo check sugli usernames locali
					*/
					list = Utils.getServer ().getObjectsFromCache ( "User" );
					ret = true;

					for ( int i = 0; i < list.size (); i++ ) {
						iter = ( FromServer ) list.get ( i );

						if ( ( iter.equals ( object ) == false ) &&
								( iter.getString ( "login" ).equals ( text ) ) ) {

							Utils.showNotification ( "Nome account non univoco" );
							ret = false;
							break;
						}
					}

					return ret;
				}
			};
	}

	/*
		Questo serve solo ad aggirare le restrizioni sulla callback
		anonima di validazione
	*/
	private void setValueInternal ( String value ) {
		setValue ( value );
	}
}
