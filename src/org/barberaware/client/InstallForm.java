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
import com.google.gwt.core.client.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class InstallForm extends Composite {
	private VerticalPanel		main;

	public InstallForm () {
		String message;
		Button installbutton;
		VerticalPanel container;

		main = new VerticalPanel ();
		main.setStyleName ( "genericpanel" );
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		initWidget ( main );

		message = "<p>Sembra che il tuo sistema GASdotto non sia ancora stato configurato.</p>";
		message += "<p>Se sei sicuro di aver già installato correttamente il tutto, potrebbe essere occorso un problema nel mentre: ricarica la pagina (premendo il tasto F5) per provare ad ottenere la scheramata di accesso.</p>";
		message += "<p>Se invece questa è la prima volta che usi GASdotto, clicca sul pulsante qui sotto per iniziare la procedura di configurazione.</p>";
		main.add ( new HTML ( message ) );

		installbutton = new Button ( "Installa GASdotto" );
		installbutton.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				doProbe ();
			}
		} );
		main.add ( installbutton );
	}

	private void doProbe () {
		ObjectRequest params;

		params = new ObjectRequest ( "Probe" );

		Utils.getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				String message;
				Probe probe;

				probe = ( Probe ) FromServer.instance ( response.isObject () );
				main.clear ();

				if ( probe.getBool ( "writable" ) == false ) {
					message = "<p>La cartella server/ o il file server/config.php non sono modificabili da questa applicazione, e non è possibile sistemare questa impostazione automaticamente.</p>";
					message += "<p>Provvedi a correggere manualmente questo problema. Probabilmente puoi farlo dall'interfaccia di file management del tuo servizio di hosting, oppure intervenendo direttamente sul server.</p>";
					message += "<p>Quando hai fatto, torna su <a href=\"GASdotto.html\">questa pagina</a> per procedere nell'installazione.</p>";
					main.add ( new HTML ( message ) );
				}
				else if ( probe.getString ( "dbdrivers" ).equals ( "" ) == true ) {
					message = "<p>Sembra che su questo server non sia installato alcun database, oppure alcun driver per poterlo utilizzare.</p>";
					message += "<p>Per usare GASdotto è necessario avere <a href=\"http://www.php.net\">PHP</a>, l'estensione <a href=\"http://www.php.net/manual/en/book.pdo.php\">PDO</a>, un database a scelta tra <a href=\"http://www.mysql.com\">MySQL</a> e <a href=\"http://www.postgresql.org/\">PostGreSQL</a> ed i relativi driver: verifica che essi siano installati, o chiedi assistenza a qualcuno con maggiore dimestichezza col PC.</p>";
					main.add ( new HTML ( message ) );
				}
				else {
					fillForm ( probe );
				}
			}
			public void onError () {
				Window.Location.reload ();
			}
		} );
	}

	private void fillForm ( Probe probe ) {
		FromServerForm form;

		form = new FromServerForm ( probe, FromServerForm.EDITABLE_UNDELETABLE_UNCANCELLABLE );
		form.alwaysOpened ( true );

		form.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerRappresentationFull form ) {
				if ( form.getValue ().getLocalID () == 1 )
					installationComplete ();
				else
					Utils.showNotification ( "E' occorso un problema durante l'installazione" );
			}
		} );

		form.add ( new HTML ( "<p>Qui devi inserire le informazioni per accedere al database che conterrà le informazioni prodotte dal programma. Esso deve già essere stato creato, magari per mezzo degli strumenti offerti dal tuo servizio di hosting.</p>" ) );
		form.add ( doDbSettingForm ( form, probe ) );
		form.add ( new HTML ( "<p>Qui definisci alcune informazioni per l'utilizzo immediato di GASdotto, potrai sempre cambiarle in futuro dal pannello 'Configurazioni'. Tieni presente che l'utente amministratore, inizializzato automaticamente, ha sempre come username \"root\".</p>" ) );
		form.add ( doConfigSettingForm ( form, probe ) );

		main.add ( form );

		main.add ( Utils.getNotificationsArea () );
	}

	private Widget doDbSettingForm ( FromServerForm form, Probe probe ) {
		String [] drivers;
		CustomCaptionPanel db;

		db = new CustomCaptionPanel ( "Impostazione del Database" );
		db.addPair ( "Nome del Database", form.getWidget ( "dbname" ) );
		db.addPair ( "Host del Database", form.getWidget ( "dbhost" ) );

		drivers = probe.getString ( "dbdrivers" ).split ( ";" );
		if ( drivers.length > 1 ) {
			db.addPair ( "Tipo di Database", form.getPersonalizedWidget ( "dbdriver", new PrimitiveStringSelect ( drivers ) ) );
		}
		else {
			/*
				Di questo non ci sarebbe bisogno perche' il driver viene gia'
				inizializzato nel Probe, ma male non fa
			*/
			drivers = drivers [ 0 ].split ( "/" );
			probe.setString ( "dbdriver", drivers [ 1 ] );
		}

		db.addPair ( "Username", form.getWidget ( "dbuser" ) );
		db.addPair ( "Password", form.getPersonalizedWidget ( "dbpassword", new PasswordBox () ) );

		return db;
	}

	private Widget doConfigSettingForm ( FromServerForm form, Probe probe ) {
		CustomCaptionPanel app;

		app = new CustomCaptionPanel ( "Informazioni Generali" );
		app.addPair ( "Nome del GAS", form.getWidget ( "gasname" ) );
		app.addPair ( "Indirizzo Mail del Gruppo", form.getWidget ( "gasmail" ) );
		app.addPair ( "Scegli una Password per l'Amministratore", form.getPersonalizedWidget ( "rootpassword", new PasswordBox () ) );
		form.setValidation ( "rootpassword", FromServerValidateCallback.defaultPasswordValidationCallback () );
		return app;
	}

	private void installationComplete () {
		String message;

		main.clear ();

		message = "<p>Installazione completata con successo.</p>";
		message += "<p>Ricaricando <a href=\"GASdotto.html\">questa pagina</a> ti verrà presentato il pannello di login: entra nell'applicazione usando username 'root' e la password che hai definito nel passaggio precedente.</p>";
		message += "<p>Per consigli ed indicazioni sull'uso di GASdotto visita <a href=\"http://gasdotto.net\">il sito del progetto</a>.</p>";
		main.add ( new HTML ( message ) );
	}
}
