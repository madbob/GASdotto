/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.i18n.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class Utils {
	private static SmoothingNotify	notifies;
	private static DisasterNotify	disaster;
	private static ServerHook	server;
	private static NumberFormat	floatFormatter;
	private static NumberFormat	priceFormatter;
	private static HashMap		emblemsCache;
	private static HashMap		classesNames;

	public static void initEnvironment () {
		notifies = new SmoothingNotify ();
		disaster = new DisasterNotify ();
		server = new ServerHook ();
		floatFormatter = NumberFormat.getDecimalFormat ();
		priceFormatter = NumberFormat.getCurrencyFormat ();
		emblemsCache = new HashMap ();
		classesNames = new HashMap ();
	}

	/****************************************************** notifiche */

	/*
		Questa viene usata solo per piazzare una volta per tutte la finestra delle
		notifiche, non abusarne
	*/
	public static SmoothingNotify getNotificationsArea () {
		return notifies;
	}

	public static DisasterNotify getDisasterArea () {
		return disaster;
	}

	public static void showNotification ( String text ) {
		notifies.show ( text, Notification.ERROR );
	}

	public static void showNotification ( String text, int type ) {
		notifies.show ( text, type );
	}

	public static void bigError ( String message, String url, String line ) {
		disaster.setMessage ( message );
		disaster.show ();
	}

	public static void infoDialog ( String title, String content ) {
		final DialogBox box;
		VerticalPanel contents;
		HTML message;
		HorizontalPanel buttons;
		Button button;

		box = new DialogBox ();
		box.setText ( title );

		contents = new VerticalPanel ();

		message = new HTML ( content );
		message.setStyleName ( "message" );
		contents.add ( message );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		contents.add ( buttons );

		button = new Button ( "OK", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				box.hide ();
			}
		} );
		buttons.add ( button );

		box.setWidget ( contents );
		box.center ();
		box.show ();
	}

	/****************************************************** emblemi */

	public static void initEmblems () {
		ArrayList paths;
		ArrayList desc;
		EmblemsInfo info;

		info = new EmblemsInfo ();
		paths = new ArrayList ();
		desc = new ArrayList ();
		paths.add ( "images/notifications/order_open.png" );
		desc.add ( "Ordine Aperto" );
		paths.add ( "images/notifications/order_closed.png" );
		desc.add ( "Ordine Chiuso" );
		paths.add ( "images/notifications/order_suspended.png" );
		desc.add ( "Ordine Sospeso" );
		paths.add ( "images/notifications/order_completed.png" );
		desc.add ( "Ordine Consegnato" );
		info.addSymbol ( "status", paths, desc );
		info.addSymbol ( "multiuser", "images/notifications/multiuser_order.png", "Tu sei Referente per questo Ordine" );
		info.addSymbol ( "aggregate", "images/notifications/order_aggregate.png", "Ordine Aggregato" );
		emblemsCache.put ( "orders", info );

		info = new EmblemsInfo ();
		paths = new ArrayList ();
		desc = new ArrayList ();
		paths.add ( "" );
		desc.add ( "" );
		paths.add ( "images/notifications/order_shipping.png" );
		desc.add ( "Consegna Parziale" );
		paths.add ( "images/notifications/order_shipped.png" );
		desc.add ( "Consegnato" );
		paths.add ( "images/notifications/order_saved.png" );
		desc.add ( "Consegna Salvata" );
		info.addSymbol ( "status", paths, desc );

		if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
			paths = new ArrayList ();
			desc = new ArrayList ();
			paths.add ( "" );
			desc.add ( "" );
			paths.add ( "images/notifications/pay_by_bank.png" );
			desc.add ( "Pagato con Credito" );
			paths.add ( "images/notifications/pay_by_cash.png" );
			desc.add ( "Pagato in Contanti" );
			info.addSymbol ( "paymethod", paths, desc );
		}

		if ( Session.getGAS ().getBool ( "payments" ) == true )
			info.addSymbol ( "paying", "images/notifications/user_not_paying.png", "L'Utente non ha Pagato l'Iscrizione" );

		if ( Session.getGAS ().getBool ( "use_bank" ) == true ) {
			paths = new ArrayList ();
			desc = new ArrayList ();
			paths.add ( "" );
			desc.add ( "" );
			paths.add ( "images/notifications/user_yes_credit.png" );
			desc.add ( "Credito Sufficiente" );
			paths.add ( "images/notifications/user_half_credit.png" );
			desc.add ( "Credito Insufficiente ma Caparra Disponibile" );
			paths.add ( "images/notifications/user_no_credit.png" );
			desc.add ( "Credito Insufficiente" );
			info.addSymbol ( "credit", paths, desc );
		}

		emblemsCache.put ( "delivery", info );

		info = new EmblemsInfo ();
		info.addSymbol ( "iamreference", "images/notifications/user_responsable.png", "Tu sei Referente per questo Fornitore" );
		info.addSymbol ( "hidden", "images/notifications/user_leaved.png", "Fornitore Nascosto" );
		emblemsCache.put ( "supplier", info );

		info = new EmblemsInfo ();
		paths = new ArrayList ();
		desc = new ArrayList ();
		paths.add ( "images/notifications/info.png" );
		desc.add ( "Informazione" );
		paths.add ( "images/notifications/warning.png" );
		desc.add ( "Avviso" );
		paths.add ( "images/notifications/error.png" );
		desc.add ( "Problema" );
		info.addSymbol ( "type", paths, desc );
		emblemsCache.put ( "notifications", info );

		info = new EmblemsInfo ();
		paths = new ArrayList ();
		desc = new ArrayList ();
		paths.add ( "" );
		desc.add ( "" );
		paths.add ( "images/notifications/user_responsable.png" );
		desc.add ( "Utente Referente" );
		paths.add ( "images/notifications/user_admin.png" );
		desc.add ( "Utente Amministratore" );
		paths.add ( "images/notifications/user_leaved.png" );
		desc.add ( "Utente Cessato" );
		info.addSymbol ( "privileges", paths, desc );

		if ( Session.getGAS ().getBool ( "payments" ) == true )
			info.addSymbol ( "paying", "images/notifications/user_not_paying.png", "L'Utente non ha Pagato l'Iscrizione" );

		emblemsCache.put ( "users", info );
	}

	public static void setEmblemsCache ( String name, EmblemsInfo info ) {
		emblemsCache.put ( name, info );
	}

	public static EmblemsInfo getEmblemsCache ( String name ) {
		return ( EmblemsInfo ) emblemsCache.get ( name );
	}

	/****************************************************** server */

	public static ServerHook getServer () {
		return server;
	}

	/****************************************************** numeri */

	public static String priceToString ( float f ) {
		return priceFormatter.format ( f );
	}

	public static float stringToPrice ( String s ) {
		if ( s == "" )
			return 0;
		else
			return ( float ) priceFormatter.parse ( s );
	}

	public static String floatToString ( float f ) {
		return floatFormatter.format ( f );
	}

	public static float stringToFloat ( String s ) {
		return ( float ) floatFormatter.parse ( s );
	}

	/*
		Ispirato da
		http://forums.devarticles.com/javascript-development-22/javascript-to-round-to-2-decimal-places-36190.html
	*/
	public static float roundNumber ( float num ) {
		return ( float ) Math.round ( num * 100 ) / ( float ) 100;
	}

	/****************************************************** classi */

	public static String classFinalName ( String name ) {
		String ret;

		ret = ( String ) classesNames.get ( name );
		if ( ret == null ) {
			ret = name.substring ( name.lastIndexOf ( "." ) + 1 );
			classesNames.put ( name, ret );
		}

		return ret;
	}

	/****************************************************** datatype */

	public static ArrayList<FromServer> duplicateFromServerArray ( ArrayList<FromServer> array ) {
		FromServer iter;
		ArrayList ret;

		if ( array == null )
			return null;

		ret = new ArrayList<FromServer> ();

		for ( int i = 0; i < array.size (); i++ ) {
			iter = array.get ( i );
			ret.add ( iter.duplicate () );
		}

		return ret;
	}

	public static boolean compareFromServerArray ( ArrayList first, ArrayList second ) {
		int num_first;
		int num_second;
		FromServer comp_first;
		FromServer comp_second;

		if ( first == null && second == null )
			return true;
		else if ( first == null || second == null )
			return false;

		num_first = first.size ();
		num_second = second.size ();

		if ( num_first != num_second )
			return false;

		for ( int i = 0; i < num_first; i++ ) {
			comp_first = ( FromServer ) first.get ( i );
			comp_second = ( FromServer ) second.get ( i );

			if ( comp_first.equals ( comp_second ) == false )
				return false;
		}

		return true;
	}

	public static ArrayList<FromServer> sortArrayByName ( ArrayList<FromServer> array ) {
		ArrayList<FromServer> ret;

		ret = Utils.duplicateFromServerArray ( array );
		Collections.sort ( ret, new Comparator () {
			public int compare ( Object first, Object second ) {
				FromServer tmp;
				String name_first;
				String name_second;

				tmp = ( FromServer ) first;
				name_first = tmp.getString ( "name" );
				tmp = ( FromServer ) second;
				name_second = tmp.getString ( "name" );

				return name_first.compareTo ( name_second );
			}

			public boolean equals ( Object obj ) {
				return false;
			}
		} );

		return ret;
	}

	public static float sumPercentage ( float origin, String percentage ) {
		float perc;

		if ( percentage == null || percentage.length () == 0 )
			return 0;

		if ( percentage.endsWith ( "%" ) ) {
			int symbol;

			symbol = percentage.indexOf ( "%" );
			perc = Float.parseFloat ( percentage.substring ( 0, symbol ) );
			return ( origin * perc ) / 100;
		}
		else {
			perc = Float.parseFloat ( percentage );
			return perc;
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

	public static String randomString () {
		return Math.random () + "-" + Math.random () + "-" + Math.random ();
	}

	public static void triggerChangesCallbacks ( ArrayList callbacks, Widget target ) {
		int num;
		ChangeListener call;

		if ( callbacks == null )
			return;

		num = callbacks.size ();

		for ( int i = 0; i < num; i++ ) {
			call = ( ChangeListener ) callbacks.get ( i );
			call.onChange ( target );
		}
	}

	public static void triggerSaveCallbacks ( ArrayList<SavingDialogCallback> callbacks, SavingDialog target, int mode ) {
		if ( callbacks == null )
			return;

		for ( SavingDialogCallback call : callbacks ) {
			if ( mode == 0 )
				call.onSave ( target );
			else if ( mode == 1 )
				call.onCancel ( target );
		}
	}

	/****************************************************** date */

	public static String[] months = new String[] {
		"Gennaio", "Febbraio", "Marzo",
		"Aprile", "Maggio", "Giugno",
		"Luglio", "Agosto", "Settembre",
		"Ottobre", "Novembre", "Dicembre" };

	public static String [] days = new String [] { "Lunedi", "Martedi", "Mercoledi", "Giovedi", "Venerdi", "Sabato", "Domenica" };

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
		if ( d == null )
			return "Non settato";
		else
			return d.getDate () + " " + months [ d.getMonth () ] + " " + ( d.getYear () + 1900 );
	}

	public static String printableDateShort ( Date d ) {
		if ( d == null )
			return "Non settato";
		else
			return d.getDate () + "/" + ( d.getMonth () + 1 ) + "/" + ( d.getYear () + 1900 );
	}

	public static String printableDate ( Date d, boolean with_year ) {
		String ret;

		if ( d == null ) {
			return "Non settato";
		}
		else {
			ret = d.getDate () + " " + months [ d.getMonth () ];
			if ( with_year == true )
				ret += " " + ( d.getYear () + 1900 );
			return ret;
		}
	}

	public static String printableDateHour ( Date d ) {
		if ( d == null )
			return "Non settato";
		else
			return printableDate ( d ) + " " + d.getHours () + ":" + d.getMinutes () + ":" + d.getSeconds ();
	}

	public static boolean dateIsToday ( Date a ) {
		Date today;

		today = new Date ( System.currentTimeMillis () );
		return ( printableDate ( a, true ) == printableDate ( today, true ) );
	}

	/****************************************************** sessione */

	public static void performLogout () {
		ObjectRequest params;

		params = new ObjectRequest ( "Logout" );

		getServer ().serverGet ( params, new ServerResponse () {
			public void onComplete ( JSONValue response ) {
				Window.Location.reload ();
			}
		} );
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
