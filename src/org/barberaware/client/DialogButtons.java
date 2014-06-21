/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class DialogButtons extends HorizontalPanel implements SavingDialog {
	private ArrayList<SavingDialogCallback>	callbacks;
	private Button				saveButt;

	public DialogButtons () {
		Button but;

		this.setStyleName ( "dialog-buttons" );
		this.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );

		saveButt = new Button ( "Salva", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				fireClick ( 0 );
			}
		} );
		this.add ( saveButt );

		but = new Button ( "Annulla", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				fireClick ( 1 );
			}
		} );
		this.add ( but );
	}

	public void customSaveLabel ( String label ) {
		saveButt.setText ( label );
	}

	private void fireClick ( int mode ) {
		Utils.triggerSaveCallbacks ( callbacks, this, mode );
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback listener ) {
		if ( callbacks == null )
			callbacks = new ArrayList<SavingDialogCallback> ();
		callbacks.add ( listener );
	}

	public void removeCallback ( SavingDialogCallback listener ) {
		if ( callbacks != null )
			callbacks.remove ( listener );
	}
}

