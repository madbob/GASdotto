/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class UsersUneditablePanel extends GenericPanel {
	private FormCluster		main;

	public UsersUneditablePanel () {
		super ();

		main = new FormCluster ( "User", null, true ) {
			private Widget doString () {
				StringLabel s;

				s = new StringLabel ();
				s.setStyleName ( "static-value" );
				return s;
			}

			protected FromServerForm doEditableRow ( FromServer u ) {
				FromServerForm form;
				FlexTable frame;

				if ( u.getInt ( "privileges" ) == User.USER_LEAVED )
					return null;

				form = new FromServerForm ( u, FromServerForm.NOT_EDITABLE );
				form.emblemsAttach ( Utils.getEmblemsCache ( "users" ) );

				frame = new FlexTable ();
				form.add ( frame );

				frame.setWidget ( 0, 0, new Label ( "Nome" ) );
				frame.setWidget ( 0, 1, form.getPersonalizedWidget ( "firstname", doString () ) );
				frame.setWidget ( 1, 0, new Label ( "Cognome" ) );
				frame.setWidget ( 1, 1, form.getPersonalizedWidget ( "surname", doString () ) );
				frame.setWidget ( 2, 0, new Label ( "Telefono" ) );
				frame.setWidget ( 2, 1, form.getPersonalizedWidget ( "phone", doString () ) );
				frame.setWidget ( 3, 0, new Label ( "Cellulare" ) );
				frame.setWidget ( 3, 1, form.getPersonalizedWidget ( "mobile", doString () ) );
				frame.setWidget ( 4, 0, new Label ( "Mail" ) );
				frame.setWidget ( 4, 1, form.getPersonalizedWidget ( "mail", doString () ) );
				frame.setWidget ( 5, 0, new Label ( "Mail 2" ) );
				frame.setWidget ( 5, 1, form.getPersonalizedWidget ( "mail2", doString () ) );

				if ( Session.getSystemConf ().getBool ( "has_file" ) == true ) {
					frame.setWidget ( 6, 0, new Label ( "Foto" ) );
					frame.setWidget ( 6, 1, form.getPersonalizedWidget ( "photo", new ImageViewer () ) );
				}

				setRoleIcon ( form, u );
				return form;
			}

			protected FromServerForm doNewEditableRow () {
				return null;
			}
		};

		addTop ( Utils.getEmblemsCache ( "users" ).getLegend () );
		addTop ( main );
		doFilterOptions ();
	}

	private void doFilterOptions () {
		HorizontalPanel pan;
		FormClusterFilter filter;

		pan = new HorizontalPanel ();
		pan.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_LEFT );
		pan.setStyleName ( "panel-up" );
		addTop ( pan );

		filter = new FormClusterFilter ( main, new FilterCallback () {
			public boolean check ( FromServer obj, String text ) {
				int len;
				String start;

				len = text.length ();

				start = obj.getString ( "firstname" ).substring ( 0, len );
				if ( start.compareToIgnoreCase ( text ) == 0 )
					return true;

				start = obj.getString ( "surname" ).substring ( 0, len );
				if ( start.compareToIgnoreCase ( text ) == 0 )
					return true;

				return false;
			}
		} );
		pan.add ( filter );
	}

	private void setRoleIcon ( FromServerForm form, FromServer user ) {
		int priv;
		EmblemsBar bar;

		priv = user.getInt ( "privileges" );
		bar = form.emblems ();
		bar.activate ( "privileges", priv );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Lista Utenti";
	}

	public String getSystemID () {
		return "users";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_users.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "User" );
	}
}
