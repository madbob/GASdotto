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

				private void addSaveProducts ( FromServerForm form ) {
					form.setCallback ( new FromServerFormCallbacks () {
						public void onClose ( FromServerForm form ) {
							OrderSummary complete_list;

							complete_list = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
							if ( complete_list != null )
								complete_list.saveContents ();
						}
					} );
				}

				protected FromServerForm doEditableRow ( FromServer ord ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					Supplier supplier;

					if ( ord.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false )
						return null;

					supplier = ( Supplier ) ord.getObject ( "supplier" );
					if ( supplier.iAmReference () == false )
						return null;

					ver = new FromServerForm ( ord );
					ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );
					addSaveProducts ( ver );

					/*
						L'icona di editing la aggiungo sempre sebbene sia sottinteso che
						l'ordine e' editabile (altrimenti manco apparirebbe nella lista), per
						coerenza con gli altri pannelli
					*/
					ver.emblems ().activate ( "multiuser" );

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

					ver.emblems ().activate ( "status", ord.getInt ( "status" ) );
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
					ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

					/**
						TODO	Questa callback e' valida solo per i nuovi ordini, una volta
							salvato andrebbe rimossa dal form
					*/
					ver.setCallback ( new FromServerFormCallbacks () {
						public void onSaved ( FromServerForm form ) {
							if ( form.getObject ().getInt ( "status" ) == Order.OPENED )
								Utils.showNotification ( "Un nuovo ordine è ora disponibile nel pannello 'Ordini'",
												SmoothingNotify.NOTIFY_INFO );
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
							FromServer selected;
							FromServer prod;

							found = false;

							supps = ( FromServerSelector ) sender;
							selected = supps.getValue ();

							products = Utils.getServer ().getObjectsFromCache ( "Product" );
							num_products = products.size ();

							for ( int i = 0; i < num_products; i++ ) {
								prod = ( FromServer ) products.get ( i );

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
						addSaveProducts ( form );

						form.emblems ().activate ( "status", object.getInt ( "status" ) );
						form.emblems ().activate ( "multiuser" );
					}
				}

				protected void customModify ( FromServerForm form ) {
					FromServer obj;

					obj = form.getObject ();
					form.emblems ().activate ( "status", obj.getInt ( "status" ) );
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
					OrderSummary complete_list;

					complete_list = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
					if ( complete_list != null )
						complete_list.unlock ();

					ord = ( Order ) form.getObject ();
					ord.asyncLoadUsersOrders ();
				}
		};

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private void syncOrder ( OrderUser user ) {
				FromServer order;
				FromServerForm form;
				OrderSummary summary;

				order = user.getObject ( "baseorder" );
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
				syncOrder ( ( OrderUser ) object );
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

		doFilterOptions ();
	}

	private void doFilterOptions () {
		HorizontalPanel pan;
		CheckBox toggle_view;

		pan = new HorizontalPanel ();
		pan.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );
		pan.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_LEFT );
		pan.setStyleName ( "panel-up" );
		addTop ( pan );

		toggle_view = new CheckBox ( "Mostra Ordini Vecchi" );
		OrdersHub.syncCheckboxOnShippedOrders ( toggle_view, new ClickListener () {
			public void onClick ( Widget sender ) {
				boolean show;
				ArrayList forms;
				CheckBox myself;
				FromServerForm form;

				myself = ( CheckBox ) sender;
				forms = main.collectForms ();
				show = myself.isChecked ();
				OrdersHub.toggleShippedOrdersStatus ( show );

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					if ( form.getObject ().getInt ( "status" ) == Order.SHIPPED )
						form.setVisible ( show );
				}
			}
		} );
		pan.add ( toggle_view );
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
		CaptionPanel p_summary;

		p_summary = addOrderSummary ( form );
		if ( p_summary != null )
			form.add ( p_summary );
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
