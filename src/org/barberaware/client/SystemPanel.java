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

public class SystemPanel extends GenericPanel {
	private FormCluster	categories;
	private FormCluster	measures;

	public SystemPanel () {
		super ();

		add ( new Label ( "Configurazione GAS" ) );
		add ( doGlobalConfForm () );

		add ( new HTML ( "<hr>" ) );

		add ( new Label ( "Categorie" ) );
		categories = new FormCluster ( "Category", "images/new_category.png" ) {
			protected FromServerForm doEditableRow ( FromServer cat ) {
				return doCategoryForm ( cat );
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new Category () );
			}
		};
		add ( categories );

		add ( new HTML ( "<hr>" ) );

		add ( new Label ( "Unità di misura" ) );
		measures = new FormCluster ( "Measure", "images/new_measure.png" ) {
			protected FromServerForm doEditableRow ( FromServer measure ) {
				return doMeasureForm ( measure );
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new Measure () );
			}
		};
		add ( measures );
	}

	private FromServerForm doGlobalConfForm () {
		FromServerForm ver;
		FlexTable fields;

		ver = new FromServerForm ( Session.getGAS () );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onSave ( FromServerForm form ) {
				/*
					Poiche' i settaggi sul GAS possono andare a toccare
					numerosissimi aspetti dell'interfaccia, provvedere qui ad
					un riavvio dell'applicazione quando i settaggi sono
					modificati e salvati
				*/
				Window.Location.reload ();
			}
		} );

		fields = new FlexTable ();
		ver.add ( fields );

		fields.setWidget ( 0, 0, new Label ( "Nome" ) );
		fields.setWidget ( 0, 1, ver.getWidget ( "name" ) );

		fields.setWidget ( 1, 0, new Label ( "Mail" ) );
		fields.setWidget ( 1, 1, ver.getWidget ( "mail" ) );

		fields.setWidget ( 2, 0, new Label ( "Gestione Pagamenti" ) );
		fields.setWidget ( 2, 1, ver.getWidget ( "payments" ) );

		ver.add ( new Label ( "Descrizione" ) );
		ver.add ( ver.getWidget ( "description" ) );

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
		HorizontalPanel hor;
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

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Configurazioni";
	}

	public Image getIcon () {
		return new Image ( "images/path_system.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Category" );
		Utils.getServer ().testObjectReceive ( "Measure" );
	}
}
