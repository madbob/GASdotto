/*  GASdotto 0.1
 *  Copyright (C) 2008 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class MainStack extends Composite {
	private DeckPanel		main;

	public MainStack () {
		int privileges;

		main = new DeckPanel ();
		main.setStyleName ( "main-stack" );
		initWidget ( main );

		privileges = Session.getPrivileges ();

		main.add ( new HomePanel () );
		main.add ( new ProfilePanel () );

		if ( privileges == User.USER_ADMIN ) {
			main.add ( new UsersPanel () );
			main.add ( new SuppliersEditPanel () );
			main.add ( new OrdersEditPanel () );
			main.add ( new SystemPanel () );
		}

		else if ( privileges == User.USER_RESPONSABLE ) {
			main.add ( new SuppliersEditPanel () );
			main.add ( new OrdersEditPanel () );
			main.add ( new SystemPanel () );
		}

		else {
			main.add ( new SuppliersPanel () );
			main.add ( new OrdersPanel () );
		}
	}

	public ArrayList getPanels () {
		int count;
		ArrayList ret;

		ret = new ArrayList ();
		count = main.getWidgetCount ();

		for ( int i = 0; i < count; i++ )
			ret.add ( main.getWidget ( i ) );

		return ret;
	}

	public void showPanelAtPos ( int pos ) {
		GenericPanel to_show;

		to_show = ( GenericPanel ) main.getWidget ( pos );
		to_show.initView ();
		main.showWidget ( pos );
	}
}
