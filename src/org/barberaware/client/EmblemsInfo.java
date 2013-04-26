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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class EmblemsInfo {
	private class Emblem {
		public int index		= 0;
		public String name		= null;
		public String path		= null;
		public ArrayList paths		= null;
		public String explain		= null;
		public ArrayList explains	= null;

		public Emblem ( int i, String n, String p, String e ) {
			index = i;
			name = n;
			path = p;
			explain = e;
		}

		public Emblem ( int i, String n, ArrayList p, ArrayList e ) {
			index = i;
			name = n;
			paths = p;
			explains = e;
		}
	}

	private HashMap		symbols;

	public EmblemsInfo () {
		symbols = new HashMap ();
	}

	public void addSymbol ( String name, String path, String description ) {
		Emblem em;

		em = new Emblem ( symbols.size (), name, path, description );
		symbols.put ( name, em );
	}

	public void addSymbol ( String name, ArrayList paths, ArrayList descriptions ) {
		Emblem em;

		em = new Emblem ( symbols.size (), name, paths, descriptions );
		symbols.put ( name, em );
	}

	public int numSymbols () {
		return symbols.size ();
	}

	public String getSymbol ( String name, int index ) {
		Emblem m;

		m = ( Emblem ) symbols.get ( name );

		if ( m == null ) {
			Window.alert ( "Emblema " + name + " non definito" );
			return null;
		}
		else {
			if ( m.path != null && index == 0 ) {
				return m.path;
			}
			else {
				if ( m.paths.size () <= index )
					return null;
				else
					return ( String ) m.paths.get ( index );
			}
		}
	}

	public int getIndex ( String name ) {
		Emblem m;

		m = ( Emblem ) symbols.get ( name );

		if ( m == null ) {
			Window.alert ( "Emblema " + name + " non definito" );
			return -1;
		}
		else {
			return m.index;
		}
	}

	private void populateLegend ( FlexTable legend ) {
		String k;
		String path;
		Object [] keys;
		Emblem emblem;

		keys = symbols.keySet ().toArray ();
		if ( keys.length == 0 )
			return;

		for ( int i = 0, row = legend.getRowCount (); i < keys.length; i++ ) {
			k = ( String ) keys [ i ];
			emblem = ( Emblem ) symbols.get ( k );

			if ( emblem.path != null ) {
				legend.setWidget ( row, 0, new Image ( emblem.path ) );
				legend.setWidget ( row, 1, new Label ( emblem.explain ) );
				row++;
			}
			else if ( emblem.paths != null ) {
				for ( int a = 0; a < emblem.paths.size (); a++ ) {
					path = ( String ) emblem.paths.get ( a );

					if ( path != "" ) {
						legend.setWidget ( row, 0, new Image ( path ) );
						legend.setWidget ( row, 1, new Label ( ( String ) emblem.explains.get ( a ) ) );
						row++;
					}
				}
			}
		}
	}

	private HorizontalPanel prepareBox () {
		HorizontalPanel panel;
		Image tab;
		FlexTable legend;

		panel = new HorizontalPanel ();
		panel.setStyleName ( "legend-box" );

		tab = new Image ( "images/legend_closed.png" );
		panel.add ( tab );

		tab.addMouseListener ( new MouseListener () {
			public void onMouseDown ( Widget sender, int x, int y ) {
				HorizontalPanel panel;
				Image tab;
				FlexTable legend;
				Element el;

				tab = ( Image ) sender;
				panel = ( HorizontalPanel ) tab.getParent ();
				el = panel.getElement ();

				if ( DOM.getStyleAttribute ( el, "right" ) != "5px" ) {
					DOM.setStyleAttribute ( el, "right", "5px" );
					tab.setUrl ( "images/legend_opened.png" );
				}
				else {
					legend = ( FlexTable ) panel.getWidget ( 1 );
					/*
						350 e' la larghezza per la classe legend-table nel CSS
					*/
					DOM.setStyleAttribute ( el, "right", "-350px" );
					tab.setUrl ( "images/legend_closed.png" );
				}
			}

			public void onMouseEnter ( Widget sender ) {
				/* dummy */
			}

			public void onMouseLeave ( Widget sender ) {
				/* dummy */
			}

			public void onMouseMove ( Widget sender, int x, int y ) {
				/* dummy */
			}

			public void onMouseUp ( Widget sender, int x, int y ) {
				/* dummy */
			}
		} );

		legend = new FlexTable ();
		legend.setStyleName ( "legend-table" );
		panel.add ( legend );

		return panel;
	}

	public Widget getLegend () {
		HorizontalPanel panel;
		FlexTable legend;

		panel = prepareBox ();
		legend = ( FlexTable ) panel.getWidget ( 1 );
		populateLegend ( legend );
		return panel;
	}

	public Widget getLegend ( EmblemsInfo merge ) {
		HorizontalPanel panel;
		FlexTable legend;

		panel = prepareBox ();
		legend = ( FlexTable ) panel.getWidget ( 1 );
		populateLegend ( legend );
		merge.populateLegend ( legend );
		return panel;
	}
}
