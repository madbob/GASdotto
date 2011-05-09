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
	private FromServerRappresentation	parent;
	private FromServerRappresentation	wrap;

	public FromServerRappresentation () {
		widgets = new HashMap ();
		children = new ArrayList ();
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

	public FromServerRappresentation removeChild ( FromServer obj ) {
		FromServerRappresentation child;

		for ( int i = 0; i < children.size (); i++ ) {
			child = ( FromServerRappresentation ) children.get ( i );
			if ( child.getValue ().equals ( obj ) ) {
				children.remove ( child );
				child.parent = null;
				return child;
			}
		}

		return null;
	}

	public FromServerRappresentation getChild ( FromServer obj ) {
		FromServerRappresentation child;

		for ( int i = 0; i < children.size (); i++ ) {
			child = ( FromServerRappresentation ) children.get ( i );
			if ( child.getValue ().equals ( obj ) )
				return child;
		}

		return null;
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
		removeFromParent ();
	}

	public FromServerRappresentation getParent () {
		return parent;
	}

	public void refreshContents ( FromServer obj ) {
		Object [] wids;
		FromServerWidget iter;
		FromServerRappresentation child;

		if ( obj == null )
			obj = getValue ();

		if ( object != null && obj.getType () == object.getType () ) {
			wids = widgets.values ().toArray ();

			for ( int i = 0; i < wids.length; i++ ) {
				iter = ( FromServerWidget ) wids [ i ];
				iter.set ( obj );
			}
		}
		else {
			for ( int i = 0; i < children.size (); i++ ) {
				child = ( FromServerRappresentation ) children.get ( i );
				if ( child.getValue ().equals ( obj ) ) {
					child.refreshContents ( obj );
					break;
				}
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
		FromServerWidget tmp;
		FromServerRappresentation child;

		if ( object != null ) {
			wids = widgets.values ().toArray ();

			for ( int i = 0; i < wids.length; i++ ) {
				tmp = ( FromServerWidget ) wids [ i ];
				if ( tmp.assign ( object ) == false )
					return false;
			}
		}

		if ( children.size () != 0 ) {
			subchildren = new ArrayList ();

			for ( int i = 0; i < children.size (); i++ ) {
				child = ( FromServerRappresentation ) children.get ( i );
				if ( child.rebuildObject () == false )
					return false;

				subchildren.add ( child.getValue () );
			}

			if ( object instanceof FromServerAggregate )
				( ( FromServerAggregate ) object ).setObjects ( subchildren );
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
