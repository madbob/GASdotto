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

import com.allen_sauer.gwt.log.client.Log;

public class ProfilePanel extends GenericPanel {
	public ProfilePanel () {
		super ();

		User user;
		FromServerForm ver;
		HorizontalPanel hor;
		CustomCaptionPanel frame;
		DateSelector birth;

		user = Session.getUser ();
		ver = new FromServerForm ( user, FromServerForm.EDITABLE_UNDELETABLE );
		ver.alwaysOpened ( true );

		hor = new HorizontalPanel ();
		hor.setWidth ( "100%" );
		ver.add ( hor );

		/*
			Per non creare troppa confusione ho mantenuto laddove possibile lo stesso
			layout del pannello di configurazioni globale degli utenti
		*/

		frame = new CustomCaptionPanel ( "Anagrafica" );
		hor.add ( frame );

		frame.addPair ( "Nome", ver.getWidget ( "firstname" ) );
		frame.addPair ( "Cognome", ver.getWidget ( "surname" ) );

		frame.addPair ( "Telefono", ver.getWidget ( "phone" ) );
		ver.setValidation ( "phone", FromServerValidateCallback.defaultPhoneValidationCallback () );

		frame.addPair ( "Cellulare", ver.getWidget ( "mobile" ) );
		ver.setValidation ( "mobile", FromServerValidateCallback.defaultPhoneValidationCallback () );

		frame.addPair ( "Mail", ver.getWidget ( "mail" ) );
		ver.setValidation ( "mail", FromServerValidateCallback.defaultMailValidationCallback () );

		frame.addPair ( "Mail 2", ver.getWidget ( "mail2" ) );
		ver.setValidation ( "mail2", FromServerValidateCallback.defaultMailValidationCallback () );

		frame.addPair ( "Indirizzo", ver.getWidget ( "address" ) );

		birth = new DateSelector ();
		birth.yearSelectable ( true );
		frame.addPair ( "Data di Nascita", ver.getPersonalizedWidget ( "birthday", birth ) );

		frame.addPair ( "Persone in Famiglia", ver.getWidget ( "family" ) );

		/*
			La foto personale puo' essere personalizzato solo
			se e' concesso il caricamento di files sul server
		*/
		if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
			String path;
			FileUploadDialog photo;
			final Image image;

			photo = new FileUploadDialog ();
			image = new Image ();

			path = user.getString ( "photo" );
			if ( path == null || path == "" )
				image.setVisible ( false );
			else
				image.setUrl ( path );

			frame.addPair ( "Foto", ver.getPersonalizedWidget ( "photo", photo ) );
			frame.addRight ( image );

			photo.setDestination ( "upload_image.php" );

			photo.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					FileUploadDialog photo;

					photo = ( FileUploadDialog ) sender;
					image.setVisible ( true );
					image.setUrl ( Utils.getServer ().getDomain () + photo.getValue () );
				}
			} );
		}

		frame = new CustomCaptionPanel ( "Nel GAS" );
		hor.add ( frame );

		frame.addPair ( "Password", ver.getPersonalizedWidget ( "password", new PasswordBox () ) );
		ver.setValidation ( "password", FromServerValidateCallback.defaultPasswordValidationCallback () );

		if ( Session.getGAS ().getBool ( "use_shipping" ) == true )
			frame.addPair ( "Luogo Consegna", ver.getPersonalizedWidget ( "shipping", new FromServerSelector ( "ShippingPlace", false, false, false ) ) );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerRappresentationFull form ) {
				Utils.showNotification ( "Profilo Salvato", Notification.INFO );
			}
		} );

		addTop ( ver );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Profilo Utente";
	}

	public String getSystemID () {
		return "profile";
	}

	public Image getIcon () {
		return new Image ( "images/path_profile.png" );
	}

	public void initView () {
	}
}
