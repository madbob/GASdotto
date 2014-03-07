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

import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class DownloadButton extends HTML implements StringWidget {
	private String			url;

	public DownloadButton () {
		url = null;
		setHTML ( "Nessun File da Scaricare" );
	}

	public void setValue ( String title, String value ) {
		url = value;

		if ( url == null || url == "" )
			setHTML ( "Nessun File da Scaricare" );
		else
			setHTML ( "<a href=\"" + Utils.getServer ().getURL () + "downloader.php?path=" + url + "\">" + title + "</a>" );
	}

	public void setValue ( String value ) {
		setValue ( "Scarica File", value );
	}

	public String getValue () {
		return url;
	}
}
