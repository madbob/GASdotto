/*  GASdotto
 *  Copyright (C) 2009/2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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
				findAndAlign ( ( OrderUser ) object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 2 );
			}

			protected String debugName () {
				return "OrderUser in OrdersPrivilegedPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUserAggregate", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				findAndAlignAggregate ( ( OrderUserAggregate ) object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndAlignAggregate ( ( OrderUserAggregate ) object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndAlignAggregate ( ( OrderUserAggregate ) object, 2 );
			}

			protected String debugName () {
				return "OrderUserAggregate in OrdersPrivilegedPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Order", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int index;
				int status;
				boolean multi;
				FromServerForm form;
				Order ord;

				if ( object.getBool ( "parent_aggregate" ) == true )
					return;

				if ( object.getRelatedInfo ( "OrdersPrivilegedPanel" ) == null ) {
					ord = ( Order ) object;
					status = object.getInt ( "status" );
					multi = canMultiUser ( object );

					if ( status == Order.OPENED ) {
						index = getSortedPosition ( object );
						form = doOrderRow ( ord, multi );
						insert ( form, index );
					}
					else if ( status == Order.CLOSED ) {
						if ( multi == true ) {
							index = getSortedPosition ( object );
							form = doOrderRow ( ord, true );
							closedOrderAlert ( form, true );
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
				FromServerRappresentation form;
				FromServerForm f;

				if ( object.getBool ( "parent_aggregate" ) == true ) {
					form = ( FromServerRappresentation ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
					if ( form != null ) {
						if ( form instanceof FromServerForm )
							onDestroy ( object );
						else
							form.refreshContents ( object );
					}
				}
				else {
					form = ( FromServerRappresentation ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
					status = object.getInt ( "status" );

					if ( form != null && form instanceof FromServerForm ) {
						f = ( FromServerForm ) form;

						if ( status == Order.OPENED ) {
							closedOrderAlert ( f, false );
							f.emblems ().activate ( "status", status );

							/*
								Il refresh dell'ordine serve sostanzialmente a correggere
								l'intestazione del form qualora vengano cambiate le date
								dell'ordine di riferimento
							*/
							form.refreshContents ( null );

							syncProductsInForm ( form, object );
						}
						else if ( status == Order.CLOSED ) {
							if ( canMultiUser ( object ) == true ) {
								f.emblems ().activate ( "status", status );
								closedOrderAlert ( f, true );
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

								f.invalidate ();
								checkNoAvailableOrders ();
							}
						}
						else {
							f.invalidate ();
							checkNoAvailableOrders ();
						}
					}
					else if ( form != null && form instanceof OrderUserManager ) {
						form.removeFromParent ();
						if ( status == Order.OPENED )
							onReceive ( object );
					}
					else {
						/*
							Questo per gestire il ben raro caso in
							cui un ordine viene ri-aperto oppure
							viene rimosso da un aggregato
						*/
						if ( status == Order.OPENED )
							onReceive ( object );
					}
				}
			}

			public void onDestroy ( FromServer object ) {
				FromServerRappresentation rappr;

				rappr = ( FromServerRappresentation ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
				if ( rappr != null ) {
					rappr.invalidate ();
					checkNoAvailableOrders ();
				}
			}

			protected String debugName () {
				return "Order in OrdersPrivilegedPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderAggregate", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int index;
				int status;
				FromServerForm form;

				form = ( FromServerForm ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
				if ( form == null ) {
					status = object.getInt ( "status" );

					if ( status == Order.OPENED ) {
						/*
							N.B. la funzione doMultiOrderRow() provvede ad eliminare gli
							Order che sono contemplati all'interno dell'aggregato, in
							modo da evitare la doppia citazione, ma per questo motivo la
							funzione getSortedPosition() deve necessariamente essere
							chiamata dopo affinche' i controlli sulla posizione siano
							effettuati sulla lista finali di voci che appaiono nel pannello
						*/
						form = doOrderRow ( object, true );
						index = getSortedPosition ( object );
						insert ( form, index );
					}
					else if ( status == Order.CLOSED ) {
						if ( canMultiUser ( object ) == true ) {
							form = doOrderRow ( object, true );
							index = getSortedPosition ( object );
							closedOrderAlert ( form, true );
							insert ( form, index );
						}
					}
				}
			}

			public void onModify ( FromServer object ) {
				int index;
				int status;
				Order ord;
				FromServerForm form;

				form = ( FromServerForm ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
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

						/*
							TODO	Controllare se questo serve davvero...
						*/
						form.emblems ().activate ( "status", status );

						syncProductsInForm ( form, object );
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
							checkNoAvailableOrders ();
						}
					}
					else {
						form.invalidate ();
						checkNoAvailableOrders ();
					}
				}
				else {
					/*
						Questo per gestire il ben raro caso in cui un
						ordine viene ri-aperto, oppure quando viene
						escluso da un aggregato
					*/
					if ( status == Order.OPENED )
						onReceive ( object );
				}
			}

			public void onDestroy ( FromServer object ) {
				ArrayList children;
				FromServerRappresentation form;
				FromServerRappresentation child;

				form = ( FromServerRappresentation ) object.getRelatedInfo ( "OrdersPrivilegedPanel" );
				if ( form != null ) {
					children = form.getChildren ();

					/*
						Non basta sperare nell'invalidazione in cascata del
						FromServerRappresentation, in quanto i vari elementi qui
						rappresentati hanno come oggetto di riferimento un OrderUser
						assegnato all'ordine anziche' l'ordine stesso dunque invalidate(form)
						non funge e dagli ordini non vengono correttamente rimossi i
						riferimenti al form (forzati programmaticamente alla loro creazione).
						Dunque devo ripassarmi a mano la lista degli ordini e togliere da
						ciascuno il riferimento al form esistente
					*/
					for ( int i = 0; i < children.size (); i++ ) {
						child = ( FromServerRappresentation ) children.get ( i );
						child.getValue ().getObject ( "baseorder" ).delRelatedInfo ( "OrdersPrivilegedPanel" );
					}

					form.invalidate ();
					checkNoAvailableOrders ();
				}
			}

			protected String debugName () {
				return "OrderAggregate in OrdersPrivilegedPanel";
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
			object_2 = iter.getValue ();

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
		ArrayList orders;
		FromServer suborder;
		Supplier supplier;

		if ( order.getType () == "Order" ) {
			supplier = ( Supplier ) order.getObject ( "supplier" );
			return supplier.iAmReference ();
		}
		else {
			orders = order.getArray ( "orders" );

			for ( int i = 0; i < orders.size (); i++ ) {
				suborder = ( FromServer ) orders.get ( i );
				supplier = ( Supplier ) suborder.getObject ( "supplier" );
				if ( supplier.iAmReference () == true )
					return true;
			}

			return false;
		}
	}

	private FromServerForm doOrderRow ( FromServer order, boolean editable ) {
		boolean is_aggregate;
		final FromServerForm ver;
		FromServer uorder;
		OrderUserManager manager;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		is_aggregate = ( order instanceof OrderAggregate );

		if ( is_aggregate == false )
			uorder = new OrderUser ();
		else
			uorder = new OrderUserAggregate ();

		uorder.setObject ( "baseorder", order );
		uorder.setObject ( "baseuser", Session.getUser () );

		ver = new FromServerForm ( uorder );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		manager = new OrderUserManager ( order, true );
		ver.setWrap ( manager );
		ver.add ( manager );

		ver.setCallback ( new FromServerFormCallbacks () {
			/*
				Questo e' perche' in OrderUser.php un ordine senza prodotti viene comunque eliminato,
				tanto vale farlo direttamente da qui e almeno lo cancello pure dalla cache locale
			*/
			private boolean filterEmptyOrders ( FromServer order ) {
				int filtered;
				ArrayList products;
				ArrayList orders;

				if ( order instanceof OrderUser ) {
					products = order.getArray ( "products" );
					if ( products == null || products.size () == 0 ) {
						order.destroy ( null );
						return true;
					}
				}
				else {
					orders = order.getArray ( "orders" );
					filtered = 0;

					for ( int i = 0; i < orders.size (); i++ ) {
						if ( filterEmptyOrders ( ( FromServer ) orders.get ( i ) ) == true )
							filtered++;
					}

					if ( filtered == orders.size () ) {
						order.destroy ( null );
						return true;
					}
				}

				return false;
			}

			public void onOpen ( FromServerRappresentationFull form ) {
				OrderInterface ord;
				FromServerForm f;

				ord = ( OrderInterface ) form.getValue ().getObject ( "baseorder" );
				ord.asyncLoadUsersOrders ();

				f = ( FromServerForm ) form;
				f.forceNextSave ( true );
			}

			public boolean onSave ( FromServerRappresentationFull form ) {
				FromServer obj;

				obj = form.getValue ();
				if ( obj == null )
					return false;

				if ( obj.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					form.setValue ( null );

				filterEmptyOrders ( obj );
				form.setValue ( obj );
				return true;
			}

			public boolean onDelete ( final FromServerRappresentationFull form ) {
				FromServer obj;
				OrderUserManager manager;

				obj = form.getValue ();
				if ( obj == null )
					return false;

				if ( obj.isValid () ) {
					obj.destroy ( new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							FromServerForm f;

							f = ( FromServerForm ) form;
							f.open ( false );
						}
					} );

					if ( obj.getObject ( "baseuser" ).equals ( Session.getUser () ) ) {
						manager = ( OrderUserManager ) form.getWrap ();
						manager.clean ();
					}
					else {
						form.setValue ( null );
					}
				}

				return false;
			}
		} );

		if ( editable == true )
			ver.emblems ().activate ( "multiuser" );

		ver.emblems ().activate ( "status", order.getInt ( "status" ) );

		if ( is_aggregate )
			ver.emblems ().activate ( "aggregate" );

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
		ver.add ( manager );
		ver.setWrap ( manager );
		ver.setValue ( uorder );

		return ver;
	}

	private void findAndAlign ( OrderUser uorder, int action ) {
		int index;
		FromServer order;
		FromServerForm ver;
		FromServerRappresentation form;

		if ( uorder.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
			return;

		order = uorder.getObject ( "baseorder" );
		if ( order.getInt ( "status" ) == Order.SHIPPED || order.getBool ( "parent_aggregate" ) == true )
			return;

		form = ( FromServerRappresentation ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );
		if ( form == null ) {
			index = getSortedPosition ( order );
			ver = doUneditableOrderRow ( order );
			form = ver.getWrap ();
			insert ( ver, index );
		}

		alignOrderRow ( form, uorder, action );
	}

	private void findAndAlignAggregate ( OrderUserAggregate uorder, int action ) {
		int index;
		FromServer order;
		FromServerForm ver;
		FromServerRappresentation form;

		if ( uorder.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
			return;

		order = uorder.getObject ( "baseorder" );
		if ( order.getInt ( "status" ) == Order.SHIPPED )
			return;

		form = ( FromServerRappresentation ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );
		if ( form == null ) {
			index = getSortedPosition ( order );
			ver = doUneditableOrderRow ( OrderAggregate.retrieveAggregate ( order ) );
			form = ( FromServerRappresentation ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );
			insert ( ver, index );
		}

		alignOrderRow ( form, uorder, action );
	}

	private void alignOrderRow ( FromServerRappresentation ver, FromServer uorder, int action ) {
		FromServer order;

		if ( action == 2 ) {
			order = uorder.getObject ( "baseorder" );
			uorder = FromServerFactory.create ( uorder.getType () );
			uorder.setObject ( "baseorder", order );
			uorder.setObject ( "baseuser", Session.getUser () );
		}

		ver.setValue ( uorder );
	}

	private void syncProductsInForm ( FromServerRappresentation form, FromServer order ) {
		OrderUserManager select;

		select = ( OrderUserManager ) form.getWrap ();
		select.upgradeProductsList ( order );
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
					index = iter.getValue ().getObject ( "baseorder" ).getLocalID ();
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
