/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class FromServerForm extends Composite {
	private FromServer		object;
	private DisclosurePanel		main;
	private IconsBar		icons;
	private Label			summary;
	private VerticalPanel		contents;
	private ButtonsBar		buttons;
	private boolean			alwaysShow;
	private FromServerFormCallbacks	callbacks;

	/**
		TODO	Sostituire l'ArrayList con una HashMap
	*/

	private ArrayList		widgets;

	public static int		FULL_EDITABLE		= 0;
	public static int		EDITABLE_UNDELETABLE	= 1;
	public static int		NOT_EDITABLE		= 2;

	/****************************************************************** init */

	private void buildCommon ( FromServer obj, int editable ) {
		object = obj;

		widgets = new ArrayList ();

		callbacks = new FromServerFormCallbacks () {
			public void onSave ( FromServerForm form ) {
				/* dummy */
			}

			public void onReset ( FromServerForm form ) {
				/* dummy */
			}

			public void onDelete ( FromServerForm form ) {
				/* dummy */
			}

			public void onClose ( FromServerForm form ) {
				/* dummy */
			}
		};

		main = new DisclosurePanel ( doSummary ( object ) );
		main.setAnimationEnabled ( true );
		main.addEventHandler ( new DisclosureHandler () {
			public void onClose ( DisclosureEvent event ) {
				if ( contentsChanged () ) {
					if ( Window.confirm ( "Vuoi salvare le modifiche effettuate?" ) == true )
						savingObject ();
					else
						resetObject ();
				}

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

		buttons = doButtons ( editable );
		contents.add ( buttons );
		contents.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );

		alwaysShow = false;
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

	public void open ( boolean open ) {
		main.setOpen ( open );
		onOpenCb ();
	}

	public FromServer getObject () {
		return object;
	}

	public void setObject ( FromServer obj ) {
		object = obj;
	}

	public void setCallback ( FromServerFormCallbacks routine ) {
		callbacks = routine;
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

	private Panel doSummary ( FromServer object ) {
		HorizontalPanel main;
		Image marker;

		main = new HorizontalPanel ();
		main.setStyleName ( "element-summary" );

		marker = new Image ( "images/details-closed.png" );
		marker.setStyleName ( "element-marker" );
		main.add ( marker );
		main.setCellWidth ( marker, "5%" );
		main.setCellHorizontalAlignment ( marker, HasHorizontalAlignment.ALIGN_LEFT );

		summary = new Label ( object.getString ( "name" ) );
		main.add ( summary );
		main.setCellHorizontalAlignment ( summary, HasHorizontalAlignment.ALIGN_LEFT );

		icons = doIconsBar ();
		main.add ( icons );
		main.setCellHorizontalAlignment ( icons, HasHorizontalAlignment.ALIGN_RIGHT );

		return main;
	}

	private IconsBar doIconsBar () {
		IconsBar bar;
		bar = new IconsBar ();
		return bar;
	}

	private ButtonsBar doButtons ( int editable ) {
		ButtonsBar panel;
		PushButton button;
		final FromServerForm myself		= this;

		panel = new ButtonsBar ();

		if ( editable == FULL_EDITABLE ) {
			button = new PushButton ( new Image ( "images/delete.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					if ( object.isValid () == false )
						main.setVisible ( false );

					else {
						if ( Window.confirm ( "Sei sicuro di voler eliminare l'elemento?" ) == true ) {
							callbacks.onDelete ( myself );
							object.destroy ( null );
							main.setOpen ( false );
							main.setVisible ( false );
						}
					}
				}
			} );
			panel.add ( button, "Elimina" );
		}

		button = new PushButton ( new Image ( "images/cancel.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( object.isValid () == false )
					main.setVisible ( false );

				else {
					resetObject ();
					callbacks.onReset ( myself );
					main.setOpen ( false );
				}
			}
		} );
		panel.add ( button, "Annulla" );

		if ( editable == FULL_EDITABLE || editable == EDITABLE_UNDELETABLE ) {
			button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					if ( contentsChanged () )
						if ( savingObject () == false )
							return;

					main.setOpen ( false );
				}
			} );
			panel.add ( button, "Salva" );
		}

		return panel;
	}

	private void onOpenCb () {
		// DOM.scrollIntoView ( main.getElement () );
	}

	private void onCloseCb () {
		callbacks.onClose ( this );
	}

	/****************************************************************** build */

	private FromServerWidget retriveWidgetFromList ( String attribute ) {
		int existing;
		FromServerWidget tmp;

		existing = widgets.size ();

		for ( int i = 0; i < existing; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );

			if ( tmp.name.equals ( attribute ) )
				return tmp;
		}

		return null;
	}

	public Widget getWidget ( String attribute ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );
		if ( tmp == null ) {
			tmp = new FromServerWidget ( object, attribute );
			widgets.add ( tmp );
		}

		return tmp;
	}

	public Widget getPersonalizedWidget ( String attribute, Widget widget ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );
		if ( tmp == null ) {
			tmp = new FromServerWidget ( object, attribute, widget );
			widgets.add ( tmp );
		}

		return tmp;
	}

	public Widget retriveInternalWidget ( String attribute ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );

		if ( tmp != null )
			return tmp.wid;
		else
			return null;
	}

	public void setValidation ( String attribute, FromServerValidateCallback callback ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );
		if ( tmp != null )
			tmp.validation = callback;
	}

	/*
		Usato per aggiungere nuovi widgets destinati alla formattazione, che non
		rientrano nella rappresentazione dell'oggetto. Sono recuperabili per mezzo di
		getWidget(), ma vengono saltati nella fase di ricostruzione dell'oggetto
		FromServer
	*/
	public void setExtraWidget ( String name, Widget extra ) {
		widgets.add ( new FromServerWidget ( name, extra ) );
	}

	public IconsBar getIconsBar () {
		return icons;
	}

	/****************************************************************** handling */

	public void refreshContents ( FromServer obj ) {
		FromServerWidget iter;

		for ( int i = 0; i < widgets.size (); i++ ) {
			iter = ( FromServerWidget ) widgets.get ( i );
			iter.set ( obj );
		}
	}

	private boolean contentsChanged () {
		int num;
		boolean ret;
		FromServerWidget tmp;

		num = widgets.size ();
		ret = false;

		for ( int i = 0; i < num; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );

			if ( tmp.compare ( object ) == false ) {
				ret = true;
				break;
			}
		}

		return ret;
	}

	private boolean savingObject () {
  		if ( rebuildObject () == false )
			return false;

		callbacks.onSave ( this );
		object.save ( null );
		summary.setText ( object.getString ( "name" ) );
		return true;
	}

	private boolean rebuildObject () {
		int num;
		FromServerWidget tmp;

		num = widgets.size ();

		for ( int i = 0; i < num; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );
			if ( tmp.assign ( object ) == false )
				return false;
		}

		return true;
	}

	private void resetObject () {
		int num;
		FromServerWidget tmp;

		num = widgets.size ();

		for ( int i = 0; i < num; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );
			tmp.set ( object );
		}
	}
}
