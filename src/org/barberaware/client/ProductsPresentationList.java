/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public class ProductsPresentationList extends Composite implements FromServerArray, Lockable {
	private FormCluster		list;
	private Supplier		supplier;

	public ProductsPresentationList ( Supplier supp ) {
		VerticalPanel container;

		supplier = supp;

		container = new VerticalPanel ();
		container.setWidth ( "100%" );
		initWidget ( doListView () );
	}

	private FromServerValidateCallback filterSupplier () {
		return new FromServerValidateCallback () {
			public boolean checkObject ( FromServer object ) {
				return supplier.equals ( object.getObject ( "supplier" ) );
			}
		};
	}

	private Widget doListView () {
		list = new FormCluster ( "Product", null, filterSupplier (), true ) {
				protected FromServerForm doEditableRow ( FromServer product ) {
					FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					StringLabel desc;

					if ( product.getBool ( "archived" ) == true )
						return null;

					ver = new FromServerForm ( product, FromServerForm.NOT_EDITABLE );
					ver.addStyleName ( "subform" );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Categoria", ver.getPersonalizedWidget ( "category", new NameLabelWidget () ) );
					frame.addPair ( "Unità di misura", ver.getPersonalizedWidget ( "measure", new NameLabelWidget () ) );

					/* seconda colonna */

					frame = new CustomCaptionPanel ( "Prezzo" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Prezzo Unitario", ver.getPersonalizedWidget ( "unit_price", new PriceViewer () ) );
					frame.addPair ( "Prezzo Trasporto", ver.getPersonalizedWidget ( "shipping_price", new PriceViewer () ) );

					if ( product.getFloat ( "minimum_order" ) != 0 )
						frame.addPair ( "Minimo per Utente", ver.getPersonalizedWidget ( "minimum_order", new FloatViewer () ) );
					if ( product.getFloat ( "multiple_order" ) != 0 )
						frame.addPair ( "Multiplo per Utente", ver.getPersonalizedWidget ( "multiple_order", new FloatViewer () ) );

					sframe = new CaptionPanel ( "Descrizione" );
					desc = new StringLabel ();
					desc.setDefault ( "Non è stata settata alcuna descrizione" );
					sframe.add ( ver.getPersonalizedWidget ( "description", desc ) );
					ver.add ( sframe );

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					return null;
				}

				protected void customModify ( FromServerForm form ) {
					FromServer obj;

					obj = form.getObject ();
					if ( obj.getBool ( "archived" ) == true )
						deleteElement ( obj );
				}
		};

		return list;
	}

	/****************************************************************** Lockable */

	public void unlock () {
		list.unlock ();
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		list.addElement ( element );
	}

	public void setElements ( ArrayList elements ) {
		for ( int i = 0; i < elements.size (); i++ )
			addElement ( ( FromServer ) elements.get ( i ) );
	}

	public void removeElement ( FromServer element ) {
		list.deleteElement ( element );
	}

	public ArrayList getElements () {
		return list.collectContents ();
	}

	public void refreshElement ( FromServer element ) {
		list.refreshElement ( element );
	}
}
