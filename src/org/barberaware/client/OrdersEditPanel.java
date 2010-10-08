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
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;

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

					/**
						TODO	La ripetizione degli ordini va riccamente
							rivista, per ora la funzione e' sospesa
					*/

					// frame.addPair ( "Si ripete", form.getPersonalizedWidget ( "nextdate", new OrderCiclyc () ) );
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

				private void checkMailSummary ( FromServerForm form ) {
					HorizontalPanel container;
					FromServer ord;
					FromServerButton button;

					if ( Session.getGAS ().getBool ( "use_mail" ) == false )
						return;

					ord = form.getObject ();

					if ( ord.getInt ( "status" ) != Order.CLOSED )
						return;

					if ( ord.getBool ( "mail_summary_sent" ) == true )
						return;

					container = ( HorizontalPanel ) form.retriveInternalWidget ( "mail_summary" );
					if ( container == null ) {
						container = new HorizontalPanel ();
						container.addStyleName ( "info-cell" );

						form.setExtraWidget ( "mail_summary", container );
						form.insert ( container, 1 );

						container.add ( new Label ( "Questo ordine è stato chiuso. Clicca il bottone per inviare una mail di riscontro a coloro che hanno effettuato una prenotazione." ) );

						button = new FromServerButton ( ord, "Invia", new FromServerCallback () {
							public void execute ( final FromServer object ) {
								object.setBool ( "mail_summary_sent", true );

								object.save ( new ServerResponse () {
									protected void onComplete ( JSONValue response ) {
										FromServerForm form;

										form = main.retrieveFormById ( object.getLocalID () );
										if ( form != null )
											form.removeWidget ( "mail_summary" );
									}
								} );
							}
						} );
						container.add ( button );
					}
				}

				private void checkNewProductsAvailability ( final FromServerForm form ) {
					FromServer ord;

					ord = form.getObject ();

					if ( ord.getInt ( "status" ) != Order.OPENED )
						return;

					Utils.getServer ().rawGet ( "data_shortcuts.php?type=order_products_diff&order=" + ord.getLocalID (), new RequestCallback () {
						public void onError ( Request request, Throwable exception ) {
							Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );
						}

						public void onResponseReceived ( Request request, Response response ) {
							int num;
							JSONValue jsonObject;
							JSONArray products;
							FlexTable container;
							FromServer tmp;

							try {
								jsonObject = JSONParser.parse ( response.getText () );

								products = jsonObject.isArray ();
								if ( products == null ) {
									Utils.getServer ().dataArrived ();
									return;
								}

								num = products.size ();
								if ( num == 0 ) {
									Utils.getServer ().dataArrived ();
									return;
								}

								container = ( FlexTable ) form.retriveInternalWidget ( "diff_products" );

								if ( container == null ) {
									container = new FlexTable ();
									container.addStyleName ( "info-cell" );

									form.setExtraWidget ( "diff_products", container );
									form.insert ( container, 1 );

									container.setWidget ( 0, 0, new Label ( "Pare che nuovi prodotti siano stati introdotti dopo la creazione di questo ordine..." ) );
									container.getFlexCellFormatter ().setColSpan ( 0, 0, 3 );

									container.addTableListener ( new TableListener () {
										public void onCellClicked ( SourcesTableEvents sender, int row, int cell ) {
											ArrayList products;
											FlexTable tab;
											Hidden hidden;
											FromServer order;
											OrderSummary summary;

											if ( row == 0 || cell != 2 )
												return;

											tab = ( FlexTable ) sender;
											hidden = ( Hidden ) tab.getWidget ( row, 0 );

											order = form.getObject ();
											products = order.getArray ( "products" );
											products.add ( Utils.getServer ().getObjectFromCache ( "Product", Integer.parseInt ( hidden.getName () ) ) );
											order.setArray ( "products", products );

											summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
											summary.reFill ( ( Order ) order );

											tab.removeRow ( row );
											form.forceNextSave ( true );

											if ( tab.getRowCount () == 1 )
												form.removeWidget ( "diff_products" );
										}
									} );
								}
								else {
									while ( container.getRowCount () != 1 )
										container.removeRow ( 1 );
								}

								for ( int i = 0, row = 1; i < num; i++, row++ ) {
									tmp = FromServer.instance ( products.get ( i ).isObject () );
									container.setWidget ( row, 0, new Hidden ( Integer.toString ( tmp.getLocalID () ) ) );
									container.setWidget ( row, 1, new Label ( tmp.getString ( "name" ) ) );
									container.setWidget ( row, 2, new Button ( "Aggiungi il prodotto in questo ordine" ) );
								}
							}
							catch ( com.google.gwt.json.client.JSONException e ) {
								Utils.showNotification ( "Ricevuti dati invalidi dal server" );
							}

							Utils.getServer ().dataArrived ();
						}
					} );
				}

				protected FromServerForm doEditableRow ( FromServer ord ) {
					final FromServerForm ver;
					HorizontalPanel hor;
					CustomCaptionPanel frame;
					CaptionPanel sframe;
					Supplier supplier;

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

					if ( ord.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false )
						ver.setVisible ( false );

					return ver;
				}

				private void populateProductsPreview ( final FromServerForm form, final FromServer supplier ) {
					ObjectRequest params;

					if ( supplier != null ) {
						params = new ObjectRequest ( "Product" );
						params.add ( "supplier", supplier.getLocalID () );

						Utils.getServer ().serverGet ( params, new ServerResponse () {
							public void onComplete ( JSONValue response ) {
								int num_products;
								ArrayList products;
								ArrayList final_products;
								FromServer prod;
								FromServerTable table;

								Utils.getServer ().JSONToObjects ( response );

								final_products = new ArrayList ();
								products = Utils.getServer ().getObjectsFromCache ( "Product" );
								num_products = products.size ();

								for ( int i = 0; i < num_products; i++ ) {
									prod = ( FromServer ) products.get ( i );

									if ( prod.getBool ( "available" ) == true &&
											prod.getBool ( "archived" ) == false &&
											prod.getObject ( "supplier" ).equals ( supplier ) )
										final_products.add ( prod );
								}

								table = ( FromServerTable ) form.retriveInternalWidget ( "products_preview" );
								table.setElements ( final_products );
							}
						} );
					}
				}

				private void addStaticProductsList ( FromServerForm ver, FromServerSelector suppliers ) {
					VerticalPanel container;
					CaptionPanel products_frame;
					FromServerTable products;

					products_frame = new CaptionPanel ( "Elenco Prodotti" );
					ver.setExtraWidget ( "products_preview_frame", products_frame );
					ver.add ( products_frame );

					container = new VerticalPanel ();
					products_frame.add ( container );

					container.add ( new HTML ( "Qui di seguito, i prodotti che saranno contemplati nell'ordine.<br />Per cambiare la lista o gli attributi, intervenire nel pannello \"Prodotti\" del fornitore desiderato." ) );

					products = new FromServerTable ();
					products.setEmptyWarning ( "Non ci sono prodotti caricati per il fornitore selezionato" );

					products.addColumn ( "Nome", "name", false );
					products.addColumn ( "Prezzo Unitario", "unit_price", new WidgetFactoryCallback () {
						public Widget create () {
							return new PriceViewer ();
						}
					} );
					products.addColumn ( "Prezzo Trasporto", "shipping_price", new WidgetFactoryCallback () {
						public Widget create () {
							return new PriceViewer ();
						}
					} );

					ver.setExtraWidget ( "products_preview", products );
					container.add ( products );

					populateProductsPreview ( ver, suppliers.getValue () );
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

					ver.setCallback ( new FromServerFormCallbacks ( "notification" ) {
						public void onSaved ( FromServerForm form ) {
							if ( form.getObject ().getInt ( "status" ) == Order.OPENED ) {
								Utils.showNotification ( "Un nuovo ordine è ora disponibile nel pannello 'Ordini'",
												Notification.INFO );

								form.removeCallback ( "notification" );
							}
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
							FromServerSelector supps;
							FromServer selected;

							supps = ( FromServerSelector ) sender;
							selected = supps.getValue ();
							populateProductsPreview ( ver, selected );

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

					addStaticProductsList ( ver, suppliers );

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
					String id;
					FromServerForm form;

					form = main.retrieveFormById ( object.getLocalID () );
					if ( form != null ) {
						id = main.getIdentifier ();
						object.addRelatedInfo ( id, form );
						form.setObject ( object );

						addOrderDetails ( form );
						addSaveProducts ( form );

						/*
							Il widget in "products_preview" e' contenuto all'interno del
							frame, dunque rimuovendo uno sparisce anche l'altro
						*/
						form.removeWidget ( "products_preview_frame" );

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

					checkNewProductsAvailability ( form );
					checkMailSummary ( form );
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

		addTop ( Utils.getEmblemsCache ( "orders" ).getLegend () );
		addTop ( main );

		doFilterOptions ();
	}

	private void doFilterOptions () {
		OrdersHubWidget filter;

		filter = new OrdersHubWidget () {
			public void doFilter ( boolean show, Date start, Date end ) {
				ArrayList forms;
				FromServerForm form;
				FromServer ord;

				forms = main.collectForms ();

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					ord = form.getObject ();

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
		complete_list.reFill ( order );
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
