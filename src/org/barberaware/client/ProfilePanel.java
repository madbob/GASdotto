/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class ProfilePanel extends GenericPanel {
	public ProfilePanel () {
		super ();

		User user;
		FromServerForm ver;
		HorizontalPanel hor;
		FlexTable fields;

		user = Session.getUser ();
		ver = new FromServerForm ( user, FromServerForm.EDITABLE_UNDELETABLE );
		ver.alwaysOpened ( true );

		hor = new HorizontalPanel ();
		ver.add ( hor );

		fields = new FlexTable ();
		hor.add ( fields );

		/*
			Per non creare troppa confusione ho mantenuto laddove possibile lo stesso
			layout del pannello di configurazioni globale degli utenti
		*/

		fields.setWidget ( 0, 0, new Label ( "Nome" ) );
		fields.setWidget ( 0, 1, ver.getWidget ( "firstname" ) );

		fields.setWidget ( 1, 0, new Label ( "Cognome" ) );
		fields.setWidget ( 1, 1, ver.getWidget ( "surname" ) );

		fields.setWidget ( 2, 0, new Label ( "Telefono" ) );
		fields.setWidget ( 2, 1, ver.getWidget ( "phone" ) );
		ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

		fields.setWidget ( 3, 0, new Label ( "Cellulare" ) );
		fields.setWidget ( 3, 1, ver.getWidget ( "mobile" ) );
		ver.setValidation ( "mobile", FromServerValidateCallback.defaultPhoneValidationCallback () );

		fields.setWidget ( 4, 0, new Label ( "Mail" ) );
		fields.setWidget ( 4, 1, ver.getWidget ( "mail" ) );
		ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

		fields.setWidget ( 5, 0, new Label ( "Indirizzo" ) );
		fields.setWidget ( 5, 1, ver.getWidget ( "address" ) );

		fields = new FlexTable ();
		hor.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Password" ) );
		fields.setWidget ( 0, 1, ver.getPersonalizedWidget ( "password", new PasswordBox () ) );

		addTop ( ver );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Profilo Utente";
	}

	public Image getIcon () {
		return new Image ( "images/path_profile.png" );
	}

	public void initView () {
	}
}