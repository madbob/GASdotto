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
	private class FromServerWidget extends Composite {
		public String				name;
		public int				type;
		public Widget				wid;
		public FromServerValidateCallback	validation;

		private void buildCommon ( String attribute, int attribute_type, Widget widget ) {
			name = attribute;
			type = attribute_type;
			wid = widget;
			validation = defaultValidationCallback ();
		}

		public FromServerWidget ( FromServer object, String attribute ) {
			buildCommon ( attribute, object.getAttributeType ( attribute ), null );

			if ( type == FromServer.STRING ) {
				TextBox tb;

				tb = new TextBox ();
				tb.setMaxLength ( 100 );
				wid = tb;
			}

			else if ( type == FromServer.LONGSTRING ) {
				TextArea ta;

				ta = new TextArea ();
				ta.setVisibleLines ( 3 );
				ta.setCharacterWidth ( 70 );
				wid = ta;
			}

			else if ( type == FromServer.INTEGER )
				wid = new NumberBox ();

			else if ( type == FromServer.FLOAT )
				wid = new FloatBox ();

			else if ( type == FromServer.PERCENTAGE )
				wid = new PercentageBox ();

			else if ( type == FromServer.DATE )
				wid = new DateSelector ();

			else if ( type == FromServer.BOOLEAN )
				wid = new BooleanSelector ();

			else if ( type == FromServer.ADDRESS )
				wid = new AddressSelector ();

			/*
				Il tipo FromServer.ARRAY non viene gestito
			*/

			else if ( type == FromServer.OBJECT )
				wid = new FromServerSelector ( object.getClassName ( attribute ) );

			initWidget ( wid );
			set ( object );
		}

		public FromServerWidget ( String attribute, Widget widget ) {
			buildCommon ( attribute, -1, widget );
		}

		private FromServerValidateCallback defaultValidationCallback () {
			return new FromServerValidateCallback () {
				public boolean check ( FromServer object, String attribute, Widget widget ) {
					return true;
				}
			};
		}

		public void set ( FromServer object ) {
			if ( type == -1 )
				return;

			else if ( type == FromServer.STRING )
				( ( TextBox ) wid ).setText ( object.getString ( name ) );

			else if ( type == FromServer.LONGSTRING )
				( ( TextArea ) wid ).setText ( object.getString ( name ) );

			else if ( type == FromServer.INTEGER )
				( ( NumberBox ) wid ).setValue ( object.getInt ( name ) );

			else if ( type == FromServer.FLOAT )
				( ( FloatBox ) wid ).setValue ( object.getFloat ( name ) );

			else if ( type == FromServer.PERCENTAGE )
				( ( PercentageBox ) wid ).setValue ( object.getString ( name ) );

			else if ( type == FromServer.DATE )
				( ( DateSelector ) wid ).setValue ( object.getDate ( name ) );

			else if ( type == FromServer.BOOLEAN )
				( ( BooleanSelector ) wid ).setDown ( object.getBool ( name ) );

			else if ( type == FromServer.ADDRESS )
				( ( AddressSelector ) wid ).setValue ( object.getAddress ( name ) );

			else if ( type == FromServer.OBJECT )
				( ( FromServerSelector ) wid ).setSelected ( object.getObject ( name ) );
		}

		public boolean assign ( FromServer object ) {
			if ( type == -1 )
				return true;

			if ( validation.checkAttribute ( object, name, wid ) == false )
				return false;

			else if ( type == FromServer.STRING )
				object.setString ( name, ( ( TextBox ) wid ).getText () );

			else if ( type == FromServer.LONGSTRING )
				object.setString ( name, ( ( TextArea ) wid ).getText () );

			else if ( type == FromServer.INTEGER )
				object.setInt ( name, ( ( NumberBox ) wid ).getValue () );

			else if ( type == FromServer.FLOAT )
				object.setFloat ( name, ( ( FloatBox ) wid ).getValue () );

			else if ( type == FromServer.PERCENTAGE )
				object.setString ( name, ( ( PercentageBox ) wid ).getValue () );

			else if ( type == FromServer.DATE )
				object.setDate ( name, ( ( DateSelector ) wid ).getValue () );

			else if ( type == FromServer.BOOLEAN )
				object.setBool ( name, ( ( BooleanSelector ) wid ).isDown () );

			else if ( type == FromServer.ADDRESS )
				object.setAddress ( name, ( ( AddressSelector ) wid ).getValue () );

			else if ( type == FromServer.OBJECT )
				object.setObject ( name, ( ( FromServerSelector ) wid ).getSelected () );

			return true;
		}

		public boolean compare ( FromServer object ) {
			if ( type == -1 )
				return true;

			else if ( type == FromServer.STRING )
				return object.getString ( name ).equals ( ( ( TextBox ) wid ).getText () );

			else if ( type == FromServer.LONGSTRING )
				return object.getString ( name ).equals ( ( ( TextArea ) wid ).getText () );

			else if ( type == FromServer.INTEGER )
				return object.getInt ( name ) == ( ( NumberBox ) wid ).getValue ();

			else if ( type == FromServer.FLOAT )
				return object.getFloat ( name ) == ( ( FloatBox ) wid ).getValue ();

			else if ( type == FromServer.PERCENTAGE )
				return object.getString ( name ).equals ( ( ( PercentageBox ) wid ).getValue () );

			else if ( type == FromServer.DATE )
				return object.getDate ( name ).equals ( ( ( DateSelector ) wid ).getValue () );

			else if ( type == FromServer.BOOLEAN )
				return object.getBool ( name ) == ( ( BooleanSelector ) wid ).isDown ();

			else if ( type == FromServer.ADDRESS )
				return object.getAddress ( name ).equals ( ( ( AddressSelector ) wid ).getValue () );

			else if ( type == FromServer.OBJECT )
				return object.getObject ( name ).equals ( ( ( FromServerSelector ) wid ).getSelected () );

			return true;
		}
	}

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

	public FromServerForm ( FromServer obj ) {
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

		buttons = doButtons ();
		contents.add ( buttons );
		contents.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );
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

	private ButtonsBar doButtons () {
		ButtonsBar panel;
		PushButton button;
		final FromServerForm myself		= this;

		panel = new ButtonsBar ();

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

		return panel;
	}

	private void onOpenCb () {
		// DOM.scrollIntoView ( buttons.getElement () );
	}

	/****************************************************************** build */

	public Widget getWidget ( String attribute ) {
		int existing;
		FromServerWidget tmp;

		existing = widgets.size ();

		for ( int i = 0; i < existing; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );

			if ( tmp.name.equals ( attribute ) )
				return tmp;
		}

		tmp = new FromServerWidget ( object, attribute );
		widgets.add ( tmp );
		return tmp;
	}

	public Widget retriveInternalWidget ( String attribute ) {
		int existing;
		FromServerWidget tmp;

		existing = widgets.size ();

		for ( int i = 0; i < existing; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );

			if ( tmp.name.equals ( attribute ) )
				return tmp.wid;
		}

		return null;
	}

	public void setValidation ( String attribute, FromServerValidateCallback callback ) {
		int existing;
		FromServerWidget tmp;

		existing = widgets.size ();

		for ( int i = 0; i < existing; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );

			if ( tmp.name.equals ( attribute ) ) {
				tmp.validation = callback;
				break;
			}
		}
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

	private boolean contentsChanged () {
		int num;
		FromServerWidget tmp;

		num = widgets.size ();

		for ( int i = 0; i < num; i++ ) {
			tmp = ( FromServerWidget ) widgets.get ( i );

			if ( tmp.compare ( object ) == false )
				return true;
		}

		return false;
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
