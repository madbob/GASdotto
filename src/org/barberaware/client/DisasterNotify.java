/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

public class DisasterNotify extends Composite {
	private VerticalPanel		main;

	public DisasterNotify () {
		main = new VerticalPanel ();
		main.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		main.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		main.setVisible ( false );
		main.setStyleName ( "big-error-dialog" );
		initWidget ( main );
	}

	public void setMessage ( String message ) {
		main.clear ();
		main.add ( new HTML ( "<p>Si è verificato un errore irreversibile!</p>" ) );
		main.add ( new HTML ( "<p>Non si tratta di nulla di grave, ma è consigliato ricaricare questa pagina web onde evitare di perdere i dati su cui si sta lavorando.</p>" ) );
		main.add ( new HTML ( "<hr>" ) );
		main.add ( new HTML ( "<p>Se l'errore dovesse ripetersi, si raccomanda di segnalare il problema al team GASdotto.</p>" ) );
		main.add ( new HTML ( "<p>Puoi inviare una mail all'indirizzo <a href=\"info@gasdotto.net\">info@gasdotto.net</a> indicando cosa stavi facendo al momento dell'errore, e copiare ed incollare il seguente messaggio:</p>" ) );
		main.add ( new HTML ( "<p>" + message + "</p>" ) );
		main.add ( new HTML ( "<hr>" ) );
		main.add ( new HTML ( "<p><a href=\"GASdotto.html\">Clicca qui per ricaricare la pagina.</a></p>" ) );
	}

	public void show () {
		main.setVisible ( true );
	}
}
