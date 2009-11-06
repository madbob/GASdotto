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

import java.lang.*;
import java.util.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class Utils {
	private static SmoothingNotify	notifies;
	private static ServerHook	server;
	private static Stack		buttonsStack;

	public static void initEnvironment () {
		notifies = new SmoothingNotify ();
		server = new ServerHook ();
		buttonsStack = new Stack ();
	}

	/****************************************************** notifiche */

	/*
		Questa viene usata solo per piazzare una volta per tutte la finestra delle
		notifiche, non abusarne
	*/
	public static SmoothingNotify getNotificationsArea () {
		return notifies;
	}

	public static void showNotification ( String text ) {
		notifies.show ( text, SmoothingNotify.NOTIFY_ERROR );
	}

	public static void showNotification ( String text, int type ) {
		notifies.show ( text, type );
	}

	/****************************************************** server */

	public static ServerHook getServer () {
		return server;
	}

	/****************************************************** numeri */

	/*
		Questa funzione e' sostanzialmente un pessimo hack per avere i prezzi sempre con
		due cifre decimali, anche quando sono 00. Forse c'e' un modo migliore, ma non
		potendo contare su funzioni particolarmente sofisticate (non incluse nel set
		offerto da GWT) mi tocca ricorrere alla costruzione a mano della stringa
	*/
	public static String priceToString ( float price ) {
		int sep;
		String ret;

		ret = Float.toString ( ( float ) ( ( int ) ( ( price + 0.005 ) * 100 ) ) / 100 );
		sep = ret.indexOf ( "." );

		if ( sep == -1 )
			ret = ret + ".00";
		else
			if ( ret.substring ( sep ).length () != 3 )
				ret = ret + "0";

		return ret;
	}

	/****************************************************** classi */

	public static String classFinalName ( String name ) {
		return name.substring ( name.lastIndexOf ( "." ) + 1 );
	}

	/****************************************************** datatype */

	public static ArrayList dupliacateFromServerArray ( ArrayList array ) {
		FromServer iter;
		ArrayList ret;

		if ( array == null )
			return null;

		ret = new ArrayList ();

		for ( int i = 0; i < array.size (); i++ ) {
			iter = ( FromServer ) array.get ( i );
			ret.add ( iter.duplicate () );
		}

		return ret;
	}

	public static float sumPercentage ( float origin, String percentage ) {
		float perc;

		if ( percentage == null || percentage.length () == 0 )
			return origin;

		if ( percentage.endsWith ( "%" ) ) {
			int symbol;

			symbol = percentage.indexOf ( "%" );
			perc = Float.parseFloat ( percentage.substring ( 0, symbol ) );
			return origin + ( ( origin * perc ) / 100 );
		}
		else {
			perc = Float.parseFloat ( percentage );
			return origin + perc;
		}
	}

	public static String showPercentage ( String percentage ) {
		if ( percentage == null || percentage.length () == 0 )
			return "";

		if ( percentage.endsWith ( "%" ) )
			return percentage;
		else
			return percentage + " €";
	}

	/****************************************************** date */

	public static String[] months = new String[] {
		"Gennaio", "Febbraio", "Marzo",
		"Aprile", "Maggio", "Giugno",
		"Luglio", "Agosto", "Settembre",
		"Ottobre", "Novembre", "Dicembre" };

	public static String[] days = new String[] {
		"Domenica", "Lunedi", "Martedi",
		"Mercoledi", "Giovedi", "Venerdi", "Sabato" };

	public static String encodeDate ( Date d ) {
		int y;

		y = d.getYear ();
		if ( y < 1000 )
			y += 1900;

		return y + "-" + ( d.getMonth () + 1 ) + "-" + d.getDate ();
	}

	public static Date decodeDate ( String str ) {
		int y;
		Date ret;
		String [] dates;

		if ( str == null || str.equals ( "" ) )
			return null;

		dates = str.split ( "-" );

		y = Integer.parseInt ( dates [ 0 ] );
		if ( y > 1000 )
			y -= 1900;

		ret = new Date ( y,
				Integer.parseInt ( dates [ 1 ] ) - 1,
				Integer.parseInt ( dates [ 2 ] ) );

		return ret;
	}

	public static String printableDate ( Date d ) {
		return d.getDate () + " " + months [ d.getMonth () ] + " " + ( d.getYear () + 1900 );
	}

	/****************************************************** json */

	public static JSONArray JSONArrayRemove ( JSONArray array, int index ) {
		int size;
		JSONArray tmp;

		tmp = new JSONArray ();
		size = array.size ();

		for ( int i = 0; i < index; i++ )
			tmp.set ( i, array.get ( i ) );

		for ( int i = index + 1; i < size; i++ )
			tmp.set ( i, array.get ( i ) );

		return tmp;
	}

	/****************************************************** graphic */

	public static void graphicPulseWidget ( final Widget wid ) {
		Timer fading;
		Element tmp;

		tmp = wid.getElement ();
		DOM.setStyleAttribute ( tmp, "opacity", "0.4" );
		DOM.setStyleAttribute ( tmp, "filter", "alpha(opacity:1.0)" );

		fading = new Timer () {
			public void run () {
				double opacity;
				Element tmp;

				tmp = wid.getElement ();
				opacity = Double.parseDouble ( DOM.getStyleAttribute ( tmp, "opacity" ) );

				if ( opacity < 1.0 ) {
					String newvalue;
					newvalue = Double.toString ( opacity + 0.2 );
					DOM.setStyleAttribute ( tmp, "opacity", newvalue );
					DOM.setStyleAttribute ( tmp, "filter", "alpha(opacity:" + newvalue + ")" );
				}
				else {
					cancel ();
				}
			}
		};

		fading.scheduleRepeating ( 100 );
	}
}
