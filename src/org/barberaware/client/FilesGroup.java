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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class FilesGroup extends Composite implements FromServerArray {
	private FormGroup		main;

	public FilesGroup () {
		main = new FormGroup ( "Nuovo File" ) {
			protected FromServerForm doEditableRow ( FromServer n ) {
				FlexTable fields;
				FromServerForm form;
				FileUploadDialog uploader;

				form = new FromServerForm ( n );

				fields = new FlexTable ();
				form.add ( fields );

				fields.setWidget ( 0, 0, new Label ( "Nome" ) );
				fields.setWidget ( 0, 1, form.getWidget ( "name" ) );

				fields.setWidget ( 1, 0, new Label ( "File" ) );
				uploader = new FileUploadDialog ();
				fields.setWidget ( 1, 1, form.getPersonalizedWidget ( "server_path", uploader ) );

				return form;
			}

			protected FromServerForm doNewEditableRow () {
				return doEditableRow ( new CustomFile () );
			}
		};

		initWidget ( main );
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		if ( element == null )
			return;
		main.addElement ( element );
	}

	public void setElements ( ArrayList elements ) {
		int tot;

		if ( elements == null )
			return;

		tot = elements.size ();
		for ( int i = 0; i < tot; i++ )
			main.addElement ( ( FromServer ) elements.get ( i ) );
	}

	public void removeElement ( FromServer element ) {
		if ( element == null )
			return;
		main.deleteElement ( element );
	}

	public ArrayList getElements () {
		return main.collectContents ();
	}

	public void refreshElement ( FromServer element ) {
		main.refreshElement ( element );
	}
}
