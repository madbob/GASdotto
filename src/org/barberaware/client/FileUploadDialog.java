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
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class FileUploadDialog extends Composite implements StringWidget {
	private VerticalPanel			main;

	private Button				button;
	private DownloadButton			link;
	private FormPanel			form;
	private DialogBox			dialog;
	private FileUpload			upload;
	private Image				image;

	private boolean				opened;
	private String				customEmptyString;
	private String				completeFile;

	public FileUploadDialog () {
		HorizontalPanel top;

		opened = false;
		customEmptyString = "Nessun file selezionato";
		completeFile = "";
		image = null;

		dialog = new DialogBox ( false );
		dialog.setText ( "Seleziona File" );
		dialog.setWidget ( doDialog () );

		main = new VerticalPanel ();
		main.setSpacing ( 5 );

		top = new HorizontalPanel ();
		main.add ( top );

		button = new Button ();
		button.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				if ( opened == false ) {
					opened = true;
					dialog.center ();
					dialog.show ();
				}
			}
		} );
		top.add ( button );

		link = new DownloadButton ();
		top.add ( link );

		initWidget ( main );
		showFileName ();
	}

	public void setDestination ( String destination ) {
		form.setAction ( Utils.getServer ().getURL () + destination );
	}

	public void isImageUpload ( boolean do_image ) {
		if ( do_image == true ) {
			image = new Image ();
			main.add ( image );
			setDestination ( "upload_image.php" );

			showFileName ();

			addDomHandler ( new ChangeHandler () {
				public void onChange ( ChangeEvent event ) {
					image.setVisible ( true );
					image.setUrl ( Utils.getServer ().getDomain () + getValue () );
				}
			}, ChangeEvent.getType () );
		}
		else {
			/* dummy */
		}
	}

	private void showFileName () {
		String [] path;
		String p;

		if ( completeFile == null || completeFile.equals ( "" ) ) {
			button.setText ( customEmptyString );
			link.setVisible ( false );

			if ( image != null )
				image.setVisible ( false );
		}
		else {
			path = completeFile.split ( "/" );
			p = path [ path.length - 1 ];
			button.setText ( p );

			link.setVisible ( true );
			link.setValue ( completeFile );

			if ( image != null ) {
				image.setVisible ( true );
				image.setUrl ( Utils.getServer ().getDomain () + "uploads/" + p );
			}
		}
	}

	private void callCallbacks () {
		DomEvent.fireNativeEvent ( Document.get ().createChangeEvent (), this );
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

		but = new Button ( "Salva", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				opened = false;
				form.submit ();
			}
		} );
		buttons.add ( but );

		but = new Button ( "Annulla", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
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
				JSONValue jsonObject;

				Utils.getServer ().loadingAlert ( false );
				str = event.getResults ();
				dialog.hide ();

				try {
					jsonObject = JSONParser.parseStrict ( str );
					if ( manageUploadResponse ( jsonObject ) == false )
						return;
				}
				catch ( Exception e ) {
					/* dummy */
				}

				completeFile = str;
				showFileName ();
				callCallbacks ();
			}
		} );

		return form;
	}

	protected void setEmptyString ( String empty ) {
		customEmptyString = empty;
		showFileName ();
	}

	/*
		Questa funzione e' scorporata per permettere alle sottoclassi di
		sovrascriverla
	*/
	protected boolean manageUploadResponse ( JSONValue response ) {
		JSONString check;

		check = response.isString ();

		if ( check != null && check.stringValue ().startsWith ( "Errore" ) ) {
			Utils.showNotification ( check.stringValue () );
			return false;
		}
		else {
			return true;
		}
	}

	/****************************************************************** StringWidget */

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
