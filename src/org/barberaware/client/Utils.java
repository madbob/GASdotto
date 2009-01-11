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

public class Utils {
	private static SmoothingNotify	notifies;
	private static ServerHook	server;

	public static void initEnvironment () {
		notifies = new SmoothingNotify ();
		server = new ServerHook ();
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
		notifies.show ( text );
	}

	/****************************************************** server */

	public static ServerHook getServer () {
		return server;
	}

	/****************************************************** classi */

	public static String classFinalName ( String name ) {
		return name.substring ( name.lastIndexOf ( "." ) + 1 );
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
}
