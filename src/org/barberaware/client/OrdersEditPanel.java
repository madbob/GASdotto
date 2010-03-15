/*  GASdotto
 *  Copyright (C) 2008/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.allen_sauer.gwt.log.client.Log;

public class OrdersEditPanel extends GenericPanel {
	private FormCluster		main;

	public OrdersEditPanel () {
		super ();

		main = new FormCluster ( "Order", "Nuovo Ordine" ) {
				private void addDatesFrame ( FromServerForm form, HorizontalPanel hor ) {
					CustomCaptionPanel frame;

					frame = new CustomCaptionPanel ( "Date" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Data apertura", form.getWidget ( "startdate" ) );

					frame.addPair ( "Data chiusura", form.getWidget ( "enddate" ) );
					form.setValidation ( "enddate", new FromServerValidateCallback () {
						public boolean check ( FromServer object, String attribute, Widget widget ) {
							DateWidget enddate;
							Date end;

							enddate = ( DateWidget ) widget;
							end = enddate.getValue ();

							if ( end == null ) {
								Utils.showNotification ( "Non hai definito una data di chiusura" );
								return false;
							}
							else if ( end.compareTo ( object.getDate ( "startdate" ) ) < 0 ) {
								Utils.showNotification ( "La data di chiusura non può essere precedente la data di apertura" );
								return false;
							}
							else
								return true;
						}
					} );

					frame.addPair ( "Data consegna", form.getWidget ( "shippingdate" ) );
					form.setValidation ( "shippingdate", new FromServerValidateCallback () {
						public boolean check ( FromServer object, String attribute, Widget widget ) {
							DateWidget shipdate;
							Date ship;

							shipdate = ( DateWidget ) widget;
							ship = shipdate.getValue ();

							if ( ship != null && ship.compareTo ( object.getDate ( "enddate" ) ) < 0 ) {
								Utils.showNotification ( "La data di consegna non può essere precedente la data di chiusura" );
								return false;
							}
							else
								return true;
						}
					} );

					frame.addPair ( "Si ripete", form.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );
				}

				protected FromServerForm doEditableRow ( FromServer ord ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					Order order;
					Supplier supplier;

					order = ( Order ) ord;

					if ( order.getInt ( "status" ) == Order.SHIPPED )
						return null;

					supplier = ( Supplier ) order.getObject ( "supplier" );
					if ( supplier.iAmReference () == false )
						return null;

					ver = new FromServerForm ( order );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Fornitore", ver.getPersonalizedWidget ( "supplier", new NameLabelWidget () ) );
					frame.addPair ( "Stato", ver.getPersonalizedWidget ( "status", doOrderStatusSelector () ) );
					frame.addPair ( "Anticipo", ver.getWidget ( "anticipated" ) );

					addDatesFrame ( ver, hor );
					addOrderDetails ( ver );

					return ver;
				}

				protected FromServerForm doNewEditableRow () {
					Order order;
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					Widget suppliers_main;
					Date now;
					FromServerSelector suppliers;
					DateWidget date;

					order = new Order ();

					ver = new FromServerForm ( order );

					/**
						TODO	Questa callback e' valida solo per i nuovi ordini, una volta
							salvato andrebbe rimossa dal form
					*/
					ver.setCallback ( new FromServerFormCallbacks () {
						public void onSaved ( FromServerForm form ) {
							if ( form.getObject ().getInt ( "status" ) == Order.OPENED )
								Utils.showNotification ( "Un nuovo ordine è ora disponibile nel pannello 'Ordini'", SmoothingNotify.NOTIFY_INFO );
						}
					} );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );

					suppliers = new FromServerSelector ( "Supplier", true, true );
					suppliers.addFilter ( new FromServerValidateCallback () {
						public boolean checkObject ( FromServer object ) {
							Supplier sup;
							sup = ( Supplier ) object;
							return sup.iAmReference ();
						}
					} );

					suppliers.addChangeListener ( new ChangeListener () {
						public void onChange ( Widget sender ) {
							int num_products;
							boolean found;
							ArrayList products;
							FromServerSelector supps;
							Supplier selected;
							Product prod;

							found = false;

							supps = ( FromServerSelector ) sender;
							selected = ( Supplier ) supps.getValue ();

							products = Utils.getServer ().getObjectsFromCache ( "Product" );
							num_products = products.size ();

							for ( int i = 0; i < num_products; i++ ) {
								prod = ( Product ) products.get ( i );

								if ( prod.getObject ( "supplier" ).equals ( selected ) ) {
									found = true;
									break;
								}
							}

							if ( found == false )
								Utils.showNotification ( "Non ci sono prodotti caricati per il fornitore selezionato" );
						}
					} );

					frame.addPair ( "Fornitore", ver.getPersonalizedWidget ( "supplier", suppliers ) );
					ver.setValidation ( "supplier", FromServerValidateCallback.defaultObjectValidationCallback () );

					frame.addPair ( "Stato", ver.getPersonalizedWidget ( "status", doOrderStatusSelector () ) );
					frame.addPair ( "Anticipo", ver.getWidget ( "anticipated" ) );

					/* seconda colonna */

					addDatesFrame ( ver, hor );

					now = new Date ( System.currentTimeMillis () );
					date = ( DateWidget ) ver.retriveInternalWidget ( "startdate" );
					date.setValue ( now );

					return ver;
				}

				protected void customNew ( FromServer object, boolean true_new ) {
					/*
						Quando si crea un nuovo Order, l'oggetto assegnato al form resta
						sempre l'originale, dunque senza la lista di prodotti contemplati
						(che vengono assegnati dalla relativa componente server ed
						accessibili quando si ricarica l'oggetto).
						Qui si forza il ri-assegnamento dell'oggetto che viene ripescato dal
						server, in modo che abbia la lista dei prodotti e se ne possa
						compilare il summary
					*/
					FromServerForm form;

					form = main.retrieveFormById ( object.getLocalID () );
					if ( form != null ) {
						form.setObject ( object );
						addOrderDetails ( form );
					}
				}

				protected int sorting ( FromServer first, FromServer second ) {
					if ( first == null )
						return 1;
					else if ( second == null )
						return -1;

					return -1 * ( first.getDate ( "enddate" ).compareTo ( second.getDate ( "enddate" ) ) );
				}

				protected void asyncLoad ( FromServerForm form ) {
					Order ord;

					ord = ( Order ) form.getObject ();
					ord.asyncLoadUsersOrders ();
				}
		};

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private boolean		lock		= false;
			private ArrayList	orders		= null;

			private void syncOrder ( OrderUser user ) {
				Order order;
				FromServerForm form;
				OrderSummary summary;

				order = ( Order ) user.getObject ( "baseorder" );
				if ( order == null )
					return;

				form = main.retrieveForm ( order );

				if ( form != null ) {
					summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
					if ( summary != null )
						summary.syncOrders ();
				}
			}

			public void onReceive ( FromServer object ) {
				if ( lock == false )
					syncOrder ( ( OrderUser ) object );
				else
					orders.add ( object.getObject ( "baseorder" ) );
			}

			public void onBlockBegin () {
				lock = true;

				if ( orders == null )
					orders = new ArrayList ();
			}

			public void onBlockEnd () {
				FromServerForm form;
				Order ord;
				OrderSummary summary;

				for ( int a = 0; a < orders.size (); a++ ) {
					ord = ( Order ) orders.get ( a );

					for ( int i = 0; i < main.latestIterableIndex (); i++ ) {
						form = main.retrieveForm ( i );

						if ( ord.equals ( form.getObject () ) ) {
							summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
							if ( summary != null )
								summary.syncOrders ();
						}
					}
				}

				lock = false;
				orders.clear ();
			}

			public void onModify ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}

			public void onDestroy ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}

			protected String debugName () {
				return "OrdersEditPanel per OrderUser";
			}
		} );

		addTop ( main );
	}

	private Widget doOrderStatusSelector () {
		CyclicToggle status;

		/*
			Nella selezione non appare lo stato 3, usato per l'auto-sospensione
			(nel caso di un ordine con data di apertura nel futuro)
		*/

		status = new CyclicToggle ();
		status.addState ( "images/order_status_opened.png" );
		status.addState ( "images/order_status_closed.png" );
		status.addState ( "images/order_status_suspended.png" );
		status.addState ( "images/order_status_shipped.png" );
		status.setDefaultSelection ( 2 );
		return status;
	}

	private void reloadOrdersBySupplier ( Supplier supplier ) {
		ObjectRequest req;

		req = new ObjectRequest ( "Order" );
		req.add ( "supplier", supplier, Supplier.class );
		req.add ( "status", Order.OPENED );
		Utils.getServer ().invalidateCacheByCondition ( req );
	}

	private void addOrderDetails ( FromServerForm form ) {
		HorizontalPanel pan;
		CaptionPanel p_summary;
		CaptionPanel p_products;

		p_summary = addOrderSummary ( form );
		p_products = addProductsHandler ( form );

		if ( p_summary != null && p_products != null ) {
			pan = new HorizontalPanel ();
			pan.setWidth ( "100%" );

			pan.add ( p_summary );
			pan.setCellWidth ( p_summary, "50%" );

			pan.add ( p_products );
			pan.setCellWidth ( p_products, "50%" );

			form.add ( pan );
		}
	}

	private CaptionPanel addOrderSummary ( FromServerForm form ) {
		Order order;
		CaptionPanel sframe;
		OrderSummary complete_list;

		if ( form == null )
			return null;

		complete_list = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
		if ( complete_list != null )
			return null;

		order = ( Order ) form.getObject ();
		sframe = new CaptionPanel ( "Stato Ordini" );

		complete_list = new OrderSummary ( order );
		form.setExtraWidget ( "summary", complete_list );
		sframe.add ( complete_list );
		return sframe;
	}

	private CaptionPanel addProductsHandler ( final FromServerForm form ) {
		final FromServerTable table;
		VerticalPanel container;
		CaptionPanel sframe;
		ButtonsBar buttons;
		PushButton button;

		if ( ( FromServerTable ) form.retriveInternalWidget ( "products" ) != null )
			return null;

		container = new VerticalPanel ();

		table = new FromServerTable ();
		table.addColumn ( "Nome", "name", false );
		table.addColumn ( "Ordinabile", "available", true );
		table.addColumn ( "Prezzo Unitario", "unit_price", true );
		table.addColumn ( "Prezzo Trasporto", "shipping_price", true );
		container.add ( form.getPersonalizedWidget ( "products", table ) );

		buttons = new ButtonsBar ();
		container.add ( buttons );

		/**
			TODO	Visualizzare il tasto di reset solo se si apportano modifiche
		*/
		button = new PushButton ( new Image ( "images/cancel.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				table.revertChanges ();
			}
		} );
		buttons.add ( button, "Annulla" );

		button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				Order order;
				ArrayList products;
				ObjectRequest req;
				OrderSummary summary;

				order = ( Order ) form.getObject ();
				table.saveChanges ();
				products = table.getElements ();
				order.setArray ( "products", products );

				req = new ObjectRequest ( "OrderUser" );
				req.add ( "baseorder", order, Order.class );
				Utils.getServer ().invalidateCacheByCondition ( req );

				summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
				summary.reFill ( order );

				Utils.getServer ().triggerObjectModification ( order );
			}
		} );
		buttons.add ( button, "Salva" );

		sframe = new CaptionPanel ( "Prodotti" );
		sframe.add ( container );
		return sframe;
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Gestione Ordini";
	}

	public String getSystemID () {
		return "edit_orders";
	}

	public String getCurrentInternalReference () {
		return Integer.toString ( main.getCurrentlyOpened () );
	}

	public Image getIcon () {
		return new Image ( "images/path_orders_edit.png" );
	}

	public void initView () {
		Utils.getServer ().testObjectReceive ( "Order" );
		Utils.getServer ().testObjectReceive ( "Product" );
	}
}
