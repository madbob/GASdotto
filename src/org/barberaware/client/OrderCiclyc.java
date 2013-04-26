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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class OrderCiclyc extends Composite implements StringWidget {
	public int			ORDER_NEVER_REPEATS		= 0;
	public int			ORDER_REPEATS_EACH_WEEK		= 1;
	public int			ORDER_REPEATS_EACH_MONTH	= 2;
	public int			ORDER_REPEATS_EACH_TWO_MONTH	= 3;
	public int			ORDER_REPEATS_EACH_THREE_MONTH	= 4;
	public int			ORDER_REPEATS_EACH_SIX_MONTH	= 5;
	public int			ORDER_REPEATS_EACH_YEAR		= 6;
	public int			ORDER_REPEATS_DEFINED		= 7;

	private TextBox			main;
	private String			currentValue;
	private int			intValue;

	private DialogBox		dialog;
	private ListBox			selector;
	private DateSelector		dateSelector;

	private boolean			opened;

	public OrderCiclyc () {
		opened = false;

		dialog = new DialogBox ( false );
		dialog.setText ( "Seleziona Ciclicità" );
		dialog.setWidget ( doDialog () );

		main = new TextBox ();
		main.setStyleName ( "cycle-selector" );
		main.setVisibleLength ( 40 );
		main.addFocusListener ( new FocusListener () {
			public void onFocus ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					syncToDialog ();
					dialog.center ();
					dialog.show ();
				}
			}

			public void onLostFocus ( Widget sender ) {
				/* dummy */
			}
		} );

		initWidget ( main );
		clean ();
	}

	private Panel doDialog () {
		VerticalPanel pan;
		HorizontalPanel buttons;
		Button but;

		pan = new VerticalPanel ();

		selector = new ListBox ();
		selector.addItem ( "Mai", Integer.toString ( ORDER_NEVER_REPEATS ) );
		selector.addItem ( "Ogni settimana", Integer.toString ( ORDER_REPEATS_EACH_WEEK ) );
		selector.addItem ( "Ogni mese", Integer.toString ( ORDER_REPEATS_EACH_MONTH ) );
		selector.addItem ( "Ogni due mesi", Integer.toString ( ORDER_REPEATS_EACH_TWO_MONTH ) );
		selector.addItem ( "Ogni tre mesi", Integer.toString ( ORDER_REPEATS_EACH_THREE_MONTH ) );
		selector.addItem ( "Ogni sei mesi", Integer.toString ( ORDER_REPEATS_EACH_SIX_MONTH ) );
		selector.addItem ( "Ogni anno", Integer.toString ( ORDER_REPEATS_EACH_YEAR ) );
		selector.addItem ( "Specifica manuale", Integer.toString ( ORDER_REPEATS_DEFINED ) );
		pan.add ( selector );
		selector.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				if ( selector.getSelectedIndex () == ORDER_REPEATS_DEFINED )
					dateSelector.setVisible ( true );
				else
					dateSelector.setVisible ( false );
			}
		} );

		dateSelector = new DateSelector ();
		pan.add ( dateSelector );
		dateSelector.setVisible ( false );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				syncFromDialog ();
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		return pan;
	}

	private void syncToDialog () {
		selector.setItemSelected ( intValue, true );

		if ( intValue == ORDER_REPEATS_DEFINED )
			dateSelector.setVisible ( true );
		else
			dateSelector.setVisible ( false );
	}

	private void syncFromDialog () {
		String str;

		intValue = selector.getSelectedIndex ();

		if ( intValue == ORDER_REPEATS_DEFINED )
			str = Utils.printableDate ( dateSelector.getValue () );
		else
			str = selector.getItemText ( intValue );

		main.setText ( str );
	}

	public void setValue ( String value ) {
		String [] tokens;

		tokens = value.split ( ":" );

		try {
			intValue = Integer.parseInt ( tokens [ 0 ] );
		}
		catch ( NumberFormatException e ) {
			clean ();
			return;
		}

		if ( intValue == ORDER_REPEATS_DEFINED )
			dateSelector.setValue ( Utils.decodeDate ( tokens [ 1 ] ) );

		selector.setItemSelected ( intValue, true );
		syncFromDialog ();
	}

	public String getValue () {
		String ret;

		ret = Integer.toString ( intValue );
		if ( intValue == ORDER_REPEATS_DEFINED )
			ret += ":" + Utils.encodeDate ( dateSelector.getValue () );

		return ret;
	}

	public void clean () {
		setValue ( Integer.toString ( ORDER_NEVER_REPEATS ) );
	}
}
