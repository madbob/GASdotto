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

import com.allen_sauer.gwt.log.client.Log;

public class FromServerWidget extends Composite {
	public String				name;
	public int				type;
	public Widget				wid;
	public FromServerValidateCallback	validation;

	private void buildCommon ( String attribute, int attribute_type, Widget widget ) {
		name = attribute;
		type = attribute_type;
		wid = widget;
		validation = null;
	}

	/*
		Sono forniti tre costruttori per questo tipo di oggetto:

		- in cui si specifica tutto, l'oggetto ed il widget rappresentativo
			dell'attributo, da usare primariamente per i FromServer.ARRAY
			(che vengono gestiti con widgets personalizzati e settati con
			getPersonalizedWidget() )
		- in cui si specifica oggetto ed attributo, e che ricava da solo il
			widget da piazzare nel form completo
		- in cui si specifica il widget per il form, da usare solo per
			incapsularlo insieme agli altri ma marcato per essere saltato in
			fase di ricostruzione dell'oggetto completo. Da usare per
			immettere elementi di formattazione con setExtraWidget()
	*/

	public FromServerWidget ( FromServer object, String attribute, Widget widget ) {
		buildCommon ( attribute, object.getAttributeType ( attribute ), widget );
		initWidget ( widget );
		set ( object );
	}

	public FromServerWidget ( FromServer object, String attribute ) {
		buildCommon ( attribute, object.getAttributeType ( attribute ), null );

		if ( type == FromServer.STRING ) {
			DummyTextBox tb;

			tb = new DummyTextBox ();
			tb.setMaxLength ( 100 );
			wid = tb;
		}

		else if ( type == FromServer.LONGSTRING )
			wid = new DummyTextArea ();

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

		else if ( type == FromServer.OBJECT ) {
			/*
				Di default per la selezione di un oggetto si crea un
				FromServerSelector ordinato per nome e che non permette selezione
				vuota: per personalizzare i propri parametri occorre crearsi per
				conto proprio il FromServerSelector ed aggiungerlo nel form a mano
			*/
			wid = new FromServerSelector ( object.getClassName ( attribute ), false, true, false );
		}

		else if ( type == FromServer.PRICE )
			wid = new PriceBox ();

		/*
			Il tipo FromServer.ARRAY non viene gestito direttamente, occorre
			implementare un FromServerArray
		*/

		initWidget ( wid );
		set ( object );
	}

	public FromServerWidget ( String attribute, Widget widget ) {
		buildCommon ( attribute, -1, widget );
	}

	public void set ( FromServer object ) {
		if ( type == -1 || object == null ) {
			return;
		}
		else if ( type == FromServer.STRING || type == FromServer.LONGSTRING ) {
			( ( StringWidget ) wid ).setValue ( object.getString ( name ) );
		}
		else if ( type == FromServer.INTEGER ) {
			( ( IntNumericWidget ) wid ).setVal ( object.getInt ( name ) );
		}
		else if ( type == FromServer.FLOAT || type == FromServer.PRICE ) {
			( ( FloatWidget ) wid ).setVal ( object.getFloat ( name ) );
		}
		else if ( type == FromServer.PERCENTAGE ) {
			( ( PercentageWidget ) wid ).setValue ( object.getString ( name ) );
		}
		else if ( type == FromServer.DATE ) {
			( ( DateWidget ) wid ).setValue ( object.getDate ( name ) );
		}
		else if ( type == FromServer.BOOLEAN ) {
			( ( BooleanWidget ) wid ).setVal ( object.getBool ( name ) );
		}
		else if ( type == FromServer.ADDRESS ) {
			( ( AddressWidget ) wid ).setValue ( object.getAddress ( name ) );
		}
		else if ( type == FromServer.ARRAY ) {
			( ( FromServerArray ) wid ).setElements ( object.getArray ( name ) );
		}
		else if ( type == FromServer.OBJECT ) {
			FromServer tmp;

			tmp = object.getObject ( name );
			if ( tmp != null )
				( ( ObjectWidget ) wid ).setValue ( tmp.duplicate () );
		}
	}

	public boolean assign ( FromServer object ) {
		if ( type == -1 )
			return true;

		if ( ( validation != null ) && ( validation.check ( object, name, wid ) == false ) )
			return false;

		else if ( type == FromServer.STRING || type == FromServer.LONGSTRING )
			object.setString ( name, ( ( StringWidget ) wid ).getValue () );

		else if ( type == FromServer.INTEGER )
			object.setInt ( name, ( ( IntNumericWidget ) wid ).getVal () );

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE ) {
			object.setFloat ( name, ( ( FloatWidget ) wid ).getVal () );
		}

		else if ( type == FromServer.PERCENTAGE )
			object.setString ( name, ( ( PercentageWidget ) wid ).getValue () );

		else if ( type == FromServer.DATE )
			object.setDate ( name, ( ( DateWidget ) wid ).getValue () );

		else if ( type == FromServer.BOOLEAN )
			object.setBool ( name, ( ( BooleanWidget ) wid ).getVal () );

		else if ( type == FromServer.ADDRESS )
			object.setAddress ( name, ( ( AddressWidget ) wid ).getValue () );

		else if ( type == FromServer.ARRAY )
			object.setArray ( name, ( ( FromServerArray ) wid ).getElements () );

		else if ( type == FromServer.OBJECT )
			object.setObject ( name, ( ( ObjectWidget ) wid ).getValue () );

		return true;
	}

	public boolean compare ( FromServer object ) {
		boolean ret;

		ret = true;

		if ( type == -1 )
			ret = true;

		else if ( type == FromServer.STRING || type == FromServer.LONGSTRING )
			ret = object.getString ( name ).equals ( ( ( StringWidget ) wid ).getValue () );

		else if ( type == FromServer.INTEGER )
			ret = object.getInt ( name ) == ( ( IntNumericWidget ) wid ).getVal ();

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE )
			ret = object.getFloat ( name ) == ( ( FloatWidget ) wid ).getVal ();

		else if ( type == FromServer.PERCENTAGE )
			ret = object.getString ( name ).equals ( ( ( PercentageWidget ) wid ).getValue () );

		else if ( type == FromServer.DATE ) {
			Date newer;
			Date older;

			newer = ( ( DateWidget ) wid ).getValue ();
			older = object.getDate ( name );

			if ( newer == null && older == null )
				ret = true;
			else if ( newer == null || older == null )
				ret = false;
			else
				ret = older.equals ( newer );
		}

		else if ( type == FromServer.BOOLEAN )
			ret = object.getBool ( name ) == ( ( BooleanWidget ) wid ).getVal ();

		else if ( type == FromServer.ADDRESS )
			ret = object.getAddress ( name ).equals ( ( ( AddressWidget ) wid ).getValue () );

		else if ( type == FromServer.ARRAY ) {
			ArrayList first;
			ArrayList second;

			first = object.getArray ( name );
			if ( first != null )
				first = Utils.duplicateFromServerArray ( object.getArray ( name ) );

			second = ( ( FromServerArray ) wid ).getElements ();

			if ( first == null && second == null )
				return true;
			else if ( first == null || second == null )
				return false;

			ret = Utils.compareFromServerArray ( first, second );
		}

		else if ( type == FromServer.OBJECT ) {
			FromServer previous;
			FromServer selected;

			previous = object.getObject ( name );
			selected = ( ( ObjectWidget ) wid ).getValue ();

			if ( previous != null && selected != null ) {
				ret = previous.equals ( selected );
			}
			else {
				if ( previous == null && selected == null )
					ret = true;
				else
					ret = false;
			}
		}

		return ret;
	}
}
