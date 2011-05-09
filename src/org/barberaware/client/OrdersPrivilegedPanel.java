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
				findAndAlign ( ( OrderUser ) object, 0 );
			}

			public void onModify ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 1 );
			}

			public void onDestroy ( FromServer object ) {
				findAndAlign ( ( OrderUser ) object, 2 );
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
					ord = ( Order ) object;

					form = ( FromServerRappresentation ) ord.getRelatedInfo ( "OrdersPrivilegedPanel" );
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

							syncProductsInForm ( form, ord );
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
				return "OrdersPrivilegedPanel";
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
						index = getSortedPosition ( object );
						form = doMultiOrderRow ( object );
						insert ( form, index );
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

						form.emblems ().activate ( "status", status );
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
		Supplier supplier;

		supplier = ( Supplier ) order.getObject ( "supplier" );
		return supplier.iAmReference ();
	}

	private FromServerForm doOrderRow ( Order order, boolean editable ) {
		final FromServerForm ver;
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

		manager = new OrderUserManager ( order, true );
		ver.setWrap ( manager );
		ver.add ( manager );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerForm form ) {
				Order ord;

				ord = ( Order ) form.getValue ().getObject ( "baseorder" );
				ord.asyncLoadUsersOrders ();

				form.forceNextSave ( true );
			}

			public boolean onSave ( FromServerForm form ) {
				FromServer obj;
				ArrayList products;

				obj = form.getValue ();
				if ( obj == null )
					return false;

				if ( obj.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
					form.setValue ( null );

				/*
					Questo e' perche' in OrderUser.php un ordine senza prodotti viene comunque
					eliminato, tanto vale farlo direttamente da qui e almeno lo cancello pure
					dalla cache locale
				*/
				products = obj.getArray ( "products" );
				if ( products == null || products.size () == 0 )
					obj.destroy ( null );

				form.setValue ( obj );
				return true;
			}

			public boolean onDelete ( final FromServerForm form ) {
				FromServer obj;
				OrderUserManager manager;

				obj = form.getValue ();
				if ( obj == null )
					return false;

				if ( obj.isValid () ) {
					obj.destroy ( new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							form.open ( false );
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
		order.addRelatedInfo ( "OrdersPrivilegedPanel", ver );
		return ver;
	}

	private FromServerForm doMultiOrderRow ( FromServer aggregate ) {
		boolean multi;
		ArrayList orders;
		final FromServerForm ver;
		FromServerRappresentation rappr;
		FromServerForm form;
		Order order;
		OrderUserAggregate uorder;
		OrderUser sub_uorder;
		OrderUserManager manager;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 1 );
		}

		uorder = new OrderUserAggregate ();
		uorder.setObject ( "baseorder", aggregate );
		uorder.setObject ( "baseuser", Session.getUser () );

		ver = new FromServerForm ( uorder );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerForm form ) {
				ArrayList orders;
				Order order;

				orders = form.getValue ().getObject ( "baseorder" ).getArray ( "orders" );

				for ( int i = 0; i < orders.size (); i++ ) {
					order = ( Order ) orders.get ( i );
					order.asyncLoadUsersOrders ();
				}

				form.forceNextSave ( true );
			}

			public boolean onSave ( FromServerForm form ) {
				ArrayList children;
				FromServer obj;
				ArrayList products;
				OrderUserManager manager;

				children = form.getChildren ();

				for ( int i = 0; i < children.size (); i++ ) {
					manager = ( OrderUserManager ) children.get ( i );

					obj = manager.getValue ();
					if ( obj == null )
						continue;

					if ( obj.getObject ( "baseuser" ).equals ( Session.getUser () ) == false )
						manager.setValue ( null );

					/*
						Questo e' perche' in OrderUser.php un ordine senza prodotti viene comunque
						eliminato, tanto vale farlo direttamente da qui e almeno lo cancello pure
						dalla cache locale
					*/
					products = obj.getArray ( "products" );
					if ( products == null || products.size () == 0 )
						obj.destroy ( null );
					else
						obj.save ( null );
				}

				form.open ( false );
				return false;
			}

			public boolean onDelete ( final FromServerForm form ) {
				ArrayList children;
				FromServer obj;
				OrderUserManager manager;

				children = form.getChildren ();

				for ( int i = 0; i < children.size (); i++ ) {
					manager = ( OrderUserManager ) children.get ( i );

					obj = manager.getValue ();
					if ( obj == null )
						return false;

					if ( obj.isValid () ) {
						obj.destroy ( null );

						if ( obj.getObject ( "baseuser" ).equals ( Session.getUser () ) )
							manager.clean ();
						else
							manager.setValue ( null );
					}
				}

				return false;
			}
		} );

		orders = aggregate.getArray ( "orders" );
		multi = false;

		for ( int i = 0; i < orders.size (); i++ ) {
			order = ( Order ) orders.get ( i );

			rappr = ( FromServerRappresentation ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );
			if ( rappr != null ) {
				rappr.invalidate ();
				order.delRelatedInfo ( "OrdersPrivilegedPanel" );
			}

			sub_uorder = new OrderUser ();
			sub_uorder.setObject ( "baseorder", order );
			sub_uorder.setObject ( "baseuser", Session.getUser () );

			if ( canMultiUser ( order ) == true )
				multi = true;

			manager = new OrderUserManager ( order, true );
			ver.add ( manager );
			ver.addChild ( manager );

			manager.setValue ( sub_uorder );
			order.addRelatedInfo ( "OrdersPrivilegedPanel", manager );
		}

		if ( multi == true )
			ver.emblems ().activate ( "multiuser" );

		ver.emblems ().activate ( "status", aggregate.getInt ( "status" ) );
		ver.emblems ().activate ( "aggregate" );

		aggregate.addRelatedInfo ( "OrdersPrivilegedPanel", ver );
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
		ver.setWrap ( new OrderUserManager ( order, false ) );
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
		if ( order.getInt ( "status" ) == Order.SHIPPED )
			return;

		form = ( FromServerRappresentation ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );
		if ( form == null ) {
			index = getSortedPosition ( order );
			ver = doUneditableOrderRow ( order );
			insert ( ver, index );
			form = ver.getWrap ();
		}

		alignOrderRow ( form, uorder, action );
	}

	private void alignOrderRow ( FromServerRappresentation ver, OrderUser uorder, int action ) {
		FromServer order;
		OrderUserManager manager;

		if ( action == 2 ) {
			order = uorder.getObject ( "baseorder" );
			uorder = new OrderUser ();
			uorder.setObject ( "baseorder", order );
			uorder.setObject ( "baseuser", Session.getUser () );
		}

		ver.setValue ( uorder );
	}

	private void syncProductsInForm ( FromServerRappresentation form, Order order ) {
		OrderUserManager select;

		select = ( OrderUserManager ) form.getWrap ();
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
