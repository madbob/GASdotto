/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		installbutton.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				doProbe ( 0 );
			}
		} );
		main.add ( installbutton );

		main.add ( new HTML ( "<br /><br /><br />" ) );
		main.add ( new HTML ( "<p>Oppure, se hai già una istanza di una vecchia versione installata, clicca qui per importare i dati da essa.</p>" ) );

		installbutton = new Button ( "Importa da versione già installata" );
		installbutton.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				doProbe ( 1 );
			}
		} );
		main.add ( installbutton );
	}

	private void doProbe ( final int mode ) {
		ObjectRequest params;

		params = new ObjectRequest ( "Probe" );

		Utils.getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				String message;
				Probe probe;

				probe = ( Probe ) FromServer.instance ( response.isObject () );
				main.clear ();

				if ( probe.getBool ( "writable" ) == false ) {
					message = "<p>Sembra che il file server/config.php non sia scrivibile da questa applicazione, e non è possibile modificare questa impostazione automaticamente.</p>";
					message += "<p>Per favore: provvedi a correggere manualmente questo problema. Probabilmente puoi farlo dall'interfaccia di file management del tuo servizio di hosting, oppure intervieni direttamente sul server.</p>";
					message += "<p>Quando hai fatto, torna su <a href=\"GASdotto.html\">questa pagina</a> per procedere nell'installazione.</p>";
					main.add ( new HTML ( message ) );
				}
				else if ( probe.getString ( "dbdrivers" ).equals ( "" ) == true ) {
					message = "<p>Sembra che su questo server non sia installato alcun database, oppure alcun driver per poterlo utilizzare.</p>";
					message += "<p>Per usare GASdotto è necessario avere <a href=\"http://www.php.net\">PHP</a>, l'estensione <a href=\"http://www.php.net/manual/en/book.pdo.php\">PDO</a>, un database a scelta tra <a href=\"http://www.mysql.com\">MySQL</a> e <a href=\"http://www.postgresql.org/\">PostGreSQL</a> ed i relativi driver: verifica che essi siano installati, o chiedi assistenza a qualcuno con maggiore dimestichezza col PC.</p>";
					main.add ( new HTML ( message ) );
				}
				else {
					if ( mode == 0 )
						fillForm ( probe );
					else
						upgradePanel ( probe );
				}
			}
			public void onError () {
				Window.Location.reload ();
			}
		} );
	}

	private void upgradePanel ( Probe probe ) {
		HorizontalPanel url;
		FromServerForm form;
		DummyTextBox tb;
		CustomCaptionPanel setts;

		probe.setBool ( "upgrade", true );
		probe.setBool ( "trylocate", true );

		form = new FromServerForm ( probe, FromServerForm.EDITABLE_UNDELETABLE );
		form.alwaysOpened ( true );

		form.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerForm form ) {
				if ( form.getObject ().getLocalID () == 1 )
					upgradeComplete ();
			}

			public void onError ( FromServerForm form ) {
				FromServer probe;

				probe = form.getObject ();

				if ( form.getObject ().getLocalID () == -1 ) {
					main.clear ();
					manualDatabase ( ( Probe ) probe );
				}
			}
		} );

		form.add ( new HTML ( "<p>Qualche informazione in merito alla installazione che vuoi aggiornare, e sul comportamento da adottare. Puoi scegliere di sovrascrivere completamente la precedente versione oppure di installare questa qui a parte per mantenere la vecchia come riferimento.</p>" ) );

		setts = new CustomCaptionPanel ( "Dettagli" );

		url = new HorizontalPanel ();
		url.setStyleName ( "multi-selector" );
		url.add ( new Label ( "http://" + probe.getString ( "servername" ) + "/" ) );
		tb = new DummyTextBox ();
		tb.setVisibleLength ( 100 );
		tb.setMaxLength ( 100 );
		url.add ( form.getPersonalizedWidget ( "oldurl", tb ) );
		url.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		setts.addPair ( "Indirizzo web presso cui si trova la precedente versione", url );

		form.add ( setts );
		main.add ( form );
	}

	private void manualDatabase ( Probe probe ) {
		Widget dbsettings;
		FromServerForm form;

		probe.setBool ( "upgrade", true );
		probe.setBool ( "trylocate", false );

		form = new FromServerForm ( probe, FromServerForm.EDITABLE_UNDELETABLE );
		form.alwaysOpened ( true );

		form.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerForm form ) {
				if ( form.getObject ().getLocalID () == 1 )
					upgradeComplete ();
				else
					Utils.showNotification ( "E' occorso un problema durante l'aggiornamento" );
			}
		} );

		form.add ( new HTML ( "<p>Purtroppo non è stato possibile rilevare automaticamente la configurazione della vecchia installazione.</p>" ) );
		form.add ( new HTML ( "<p>Inserisci manualmente i dati per connettersi al database in cui si trovano i dati che vuoi importare.</p>" ) );

		dbsettings = doDbSettingForm ( form, null );
		form.add ( dbsettings );

		main.add ( form );
	}

	private void fillForm ( Probe probe ) {
		FromServerForm form;

		form = new FromServerForm ( probe, FromServerForm.EDITABLE_UNDELETABLE );
		form.alwaysOpened ( true );

		form.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerForm form ) {
				if ( form.getObject ().getLocalID () == 1 )
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
	}

	private Widget doDbSettingForm ( FromServerForm form, Probe probe ) {
		CustomCaptionPanel db;

		db = new CustomCaptionPanel ( "Impostazione del Database" );
		db.addPair ( "Nome del Database", form.getWidget ( "dbname" ) );
		db.addPair ( "Host del Database", form.getWidget ( "dbhost" ) );
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
		message += "<p>Per consigli ed indicazioni sull'uso di GASdotto visita <a href=\"http://gasdotto.barberaware.org\">il sito del progetto</a>.</p>";
		main.add ( new HTML ( message ) );
	}

	private void upgradeComplete () {
		String message;

		main.clear ();

		message = "<p>Upgrade completato con successo.</p>";
		message += "<p>Ricaricando <a href=\"GASdotto.html\">questa pagina</a> ti verrà presentato il pannello di login: entra nell'applicazione usando i tuoi soliti username e password.</p>";
		message += "<p>Per consigli ed indicazioni sull'uso di GASdotto visita <a href=\"http://gasdotto.barberaware.org\">il sito del progetto</a>.</p>";
		main.add ( new HTML ( message ) );
	}
}
