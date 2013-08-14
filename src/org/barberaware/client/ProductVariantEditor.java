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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class ProductVariantEditor extends DialogBox implements ObjectWidget, SavingDialog {
	private TextBox				name;
	private ProductVariantValuesList	values;

	private FromServer			currentValue;

	private ArrayList			callbacks;

	public ProductVariantEditor ( boolean new_item ) {
		VerticalPanel pan;
		DialogButtons buttons;
		HorizontalPanel row;
		Button but;

		callbacks = null;
		currentValue = null;

		setText ( "Edita Variante" );

		pan = new VerticalPanel ();
		setWidget ( pan );

		if ( new_item == true ) {
			but = new Button ( "Duplica da variante esistente", new ClickHandler () {
				public void onClick ( ClickEvent event ) {
					runDuplication ();
				}
			} );

			pan.add ( but );
			pan.setCellHorizontalAlignment ( but, HasHorizontalAlignment.ALIGN_CENTER );
		}

		name = new TextBox ();
		row = new HorizontalPanel ();
		row.add ( new Label ( "Nome Variante: " ) );
		row.add ( name );
		pan.add ( row );

		values = new ProductVariantValuesList ();
		pan.add ( values );

		buttons = new DialogButtons ();
		pan.add ( buttons );
		buttons.addCallback ( new SavingDialogCallback () {
			public void onSave ( SavingDialog dialog ) {
				String n;
				ArrayList vals;

				n = name.getText ();
				if ( n == "" ) {
					Utils.showNotification ( "Devi specificare un nome per questa variante" );
					return;
				}

				vals = values.getElements ();
				if ( vals.size () == 0 ) {
					Utils.showNotification ( "Non hai specificato alcun valore per questa variante" );
					return;
				}

				if ( currentValue == null )
					currentValue = new ProductVariant ();

				currentValue.setString ( "name", n );
				currentValue.setArray ( "values", vals );
				fireCallbacks ();
				hide ();
			}

			public void onCancel ( SavingDialog dialog ) {
				hide ();
			}
		} );
	}

	private void runDuplication () {
		ArrayList products;
		ArrayList variants;
		VerticalPanel pan;
		DialogButtons buttons;
		Button but;
		FromServer product;
		FromServer variant;
		final DialogBox dialog;
		final ListBox list;

		dialog = new DialogBox ();
		dialog.setText ( "Seleziona variante da duplicare" );

		pan = new VerticalPanel ();
		pan.setWidth ( "100%" );
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		dialog.setWidget ( pan );

		list = new ListBox ();
		list.addStyleName ( "multirow-select" );
		list.setVisibleItemCount ( 20 );
		list.setWidth ( "100%" );

		products = Utils.getServer ().getObjectsFromCache ( "Product" );

		for ( int i = 0; i < products.size (); i++ ) {
			product = ( FromServer ) products.get ( i );
			variants = product.getArray ( "variants" );

			if ( variants == null || variants.size () == 0 )
				continue;

			for ( int a = 0; a < variants.size (); a++ ) {
				variant = ( FromServer ) variants.get ( a );
				list.addItem ( product.getString ( "name" ) + " : " + variant.getString ( "name" ), Integer.toString ( variant.getLocalID () ) );
			}
		}

		/*
			E' un po' uno spreco creare la ListBox per poi manco aggiungerla nel
			pannello qualora non ci siano prodotti con varianti con cui popolarla, ma
			abbastanza sbrigativo
		*/
		if ( list.getItemCount () == 0 ) {
			pan.add ( new Label ( "Non ci sono altri prodotti con varianti da duplicare" ) );

			but = new Button ( "Annulla", new ClickHandler () {
				public void onClick ( ClickEvent event ) {
					dialog.hide ();
				}
			} );
			pan.add ( but );
		}
		else {
			pan.add ( list );

			buttons = new DialogButtons ();
			pan.add ( buttons );
			buttons.addCallback ( new SavingDialogCallback () {
				public void onSave ( SavingDialog sender ) {
					int sel;
					int id;

					sel = list.getSelectedIndex ();
					if ( sel == -1 ) {
						Utils.showNotification ( "Non hai selezionato alcuna variante da duplicare" );
						return;
					}

					id = Integer.parseInt ( list.getValue ( sel ) );
					setValueByID ( id );
					dialog.hide ();
				}

				public void onCancel ( SavingDialog sender ) {
					dialog.hide ();
				}
			} );
		}

		dialog.center ();
		dialog.show ();
	}

	private void setValueByID ( int id ) {
		FromServer variant;

		variant = Utils.getServer ().getObjectFromCache ( "ProductVariant", id );
		variant = variant.duplicate ();
		variant.setLocalID ( -1 );
		setValue ( variant );
	}

	private void fireCallbacks () {
		Utils.triggerSaveCallbacks ( callbacks, this, 1 );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer element ) {
		currentValue = element;

		name.setText ( currentValue.getString ( "name" ) );
		values.setElements ( currentValue.getArray ( "values" ) );
	}

	public FromServer getValue () {
		return currentValue;
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( callbacks == null )
			callbacks = new ArrayList ();
		callbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( callbacks == null )
			return;
		callbacks.remove ( callback );
	}
}
