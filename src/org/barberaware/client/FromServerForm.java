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
	private Label			summary;
	private VerticalPanel		contents;
	private ButtonsBar		buttons;
	private FromServerFormCallbacks	callbacks;
	private FromServerFormIcons	iconsCallback;

	/**
		TODO	Sostituire l'ArrayList con una HashMap
	*/

	private ArrayList		widgets;

	/****************************************************************** init */

	private void buildCommon ( FromServer obj, boolean editable ) {
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
		};

		iconsCallback = new FromServerFormIcons () {
			public Panel retrive ( FromServer obj ) {
				return null;
			}
		};

		main = new DisclosurePanel ( doSummary ( object ) );
		main.setAnimationEnabled ( true );
		main.addEventHandler ( new DisclosureHandler () {
			public void onClose ( DisclosureEvent event ) {
				if ( contentsChanged () ) {
					/**
						TODO	Chiedere se si vogliono annullare le
							modifiche fatte
					*/
				}
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
	}

	public FromServerForm ( FromServer obj ) {
		buildCommon ( obj, true );
	}

	public FromServerForm ( FromServer obj, boolean editable ) {
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

	public void setAdditionalIconsCallback ( FromServerFormIcons callback ) {
		FromServerFormIcons iconsCallback = callback;
	}

	public void addEventHandler ( DisclosureHandler handler ) {
		main.addEventHandler ( handler );
	}

	private Panel doSummary ( FromServer object ) {
		HorizontalPanel main;
		Image marker;

		main = new HorizontalPanel ();
		main.setStyleName ( "element-summary" );

		marker = new Image ( "images/details-closed.png" );
		marker.setStyleName ( "element-marker" );
		main.add ( marker );

		summary = new Label ( object.getString ( "name" ) );
		main.add ( summary );

		return main;
	}

	private ButtonsBar doButtons ( boolean editable ) {
		ButtonsBar panel;
		PushButton button;
		final FromServerForm myself		= this;

		panel = new ButtonsBar ();

		if ( editable ) {
			button = new PushButton ( new Image ( "images/delete.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					/**
						TODO	Chiedere conferma per l'eliminazione
					*/

					callbacks.onDelete ( myself );
					object.destroy ( null );
					main.setOpen ( false );
					main.setVisible ( false );
				}
			} );
			panel.add ( button, "Elimina" );
		}

		button = new PushButton ( new Image ( "images/cancel.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				resetObject ();
				callbacks.onReset ( myself );
				main.setOpen ( false );

				if ( object.isValid () == false )
					main.setVisible ( false );
			}
		} );
		panel.add ( button, "Annulla" );

		if ( editable ) {
			button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					if ( contentsChanged () ) {
						rebuildObject ();
						callbacks.onSave ( myself );
						object.save ( null );
						summary.setText ( object.getString ( "name" ) );
					}

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
