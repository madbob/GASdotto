/*  GASdotto
 *  Copyright (C) 2008/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	private FromServer			object;
	private HashMap				widgets;
	private ArrayList			children;
	private ArrayList			callbacks;
	private FromServerRappresentation	parent;
	private FromServerRappresentation	wrap;

	public FromServerRappresentation () {
		widgets = new HashMap ();
		children = new ArrayList ();
		callbacks = new ArrayList ();
		parent = null;
		wrap = null;
	}

	protected FromServerWidget retriveWidgetFromList ( String attribute ) {
		return ( FromServerWidget ) widgets.get ( attribute );
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

	public void setWrap ( FromServerRappresentation wrapped ) {
		wrap = wrapped;
		wrap.setValue ( getValue () );
	}

	public FromServerRappresentation getWrap () {
		if ( wrap == null )
			return this;
		else
			return wrap;
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
		FromServerRappresentation child;

		for ( int i = 0; i < children.size (); i++ ) {
			child = ( FromServerRappresentation ) children.get ( i );
			if ( child.getValue ().getLocalID () == id )
				return child;
		}

		return null;
	}

	public FromServerRappresentation getChild ( FromServer obj ) {
		return getChild ( obj.getLocalID () );
	}

	public ArrayList getChildren () {
		return children;
	}

	public void invalidate () {
		FromServerRappresentation child;

		if ( object != null )
			object.delRelatedInfo ( this );

		if ( children != null ) {
			for ( int i = 0; i < children.size (); i++ ) {
				child = ( FromServerRappresentation ) children.get ( i );
				child.invalidate ();
			}

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
		FromServerRappresentation child;

		objects = obj.getObjects ();

		for ( int i = 0; i < objects.size (); i++ ) {
			subobj = ( FromServer ) objects.get ( i );
			if ( refreshChild ( subobj ) == false )
				addChildCallback ( subobj );
		}

		if ( children.size () != objects.size () ) {
			for ( int a = 0; a < children.size (); a++ ) {
				child = ( FromServerRappresentation ) children.get ( a );
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
		FromServerFormCallbacks call;

		for ( int i = 0; i < callbacks.size (); i++ ) {
			call = ( FromServerFormCallbacks ) callbacks.get ( i );
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
		FromServerRappresentation child;

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
			for ( int i = 0; i < children.size (); i++ ) {
				child = ( FromServerRappresentation ) children.get ( i );
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
		ArrayList subchildren;
		FromServer c;
		FromServerAggregate myself_aggregate;
		FromServerWidget tmp;
		FromServerRappresentation child;

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
			subchildren = new ArrayList ();

			for ( int i = 0; i < children.size (); i++ ) {
				child = ( FromServerRappresentation ) children.get ( i );
				if ( child.rebuildObject () == false )
					return false;

				c = child.getValue ();
				if ( c != null && ( myself_aggregate == null || myself_aggregate.validateNewChild ( c ) == true ) )
					subchildren.add ( c );
			}

			if ( myself_aggregate != null )
				myself_aggregate.setObjects ( subchildren );
		}

		return true;
	}

	protected void resetObject () {
		Object [] wids;
		FromServerWidget tmp;
		FromServerRappresentation child;

		if ( object != null ) {
			wids = widgets.values ().toArray ();

			for ( int i = 0; i < wids.length; i++ ) {
				tmp = ( FromServerWidget ) wids [ i ];
				tmp.set ( object );
			}
		}

		for ( int i = 0; i < children.size (); i++ ) {
			child = ( FromServerRappresentation ) children.get ( i );
			child.resetObject ();
		}
	}

	private void invalidateChild ( FromServerRappresentation child ) {
		if ( children.remove ( child ) == true )
			if ( children.size () == 0 )
				this.invalidate ();
	}

	private void addChildCallback ( FromServer obj ) {
		FromServerRappresentation child;
		FromServerRappresentationCallbacks call;

		child = null;

		for ( int i = 0; i < callbacks.size (); i++ ) {
			call = ( FromServerRappresentationCallbacks ) callbacks.get ( i );
			child = call.onAddChild ( this, obj );
			if ( child != null )
				break;
		}

		if ( child != null )
			addChild ( child );
	}

	private void delChildCallback ( FromServerRappresentation child ) {
		boolean remove;
		FromServerRappresentationCallbacks call;

		remove = true;

		for ( int i = 0; i < callbacks.size (); i++ ) {
			call = ( FromServerRappresentationCallbacks ) callbacks.get ( i );
			remove = ( call.onRemoveChild ( child ) && remove );
		}

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
	}

	public FromServer getValue () {
		if ( wrap != null )
			return wrap.getValue ();
		else
			return object;
	}
}
