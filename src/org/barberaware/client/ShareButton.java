/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class ShareButton extends PushButton implements ObjectWidget {
	private FromServer		currentObject;
	private boolean			opened;

	private DialogBox		dialog;
	private FromServerTable		localPrivileges;

	public ShareButton () {
		super ( new Image ( "images/share.png" ) );

		addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				showDialog ();
			}
		} );

		localPrivileges = null;
		opened = false;
	}

	private void showDialog () {
		dialog = new DialogBox ( false );
		dialog.setText ( "Condividi" );
		dialog.setWidget ( doDialog () );

		opened = true;
		dialog.center ();
		dialog.show ();
	}

	private Panel doDialog () {
		VerticalPanel ret;
		HorizontalPanel box;
		CaptionPanel frame;
		CellPanel main;
		Button but;

		ret = new VerticalPanel ();
		box = null;

		if ( Session.getSystemConf ().getBool ( "has_multigas" ) ) {
			box = new HorizontalPanel ();
			ret.add ( box );

			localPrivileges = new FromServerTable ();
			localPrivileges.addColumn ( "GAS", "gas", false );			
			localPrivileges.addColumn ( "Permessi", "privileges", new WidgetFactoryCallback () {
				public Widget create () {
					CyclicToggle ret;

					ret = new CyclicToggle ( currentObject.sharingStatus () == ACL.ACL_OWNER );
					ret.addState ( "images/share_owner.png" );
					ret.addState ( "images/share_readwrite.png" );
					ret.addState ( "images/share_readonly.png" );
					ret.addState ( "images/share_none.png" );
					ret.setDefaultSelection ( 3 );

					return ret;
				}
			} );

			frame = new CaptionPanel ( "GAS Locali" );
			frame.add ( localPrivileges );
			box.add ( frame );
			box.setCellWidth ( frame, "50%" );

			checkRemoteACL ();

			main = box;
		}
		else {
			localPrivileges = null;
			main = ret;
		}

		frame = new CaptionPanel ( "Esporta File" );
		frame.add ( exportContents () );
		main.add ( frame );
		if ( box != null )
			box.setCellWidth ( frame, "50%" );

		box = new HorizontalPanel ();
		box.setStyleName ( "dialog-buttons" );
		box.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		ret.add ( box );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( localPrivileges != null )
					localPrivileges.saveChanges ();

				closeDialog ();
			}
		} );
		box.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				closeDialog ();
			}
		} );
		box.add ( but );

		return ret;
	}

	private void checkRemoteACL () {
		ObjectRequest params;

		Utils.getServer ().onObjectEvent ( "ACL", new ServerObjectReceive () {
			private boolean checkReq ( FromServer obj ) {
				return ( currentObject != null &&
						obj.getString ( "target_type" ) == currentObject.getType () &&
						obj.getInt ( "target_id" ) == currentObject.getLocalID () );
			}

			public void onReceive ( FromServer obj ) {
				if ( checkReq ( obj ) && localPrivileges != null ) {
					localPrivileges.addElement ( obj );

					if ( opened == true )
						dialog.center ();
				}
			}

			public void onModify ( FromServer obj ) {
				if ( checkReq ( obj ) && localPrivileges != null )
					localPrivileges.refreshElement ( obj );
			}

			public void onDestroy ( FromServer obj ) {
				if ( checkReq ( obj ) && localPrivileges != null )
					localPrivileges.removeElement ( obj );
			}

			public String handleId () {
				return "ShareButton";
			}
		} );

		params = new ObjectRequest ( "ACL" );
		params.add ( "target_type", currentObject.getType () );
		params.add ( "target_id", currentObject.getLocalID () );
		Utils.getServer ().testObjectReceive ( params );
	}

	private Widget exportContents () {
		VerticalPanel ret;
		HTML content;

		ret = new VerticalPanel ();
		ret.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );

		content = new HTML ( "<p>Clicca il link sotto per ottenere un file rappresentante questo elemento.</p>" +
					"<p>Tale file potrà essere importato in qualsiasi applicativo che implementa il formato GDXP.</p>" +
					"<p>(<a href=\"http://trac.gasdotto.net/wiki/GDXP\" target=\"_blank\">Clicca qui</a> per maggiori informazioni)</p>" );

		content.addStyleName ( "small-text" );
		ret.add ( content );

		content = Utils.getServer ().fileLink ( "Esporta", "", "exporter.php?type=" + currentObject.getType () + "&id=" + currentObject.getLocalID () );
		content.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				closeDialog ();
			}
		} );
		content.addStyleName ( "top-spaced" );

		ret.add ( content );

		return ret;
	}

	private void closeDialog () {
		Utils.getServer ().removeObjectEvent ( "ACL", "ShareButton" );
		opened = false;
		dialog.hide ();
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer object ) {
		currentObject = object;
	}

	public FromServer getValue () {
		return currentObject;
	}
}
