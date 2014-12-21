/*  GASdotto
 *  Copyright (C) 2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public abstract class BankMovementComponent extends FromServerRappresentation {
	protected int			defaultMethod		= 0;
	protected float			defaultAmount		= 0;
    	protected float			forceAmount		= 0;
	protected Date			defaultDate		= null;
	protected FromServer		defaultTargetUser	= null;
	protected FromServer		defaultTargetSupplier	= null;
	protected boolean		defaultCro		= true;
	protected int			defaultType		= -1;
	protected String		defaultNote		= "";

	protected boolean		editable		= true;
	protected boolean		justDate		= false;

	protected FromServer		originalValue		= null;
	private Date			originalDate		= null;
	private float			originalAmount		= 0;

	public void setDefaultDate ( Date date ) {
		defaultDate = date;
	}

	public void setDefaultMethod ( int method ) {
		defaultMethod = method;
	}

	public void setDefaultAmount ( float amount ) {
		defaultAmount = amount;
	}

    	public void forceDefaultAmount ( float amount ) {
		forceAmount = amount;
	}

	public float getDefaultAmount () {
		return defaultAmount;
	}

	public void setDefaultTargetUser ( FromServer target ) {
		defaultTargetUser = target;
	}

	public void setDefaultTargetSupplier ( FromServer target ) {
		defaultTargetSupplier = target;
	}

	public void setDefaultType ( int type ) {
		defaultType = type;
	}

	public void setDefaultNote ( String note ) {
		defaultNote = note;
	}

	public void showCro ( boolean show ) {
		defaultCro = show;
	}

	public void showJustDate ( boolean just ) {
		justDate = just;
	}

	public void setEditable ( boolean edit ) {
		editable = edit;
	}

	public void transferBankAttributesTo ( BankMovementComponent destination ) {
		destination.setEditable ( editable );
		destination.showJustDate ( justDate );
		destination.setDefaultMethod ( defaultMethod );
		destination.setDefaultDate ( defaultDate );
		destination.setDefaultAmount ( defaultAmount );
		destination.forceDefaultAmount ( forceAmount );
		destination.setDefaultTargetUser ( defaultTargetUser );
		destination.setDefaultTargetSupplier ( defaultTargetSupplier );
		destination.setDefaultNote ( defaultNote );
		destination.showCro ( defaultCro );
	}

	protected void saveOriginal () {
		originalValue = getValue ();

		if ( originalValue != null ) {
			originalDate = originalValue.getDate ( "date" );
			originalAmount = originalValue.getFloat ( "amount" );
		}
	}

	protected void restoreOriginal () {
		originalValue.setDate ( "date", originalDate );
		originalValue.setFloat ( "amount", originalAmount );
		setValue ( originalValue );
	}

	protected void newOriginal () {
		originalValue = getValue ();
		originalValue.forceMod ( true );
	}

	/****************************************************************** FromServerRappresentation */

	public FromServer getValue () {
		FromServer ret;

		rebuildObject ();

		ret = super.getValue ();
		if ( ret != null && defaultType != -1 )
			ret.setInt ( "movementtype", defaultType );

		return ret;
	}
}

