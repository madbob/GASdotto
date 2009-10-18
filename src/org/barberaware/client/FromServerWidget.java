/*  GASdotto 0.1
 *  Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

		else if ( type == FromServer.LONGSTRING ) {
			DummyTextArea ta;

			ta = new DummyTextArea ();
			ta.setVisibleLines ( 2 );
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

		else if ( type == FromServer.OBJECT ) {
			/*
				Di default per la selezione di un oggetto si crea un
				FromServerSelector ordinato per nome e che non permette selezione
				vuota: per personalizzare i propri parametri occorre crearsi per
				conto proprio il FromServerSelector ed aggiungerlo nel form a mano
			*/
			wid = new FromServerSelector ( object.getClassName ( attribute ), false, true );
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
		if ( type == -1 )
			return;

		else if ( type == FromServer.STRING || type == FromServer.LONGSTRING )
			( ( StringWidget ) wid ).setValue ( object.getString ( name ) );

		else if ( type == FromServer.INTEGER )
			( ( IntNumericWidget ) wid ).setVal ( object.getInt ( name ) );

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE )
			( ( FloatWidget ) wid ).setVal ( object.getFloat ( name ) );

		else if ( type == FromServer.PERCENTAGE )
			( ( PercentageBox ) wid ).setValue ( object.getString ( name ) );

		else if ( type == FromServer.DATE )
			( ( DateSelector ) wid ).setValue ( object.getDate ( name ) );

		else if ( type == FromServer.BOOLEAN )
			( ( BooleanSelector ) wid ).setDown ( object.getBool ( name ) );

		else if ( type == FromServer.ADDRESS )
			( ( AddressSelector ) wid ).setValue ( object.getAddress ( name ) );

		else if ( type == FromServer.ARRAY )
			( ( FromServerArray ) wid ).setElements ( object.getArray ( name ) );

		else if ( type == FromServer.OBJECT )
			( ( ObjectWidget ) wid ).setValue ( object.getObject ( name ) );
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

		else if ( type == FromServer.FLOAT || type == FromServer.PRICE )
			object.setFloat ( name, ( ( FloatWidget ) wid ).getVal () );

		else if ( type == FromServer.PERCENTAGE )
			object.setString ( name, ( ( PercentageBox ) wid ).getValue () );

		else if ( type == FromServer.DATE )
			object.setDate ( name, ( ( DateSelector ) wid ).getValue () );

		else if ( type == FromServer.BOOLEAN )
			object.setBool ( name, ( ( BooleanSelector ) wid ).isDown () );

		else if ( type == FromServer.ADDRESS )
			object.setAddress ( name, ( ( AddressSelector ) wid ).getValue () );

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
			ret = object.getString ( name ).equals ( ( ( PercentageBox ) wid ).getValue () );

		else if ( type == FromServer.DATE ) {
			Date newer;
			Date older;

			newer = ( ( DateSelector ) wid ).getValue ();
			older = object.getDate ( name );

			if ( newer == null && older == null )
				ret = true;
			else if ( newer == null || older == null )
				ret = false;
			else
				ret = older.equals ( newer );
		}

		else if ( type == FromServer.BOOLEAN )
			ret = object.getBool ( name ) == ( ( BooleanSelector ) wid ).isDown ();

		else if ( type == FromServer.ADDRESS )
			ret = object.getAddress ( name ).equals ( ( ( AddressSelector ) wid ).getValue () );

		else if ( type == FromServer.ARRAY ) {
			int flen;
			int slen;
			ArrayList first;
			ArrayList second;
			FromServer ftmp;
			FromServer stmp;

			first = object.getArray ( name );
			if ( first != null )
				first = Utils.dupliacateFromServerArray ( object.getArray ( name ) );

			second = ( ( FromServerArray ) wid ).getElements ();

			if ( first == null && second == null )
				return true;
			else if ( first == null || second == null )
				return false;

			flen = first.size ();
			slen = second.size ();

			if ( flen != slen )
				ret = false;

			else {
				int i;

				for ( i = 0; i < flen; i++ ) {
					ftmp = ( FromServer ) first.get ( i );
					stmp = ( FromServer ) second.get ( i );

					if ( ftmp.compare ( ftmp, stmp ) != 0 ) {
						ret = false;
						break;
					}
				}

				if ( i == flen )
					ret = true;
			}
		}

		else if ( type == FromServer.OBJECT ) {
			FromServer previous;
			FromServer selected;

			previous = object.getObject ( name );
			selected = ( ( ObjectWidget ) wid ).getValue ();

			if ( previous != null && selected != null )
				ret = previous.equals ( selected );

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
