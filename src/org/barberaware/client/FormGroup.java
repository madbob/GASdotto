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
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public abstract class FormGroup extends Composite {
	private VerticalPanel			main;
	private boolean				addable;

	public FormGroup ( String icon_path ) {
		main = new VerticalPanel ();
		main.setStyleName ( "form-cluster" );
		initWidget ( main );

		addable = false;

		if ( icon_path != null ) {
			main.add ( doAddButton ( icon_path ) );
			addable = true;
		}
	}

	private void closeOtherForms ( FromServerForm target ) {
		int tot;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( int i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );

			if ( iter != target ) {
				if ( iter.isOpen () ) {
					/*
						Tendenzialmente qui si potrebbe assumere che un
						solo Form e' aperto in ogni momento, e se si
						trova quello precedentemente "open" basta
						chiuderlo ed uscire dal ciclo, ma per sicurezza
						me li passo tutti
					*/
					iter.open ( false );
				}
			}
		}
	}

	private Panel doAddButton ( String icon_path ) {
		PushButton button;
		HorizontalPanel pan;

		pan = new HorizontalPanel ();
		pan.setStyleName ( "bottom-buttons" );

		button = new PushButton ( new Image ( icon_path ), new ClickListener () {
			public void onClick ( Widget sender ) {
				FromServerForm new_form;

				new_form = doNewEditableRow ();

				new_form.setCallback ( new FromServerFormCallbacks () {
					public void onOpen ( FromServerForm form ) {
						closeOtherForms ( form );
					}
				} );

				new_form.open ( true );
				main.insert ( new_form, latestIterableIndex () );
			}
		} );

		pan.add ( button );
		return pan;
	}

	public FromServerForm retrieveFormById ( int id ) {
		int i;
		int tot;
		FromServerForm iter;
		FromServer cmp;

		iter = null;
		tot = latestIterableIndex ();

		for ( i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );
			cmp = iter.getObject ();

			if ( cmp != null && cmp.getLocalID () == id )
				break;
		}

		if ( i == tot )
			iter = null;

		return iter;
	}

	public FromServerForm retrieveForm ( FromServer obj ) {
		return retrieveFormById ( obj.getLocalID () );
	}

	/*
		Attenzione: questo e' per ottenere il form ad una specifica posizione, per avere
		il form che descrive un oggetto con un dato ID usare retrieveFormById()
	*/
	public FromServerForm retrieveForm ( int index ) {
		return ( FromServerForm ) main.getWidget ( index );
	}

	public int latestIterableIndex () {
		return main.getWidgetCount () - ( addable ? 1 : 0 );
	}

	private int getPosition ( FromServer object ) {
		int i;
		FromServer object_2;
		FromServerForm iter;

		for ( i = 0; i < latestIterableIndex (); i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );
			object_2 = iter.getObject ();

			if ( object_2 != null && sorting ( object, object_2 ) > 0 )
				break;
		}

		return i;
	}

	/*
		Questa funzione ritorna:

		0 - se il form per l'oggetto da aggiungere esiste gia' (solitamente nel caso in
			cui e' stato salvato un elemento elaborato appunto all'interno della
			lista, il cui form e' stato costruito con doNewEditableRow() )
		1 - se il nuovo form per il nuovo elemento e' stato aggiunto
		2 - se non e' possibile creare il nuovo form per il nuovo elemento, cioe' se
			doEditableRow() torna null
	*/
	public int addElement ( FromServer object ) {
		int ret;
		int pos;
		FromServerForm iter;

		/*
			E' possibile che questa funzione venga chiamata su un elemento che gia'
			e' nella lista, ad esempio quando viene eseguita la callback onReceive
			(triggerata anche da oggetti creati per mezzo di un form gia' messo qui).
			Occorre dunque fare un controllino...
		*/

		iter = retrieveForm ( object );

		if ( iter == null ) {
			iter = doEditableRow ( object );

			if ( iter != null ) {
				iter.setCallback ( new FromServerFormCallbacks () {
					public void onOpen ( FromServerForm form ) {
						closeOtherForms ( form );
					}
				} );

				pos = getPosition ( object );
				main.insert ( iter, pos );

				ret = 1;
			}
			else
				ret = 2;
		}
		else
			ret = 0;

		return ret;
	}

	public FromServerForm refreshElement ( FromServer object ) {
		FromServerForm iter;

		iter = retrieveForm ( object );
		if ( iter != null )
			iter.refreshContents ( object );

		return iter;
	}

	public void deleteElement ( FromServer object ) {
		int i;
		int tot;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );

			if ( iter.equals ( object ) == true ) {
				main.remove ( i );
				break;
			}
		}
	}

	public int getCurrentlyOpened () {
		FromServer obj;
		FromServerForm iter;

		for ( int i = 0; i < latestIterableIndex (); i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );
			if ( iter.isOpen () == true ) {
				obj = iter.getObject ();

				if ( obj != null )
					return obj.getLocalID ();
				else
					break;
			}
		}

		return -1;
	}

	public ArrayList collectContents () {
		int i;
		int tot;
		FromServerForm iter;
		FromServer cmp;
		ArrayList array;

		array = new ArrayList ();
		iter = null;
		tot = latestIterableIndex ();

		for ( i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );

			cmp = iter.getObject ();
			if ( cmp != null )
				array.add ( cmp );
		}

		return array;
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare l'ordinamento
		all'interno della lista di forms
	*/
	protected int sorting ( FromServer first, FromServer second ) {
		return 0;
	}

	protected abstract FromServerForm doNewEditableRow ();
	protected abstract FromServerForm doEditableRow ( FromServer obj );
}