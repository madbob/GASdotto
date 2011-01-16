/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class FileUploadDialog extends Composite implements StringWidget, SourcesChangeEvents {
	private HorizontalPanel			main;

	private Button				button;
	private DownloadButton			link;
	private FormPanel			form;
	private DialogBox			dialog;
	private FileUpload			upload;

	private ChangeListenerCollection	changeCallbacks;

	private boolean				opened;
	private String				completeFile;

	public FileUploadDialog () {
		opened = false;
		completeFile = "";
		changeCallbacks = null;

		dialog = new DialogBox ( false );
		dialog.setText ( "Seleziona File" );
		dialog.setWidget ( doDialog () );

		main = new HorizontalPanel ();
		main.setSpacing ( 5 );

		button = new Button ();
		button.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					dialog.center ();
					dialog.show ();
				}
			}
		} );
		main.add ( button );

		link = new DownloadButton ();
		main.add ( link );

		initWidget ( main );
		showFileName ();
	}

	private void showFileName () {
		String [] path;

		if ( completeFile == null || completeFile.equals ( "" ) ) {
			button.setText ( "Nessun file selezionato" );
			link.setVisible ( false );
		}
		else {
			path = completeFile.split ( "/" );
			button.setText ( path [ path.length - 1 ] );

			link.setVisible ( true );
			link.setValue ( completeFile );
		}
	}

	private void callCallbacks () {
		if ( changeCallbacks != null )
			changeCallbacks.fireChange ( this );
	}

	private Panel doDialog () {
		VerticalPanel pan;
		HorizontalPanel buttons;
		Button but;

		form = new FormPanel ();
		form.setAction ( Utils.getServer ().getURL () + "upload_file.php" );
		form.setEncoding ( FormPanel.ENCODING_MULTIPART );
		form.setMethod ( FormPanel.METHOD_POST );

		pan = new VerticalPanel ();
		form.setWidget ( pan );

		upload = new FileUpload ();
		upload.setName ( "uploadedfile" );
		pan.add ( upload );

		buttons = new HorizontalPanel ();
		buttons.setStyleName ( "dialog-buttons" );
		buttons.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
		pan.add ( buttons );

		but = new Button ( "Salva", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				form.submit ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				opened = false;
				dialog.hide ();
			}
		} );
		buttons.add ( but );

		form.addFormHandler ( new FormHandler () {
			public void onSubmit ( FormSubmitEvent event ) {
				Utils.getServer ().loadingAlert ( true );
			}

			public void onSubmitComplete ( FormSubmitCompleteEvent event ) {
				String str;
				JSONString ret;
				JSONValue jsonObject;

				Utils.getServer ().loadingAlert ( false );
				str = event.getResults ();

				try {
					jsonObject = JSONParser.parse ( str );
					ret = jsonObject.isString ();

					if ( ret != null && ret.stringValue ().startsWith ( "Errore" ) ) {
						Utils.showNotification ( ret.stringValue () );
						return;
					}
				}
				catch ( Exception e ) {
					/* dummy */
				}

				completeFile = str;
				showFileName ();
				dialog.hide ();
				callCallbacks ();
			}
		} );

		return form;
	}

	public void setDestination ( String destination ) {
		form.setAction ( Utils.getServer ().getURL () + destination );
	}

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks == null )
			changeCallbacks = new ChangeListenerCollection ();
		changeCallbacks.add ( listener );
	}

	public void removeChangeListener ( ChangeListener listener ) {
		if ( changeCallbacks != null )
			changeCallbacks.remove ( listener );
	}

	public void setValue ( String value ) {
		if ( value == null )
			value = "";

		completeFile = value;
		showFileName ();
	}

	public String getValue () {
		return completeFile;
	}
}
