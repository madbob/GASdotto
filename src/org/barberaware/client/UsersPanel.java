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

public class UsersPanel extends GenericPanel {
	private FormCluster		main;

	public UsersPanel () {
		super ();

		main = new FormCluster ( "User", "images/new_user.png" ) {
				protected FromServerForm doEditableRow ( FromServer u ) {
					FromServerForm ver;
					HorizontalPanel hor;
					FlexTable fields;
					User user;
					CyclicToggle privileges;

					user = ( User ) u;
					ver = new FromServerForm ( user );

					ver.setAdditionalIconsCallback ( new FromServerFormIcons () {
						public Panel retrive ( FromServer obj ) {
							HorizontalPanel hor;

							hor = new HorizontalPanel ();

							if ( obj.getBool ( "paying" ) == false )
								hor.add ( new Image ( "images/notifications/user_not_paying.png" ) );

							return hor;
						}
					} );

					hor = new HorizontalPanel ();
					ver.add ( hor );

					fields = new FlexTable ();
					hor.add ( fields );

					/**
						TODO	Costruire campo "login" parallelamente a nome e cognome (magari
							nella classica forma nome.cognome), ma se e solo se non e' gia'
							stato editato manualmente ed i due campi differiscono
							completamente
					*/

					fields.setWidget ( 0, 0, new Label ( "Login Accesso" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "login" ) );

					/**
						TODO	Prevedere aggiornamento della password, qualora qualcuno se la
							scordasse
					*/

					fields.setWidget ( 1, 0, new Label ( "Nome" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "firstname" ) );

					fields.setWidget ( 2, 0, new Label ( "Cognome" ) );
					fields.setWidget ( 2, 1, ver.getWidget ( "surname" ) );

					fields.setWidget ( 3, 0, new Label ( "Telefono" ) );
					fields.setWidget ( 3, 1, ver.getWidget ( "phone" ) );
					ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

					fields.setWidget ( 4, 0, new Label ( "Cellulare" ) );
					fields.setWidget ( 4, 1, ver.getWidget ( "mobile" ) );
					ver.setValidation ( "mobile", FromServerValidateCallback.defaultPhoneValidationCallback () );

					fields.setWidget ( 5, 0, new Label ( "Mail" ) );
					fields.setWidget ( 5, 1, ver.getWidget ( "mail" ) );
					ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

					fields.setWidget ( 6, 0, new Label ( "Indirizzo" ) );
					fields.setWidget ( 6, 1, ver.getWidget ( "address" ) );

					fields = new FlexTable ();
					hor.add ( fields );

					fields.setWidget ( 0, 0, new Label ( "Iscritto da" ) );
					fields.setWidget ( 0, 1, ver.getWidget ( "join_date" ) );

					/**
						TODO	Gestire la segnalazione del pagamento della quota in modo
							diverso. Pier suggeriva di marcare l'anno in cui e' stata pagata
							l'ultima volta, ma mi sembra poco efficiente, bisognerebbe
							trovare un compromesso
					*/
					fields.setWidget ( 1, 0, new Label ( "Quota pagata" ) );
					fields.setWidget ( 1, 1, ver.getWidget ( "paying" ) );

					fields.setWidget ( 2, 0, new Label ( "Ruolo" ) );
					privileges = new CyclicToggle ();
					privileges.addState ( "images/user_role_standard.png" );
					privileges.addState ( "images/user_role_reference.png" );
					privileges.addState ( "images/user_role_admin.png" );
					fields.setWidget ( 2, 1, ver.getPersonalizedWidget ( "privileges", privileges ) );

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					return doEditableRow ( new User () );
				}
		};

		addTop ( main );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Utenti";
	}

	public Image getIcon () {
		return new Image ( "images/path_users.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "User" );
	}
}
