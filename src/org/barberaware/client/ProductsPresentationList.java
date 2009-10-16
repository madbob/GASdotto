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

public class ProductsPresentationList extends Composite {
	private FormCluster		list;
	private Supplier		supplier;

	public ProductsPresentationList ( Supplier supp ) {
		VerticalPanel container;

		supplier = supp;

		container = new VerticalPanel ();
		container.setWidth ( "100%" );
		initWidget ( doListView () );
	}

	public void addProduct ( Product product ) {
		list.addElement ( product );
	}

	public void refreshProduct ( Product product ) {
		list.refreshElement ( product );
	}

	public void deleteProduct ( Product product ) {
		list.deleteElement ( product );
	}

	public int numProducts () {
		return list.latestIterableIndex ();
	}

	private Widget doListView () {
		list = new FormCluster ( "Product", null, false ) {
				protected FromServerForm doEditableRow ( FromServer product ) {
					FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					FloatLabel price;
					StringLabel desc;

					ver = new FromServerForm ( product, FromServerForm.NOT_EDITABLE );

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

					price = new FloatLabel ();
					price.setPostfix ( " €" );
					frame.addPair ( "Unitario", ver.getPersonalizedWidget ( "unit_price", price ) );

					price = new FloatLabel ();
					price.setPostfix ( " €" );
					frame.addPair ( "Trasporto", ver.getPersonalizedWidget ( "shipping_price", price ) );

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

				protected int sorting ( FromServer first, FromServer second ) {
					int comp_cat;
					Category first_cat;
					Category second_cat;

					first_cat = ( Category ) first.getObject ( "category" );
					second_cat = ( Category ) second.getObject ( "category" );

					comp_cat = first_cat.compare ( first_cat, second_cat );
					if ( comp_cat == 0 )
						return first.compare ( first, second );
					else
						return comp_cat;
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
}