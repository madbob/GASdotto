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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

/**
	TODO	Spezzare questa classe in due parti, quella che si occupa della formattazione dei
		FromServerForm e quella che gestisce direttamente l'hook sul server
*/

public abstract class FormCluster extends VerticalPanel {
	private boolean				automatic;
	private boolean				addable;

	private void buildCommon ( String type, String icon_path, boolean auto ) {
		automatic = auto;
		addable = false;

		Utils.getServer ().onObjectEvent ( type, new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				if ( automatic == true ) {
					int add;

					add = addElement ( object );
					if ( add == 2 )
						return;

					customNew ( object, add == 1 );
				}
			}

			public void onModify ( FromServer object ) {
				FromServerForm iter;

				iter = refreshElement ( object );
				customModify ( iter );
			}

			public void onDestroy ( FromServer object ) {
				deleteElement ( object );
			}
		} );

		if ( icon_path != null ) {
			add ( doAddButton ( icon_path ) );
			addable = true;
		}

		setStyleName ( "form-cluster" );
	}

	public FormCluster ( String type, String icon_path, boolean auto ) {
		buildCommon ( type, icon_path, auto );
	}

	public FormCluster ( String type, String icon_path ) {
		buildCommon ( type, icon_path, true );
	}

	private void closeOtherForms ( FromServerForm target ) {
		int tot;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( int i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) getWidget ( i );

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
				insert ( new_form, latestIterableIndex () );
			}
		} );

		pan.add ( button );
		return pan;
	}

	public FromServerForm retrieveForm ( FromServer obj ) {
		int i;
		int id;
		int tot;
		FromServerForm iter;
		FromServer cmp;

		iter = null;
		id = obj.getLocalID ();
		tot = latestIterableIndex ();

		for ( i = 0; i < tot; i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			cmp = iter.getObject ();

			if ( cmp != null && cmp.getLocalID () == id )
				break;
		}

		if ( i == tot )
			iter = null;

		return iter;
	}

	public int latestIterableIndex () {
		return getWidgetCount () - ( addable ? 1 : 0 );
	}

	private int getPosition ( FromServer object ) {
		int i;
		FromServerForm iter;

		for ( i = 0; i < latestIterableIndex (); i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			if ( sorting ( object, iter.getObject () ) > 0 )
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
				insert ( iter, pos );

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
			iter = ( FromServerForm ) getWidget ( i );

			if ( iter.equals ( object ) == true ) {
				remove ( i );
				break;
			}
		}
	}

	public int getCurrentlyOpened () {
		FromServerForm iter;

		for ( int i = 0; i < latestIterableIndex (); i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			if ( iter.isOpen () == true )
				return iter.getObject ().getLocalID ();
		}

		return -1;
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare l'ordinamento
		all'interno della lista di forms
	*/
	protected int sorting ( FromServer first, FromServer second ) {
		return 0;
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento in
		caso di modifica di uno degli elementi della lista
	*/
	protected void customModify ( FromServerForm form ) {
		/* dummy */
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare il comportamento sul
		salvataggio di uno degli elementi della lista. Il parametro true_new e' true
		quando si tratta di un nuovo oggetto proveniente dal server, false se e' stato
		salvato un oggetto editato appunto all'interno del FormCluster e di cui magari si
		vuole cambiare il contenuto del form costruito con doNewEditableRow()
	*/
	protected void customNew ( FromServer object, boolean true_new ) {
		/* dummy */
	}

	protected abstract FromServerForm doNewEditableRow ();
	protected abstract FromServerForm doEditableRow ( FromServer obj );
}
