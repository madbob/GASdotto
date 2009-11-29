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

import com.allen_sauer.gwt.log.client.Log;

public class SystemPanel extends GenericPanel {
	private FormCluster	categories;
	private FormCluster	measures;

	public SystemPanel () {
		super ();

		CaptionPanel sframe;

		sframe = new CaptionPanel ( "Configurazione GAS" );
		sframe.add ( doGlobalConfForm () );
		add ( sframe );

		sframe = new CaptionPanel ( "Categorie" );
		categories = new FormCluster ( "Category", "Nuova Categoria" ) {
			protected FromServerForm doEditableRow ( FromServer cat ) {
				return doCategoryForm ( cat );
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new Category () );
			}
		};
		sframe.add ( categories );
		add ( sframe );

		sframe = new CaptionPanel ( "Unità di misura" );
		measures = new FormCluster ( "Measure", "Nuova Misura" ) {
			protected FromServerForm doEditableRow ( FromServer measure ) {
				return doMeasureForm ( measure );
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new Measure () );
			}
		};
		sframe.add ( measures );
		add ( sframe );

		sframe = new CaptionPanel ( "Configurazione GASdotto" );
		sframe.add ( doApplicationConfForm () );
		add ( sframe );
	}

	private FromServerForm doGlobalConfForm () {
		FromServerForm ver;
		CustomCaptionPanel frame;
		CaptionPanel sframe;

		ver = new FromServerForm ( Session.getGAS () );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerForm form ) {
				/*
					Poiche' i settaggi sul GAS possono andare a toccare
					numerosissimi aspetti dell'interfaccia, provvedere qui ad
					un riavvio dell'applicazione quando i settaggi sono
					modificati e salvati
				*/
				Window.Location.reload ();
			}
		} );

		frame = new CustomCaptionPanel ( "Attributi" );
		ver.add ( frame );

		frame.addPair ( "Nome", ver.getWidget ( "name" ) );
		frame.addPair ( "Mail", ver.getWidget ( "mail" ) );

		/*
			Il logo in homepage puo' essere personalizzato solo se e' concesso il
			caricamento di files sul server
		*/
		if ( Session.getSystemConf ().getBool ( "has_file" ) == true )
			frame.addPair ( "Logo Homepage", ver.getPersonalizedWidget ( "image", new FileUploadDialog () ) );

		frame.addPair ( "Gestione Quote", ver.getWidget ( "payments" ) );

		/*
			Solo se il sistema permette l'invio di mail viene abilitata l'apposita
			opzione
		*/
		if ( Session.getSystemConf ().getBool ( "has_mail" ) == true )
			frame.addPair ( "Abilita Notifiche Mail", ver.getWidget ( "use_mail" ) );

		sframe = new CaptionPanel ( "Descrizione" );
		sframe.add ( ver.getWidget ( "description" ) );
		ver.add ( sframe );

		return ver;
	}

	private FromServerForm doCategoryForm ( FromServer cat ) {
		FromServerForm ver;
		FlexTable fields;

		ver = new FromServerForm ( cat );

		fields = new FlexTable ();
		ver.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Nome" ) );
		fields.setWidget ( 0, 1, ver.getWidget ( "name" ) );

		ver.add ( new Label ( "Descrizione" ) );
		ver.add ( ver.getWidget ( "description" ) );

		return ver;
	}

	private FromServerForm doMeasureForm ( FromServer measure ) {
		FromServerForm ver;
		FlexTable fields;

		ver = new FromServerForm ( measure );

		fields = new FlexTable ();
		ver.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Nome" ) );
		fields.setWidget ( 0, 1, ver.getWidget ( "name" ) );

		fields.setWidget ( 1, 0, new Label ( "Simbolo" ) );
		fields.setWidget ( 1, 1, ver.getWidget ( "symbol" ) );

		return ver;
	}

	private FromServerForm doApplicationConfForm () {
		FromServerForm ver;
		FlexTable fields;

		ver = new FromServerForm ( Session.getSystemConf (), FromServerForm.NOT_EDITABLE );

		fields = new FlexTable ();
		ver.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Versione" ) );
		fields.setWidget ( 0, 1, ver.getPersonalizedWidget ( "gasdotto_main_version", new StringLabel () ) );

		fields.setWidget ( 1, 0, new Label ( "Commit" ) );
		fields.setWidget ( 1, 1, ver.getPersonalizedWidget ( "gasdotto_commit_version", new StringLabel () ) );

		fields.setWidget ( 2, 0, new Label ( "Data Compilazione" ) );
		fields.setWidget ( 2, 1, ver.getPersonalizedWidget ( "gasdotto_build_date", new DateViewer () ) );

		return ver;
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Configurazioni";
	}

	public String getSystemID () {
		return "system";
	}

	public Image getIcon () {
		return new Image ( "images/path_system.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Category" );
		Utils.getServer ().testObjectReceive ( "Measure" );
	}
}
