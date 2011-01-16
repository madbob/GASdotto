/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

public class OrdersPrivilegedPanel extends GenericPanel {
	private boolean		hasOrders;
	private GenericPanel	myself;

	public OrdersPrivilegedPanel () {
		super ();

		/*
			Ricordarsi che se c'e' da aggiungere qualcosa in questo pannello va fatto
			prima di checkNoAvailableOrders(), la quale va a piazzare una Label in
			posizione fissa e se l'indice non torna non quaglia piu' quando e' il
			momento di toglierla
		*/

		addTop ( Utils.getEmblemsCache ( "orders" ).getLegend () );

		checkNoAvailableOrders ();

		/**
			TODO	Sarebbe cosa buona usare un FormGroup, ma c'e' da badare al fatto
				che i form per i vari ordini sono di due tipi: editabile e non
		*/

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object );
			}

			public void onModify ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object );
			}

			public void onDestroy ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object );
			}

			protected String debugName () {
				return "OrdersPrivilegedPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int index;
				int status;
				boolean multi;
				FromServerForm form;
				Order ord;

				ord = ( Order ) object;
				form = ( FromServerForm ) ord.getRelatedInfo ( "OrdersPrivilegedPanel" );

				if ( form == null ) {
					status = ord.getInt ( "status" );
					multi = canMultiUser ( object );

					if ( status == Order.OPENED ) {
						index = getSortedPosition ( object );
						form = doOrderRow ( ord, multi );
						insert ( form, index );
					}
					else if ( status == Order.CLOSED ) {
						if ( multi == true ) {
							form = doOrderRow ( ord, multi );
							closedOrderAlert ( form, true );
							index = getSortedPosition ( object );
							insert ( form, index );
						}

						/*
							Se l'ordine e' chiuso e l'utente corrente non e' abilitato a
							ritoccare le altrui quantita', il pannello puo' comunque
							essere aggiunto all'arrivo del relativo OrderUser in
							findAndAlign()
						*/
					}
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				int status;
				Order ord;
				FromServerForm form;

				ord = ( Order ) object;

				form = ( FromServerForm ) ord.getRelatedInfo ( "OrdersPrivilegedPanel" );
				status = object.getInt ( "status" );

				if ( form != null ) {
					if ( status == Order.OPENED ) {
						closedOrderAlert ( form, false );

						/*
							Il refresh dell'ordine serve sostanzialmente a correggere
							l'intestazione del form qualora vengano cambiate le date
							dell'ordine di riferimento
						*/
						form.refreshContents ( null );

						form.emblems ().activate ( "status", status );
						syncProductsInForm ( form, ord );
					}
					else if ( status == Order.CLOSED ) {
						if ( canMultiUser ( object ) == true ) {
							form.emblems ().activate ( "status", status );
							closedOrderAlert ( form, true );
						}
						else {
							/**
								TODO	Questo non e' corretto: se l'ordine viene
									chiuso repentinamente, e sono un utente
									normale, non dovrebbe sparire dal pannello ma
									diventare non editabile. Considerando che la
									situazione e' ben rara al momento lo lascio
									cosi', ma sarebbe da correggere prima o dopo
							*/
							form.invalidate ();
							ord.delRelatedInfo ( "OrdersPrivilegedPanel" );
							checkNoAvailableOrders ();
						}
					}
					else {
						form.invalidate ();
						ord.delRelatedInfo ( "OrdersPrivilegedPanel" );
						checkNoAvailableOrders ();
					}
				}
				else {
					/*
						Questo per gestire il ben raro caso in cui un
						ordine viene ri-aperto
					*/
					if ( status == Order.OPENED )
						onReceive ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerForm form;

				form = ( FromServerForm ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
				if ( form != null ) {
					form.invalidate ();
					object.delRelatedInfo ( "OrdersPrivilegedPanel" );
					checkNoAvailableOrders ();
				}
			}

			protected String debugName () {
				return "OrdersPrivilegedPanel";
			}
		} );

		myself = this;
	}

	private void closedOrderAlert ( FromServerForm form, boolean doit ) {
		InfoCell alert;

		if ( doit == true ) {
			alert = new InfoCell ( "Quest'ordine è stato chiuso, ma puoi comunque modificare le quantità ordinate dagli utenti." );
			form.setExtraWidget ( "alert", alert );
			form.insert ( alert, 0 );
		}
		else {
			form.removeWidget ( "alert" );
		}
	}

	private int getSortedPosition ( FromServer object ) {
		int i;
		int cmp;
		int num;
		Date tdate;
		FromServer object_2;
		FromServerForm iter;

		if ( hasOrders == false )
			return 1;

		num = getWidgetCount ();
		if ( object == null )
			return num;

		tdate = object.getDate ( "enddate" );

		for ( i = 1; i < num; i++ ) {
			iter = ( FromServerForm ) getWidget ( i );
			object_2 = iter.getObject ();

			if ( object_2 != null ) {
				cmp = ( object_2.getObject ( "baseorder" ).getDate ( "enddate" ).compareTo ( tdate ) );
				if ( cmp > 0 )
					break;
			}
		}

		return i;
	}

	private void checkNoAvailableOrders () {
		if ( getWidgetCount () == 1 ) {
			hasOrders = false;
			insert ( new Label ( "Non ci sono ordini aperti" ), 1 );
		}
	}

	private boolean canMultiUser ( FromServer order ) {
		Supplier supplier;

		supplier = ( Supplier ) order.getObject ( "supplier" );
		return supplier.iAmReference ();
	}

	private FromServerForm doOrderRow ( Order order, boolean editable ) {
		final FromServerForm ver;
		HorizontalPanel pan;
		OrderUser uorder;
		OrderUserManager manager;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );
		uorder.setObject ( "baseuser", Session.getUser () );

		ver = new FromServerForm ( uorder );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerForm form ) {
				Order ord;

				ord = ( Order ) form.getObject ().getObject ( "baseorder" );
				ord.asyncLoadUsersOrders ();

				form.forceNextSave ( true );
			}

			public boolean onSave ( FromServerForm form ) {
				FromServer obj;
				OrderUserManager manager;

				manager = ( OrderUserManager ) form.retriveInternalWidget ( "informations" );

				obj = manager.getValue ();
				if ( obj == null )
					return false;

				form.setObject ( obj );
				return true;
			}

			public boolean onDelete ( final FromServerForm form ) {
				FromServer obj;
				OrderUserManager manager;

				manager = ( OrderUserManager ) form.retriveInternalWidget ( "informations" );

				obj = manager.getValue ();
				if ( obj == null )
					return false;

				if ( obj.isValid () ) {
					obj.destroy ( new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							form.open ( false );
						}
					} );

					manager.setValue ( null );
				}

				return false;
			}
		} );

		if ( editable == true )
			ver.emblems ().activate ( "multiuser" );

		manager = new OrderUserManager ( order, true );
		ver.setExtraWidget ( "informations", manager );
		ver.add ( manager );

		manager.setValue ( uorder );

		ver.emblems ().activate ( "status", order.getInt ( "status" ) );
		order.addRelatedInfo ( "OrdersPrivilegedPanel", ver );
		return ver;
	}

	private FromServerForm doUneditableOrderRow ( FromServer order ) {
		FromServerForm ver;
		OrderUser uorder;
		OrderUserManager manager;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );
		uorder.setObject ( "baseuser", Session.getUser () );

		ver = new FromServerForm ( uorder, FromServerForm.NOT_EDITABLE );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		manager = new OrderUserManager ( order, false );
		ver.setExtraWidget ( "informations", manager );
		ver.add ( manager );

		manager.setValue ( uorder );

		return ver;
	}

	/*
		action: gli stessi valori di syncLocalCache
	*/
	private void findAndAlign ( OrderUser uorder ) {
		int index;
		FromServer order;
		FromServerForm form;

		if ( uorder.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
			return;

		order = uorder.getObject ( "baseorder" );
		if ( order.getInt ( "status" ) == Order.SHIPPED )
			return;

		form = ( FromServerForm ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );
		if ( form == null ) {
			index = getSortedPosition ( order );
			form = doUneditableOrderRow ( order );
			insert ( form, index );
		}

		alignOrderRow ( form, uorder );
	}

	private void alignOrderRow ( FromServerForm ver, OrderUser uorder ) {
		OrderUserManager manager;

		manager = ( OrderUserManager ) ver.retriveInternalWidget ( "informations" );
		manager.setValue ( uorder );

		ver.setObject ( uorder );
	}

	private void syncProductsInForm ( FromServerForm form, Order order ) {
		OrderUserManager select;

		select = ( OrderUserManager ) form.retriveInternalWidget ( "informations" );
		select.upgradeProductsList ( order.getArray ( "products" ) );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Ordini";
	}

	public String getSystemID () {
		return "orders";
	}

	public Image getIcon () {
		return new Image ( "images/path_orders.png" );
	}

	public String getCurrentInternalReference () {
		int index;
		FromServerForm iter;

		index = -1;

		if ( hasOrders == true ) {
			for ( int i = 1; i < getWidgetCount (); i++ ) {
				iter = ( FromServerForm ) getWidget ( i );
				if ( iter.isOpen () == true ) {
					index = iter.getObject ().getObject ( "baseorder" ).getLocalID ();
					break;
				}
			}
		}

		return Integer.toString ( index );
	}

	public void initView () {
		ObjectRequest params;

		params = new ObjectRequest ( "Order" );
		params.add ( "status", Order.OPENED );
		Utils.getServer ().testObjectReceive ( params );
	}
}
