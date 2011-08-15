/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class NotificationPanel extends GenericPanel {
	private FormCluster		main;

	public NotificationPanel () {
		super ();

		main = new FormCluster ( "Notification", "Nuova Notifica" ) {
				private MultiSelector destinationSelect ( final FromServerForm parent ) {
					MultiSelector users;

					users = new MultiSelector ( "User", SelectionDialog.SELECTION_MODE_ALL, null );

					if ( Session.getGAS ().getString ( "mailinglist" ) != "" && Session.getGAS ().getBool ( "use_mail" ) == true ) {
						users.addExtraElement ( "Mailing List" );

						/*
							Questo blocco serve solo a settare a priori l'invio della
							mail della notifica se viene selezionato "Mailing List" nella
							lista dei destinatari
						*/
						users.addCallback ( new SavingDialogCallback () {
							public void onSave ( SavingDialog dialog ) {
								BooleanSelector mail;
								MultiSelector users;

								users = ( MultiSelector ) dialog;

								if ( users.getExtraElement ( "Mailing List" ) == true ) {
									mail = ( BooleanSelector ) parent.retriveInternalWidget ( "send_mail" );
									mail.setValue ( true );
								}
							}
						} );
					}

					users.addSelectionCallbacks ( "Seleziona solo Referenti",
						new FilterCallback () {
							public boolean check ( FromServer obj, String text ) {
								if ( obj.getInt ( "privileges" ) == User.USER_RESPONSABLE ||
										obj.getInt ( "privileges" ) == User.USER_ADMIN )
									return true;
								else
									return false;
							}
						}
					);

					return users;
				}

				protected FromServerForm doEditableRow ( FromServer n ) {
					FromServerForm ver;
					DateSelector date;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					EnumSelector type_sel;
					MultiSelector users;

					if ( n.isValid () == true && n.getObject ( "sender" ).equals ( Session.getUser () ) == false )
						return null;

					ver = new FromServerForm ( n );

					ver.setCallback ( new FromServerFormCallbacks () {
						public boolean onSave ( FromServerForm form ) {
							MultiSelector users;

							if ( Session.getGAS ().getBool ( "use_mail" ) == true ) {
								users = ( MultiSelector ) form.retriveInternalWidget ( "recipent" );
								form.getValue ().setBool ( "send_mailinglist", users.getExtraElement ( "Mailing List" ) );
							}
							else {
								form.getValue ().setBool ( "send_mailinglist", false );
							}

							return true;
						}
					} );

					frame = new CustomCaptionPanel ( "Attributi" );
					ver.add ( frame );

					frame.addPair ( "Titolo", ver.getWidget ( "name" ) );
					frame.addPair ( "Destinatario", ver.getPersonalizedWidget ( "recipent", destinationSelect ( ver ) ) );

					date = new DateSelector ();
					frame.addPair ( "Data Inizio", ver.getPersonalizedWidget ( "startdate", date ) );
					if ( n.getDate ( "startdate" ) == null )
						date.setValue ( new Date ( System.currentTimeMillis () ) );

					frame.addPair ( "Date Fine", ver.getWidget ( "enddate" ) );
					ver.setValidation ( "enddate", FromServerValidateCallback.defaultDateValidationCallback () );

					type_sel = new EnumSelector ();
					type_sel.addItem ( "Informazione" );
					type_sel.addItem ( "Avvertimento" );
					frame.addPair ( "Tipo", ver.getPersonalizedWidget ( "alert_type", type_sel ) );

					if ( Session.getGAS ().getBool ( "use_mail" ) == true )
						frame.addPair ( "Invia anche via Mail", ver.getWidget ( "send_mail" ) );

					sframe = new CaptionPanel ( "Testo" );
					sframe.add ( ver.getWidget ( "description" ) );
					ver.add ( sframe );

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					Notification notify;

					notify = new Notification ();
					notify.setObject ( "sender", Session.getUser () );
					return doEditableRow ( notify );
				}
		};

		addTop ( main );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Notifiche";
	}

	public String getSystemID () {
		return "notifications";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_notify.png" );
	}

	public void initView () {
		ObjectRequest params;

		Utils.getServer ().testObjectReceive ( "User" );

		params = new ObjectRequest ( "Notification" );
		params.add ( "mine", 1 );
		Utils.getServer ().testObjectReceive ( params );
	}
}
