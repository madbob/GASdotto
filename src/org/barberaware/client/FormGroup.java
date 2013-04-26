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
import java.lang.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class FormGroup extends Composite {
	private VerticalPanel			main			= null;
	private String				identifier		= "";
	private Panel				addButtons		= null;
	private boolean				addable			= false;
	private ArrayList			aggregates		= null;

	public FormGroup ( String adding_text ) {
		main = new VerticalPanel ();
		main.setStyleName ( "form-cluster" );
		initWidget ( main );

		addable = false;
		identifier = Math.random () + "-" + Math.random () + "-" + Math.random () + "-" + Math.random () + "-" + Math.random ();

		if ( adding_text != null ) {
			addButtons = doAddButtonsBar ();
			main.add ( addButtons );
			doAddButton ( adding_text );
			addable = true;
		}
	}

	public void extraAddButton ( AddButton button ) {
		if ( addButtons == null )
			return;

		addButtons.add ( button );
	}

	public FromServerRappresentation retrieveFormById ( int id ) {
		int tot;
		FromServer cmp;
		FromServerRappresentation iter;
		FromServerRappresentation ret;

		iter = null;
		ret = null;
		tot = latestIterableIndex ();

		for ( int i = firstIterableIndex (); ret == null && i < tot; i++ ) {
			iter = ( FromServerRappresentation ) main.getWidget ( i );
			cmp = iter.getValue ();

			if ( cmp != null ) {
				if ( cmp.getLocalID () == id ) {
					ret = iter;
					break;
				}
			}
		}

		if ( ret == null )
			ret = retrieveIntoAggregates ( id );

		return ret;
	}

	public FromServerRappresentation retrieveForm ( FromServer obj ) {
		FromServerRappresentation ret;

		ret = ( FromServerRappresentation ) obj.getRelatedInfo ( identifier );
		if ( ret == null )
			ret = retrieveIntoAggregates ( obj.getLocalID () );

		return ret;
	}

	/*
		Attenzione: questo e' per ottenere il form ad una specifica posizione, per avere
		il form che descrive un oggetto con un dato ID usare retrieveFormById()
	*/
	public FromServerForm retrieveForm ( int index ) {
		return ( FromServerForm ) main.getWidget ( index );
	}

	public int firstIterableIndex () {
		return ( addable ? 1 : 0 );
	}

	public int latestIterableIndex () {
		return main.getWidgetCount ();
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

		pos = getPosition ( object, 0, false );

		if ( pos != -1 ) {
			iter = doEditableRow ( object );

			if ( iter != null ) {
				iter.setCallback ( new FromServerFormCallbacks () {
					public void onOpen ( FromServerRappresentationFull form ) {
						FromServerForm f;

						f = ( FromServerForm ) form;
						closeOtherForms ( f );
						asyncLoad ( f );
					}

					public void onSaved ( FromServerRappresentationFull form ) {
						int pos;

						pos = getPosition ( form.getValue (), 1, true );
						if ( pos != main.getWidgetIndex ( form ) )
							main.insert ( form, pos );
					}
				} );

				main.insert ( iter, pos );
				object.addRelatedInfo ( identifier, iter );

				ret = 1;

				if ( iter.getValue () instanceof FromServerAggregate ) {
					if ( aggregates == null )
						aggregates = new ArrayList ();
					aggregates.add ( iter );
				}
			}
			else {
				ret = 2;
			}
		}
		else {
			ret = 0;
		}

		return ret;
	}

	public void reSort ( FromServer object ) {
		int pos;
		FromServerRappresentation iter;

		iter = retrieveForm ( object );
		if ( iter != null ) {
			pos = getPosition ( object, 0, false );
			if ( pos != -1 )
				main.insert ( iter, pos );
		}
	}

	public FromServerRappresentation refreshElement ( FromServer object ) {
		FromServerRappresentation iter;

		iter = retrieveForm ( object );
		if ( iter != null )
			iter.refreshContents ( object );

		return iter;
	}

	public void deleteElement ( FromServer object ) {
		FromServer parent;
		FromServerRappresentation iter;

		iter = retrieveForm ( object );
		if ( iter != null ) {
			iter.removeFromParent ();

			/*
				Per qualche oscuro motivo, se provo ad eliminare un FromServerForm dal FormGroup non
				succede niente. Neanche usando remove() con l'indice del form (che comunque viene
				correttamente restituito da getWidgetIndex()). Pertanto qui provvedo nel caso ad
				invalidarlo, per renderlo invisibile: soluzione incompleta, ma l'unico hack che son
				riuscito ad escogitare alle 3:00 AM di un venerdi sera
			*/
			iter.invalidate ();

			if ( aggregates != null )
				aggregates.remove ( iter );

			object.delRelatedInfo ( identifier );
		}
	}

	public int getCurrentlyOpened () {
		int tot;
		FromServer obj;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( int i = firstIterableIndex (); i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );
			if ( iter.isOpen () == true ) {
				obj = iter.getValue ();

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

		for ( i = firstIterableIndex (); i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );

			cmp = iter.getValue ();
			if ( cmp != null )
				array.add ( cmp );
		}

		return array;
	}

	public ArrayList collectForms () {
		int i;
		int tot;
		FromServerForm iter;
		FromServer cmp;
		ArrayList array;

		array = new ArrayList ();
		iter = null;
		tot = latestIterableIndex ();

		for ( i = firstIterableIndex (); i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );

			cmp = iter.getValue ();
			if ( cmp != null )
				array.add ( iter );
		}

		return array;
	}

	public int getElementsNum () {
		int tot;
		int num;
		FromServerForm iter;

		num = 0;
		tot = latestIterableIndex ();

		for ( int i = firstIterableIndex (); i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );
			if ( iter.getValue () != null )
				num++;
		}

		return num;
	}

	public String getIdentifier () {
		return identifier;
	}

	private void closeOtherForms ( FromServerForm target ) {
		int tot;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( int i = firstIterableIndex (); i < tot; i++ ) {
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

	private Panel doAddButtonsBar () {
		HorizontalPanel pan;

		pan = new HorizontalPanel ();
		pan.setStyleName ( "bottom-buttons" );
		return pan;
	}

	private void doAddButton ( String adding_text ) {
		AddButton button;

		button = new AddButton ( adding_text, new ClickListener () {
			public void onClick ( Widget sender ) {
				FromServerForm new_form;

				new_form = doNewEditableRow ();
				if ( new_form == null )
					return;

				new_form.setCallback ( new FromServerFormCallbacks () {
					public void onOpen ( FromServerRappresentationFull form ) {
						closeOtherForms ( ( FromServerForm ) form );
					}

					public void onSaved ( FromServerRappresentationFull form ) {
						int pos;

						pos = getPosition ( form.getValue (), 1, true );
						main.insert ( form, pos );
					}
				} );

				new_form.getValue ().addRelatedInfo ( identifier, new_form );
				new_form.open ( true );
				main.insert ( new_form, firstIterableIndex () );
			}
		} );

		addButtons.add ( button );
	}

	private int getPosition ( FromServer object, int from, boolean ignore_existance ) {
		int i;
		int tot;
		FromServer object_2;
		FromServerForm iter;

		tot = latestIterableIndex ();

		for ( i = firstIterableIndex () + from; i < tot; i++ ) {
			iter = ( FromServerForm ) main.getWidget ( i );
			object_2 = iter.getValue ();

			if ( object_2 != null && sorting ( object, object_2 ) >= 0 ) {
				if ( ignore_existance == false ) {
					if ( object.getLocalID () == object_2.getLocalID () )
						i = -1;
				}

				break;
			}
		}

		return i;
	}

	private FromServerRappresentation retrieveIntoAggregates ( int id ) {
		FromServerAggregate aggr_obj;
		FromServerRappresentation aggr_form;
		FromServerRappresentation ret;

		ret = null;

		if ( aggregates != null ) {
			for ( int i = 0; i < aggregates.size (); i++ ) {
				aggr_form = ( FromServerRappresentation ) aggregates.get ( i );
				ret = aggr_form.getChild ( id );
				if ( ret != null )
					break;

				aggr_obj = ( FromServerAggregate ) aggr_form.getValue ();
				if ( aggr_obj.hasObject ( id ) ) {
					ret = aggr_form;
					break;
				}
			}
		}

		return ret;
	}

	/*
		Questa funzione puo' essere sovrascritta per personalizzare l'ordinamento
		all'interno della lista di forms
	*/
	protected int sorting ( FromServer first, FromServer second ) {
		String a;
		String b;

		if ( first == null )
			return 1;
		else if ( second == null )
			return -1;

		a = first.getString ( "name" );
		b = second.getString ( "name" );
		if ( a == null || b == null )
			return 0;

		return -1 * ( a.compareTo ( b ) );
	}

	/*
		Questa funzione puo' essere sovrascritta per compiere qualche azione quando uno
		specifico form viene aperto: particolarmente indicata per caricare
		asincronicamente dati solo quando richiesto
	*/
	protected void asyncLoad ( FromServerForm form ) {
		/* dummy */
	}

	protected abstract FromServerForm doNewEditableRow ();
	protected abstract FromServerForm doEditableRow ( FromServer obj );
}
