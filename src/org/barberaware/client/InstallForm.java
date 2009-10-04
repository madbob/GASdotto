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

import java.lang.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class InstallForm extends Composite {
	private VerticalPanel		main;

	public InstallForm () {
		Button installbutton;
		VerticalPanel container;

		main = new VerticalPanel ();
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setSize ( "100%", "100%" );
		initWidget ( main );

		main.add ( new HTML ( "<p>Sembra che il tuo sistema GASdotto non sia ancora stato configurato.</p>" ) );
		main.add ( new HTML ( "<p>Se sei sicuro di aver già installato correttamente il tutto, potrebbe essere occorso un problema nel mentre: ricarica la pagina (premendo il tasto F5) per provare ad ottenere la scheramata di accesso.</p>" ) );
		main.add ( new HTML ( "<p>Se invece questa è la prima volta che usi GASdotto, clicca sul pulsante qui sotto per iniziare la procedura di configurazione.</p>" ) );

		installbutton = new Button ( "Installa GASdotto" );
		installbutton.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				doProbe ();
			}
		} );

		main.add ( installbutton );
	}

	private void doProbe () {
		RequestBuilder builder;
		Request response;

		builder = new RequestBuilder ( RequestBuilder.POST, ServerHook.getURL () + "probe.php" );

		try {
			builder.setTimeoutMillis ( 15000 );
			response = builder.sendRequest ( "", new ServerResponse () {
				public void onComplete ( JSONValue response ) {
					Probe probe;
					probe = ( Probe ) FromServer.instance ( response.isObject () );
					main.clear ();
					fillForm ( probe );
				}
			} );
		}
		catch ( RequestException e ) {
      			Utils.showNotification ( "Fallito invio richiesta: " + e.getMessage () );
		}
	}

	private void fillForm ( Probe probe ) {
		if ( probe.getBool ( "writable" ) == false ) {
			main.add ( new HTML ( "<p>Sembra che il file server/config.php non sia scrivibile da questa applicazione, e non è possibile modificare questa impostazione automaticamente.</p>" ) );
			main.add ( new HTML ( "<p>Per favore: provvedi a correggere manualmente questo problema. Probabilmente puoi farlo dall'interfaccia di file management del tuo servizio di hosting, oppure intervieni direttamente sul server.</p>" ) );
			main.add ( new HTML ( "<p>Quando hai fatto, torna su questa pagina per procedere nell'installazione.</p>" ) );
		}
		else {
			main.add ( doDbSettingForm () );
			main.add ( doConfigSettingForm () );
		}
	}

	private Widget doDbSettingForm () {
		TextBox dbuser;
		PasswordBox dbpassword;
		TextBox dbname;
		ListBox driver;
		FlexTable contents;
		CaptionPanel db;

		db = new CaptionPanel ( "Impostazione del Database" );
		setStyleName ( "custom-caption-panel" );

		contents = new FlexTable ();
		db.setContentWidget ( contents );

		contents.setWidget ( 0, 0, new HTML ( "<p>Qui devi inserire le informazioni per accedere al database che conterrà le informazioni prodotte dal programma.</p>" ) );
		contents.getFlexCellFormatter ().setColSpan ( 0, 0, 2 );

		contents.setWidget ( 1, 0, new Label ( "Username" ) );
		dbuser = new TextBox ();
		contents.setWidget ( 1, 1, dbuser );

		contents.setWidget ( 2, 0, new Label ( "Password" ) );
		dbpassword = new PasswordBox ();
		contents.setWidget ( 2, 1, dbpassword );

		/**
			TODO	Aggiungere dettagli
		*/

		return db;
	}

	private Widget doConfigSettingForm () {
		PasswordBox rootpassword;
		PasswordBox checkrootpassword;
		FlexTable contents;
		HorizontalPanel row;
		CaptionPanel app;

		app = new CaptionPanel ( "Informazioni Generali" );
		setStyleName ( "custom-caption-panel" );

		contents = new FlexTable ();
		app.setContentWidget ( contents );

		contents.setWidget ( 0, 0, new HTML ( "<p>Qui definisci alcune informazioni per l'utilizzo immediato di GASdotto. Tieni presente che l'utente amministratore, che verrà installato automaticamente, ha sempre come username \"root\"</p>" ) );
		contents.getFlexCellFormatter ().setColSpan ( 0, 0, 2 );

		contents.setWidget ( 1, 0, new Label ( "Password dell'Amministratore" ) );
		rootpassword = new PasswordBox ();
		contents.setWidget ( 1, 1, rootpassword );

		contents.setWidget ( 2, 0, new Label ( "Ripeti Password dell'Amministratore" ) );
		checkrootpassword = new PasswordBox ();
		contents.setWidget ( 2, 1, checkrootpassword );

		return app;
	}
}
