/*  GASdotto 0.1
 *  Copyright (C) 2008/2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
	private abstract class ForeachProductListCallback {
		public abstract void doIt ( Order order, FlexTable list, Product product );
	}

	private FormCluster		main;

	public OrdersEditPanel () {
		super ();

		main = new FormCluster ( "Order", "Nuovo Ordine" ) {
				protected FromServerForm doEditableRow ( FromServer ord ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					Order order;
					Supplier supplier;
					OrderSummary complete_list;

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

					frame = new CustomCaptionPanel ( "Date" );
					hor.add ( frame );
					hor.setCellWidth ( frame, "50%" );

					frame.addPair ( "Data apertura", ver.getWidget ( "startdate" ) );
					frame.addPair ( "Data chiusura", ver.getWidget ( "enddate" ) );
					frame.addPair ( "Data consegna", ver.getWidget ( "shippingdate" ) );
					frame.addPair ( "Si ripete", ver.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );

					/* riassunto ordine */

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

					frame = new CustomCaptionPanel ( "Date" );
					hor.add ( frame );

					frame.addPair ( "Data apertura", ver.getWidget ( "startdate" ) );
					frame.addPair ( "Data chiusura", ver.getWidget ( "enddate" ) );
					ver.setValidation ( "enddate", FromServerValidateCallback.defaultDateValidationCallback () );
					frame.addPair ( "Data consegna", ver.getWidget ( "shippingdate" ) );
					frame.addPair ( "Si ripete", ver.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );

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

					form = main.retrieveForm ( object );
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

			private void syncOrder ( OrderUser user ) {
				Order order;
				FromServerForm form;
				OrderSummary summary;

				order = ( Order ) user.getObject ( "baseorder" );
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
			}

			public void onBlockBegin () {
				lock = true;
			}

			public void onBlockEnd () {
				FromServerForm form;
				OrderSummary summary;

				for ( int i = 0; i < main.latestIterableIndex (); i++ ) {
					form = main.retrieveForm ( i );
					summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
					if ( summary != null )
						summary.syncOrders ();
				}

				lock = false;
			}

			public void onModify ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}

			public void onDestroy ( FromServer object ) {
				syncOrder ( ( OrderUser ) object );
			}

			protected String debugName () {
				return "OrdersEditPanel";
			}
		} );

		Utils.getServer ().onObjectEvent ( "Product", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				int num_orders;
				int num_products;
				boolean already_has;
				ArrayList orders;
				ArrayList products;
				Order order;
				Supplier prod_supplier;
				Product order_product;

				/*
					Tutto questo gran giro per controllare che il prodotto ricevuto non sia gia'
					negli ordini immessi nel pannello. Se cio' non accadesse gli ordini
					verrebbero invalidati e ricaricati ad ogni prodotto che arriva, soprattutto
					in fase di startup dell'applicazione, generando un traffico immenso e
					possibili inconsistenze
				*/

				prod_supplier = ( Supplier ) object.getObject ( "supplier" );
				orders = main.collectContents ();
				num_orders = orders.size ();

				for ( int i = 0; i < num_orders; i++ ) {
					order = ( Order ) orders.get ( i );

					if ( order.getObject ( "supplier" ).equals ( prod_supplier ) ) {
						products = order.getArray ( "products" );
						num_products = products.size ();
						already_has = false;

						for ( int e = 0; e < num_products; e++ ) {
							order_product = ( Product ) products.get ( i );
							if ( order_product.equals ( object ) ) {
								already_has = true;
								break;
							}
						}

						if ( already_has == false ) {
							reloadOrdersBySupplier ( prod_supplier );
							break;
						}
					}
				}
			}

			public void onModify ( FromServer object ) {
				/* dummy */
			}

			public void onDestroy ( FromServer object ) {
				reloadOrdersBySupplier ( ( Supplier ) object.getObject ( "supplier" ) );
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
		Utils.getServer ().testObjectReceive ( "Supplier" );
		Utils.getServer ().testObjectReceive ( "Product" );
		Utils.getServer ().testObjectReceive ( "Order" );
	}
}
