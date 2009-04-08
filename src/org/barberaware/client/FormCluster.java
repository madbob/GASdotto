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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public abstract class FormCluster extends VerticalPanel {
	private boolean		automatic;
	private boolean		addable;

	private void buildCommon ( String type, String icon_path, boolean auto ) {
		automatic = auto;
		addable = false;

		Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( automatic == true )
					addElement ( object );
			}

			public void onModify ( FromServer object ) {
				refreshElement ( object );
			}

			public void onDestroy ( FromServer object ) {
				deleteElement ( object );
			}
		} );

		if ( icon_path != null ) {
			add ( doAddButton ( icon_path ) );
			addable = true;
		}

		setStyleName ( "form-cluster" );
	}

	public FormCluster ( String type, String icon_path, boolean auto ) {
		buildCommon ( type, icon_path, auto );
	}

	public FormCluster ( String type, String icon_path ) {
		buildCommon ( type, icon_path, true );
	}

	private void closeOtherForms ( FromServerForm target ) {
		int tot;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( int i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) getWidget ( i );

			if ( iter != target ) {
				if ( iter.isOpen () ) {
					/*
						Tendenzialmente qui si potrebbe assumere che un
						solo Form e' aperto in ogni momento, e se si
						trova quello precedentemente "open" basta
						chiuderlo ed uscire dal ciclo, ma per sicurezza
						me li passo tutti
					*/
					iter.open ( false );
				}
			}
		}
	}

	private Panel doAddButton ( String icon_path ) {
		PushButton button;
		HorizontalPanel pan;

		pan = new HorizontalPanel ();
		pan.setStyleName ( "bottom-buttons" );

		button = new PushButton ( new Image ( icon_path ), new ClickListener () {
			public void onClick ( Widget sender ) {
				FromServerForm new_form;
				new_form = doNewEditableRow ();

				new_form.setCallback ( new FromServerFormCallbacks () {
					public void onOpen ( FromServerForm form ) {
						closeOtherForms ( form );
					}
				} );

				new_form.open ( true );
				insert ( new_form, latestIterableIndex () );
			}
		} );

		pan.add ( button );
		return pan;
	}

	public FromServerForm retrieveForm ( FromServer obj ) {
		int i;
		int id;
		int tot;
		FromServerForm iter;

		iter = null;
		id = obj.getLocalID ();
		tot = latestIterableIndex ();

		for ( i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			if ( iter.getObject ().getLocalID () == id )
				break;
		}

		if ( i == tot )
			iter = null;

		return iter;
	}

	public int latestIterableIndex () {
		return getWidgetCount () - ( addable ? 1 : 0 );
	}

	public void addElement ( FromServer object ) {
		FromServerForm iter;

		iter = retrieveForm ( object );
		if ( iter == null ) {
			iter = doEditableRow ( object );

			if ( iter != null ) {
				iter.setCallback ( new FromServerFormCallbacks () {
					public void onOpen ( FromServerForm form ) {
						closeOtherForms ( form );
					}
				} );

				insert ( iter, 0 );
			}
		}
	}

	public void refreshElement ( FromServer object ) {
		FromServerForm iter;

		iter = retrieveForm ( object );
		if ( iter != null )
			iter.refreshContents ( object );
	}

	public void deleteElement ( FromServer object ) {
		FromServerForm iter;

		iter = retrieveForm ( object );
		if ( iter != null )
			iter.setVisible ( false );
	}

	protected abstract FromServerForm doNewEditableRow ();
	protected abstract FromServerForm doEditableRow ( FromServer obj );
}
