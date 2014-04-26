/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import java.lang.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class UpdateNotice extends Composite {
	public UpdateNotice () {
		String version;
		String message;
		VerticalPanel main;

		main = new VerticalPanel ();
		main.setStyleName ( "genericpanel" );
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		initWidget ( main );

		version = Session.platformUpdate ();

		message = "<p>GASdotto è stato aggiornato alla versione " + version + ".</p>";
		message += "<p>Occorre correggere manualmente il relativo file di configurazione, seguendo i seguenti passi:</p>";
		message += "<p>1) apri il file server/config.php che si trova sul server</p>";
		message += "<p>2) individua la linea che inizia con \"$dbversion\"</p>";
		message += "<p>3) sostituiscila con</p>";
		message += "<p>$dbversion = \"" + version + "\";</p>";
		message += "<p>4) salva il file</p>";
		message += "<p>Se la suddetta linea non esiste, aggiungila al fondo del file stesso.</p>";
		message += "<p>Quando hai finito ricarica questa pagina (premendo il tasto F5) per ottenere la scheramata di accesso.</p>";
		main.add ( new HTML ( message ) );
	}
}

