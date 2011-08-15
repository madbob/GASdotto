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
import com.google.gwt.user.client.ui.*;

public class CyclicToggle extends Composite implements IntNumericWidget {
	private DeckPanel		main;
	private int			defaultSelection;

	public CyclicToggle ( boolean active ) {
		FocusPanel focus;

		main = new DeckPanel ();
		defaultSelection = 0;

		main.setStyleName ( "cyclic-toggle" );
		focus = new FocusPanel ( main );
		initWidget ( focus );

		if ( active == true ) {
			focus.addClickListener ( new ClickListener () {
				public void onClick ( Widget sender ) {
					int index;

					index = main.getVisibleWidget ();
					index++;

					if ( index >= main.getWidgetCount () )
						index = 0;

					main.showWidget ( index );
				}
			} );
		}
	}

	/*
		Le rappresentazioni degli stati vanno introdotte secondo lo stesso ordine della
		enumerazione completa. Non sono ammessi buchi nella sequenza di numerazione
	*/
	public void addState ( String url ) {
		/*
			Per un qualche problema di visualizzazione, se nel DeckPanel metto la
			Image cosi' com'e' mi viene stretchata alla dimensione del pannello.
			Dunque adotto questo brutto espediente di mettere la Image in un pannello
			transitivo: sarebbe quasi da correggere in qualche modo piu' elegante...
		*/
		HorizontalPanel useless;

		useless = new HorizontalPanel ();
		useless.add ( new Image ( url ) );
		main.add ( useless );
	}

	public void setDefaultSelection ( int sel ) {
		defaultSelection = sel;
	}

	public void setVal ( int state ) {
		if ( state > -1 && state < main.getWidgetCount () )
			main.showWidget ( state );
		else
			main.showWidget ( defaultSelection );
	}

	public int getVal () {
		return main.getVisibleWidget ();
	}
}
