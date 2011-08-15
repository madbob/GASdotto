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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class OrdersList extends FormGroup {
	public OrdersList () {
		super ( null );
	}

	protected void buildMe ( FromServer supplier ) {
		/*
			Problema: il monitor registrato sugli Order non basta, in quanto
			viene eseguito prima che vengano creati i form dei fornitori e
			gli ordini non vengono dunque assegnati correttamente. Pertanto
			qui rieseguo il controllo su tutti gli ordini in cache e popolo
			il OrdersList
		*/
		checkExistingOrders ( supplier );
	}

	public void addOrder ( FromServer order ) {
		int tot;
		int added;
		FromServerForm form;
		FromServer obj;

		tot = getElementsNum ();

		if ( tot >= 10 && sorting ( retrieveForm ( latestIterableIndex () - 1 ).getValue (), order ) < 0 )
			return;

		added = addElement ( order );

		/*
			Se prima dell'inserimento "tot" era 10 adesso si suppone sia 11...
		*/
		if ( added == 1 && tot >= 10 ) {
			form = retrieveForm ( latestIterableIndex () - 1 );
			if ( form != null ) {
				obj = form.getValue ();

				if ( obj != null )
					delOrder ( obj );
			}
		}
	}

	public void modOrder ( FromServer order ) {
		refreshElement ( order );
	}

	public void delOrder ( FromServer order ) {
		deleteElement ( order );
	}

	protected abstract String getEmptyNotification ();
	protected abstract void checkExistingOrders ( FromServer supplier );

	/*
		Questo serve assolutamente a niente, solo ad aderire all'interfaccia di FormGroup
	*/
	public FromServerForm doNewEditableRow () {
		return null;
	}
}
