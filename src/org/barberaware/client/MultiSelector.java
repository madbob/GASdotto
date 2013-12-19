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

import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class MultiSelector extends Composite implements FromServerArray, SavingDialog {
	private VerticalPanel			main;

	private boolean				callbacksInited;
	private FilterCallback			filterCallback;
	private ArrayList<SavingDialogCallback>	savingCallbacks;
	private ArrayList			extraElements;

	private SelectionDialog			dialog;
	private String				objectType;

	/*
		Il parametro "mode" si riferisce ai valori in SelectionDialog
	*/
	public MultiSelector ( String type, int mode, FilterCallback filter ) {
		Button mod_button;

		objectType = type;
		filterCallback = filter;
		savingCallbacks = null;
		extraElements = null;
		callbacksInited = false;

		dialog = new SelectionDialog ( mode );
		dialog.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				rebuildMainList ();
				executeSavingCallbacks ( 0 );
			}

			public void onCancel ( SavingDialog dialog ) {
				executeSavingCallbacks ( 1 );
			}
		} );

		main = new VerticalPanel ();
		main.setStyleName ( "multi-selector" );
		initWidget ( main );

		mod_button = new Button ( "Modifica Lista" );
		mod_button.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				if ( callbacksInited == false ) {
					registerCallbacks ();
					callbacksInited = true;
				}

				dialog.center ();
				dialog.show ();
			}
		} );
		main.add ( mod_button );
	}

	public void addExtraElement ( String text ) {
		if ( extraElements == null )
			extraElements = new ArrayList ();

		extraElements.add ( text );
		dialog.addExtraElement ( text );
	}

	public boolean getExtraElement ( String text ) {
		return dialog.getExtraElement ( text );
	}

	private void registerCallbacks () {
		Utils.getServer ().onObjectEvent ( objectType, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( checkValidity ( object ) == true )
					dialog.addElementInList ( object );
			}

			public void onModify ( FromServer object ) {
				if ( checkValidity ( object ) == true )
					dialog.refreshElement ( object );
				else
					dialog.removeElementInList ( object );
			}

			public void onDestroy ( FromServer object ) {
				dialog.removeElementInList ( object );
			}

			protected String debugName () {
				return "MultiSelector su " + objectType;
			}
		} );

		Utils.getServer ().testObjectReceive ( objectType );
	}

	public void clean () {
		int num;

		num = main.getWidgetCount () - 1;
		for ( int i = 0; i < num; i++ )
			main.remove ( 1 );
	}

	public void forceInitialElements ( ArrayList elements ) {
		int num;
		FromServer obj;

		num = elements.size ();

		for ( int i = 0; i < num; i++ ) {
			obj = ( FromServer ) elements.get ( i );
			if ( checkValidity ( obj ) == true )
				dialog.addElementInList ( obj );
		}
	}

	public void addSelectionCallbacks ( String name, FilterCallback callback ) {
		dialog.addSelectionCallbacks ( name, callback );
	}

	private boolean checkValidity ( FromServer object ) {
		if ( filterCallback != null )
			return filterCallback.check ( object, null );
		else
			return true;
	}

	private void rebuildMainList () {
		int num;
		String extra;
		FromServer iter;
		ArrayList objects;

		clean ();

		if ( extraElements != null ) {
			num = extraElements.size ();

			for ( int i = 0; i < num; i++ ) {
				extra = ( String ) extraElements.get ( i );

				if ( dialog.getExtraElement ( extra ) )
					main.add ( new Label ( extra ) );
			}
		}

		objects = dialog.getElements ();
		num = objects.size ();

		for ( int i = 0; i < num; i++ ) {
			iter = ( FromServer ) objects.get ( i );
			main.add ( new Label ( iter.getString ( "name" ) ) );
		}
	}

	private void executeSavingCallbacks ( int mode ) {
		Utils.triggerSaveCallbacks ( savingCallbacks, this, mode );
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks == null )
			savingCallbacks = new ArrayList<SavingDialogCallback> ();
		savingCallbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( savingCallbacks != null )
			savingCallbacks.remove ( callback );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		dialog.addElement ( element );
		rebuildMainList ();
	}

	public void setElements ( ArrayList elements ) {
		dialog.setElements ( elements );
		rebuildMainList ();
	}

	public void removeElement ( FromServer element ) {
		dialog.removeElement ( element );
		rebuildMainList ();
	}

	public ArrayList getElements () {
		return dialog.getElements ();
	}

	public void refreshElement ( FromServer element ) {
		dialog.refreshElement ( element );
	}
}
