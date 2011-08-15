/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class FromServerForm extends FromServerRappresentation {
	private DisclosurePanel		main;
	private int			editMode;
	private EmblemsBar		icons;
	private Label			summary;
	private VerticalPanel		contents;
	private ButtonsBar		buttons;
	private boolean			alwaysShow;
	private boolean			hasSharing;
	private ArrayList		callbacks;
	private boolean			forceSave;

	public static int		FULL_EDITABLE				= 0;
	public static int		EDITABLE_UNDELETABLE			= 1;
	public static int		NOT_EDITABLE				= 2;
	public static int		EDITABLE_UNDELETABLE_UNCANCELLABLE	= 3;

	/****************************************************************** init */

	private void buildCommon ( FromServer obj, int editable ) {
		setValue ( obj );
		callbacks = new ArrayList ();
		forceSave = false;
		hasSharing = false;
		noExplicitCallbacks ();

		main = new DisclosurePanel ( doSummary () );
		main.setAnimationEnabled ( true );
		main.addEventHandler ( new DisclosureHandler () {
			public void onClose ( DisclosureEvent event ) {
				FromServer object;

				object = getValue ();
				if ( object == null )
					return;

				onClosingCb ();

				if ( object.isValid () == false ) {
					if ( Window.confirm ( "Vuoi salvare il nuovo oggetto?" ) == true )
						savingObject ();
					else
						invalidate ();

					return;
				}

				checkSaving ();
				onCloseCb ();
			}

			public void onOpen ( DisclosureEvent event ) {
				onOpenCb ();
			}
		} );
		initWidget ( main );

		contents = new VerticalPanel ();
		contents.setStyleName ( "element-details" );
		main.add ( contents );

		editMode = editable;
		buttons = null;
		placeButtons ();

		alwaysShow = false;

		/*
			Le icone vengono inizializzate solo quando richiesto
		*/
		icons = null;
	}

	public FromServerForm ( FromServer obj ) {
		buildCommon ( obj, FULL_EDITABLE );
	}

	public FromServerForm ( FromServer obj, int editable ) {
		buildCommon ( obj, editable );
	}

	public void add ( Widget wid ) {
		contents.insert ( wid, contents.getWidgetCount () - 1 );
	}

	public void insert ( Widget wid, int index ) {
		contents.insert ( wid, index );
	}

	public void open ( boolean open ) {
		main.setOpen ( open );

		if ( open == true )
			onOpenCb ();
		else
			onCloseCb ();
	}

	public void setCallback ( FromServerFormCallbacks routine ) {
		if ( routine == null ) {
			noExplicitCallbacks ();
		}
		else {
			callbacks.add ( routine );
			summary.setText ( retrieveNameInCallbacks () );
		}
	}

	public void removeCallback ( String id ) {
		FromServerFormCallbacks call;

		for ( int i = 0; i < callbacks.size (); i++ ) {
			call = ( FromServerFormCallbacks ) callbacks.get ( i );
			if ( call.getID () == id ) {
				callbacks.remove ( call );
				break;
			}
		}
	}

	public void addEventHandler ( DisclosureHandler handler ) {
		main.addEventHandler ( handler );
	}

	public void alwaysOpened ( boolean open ) {
		alwaysShow = open;

		if ( alwaysShow == true ) {
			main.setOpen ( open );

			main.addEventHandler ( new DisclosureHandler () {
				public void onClose ( DisclosureEvent event ) {
					main.setOpen ( true );
				}

				public void onOpen ( DisclosureEvent event ) {
					/* dummy */
				}
			} );
		}
	}

	public boolean isOpen () {
		return main.isOpen ();
	}

	public void setEditableMode ( int editable ) {
		if ( editMode != editable ) {
			editMode = editable;
			placeButtons ();
		}
	}

	public void addBottomButton ( String image, String text, ClickListener listener ) {
		PushButton button;

		button = new PushButton ( new Image ( image ), listener );
		buttons.add ( button, text );
	}

	public void forceNextSave ( boolean force ) {
		forceSave = force;
	}

	private void placeButtons () {
		FromServer value;
		HorizontalPanel hold;
		ShareButton share;
		ButtonsBar sharebar;

		if ( buttons != null )
			contents.remove ( buttons );
		buttons = doButtons ( editMode );

		value = getValue ();

		if ( value == null || value.isValid () == false || value.isSharable () == false ) {
			contents.add ( buttons );
			contents.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );
		}
		else {
			hold = new HorizontalPanel ();
			hold.setWidth ( "100%" );
			contents.add ( hold );

			sharebar = new ButtonsBar ();
			hold.add ( sharebar );
			hold.setCellHorizontalAlignment ( sharebar, HasHorizontalAlignment.ALIGN_LEFT );

			share = new ShareButton ();
			addPeer ( share );
			sharebar.add ( share, "Condividi" );

			hold.add ( buttons );
			hold.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );

			hasSharing = true;
		}
	}

	private void noExplicitCallbacks () {
		callbacks.clear ();

		/*
			Viene comunque sempre settata una classe di callback di default, per
			garantire che almeno il nome del form sia sempre gestito
		*/
		callbacks.add ( new FromServerFormCallbacks () );
	}

	private Panel doSummary () {
		HorizontalPanel main;
		String name;

		main = new HorizontalPanel ();
		main.setStyleName ( "element-summary" );

		summary = new Label ( retrieveNameInCallbacks () );
		main.add ( summary );

		return main;
	}

	private ButtonsBar doButtons ( int editable ) {
		ButtonsBar panel;
		PushButton button;
		final FromServerForm myself		= this;

		panel = new ButtonsBar ();

		if ( editable == FULL_EDITABLE ) {
			button = new PushButton ( new Image ( "images/delete.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					boolean ret;
					FromServer object;
					FromServerFormCallbacks callback;

					ret = true;

					for ( int i = 0; i < callbacks.size (); i++ ) {
						callback = ( FromServerFormCallbacks ) callbacks.get ( i );
						/*
							In questo modo eseguo tutte le callback di onDelete, ma se
							anche una sola torna false la funzione esce
						*/
						ret = ( ret && callback.onDelete ( myself ) );
					}

					if ( ret == false )
						return;

					object = getValue ();

					if ( object.isValid () == false ) {
						invalidate ();
					}
					else {
						if ( Window.confirm ( "Sei sicuro di voler eliminare l'elemento?" ) == true ) {
							object.destroy ( null );
							invalidate ();
							main.setOpen ( false );
						}
					}
				}
			} );
			panel.add ( button, "Elimina" );
		}

		if ( editable != EDITABLE_UNDELETABLE_UNCANCELLABLE ) {
			button = new PushButton ( new Image ( "images/cancel.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					if ( getValue ().isValid () == false ) {
						invalidate ();
					}
					else {
						resetObject ();

						for ( int i = 0; i < callbacks.size (); i++ )
							( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onReset ( myself );

						main.setOpen ( false );
					}
				}
			} );
			panel.add ( button, "Annulla" );
		}

		if ( editable == FULL_EDITABLE || editable == EDITABLE_UNDELETABLE || editable == EDITABLE_UNDELETABLE_UNCANCELLABLE ) {
			button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					if ( forceSave == true || contentsChanged () )
						if ( savingObject () == false )
							return;

					forceSave = false;
					main.setOpen ( false );
				}
			} );

			panel.add ( button, "Salva" );
		}

		return panel;
	}

	private void onOpenCb () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onOpen ( this );

		summary.addStyleName ( "element-summary-opened" );
	}

	private void onClosingCb () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onClosing ( this );
	}

	private void onCloseCb () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onClose ( this );

		summary.removeStyleName ( "element-summary-opened" );
	}

	public void emblemsAttach ( EmblemsInfo info ) {
		HorizontalPanel panel;

		if ( icons != null ) {
			Window.alert ( "Emblemi già assegnati a questo form" );
		}
		else {
			icons = new EmblemsBar ( info );
			panel = ( HorizontalPanel ) summary.getParent ();
			panel.insert ( icons, 0 );
		}
	}

	public EmblemsBar emblems () {
		return icons;
	}

	public FromServerWidget removeWidget ( String name ) {
		FromServerWidget tmp;

		tmp = super.removeWidget ( name );
		if ( tmp != null ) {
			/*
				Attenzione: in taluni casi, quando indicizzo un elemento nel form con
				setExtraWidget(), il FromServerWidget che maneggio qui non e' un vero widget ma solo
				un wrapper per l'elemento grafico vero. Dunque se provo a passarlo alla funzione
				remove() del pannello ottengo un errore, in quanto mancano le informazioni
				necessarie. Pertanto qui provo prima ad agire sul widget vero e proprio, che esiste
				per certo, ed eventualmente sul wrapper (che e' il comportamento piu' comune)
			*/
			if ( contents.remove ( tmp.wid ) == false )
				contents.remove ( tmp );
		}

		return tmp;
	}

	public void refreshContents ( FromServer obj ) {
		super.refreshContents ( obj );
		summary.setText ( retrieveNameInCallbacks () );
	}

	public void checkSaving () {
		if ( editMode != NOT_EDITABLE && contentsChanged () ) {
			if ( Window.confirm ( "Vuoi salvare le modifiche effettuate?" ) == true )
				savingObject ();
			else
				resetObject ();
		}
	}

	private String retrieveNameInCallbacks () {
		String name;

		/*
			FromServerFormCallbacks.getName() di default torna il "name" dell'oggetto, anche se tale
			funzione non viene esplicitata. Cio' vuol dire che se non si presta attenzione viene saltata
			la propria getName().
			Qui itero l'array di callbacks partendo dal fondo, ovvero dall'ultima FromServerFormCallbacks
			assegnata, ma occorre ricordarsi di far tornare esplicitamente null alle funzioni immesse in
			un punto qualunque dell'esecuzione altrimenti forzeranno sempre il "name" dell'oggetto
		*/
		for ( int i = callbacks.size () - 1; i > -1; i-- ) {
			name = ( ( FromServerFormCallbacks ) callbacks.get ( i ) ).getName ( this );
			if ( name != null && name.equals ( "" ) == false )
				return name;
		}

		return "";
	}

	private void savedCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onSaved ( this );
	}

	private void errorCallbacks () {
		for ( int i = 0; i < callbacks.size (); i++ )
			( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onError ( this );
	}

	private void reviewSharing () {
		FromServer value;

		value = getValue ();
		if ( value != null && ( value.isSharable () && hasSharing == false ) )
			placeButtons ();
	}

	public boolean savingObject () {
		boolean confirm;

		confirm = true;

		for ( int i = 0; i < callbacks.size (); i++ )
			confirm = ( ( FromServerFormCallbacks ) callbacks.get ( i ) ).onSave ( this ) || confirm;

		if ( confirm == false || rebuildObject () == false )
			return false;

		getValue ().save ( new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				savedCallbacks ();
				summary.setText ( retrieveNameInCallbacks () );
				reviewSharing ();
			}
			public void onError () {
				errorCallbacks ();
			}
		} );

		return true;
	}
}
