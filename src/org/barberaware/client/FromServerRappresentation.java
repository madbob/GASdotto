/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.json.client.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class FromServerRappresentation extends Composite implements ObjectWidget {
	private FromServer					object;
	private HashMap<String, FromServerWidget>		widgets;
	private ArrayList<FromServerRappresentation>		children;
	private ArrayList<FromServerRappresentationCallbacks>	callbacks;
	private FromServerRappresentation			parent;
	private FromServerRappresentation			wrap;
	private ArrayList<ObjectWidget>				peers;

	public FromServerRappresentation () {
		widgets = new HashMap<String, FromServerWidget> ();
		children = new ArrayList<FromServerRappresentation> ();
		callbacks = new ArrayList<FromServerRappresentationCallbacks> ();
		parent = null;
		wrap = null;
	}

	/*
		Per recuperare il wrapper interno del widget assegnato ad un attributo
	*/
	protected FromServerWidget retriveWidgetFromList ( String attribute ) {
		return widgets.get ( attribute );
	}

	public Widget getWidget ( String attribute ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );
		if ( tmp == null ) {
			tmp = new FromServerWidget ( object, attribute );
			widgets.put ( attribute, tmp );
		}

		return tmp;
	}

	/*
		Questo e' per assegnare un widget non di default ad un campo specifico
		dell'oggetto
	*/
	public Widget getPersonalizedWidget ( String attribute, Widget widget ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );
		if ( tmp == null ) {
			tmp = new FromServerWidget ( object, attribute, widget );
			widgets.put ( attribute, tmp );
		}

		return tmp;
	}

	public Widget retriveInternalWidget ( String attribute ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );

		if ( tmp != null )
			return tmp.wid;
		else
			return null;
	}

	public void setValidation ( String attribute, FromServerValidateCallback callback ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( attribute );
		if ( tmp != null )
			tmp.validation = callback;
	}

	/*
		Usato per aggiungere nuovi widgets destinati alla formattazione, che non
		rientrano nella rappresentazione dell'oggetto. Sono recuperabili per mezzo di
		getWidget(), ma vengono saltati nella fase di ricostruzione dell'oggetto
		FromServer
	*/
	public void setExtraWidget ( String name, Widget extra ) {
		widgets.put ( name, new FromServerWidget ( name, extra ) );
	}

	public FromServerWidget removeWidget ( String name ) {
		FromServerWidget tmp;

		tmp = retriveWidgetFromList ( name );
		if ( tmp != null )
			widgets.remove ( name );

		return tmp;
	}

	/*
		Il "wrap" puo' essere solo uno, e' un widget contenuto nel
		widget corrente e ne prende sostanzialmente il posto. Quando da
		this viene chiesto indietro il valore (con getValue()) torna il
		valore del widget wrappato. Usato prevalentemente per motivi di
		layout e formattazione.

		I "peers" possono essere molti, si suppone che non modifichino
		l'oggetto, gli viene assegnato quando qualcosa viene assegnato a
		this ma non gli viene mai chiesto indietro.
		Usato prevalentemente per funzionalita' avanzate
	*/

	public void setWrap ( FromServerRappresentation wrapped ) {
		wrapped.setValue ( getValue () );
		wrap = wrapped;
	}

	public FromServerRappresentation getWrap () {
		if ( wrap == null )
			return this;
		else
			return wrap;
	}

	public void unwrap () {
		wrap = null;
	}

	public void addPeer ( ObjectWidget peer ) {
		if ( peers == null )
			peers = new ArrayList<ObjectWidget> ();

		peers.add ( peer );
		peer.setValue ( getValue () );
	}

	private void assignToPeers ( FromServer object ) {
		if ( peers != null ) {
			for ( ObjectWidget tmp : peers )
				tmp.setValue ( object );
		}
	}

	public void addChild ( FromServerRappresentation child ) {
		children.add ( child );
		child.parent = this;
	}

	public FromServerRappresentation removeChild ( FromServerRappresentation child ) {
		children.remove ( child );
		child.parent = null;
		return child;
	}

	public FromServerRappresentation removeChild ( FromServer obj ) {
		FromServerRappresentation child;

		child = getChild ( obj );
		if ( child != null )
			removeChild ( child );

		return child;
	}

	public FromServerRappresentation getChild ( int id ) {
		for ( FromServerRappresentation child : children )
			if ( child.getValue ().getLocalID () == id )
				return child;

		return null;
	}

	public FromServerRappresentation getChild ( FromServer obj ) {
		return getChild ( obj.getLocalID () );
	}

	public ArrayList<FromServerRappresentation> getChildren () {
		return children;
	}

	public void invalidate () {
		if ( object != null )
			object.delRelatedInfo ( this );

		if ( children != null ) {
			for ( FromServerRappresentation child : children )
				child.invalidate ();

			children = null;
		}

		setValue ( null );
		setVisible ( false );

		if ( parent != null )
			parent.invalidateChild ( this );

		removeFromParent ();
	}

	/*
		Non chiamare questa funzione getParent(), altrimenti si va a
		sovrascrivere una funzione interna di GWT!
	*/
	public FromServerRappresentation getRappresentationParent () {
		return parent;
	}

	private boolean refreshChild ( FromServer obj ) {
		FromServerRappresentation child;

		child = getChild ( obj );
		if ( child != null )
			child.refreshContents ( obj );

		return ( child != null );
	}

	private void refreshSubContents ( FromServerAggregate obj ) {
		boolean found;
		ArrayList objects;
		FromServer subobj;

		objects = obj.getObjects ();

		for ( int i = 0; i < objects.size (); i++ ) {
			subobj = ( FromServer ) objects.get ( i );
			if ( refreshChild ( subobj ) == false )
				addChildCallback ( subobj );
		}

		if ( children.size () != objects.size () ) {
			for ( FromServerRappresentation child : children ) {
				found = false;

				for ( int i = 0; i < objects.size (); i++ ) {
					subobj = ( FromServer ) objects.get ( i );

					if ( child.getValue ().equals ( subobj ) ) {
						found = true;
						break;					
					}
				}

				if ( found == false )
					delChildCallback ( child );
			}
		}
	}

	public void refreshContents ( FromServer obj ) {
		Object [] wids;
		FromServerWidget iter;

		if ( obj == null )
			obj = getValue ();

		if ( object != null && obj.getType () == object.getType () ) {
			wids = widgets.values ().toArray ();

			for ( int i = 0; i < wids.length; i++ ) {
				iter = ( FromServerWidget ) wids [ i ];
				iter.set ( obj );
			}

			if ( obj instanceof FromServerAggregate )
				refreshSubContents ( ( FromServerAggregate ) obj );
		}
		else {
			refreshChild ( obj );
		}
	}

	public void setCallback ( FromServerRappresentationCallbacks routine ) {
		callbacks.add ( routine );
	}

	public void removeCallback ( String id ) {
		for ( FromServerRappresentationCallbacks call : callbacks ) {
			if ( call.getID () == id ) {
				callbacks.remove ( call );
				break;
			}
		}
	}

	protected boolean contentsChanged () {
		Object [] wids;
		boolean ret;
		FromServerWidget tmp;

		ret = false;

		if ( object != null ) {
			wids = widgets.values ().toArray ();

			for ( int i = 0; i < wids.length; i++ ) {
				tmp = ( FromServerWidget ) wids [ i ];

				if ( tmp.compare ( object ) == false ) {
					ret = true;
					break;
				}
			}
		}

		if ( ret == false ) {
			for ( FromServerRappresentation child : children ) {
				if ( child.contentsChanged () == true ) {
					ret = true;
					break;
				}
			}
		}

		return ret;
	}

	protected boolean rebuildObject () {
		Object [] wids;
		ArrayList<FromServer> subchildren;
		FromServer c;
		FromServerAggregate myself_aggregate;
		FromServerWidget tmp;

		if ( wrap != null ) {
			return wrap.rebuildObject ();
		}
		else {
			myself_aggregate = null;

			if ( object != null ) {
				wids = widgets.values ().toArray ();

				for ( int i = 0; i < wids.length; i++ ) {
					tmp = ( FromServerWidget ) wids [ i ];
					if ( tmp.assign ( object ) == false )
						return false;
				}

				if ( object instanceof FromServerAggregate )
					myself_aggregate = ( FromServerAggregate ) object;
			}

			if ( children.size () != 0 ) {
				subchildren = new ArrayList<FromServer> ();

				for ( FromServerRappresentation child : children ) {
					if ( child.rebuildObject () == false )
						return false;

					c = child.getValue ();
					if ( c != null && ( myself_aggregate == null || myself_aggregate.validateNewChild ( c ) == true ) )
						subchildren.add ( c );
				}

				if ( myself_aggregate != null )
					myself_aggregate.setObjects ( subchildren );
			}
		}

		return true;
	}

	protected void resetObject () {
		Object [] wids;
		FromServerWidget tmp;

		if ( object != null ) {
			wids = widgets.values ().toArray ();

			for ( int i = 0; i < wids.length; i++ ) {
				tmp = ( FromServerWidget ) wids [ i ];
				tmp.set ( object );
			}
		}

		for ( FromServerRappresentation child : children )
			child.resetObject ();
	}

	private void invalidateChild ( FromServerRappresentation child ) {
		if ( children.remove ( child ) == true )
			if ( children.size () == 0 )
				this.invalidate ();
	}

	private void addChildCallback ( FromServer obj ) {
		FromServerRappresentation child;

		child = null;

		for ( FromServerRappresentationCallbacks call : callbacks ) {
			child = call.onAddChild ( this, obj );
			if ( child != null )
				break;
		}

		if ( child != null )
			addChild ( child );
	}

	private void delChildCallback ( FromServerRappresentation child ) {
		boolean remove;

		remove = true;

		for ( FromServerRappresentationCallbacks call : callbacks )
			remove = ( call.onRemoveChild ( child ) && remove );

		if ( remove == true )
			removeChild ( child );
	}

	/****************************************************************** ObjectWidget */

	public void setValue ( FromServer obj ) {
		if ( wrap != null )
			wrap.setValue ( obj );

		object = obj;

		if ( obj != null && widgets.isEmpty () == false )
			refreshContents ( obj );

		assignToPeers ( obj );
	}

	public FromServer getValue () {
		if ( wrap != null )
			return wrap.getValue ();
		else
			return object;
	}
}
