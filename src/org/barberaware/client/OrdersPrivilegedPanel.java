/*  GASdotto
 *  Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
		doFilterOptions ();

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

				ord = ( Order ) object;
				form = ( FromServerForm ) ord.getRelatedInfo ( "OrdersPrivilegedPanel" );

				if ( form == null ) {
					status = ord.getInt ( "status" );

					if ( status == Order.OPENED ) {
						index = getSortedPosition ( object );
						multi = canMultiUser ( object );
						form = doOrderRow ( ord, multi, false );
						insert ( form, index );
						alignOrdersInCache ( ord, multi );
					}
					else if ( status == Order.CLOSED || status == Order.SHIPPED ) {
						multi = canMultiUser ( object );

						if ( multi == true ) {
							form = doOrderRow ( ord, multi, true );
							closedOrderAlert ( form, true );
							index = getSortedPosition ( object );
							insert ( form, index );

							if ( status == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false )
								form.setVisible ( false );
						}

						alignOrdersInCache ( ord, multi );

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
					if ( status == Order.OPENED ) {
						onReceive ( object );
						alignOrdersInCache ( ord, canMultiUser ( object ) );
					}
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

	private void doFilterOptions () {
		OrdersHubWidget filter;

		filter = new OrdersHubWidget () {
			public void doFilter ( boolean show, Date start, Date end ) {
				int num;
				FromServerForm form;
				FromServer ord;

				if ( hasOrders == false )
					return;

				num = myself.getWidgetCount ();

				for ( int i = 2; i < num; i++ ) {
					form = ( FromServerForm ) myself.getWidget ( i );
					ord = form.getObject ().getObject ( "baseorder" );

					if ( show == true ) {
						if ( ord.getInt ( "status" ) == Order.SHIPPED ) {
							if ( ord.getDate ( "startdate" ).after ( start ) && ord.getDate ( "enddate" ).before ( end ) )
								form.setVisible ( true );
							else
								form.setVisible ( false );
						}
					}
					else {
						if ( ord.getInt ( "status" ) == Order.SHIPPED )
							form.setVisible ( false );
					}
				}
			}
		};

		addTop ( filter );
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

	private void alignOrdersInCache ( Order order, boolean multi ) {
		int num;
		ArrayList uorders;
		User myself;
		OrderUser uorder;

		uorders = Utils.getServer ().getObjectsFromCache ( "OrderUser" );
		num = uorders.size ();

		if ( multi == true ) {
			for ( int i = 0; i < num; i++ ) {
				uorder = ( OrderUser ) uorders.get ( i );
				if ( order.equals ( uorder.getObject ( "baseorder" ) ) )
					findAndAlign ( uorder, 0 );
			}
		}
		else {
			myself = Session.getUser ();

			for ( int i = 0; i < num; i++ ) {
				uorder = ( OrderUser ) uorders.get ( i );

				/*
					Forse questo doppio controllo e' inutile considerando che findAndAlign() gia'
					verifica che l'ordine sia dell'utente corrente, ma la prudenza non e' mai
					troppa
				*/
				if ( order.equals ( uorder.getObject ( "baseorder" ) ) && myself.equals ( uorder.getObject ( "baseuser" ) ) )
					findAndAlign ( uorder, 0 );
			}
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
			return 2;

		num = getWidgetCount ();
		if ( object == null )
			return num;

		tdate = object.getDate ( "enddate" );

		for ( i = 2; i < num; i++ ) {
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
		if ( getWidgetCount () == 2 ) {
			hasOrders = false;
			insert ( new Label ( "Non ci sono ordini aperti" ), 2 );
		}
	}

	private boolean canMultiUser ( FromServer order ) {
		Supplier supplier;

		supplier = ( Supplier ) order.getObject ( "supplier" );
		return supplier.iAmReference ();
	}

	private FromServerForm doOrderRow ( Order order, boolean editable, boolean freeedit ) {
		final FromServerForm ver;
		HorizontalPanel pan;
		OrderUser uorder;
		UserSelector users;
		ProductsUserSelection products;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 2 );
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );

		ver = new FromServerForm ( uorder );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onOpen ( FromServerForm form ) {
				Order ord;

				ord = ( Order ) form.getObject ().getObject ( "baseorder" );
				ord.asyncLoadUsersOrders ();
			}

			/*
				Questo e' per forzare l'utente assegnatario dell'ordine prima che provveda il
				FromServerForm. Cio' e' dovuto alla volonta' di evitare che il form proponga il
				salvataggio dei dati (vedendo modificato lo user, che di default e' null mentre in
				fase di riassemblamento dell'oggetto sarebbe sempre valorizzato) se esso viene
				annullato, mentre qui interessa salvare il tutto solo se son stati modificati i
				prodotti ordinati
			*/
			public void onClosing ( FromServerForm form ) {
				UserSelector selector;

				selector = ( UserSelector ) form.retriveInternalWidget ( "baseuser" );
				if ( selector != null )
					form.getObject ().setObject ( "baseuser", selector.getValue () );
			}

			public boolean onDelete ( final FromServerForm form ) {
				FromServer uord;

				uord = form.getObject ();
				if ( uord.isValid () ) {
					uord.destroy ( new ServerResponse () {
						public void onComplete ( JSONValue response ) {
							cleanForm ( form, true, Session.getUser () );
							form.open ( false );
						}
					} );
				}

				return false;
			}
		} );

		if ( editable == true ) {
			ver.setCallback ( new FromServerFormCallbacks () {
				public void onClose ( FromServerForm form ) {
					cleanForm ( form, true, null );
				}

				public void onOpen ( FromServerForm form ) {
					UserSelector selector;

					selector = ( UserSelector ) form.retriveInternalWidget ( "baseuser" );
					retrieveCurrentOrderByUser ( form, selector.getValue () );
				}
			} );

			pan = new HorizontalPanel ();
			pan.setStyleName ( "highlight-part" );
			pan.add ( new Label ( "Ordine eseguito a nome di " ) );

			users = new UserSelector ();

			users.addFilter ( new FromServerValidateCallback () {
				public boolean checkObject ( FromServer object ) {
					if ( object.getInt ( "privileges" ) == User.USER_LEAVED )
						return false;
					else
						return true;
				}
			} );

			users.addChangeListener ( new ChangeListener () {
				public void onChange ( Widget sender ) {
					/**
						TODO	Accertarsi che non siano stati modificati i
							valori nel frattempo, selezionando un utente
							diverso.
							Per far le cose per bene si potrebbe aggiungere
							una funzione compareValue() in FromServerArray e
							ObjectWidget, ma implica correggere un po' tante
							cose in giro...
					*/

					UserSelector selector;
					selector = ( UserSelector ) sender;
					retrieveCurrentOrderByUser ( ver, selector.getValue () );
				}
			} );

			pan.add ( ver.getPersonalizedWidget ( "baseuser", users ) );
			ver.add ( pan );

			ver.emblems ().activate ( "multiuser" );
		}
		else {
			uorder.setObject ( "baseuser", Session.getUser () );
		}

		products = new ProductsUserSelection ( order.getArray ( "products" ), true, freeedit );
		ver.add ( ver.getPersonalizedWidget ( "products", products ) );

		ver.emblems ().activate ( "status", order.getInt ( "status" ) );
		order.addRelatedInfo ( "OrdersPrivilegedPanel", ver );
		return ver;
	}

	private FromServerForm doUneditableOrderRow ( FromServer order ) {
		FromServerForm ver;
		OrderUser uorder;
		ProductsUserSelection products;

		if ( hasOrders == false ) {
			hasOrders = true;
			remove ( 2 );
		}

		uorder = new OrderUser ();
		uorder.setObject ( "baseorder", order );

		ver = new FromServerForm ( uorder, FromServerForm.NOT_EDITABLE );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		uorder.setObject ( "baseuser", Session.getUser () );

		products = new ProductsUserSelection ( order.getArray ( "products" ), false, false );
		ver.add ( ver.getPersonalizedWidget ( "products", products ) );

		return ver;
	}

	/*
		action: gli stessi valori di syncLocalCache
	*/
	private void findAndAlign ( OrderUser uorder, int action ) {
		int index;
		FromServer order;
		FromServerForm form;

		order = uorder.getObject ( "baseorder" );
		form = ( FromServerForm ) order.getRelatedInfo ( "OrdersPrivilegedPanel" );

		if ( form != null ) {
			if ( canMultiUser ( order ) ) {
				syncLocalCache ( form, uorder, action );
			}
			else {
				/*
					Se l'ordine puo' essere manipolato solo per l'utente corrente, delle
					informazioni relative agli ordini altrui non me ne faccio niente
				*/
				if ( uorder.getObject ( "baseuser" ).equals ( Session.getUser () ) ) {
					syncLocalCache ( form, uorder, action );

					if ( action == 2 )
						resetProducts ( form );
					else
						alignOrderRow ( form, uorder );
				}
			}
		}
		else {
			/*
				In questo punto dovrei arrivarci solo quando sono un utente
				normale e voglio contemplare il mio ordine (senza poterlo
				editare). Ma un controllino extra male non fa'...
			*/

			if ( uorder.getObject ( "baseuser" ).equals ( Session.getUser () ) ) {
				index = getSortedPosition ( order );
				form = doUneditableOrderRow ( order );
				insert ( form, index );
				alignOrderRow ( form, uorder );

				if ( order.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false )
					form.setVisible ( false );
			}
		}
	}

	/*
		action:
			0 = ordine aggiunto
			1 = ordine modificato, aggiorna
			2 = ordine eliminato
	*/
	private void syncLocalCache ( FromServerForm ver, OrderUser uorder, int action ) {
		int uorder_id;
		ArrayList orders;
		FromServer iter;

		uorder_id = uorder.getLocalID ();
		orders = ver.getAddictionalData ();

		for ( int i = 0; i < orders.size (); i++ ) {
			iter = ( FromServer ) orders.get ( i );

			/*
				Forse inefficiente, ma qui faccio insieme il controllo
				sull'esistenza dell'ordine utente e la somma dei prodotti. Se
				l'ordine viene effettivamente trovato, ritorna e tutto il resto
				del lavoro viene perso
			*/
			if ( iter.getLocalID () == uorder_id ) {
				if ( action == 0 ) {
					return;
				}
				else if ( action == 1 || action == 2 ) {
					orders.remove ( i );

					/*
						Se se ne rimuove uno il totale degli elementi
						nell'array scende di uno, dunque devo allineare
						l'indice
					*/
					i--;

					continue;
				}
			}
		}

		if ( action == 0 || action == 1 )
			orders.add ( uorder );
	}

	private void retrieveCurrentOrderByUser ( FromServerForm form, FromServer user ) {
		int user_id;
		ArrayList orders;
		OrderUser iter;
		FromServer existing_user;

		user_id = user.getLocalID ();
		orders = form.getAddictionalData ();

		for ( int i = 0; i < orders.size (); i++ ) {
			iter = ( OrderUser ) orders.get ( i );
			existing_user = iter.getObject ( "baseuser" );

			if ( existing_user.getLocalID () == user_id ) {
				alignOrderRow ( form, iter );
				form.setObject ( iter );
				return;
			}
		}

		cleanForm ( form, false, user );
	}

	private void alignOrderRow ( FromServerForm ver, OrderUser uorder ) {
		ArrayList products;
		ProductsUserSelection table;

		products = uorder.getArray ( "products" );
		table = ( ProductsUserSelection ) ver.retriveInternalWidget ( "products" );
		table.setElements ( products );

		ver.setObject ( uorder );
	}

	private void resetProducts ( FromServerForm ver ) {
		ProductsUserSelection table;

		table = ( ProductsUserSelection ) ver.retriveInternalWidget ( "products" );
		table.setElements ( null );
	}

	private void cleanForm ( FromServerForm form, boolean complete, FromServer user ) {
		OrderUser uorder;
		FromServer original_uorder;

		uorder = new OrderUser ();
		original_uorder = form.getObject ();
		uorder.setObject ( "baseorder", original_uorder.getObject ( "baseorder" ) );

		if ( user != null )
			uorder.setObject ( "baseuser", user );

		form.setObject ( uorder );

		if ( complete == true )
			form.refreshContents ( uorder );
		else
			resetProducts ( form );
	}

	private void syncProductsInForm ( FromServerForm form, Order order ) {
		ProductsUserSelection select;

		select = ( ProductsUserSelection ) form.retriveInternalWidget ( "products" );
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
			for ( int i = 2; i < getWidgetCount (); i++ ) {
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
