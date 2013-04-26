/*  GASdotto
 *  Copyright (C) 2010/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class ProductVariantValuesList extends FlexTable implements FromServerArray {
	public ProductVariantValuesList () {
		reset ();

		addTableListener ( new TableListener () {
			public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
				TextBox text;

				/**
					TODO	Sarebbe piu' bello mostrare l'icona del cestino
						solo accanto alle caselle con un qualche contenuto
				*/

				if ( cell == 2 ) {
					text = ( TextBox ) getWidget ( row, 1 );
					if ( text.getText () != "" )
						removeRow ( row );
				}
			}
		} );
	}

	private void reset () {
		while ( getRowCount () != 0 )
			removeRow ( 0 );

		addRow ( null );
		addRow ( null );
	}

	private void checkAddRow () {
		int empty;
		TextBox text;

		empty = 0;

		for ( int i = 0; i < getRowCount (); i++ ) {
			text = ( TextBox ) getWidget ( i, 1 );

			if ( text.getText () == "" ) {
				empty++;
				if ( empty == 2 )
					break;
			}
		}

		for ( ; empty < 2; empty++ )
			addRow ( null );
	}

	private void addRow ( FromServer element ) {
		int row;
		TextBox text;

		row = getRowCount ();

		if ( element != null )
			setWidget ( row, 0, new Hidden ( Integer.toString ( element.getLocalID () ) ) );
		else
			setWidget ( row, 0, new Hidden ( "-1" ) );

		text = new TextBox ();

		text.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				checkAddRow ();
			}
		} );

		setWidget ( row, 1, text );

		if ( element != null )
			text.setText ( element.getString ( "name" ) );

		setWidget ( row, 2, new Image ( "images/mini_delete.png" ) );
	}

	private int findElement ( FromServer element ) {
		String el_id;
		Hidden id;

		el_id = Integer.toString ( element.getLocalID () );

		for ( int i = 0; i < getRowCount (); i++ ) {
			id = ( Hidden ) getWidget ( i, 0 );
			if ( id.getName ().equals ( el_id ) )
				return i;
		}

		return -1;
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		addRow ( element );
	}

	public void setElements ( ArrayList elements ) {
		int num;
		ArrayList sorted_elements;

		while ( getRowCount () != 0 )
			removeRow ( 0 );

		sorted_elements = Utils.sortArrayByName ( elements );
		num = sorted_elements.size ();

		for ( int i = 0; i < num; i++ )
			addRow ( ( FromServer ) sorted_elements.get ( i ) );

		addRow ( null );
		addRow ( null );
	}

	public void removeElement ( FromServer element ) {
		int index;

		index = findElement ( element );
		if ( index != -1 )
			removeRow ( index );
	}

	public ArrayList getElements () {
		ArrayList ret;
		Hidden id;
		TextBox name;
		FromServer el;

		ret = new ArrayList ();

		for ( int i = 0; i < getRowCount (); i++ ) {
			name = ( TextBox ) getWidget ( i, 1 );
			if ( name.getText () == "" )
				continue;

			id = ( Hidden ) getWidget ( i, 0 );

			if ( id.getName () != "-1" )
				el = Utils.getServer ().getObjectFromCache ( "ProductVariantValue", Integer.parseInt ( id.getName () ) );
			else
				el = new ProductVariantValue ();

			el.setString ( "name", name.getText () );
			ret.add ( el );
		}

		return ret;
	}

	public void refreshElement ( FromServer element ) {
		int index;
		TextBox text;

		index = findElement ( element );
		if ( index != -1 ) {
			text = ( TextBox ) getWidget ( index, 1 );
			text.setText ( element.getString ( "name" ) );
		}
	}
}
