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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;

public class FileUploadDialog extends Composite implements StringWidget {
	private Button			main;

	private DialogBox		dialog;
	private FileUpload		upload;

	private boolean			opened;
	private boolean			set;

	public FileUploadDialog () {
		opened = false;
		set = false;

		dialog = new DialogBox ( false );
		dialog.setText ( "Seleziona File" );
		dialog.setWidget ( doDialog () );

		main = new Button ( "Nessun file selezionato" );
		main.addClickListener ( new ClickListener () {
			public void onClick ( Widget sender ) {
				if ( opened == false ) {
					opened = true;
					dialog.center ();
					dialog.show ();
				}
			}
		} );

		initWidget ( main );
	}

	private Panel doDialog () {
		final FormPanel form;
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
				Utils.getServer ().loadingAlert ( false );
				main.setText ( event.getResults () );
				set = true;
				dialog.hide ();
			}
		} );

		return form;
	}

	public void setValue ( String value ) {
		if ( value != null && value.length () != 0 ) {
			main.setText ( value );
			set = true;
		}
		else {
			main.setText ( "Nessun file selezionato" );
			set = false;
		}
	}

	public String getValue () {
		if ( set == true )
			return main.getText ();
		else
			return "";
	}
}
