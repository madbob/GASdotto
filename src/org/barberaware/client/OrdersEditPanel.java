/*  GASdotto
 *  Copyright (C) 2008/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.event.dom.client.*;

import com.allen_sauer.gwt.log.client.Log;

public class OrdersEditPanel extends GenericPanel {
	private DeckPanel		container;
	private FormCluster		main;

	private OrdersHubWidget		filter;
	private OrdersAggregator	aggregator;
	private CheckBox		aggregateToggle;

	public OrdersEditPanel () {
		super ();

		HorizontalPanel filters;

		main = new FormCluster ( "Order", "Nuovo Ordine" ) {
				protected FromServerForm doEditableRow ( FromServer ord ) {
					FromServerForm ret;

					if ( ord.getType () == "Order" && ( ord.getBool ( "parent_aggregate" ) == false || filter.isVisible () == true ) )
						ret = editableOrder ( ord );
					else if ( ord.getType () == "OrderAggregate" )
						ret = editableOrderAggregate ( ord );
					else
						ret = null;

					return ret;
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

					suppliers = new FromServerSelector ( "Supplier", true, true, false );
					suppliers.addFilter ( new FromServerValidateCallback () {
						public boolean checkObject ( FromServer object ) {
							Supplier sup;
							sup = ( Supplier ) object;
							return sup.iAmReference ();
						}
					} );

					if ( suppliers.getNumValues () == 0 ) {
						Utils.showNotification ( "Non hai fornitori assegnati, non puoi aprire nuovi ordini", Notification.WARNING );
						return null;
					}

					order = new Order ();

					ver = new FromServerForm ( order );
					ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

					ver.setCallback ( new FromServerFormCallbacks ( "notification" ) {
						public void onSaved ( FromServerRappresentationFull form ) {
							ObjectRequest params;
							FromServer order;

							order = form.getValue ();

							if ( order.getInt ( "status" ) == Order.OPENED ) {
								Utils.showNotification ( "Un nuovo ordine è ora disponibile nel pannello 'Ordini'",
												Notification.INFO );

								form.removeCallback ( "notification" );
							}

							/*
								Questo è per forzare il ricaricamento dei prodotti del fornitore.
								I prodotti contemplati nell'ordine vengono alterati e duplicati,
								dunque i riferimenti locali non sono più validi ed è necessario
								aggiornarli
							*/
							params = new ObjectRequest ( "Product" );
							params.add ( "supplier", order.getObject ( "supplier" ).getLocalID () );
							Utils.getServer ().testObjectReceive ( params );
						}
					} );

					hor = new HorizontalPanel ();
					hor.setWidth ( "100%" );
					ver.add ( hor );

					/* prima colonna */

					frame = new CustomCaptionPanel ( "Attributi" );
					hor.add ( frame );

					suppliers.addDomHandler ( new ChangeHandler () {
						public void onChange ( ChangeEvent event ) {
							FromServerSelector supps;
							FromServer selected;

							supps = ( FromServerSelector ) event.getSource ();
							selected = supps.getValue ();
							populateProductsPreview ( ver, selected );

						}
					}, ChangeEvent.getType () );

					frame.addPair ( "Fornitore", ver.getPersonalizedWidget ( "supplier", suppliers ) );
					ver.setValidation ( "supplier", FromServerValidateCallback.defaultObjectValidationCallback () );

					frame.addPair ( "Stato", ver.getPersonalizedWidget ( "status", Order.doOrderStatusSelector ( true ) ) );
					frame.addPair ( "Anticipo", ver.getWidget ( "anticipated" ) );
					frame.addPair ( "Invia Notifiche", ver.getWidget ( "send_notification_mails" ) );

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

					if ( true_new == false )
						aggregator.addElement ( object );

					if ( object.getType () != "Order" || object.getBool ( "parent_aggregate" ) == true || true_new == true )
						return;

					/*
						Non usare main.retrieveForm(), in quanto l'oggetto all'interno del
						FormCluster non e' lo stesso che viene ricostruito dal fetch dal
						server dunque non ha gli stessi "related" assegnati, e appunto
						main.retrieveForm() ritorna NULL
					*/
					form = ( FromServerForm ) main.retrieveFormById ( object.getLocalID () );
					if ( form != null ) {
						id = main.getIdentifier ();
						object.addRelatedInfo ( id, form );

						form.setValue ( object );

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

				protected void customModify ( FromServer obj, FromServerRappresentation form ) {
					FromServerForm f;

					if ( form != null && form instanceof FromServerForm ) {
						if ( obj.getBool ( "parent_aggregate" ) == true ) {
							form.invalidate ();

							/*
								Se l'ordine e' finito in un aggregato, viene
								intercettata a parte l'inclusione nell'aggregato
								stesso
							*/
						}
						else {
							f = ( FromServerForm ) form;
							f.emblems ().activate ( "status", obj.getInt ( "status" ) );

							/*
								Questo lo chiamo nell'eventualita' che un ordine
								chiuso e' stato riaperto, per metterlo nella lista di
								ordini validi da aggregare. Se era gia' stato
								incluso, l'OrdersAggregator se la gestisce per conto
								suo
							*/
							aggregator.addElement ( obj );
						}
					}

					else if ( form != null && form instanceof OrderDetails ) {
						if ( obj.getBool ( "parent_aggregate" ) == true ) {
							/*
							obj = OrderAggregate.retrieveAggregate ( obj );
							main.refreshElement ( obj );
							aggregator.refreshElement ( obj );
							*/
						}
					}

					/*
						Calma e sangue freddo...
						- Quando arriva un Order fine a se' stesso, in doEditableRow viene creato un form dedicato.
						- Quando l'ordine si trova in un aggregato, esso viene ridotto ad un OrderDetails dentro quel form.
						- Quando un aggregato viene rimosso, occorre creare un nuovo form per l'ordine. In questo caso in
						OrdersAggregator l'ordine stesso viene modificato e salvato, dunque formalmente viene modificato,
						e nel contempo viene soppresso il FromServerRappresentation che lo rappresenta. Dunque quando sono
						qui non esiste piu' nessun elemento grafico che lo rappresenta (form == null), e lo devo creare
						forzandolo con addElement
					*/

					else if ( form == null ) {
						if ( obj.getBool ( "parent_aggregate" ) == false )
							main.putElement ( obj );
					}
				}

				protected int sorting ( FromServer first, FromServer second ) {
					return first.compare ( first, second );
				}

				protected void asyncLoad ( FromServerForm form ) {
					ArrayList orders;
					OrderInterface obj;
					OrderSummary complete_list;

					complete_list = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
					if ( complete_list != null )
						complete_list.unlock ();

					obj = ( OrderInterface ) form.getValue ();
					obj.asyncLoadUsersOrders ();

					checkNewProductsAvailability ( form );
					checkMailSummary ( form );
				}
		};

		Utils.getServer ().onObjectEvent ( "OrderAggregate", new ServerObjectReceive () {
			public void onReceive ( FromServer object ) {
				ArrayList orders;
				FromServer tmp;

				orders = object.getArray ( "orders" );
				for ( int i = 0; i < orders.size (); i++ ) {
					tmp = ( FromServer ) orders.get ( i );
					main.deleteElement ( tmp );
				}

				main.putElement ( object );
				aggregator.addElement ( object );
			}

			public void onModify ( FromServer object ) {
				main.updateElement ( object );
				aggregator.refreshElement ( object );
			}

			public void onDestroy ( FromServer object ) {
				main.deleteElement ( object );
				aggregator.removeElement ( object );
			}

			protected String debugName () {
				return "OrdersEditPanel per OrderAggregate";
			}
		} );

		Utils.getServer ().onObjectEvent ( "OrderUser", new ServerObjectReceive () {
			private boolean		lockNextRound	= false;
			private ArrayList 	targetOrders	= new ArrayList ();

			private void syncOrder ( FromServer order ) {
				FromServerRappresentation form;
				OrderSummary summary;

				form = main.retrieveForm ( order );
				if ( form != null ) {
					/*
						Questo perche' quando tratto con ordini aggregati retrieveForm() mi
						ritorna l'OrderDetails che descrive l'ordine, non la lista condivisa
						di ordini.
						Il metodo serio di aggiustare questo sarebbe scindere la gestione
						degli OrderUserAggregate in arrivo
					*/
					if ( form instanceof OrderDetails )
						form = form.getRappresentationParent ();

					summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
					if ( summary != null )
						summary.syncOrders ();
				}
			}

			private void manageRequest ( OrderUser user ) {
				FromServer order;

				order = user.getObject ( "baseorder" );
				if ( order == null )
					return;

				if ( lockNextRound == true ) {
					FromServer tmporder;

					for ( int i = 0; i < targetOrders.size (); i++ ) {
						tmporder = ( FromServer ) targetOrders.get ( i );
						if ( tmporder.equals ( order ) )
							return;
					}

					targetOrders.add ( order );
					return;
				}

				syncOrder ( order );
			}

			public void onBlockBegin () {
				lockNextRound = true;
			}

			public void onBlockEnd () {
				FromServer tmporder;

				lockNextRound = false;

				for ( int i = 0; i < targetOrders.size (); i++ ) {
					tmporder = ( FromServer ) targetOrders.get ( i );
					syncOrder ( tmporder );
				}

				targetOrders.clear ();
			}

			public void onReceive ( FromServer object ) {
				manageRequest ( ( OrderUser ) object );
			}

			public void onModify ( FromServer object ) {
				manageRequest ( ( OrderUser ) object );
			}

			public void onDestroy ( FromServer object ) {
				manageRequest ( ( OrderUser ) object );
			}

			protected String debugName () {
				return "OrdersEditPanel per OrderUser";
			}
		} );

		container = new DeckPanel ();
		container.add ( main );

		aggregator = new OrdersAggregator ();
		aggregator.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				aggregateToggle.setChecked ( false );
				container.showWidget ( 0 );
				filter.setVisible ( true );
			}
		}, ChangeEvent.getType () );
		container.add ( aggregator );

		container.showWidget ( 0 );

		addTop ( Utils.getEmblemsCache ( "orders" ).getLegend () );
		addTop ( container );

		filters = new HorizontalPanel ();
		addTop ( filters );
		filters.add ( doFilterOptions () );
		filters.add ( doAggregationOptions () );
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

					Utils.getServer ().responseToObjects ( response );

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
		products_frame.setWidth ( "100%" );
		ver.setExtraWidget ( "products_preview_frame", products_frame );
		ver.add ( products_frame );

		container = new VerticalPanel ();
		container.setWidth ( "100%" );
		products_frame.add ( container );

		container.add ( new HTML ( "Qui di seguito, i prodotti che saranno contemplati nell'ordine.<br />Per cambiare la lista o gli attributi, intervenire nel pannello \"Prodotti\" del fornitore desiderato." ) );

		products = new FromServerTable ();
		products.setWidth ( "100%" );
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
			public void onClose ( FromServerRappresentationFull form ) {
				OrderSummary complete_list;

				complete_list = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
				if ( complete_list != null ) {
					if ( complete_list.saveContents () == true )
						form.getValue ().save ( null );
				}
			}
		} );
	}

	private int mailSummaryIndex ( FromServerForm form ) {
		FromServer obj;
		ArrayList orders;

		obj = form.getValue ();

		if ( obj.getType () == "OrderAggregate" ) {
			orders = obj.getArray ( "orders" );
			return orders.size ();
		}
		else {
			return 1;
		}
	}

	private void checkMailSummary ( FromServerForm form ) {
		HorizontalPanel container;
		Date sent;
		String text;
		FromServer ord;
		FromServerButton button;

		if ( Session.getGAS ().getBool ( "use_mail" ) == false )
			return;

		ord = form.getValue ();

		if ( ord.getInt ( "status" ) != Order.CLOSED )
			return;

		/*
			TODO	Ci sarebbe da inventarsi un modo di eliminare
				questa notifica nella ben rara situazione in cui
				un ordine viene riaperto
		*/

		container = ( HorizontalPanel ) form.retriveInternalWidget ( "mail_summary" );
		if ( container == null ) {
			container = new HorizontalPanel ();
			container.addStyleName ( "info-cell" );

			form.setExtraWidget ( "mail_summary", container );
			form.insert ( container, mailSummaryIndex ( form ) );

			text = "Questo ordine è stato chiuso. Clicca il bottone per inviare una mail di riscontro a coloro che hanno effettuato una prenotazione ed ancora non hanno ritirato la merce.";

			sent = ord.getDate ( "mail_summary_sent" );
			if ( sent != null )
				text += " L'ultima mail di riepilogo è stata inviata il " + Utils.printableDate ( sent );

			container.add ( new Label ( text ) );

			button = new FromServerButton ( ord, "Invia", new FromServerCallback () {
				public void execute ( final FromServer object ) {
					manageSummaryDialog ( object );
				}
			} );
			container.add ( button );
		}
	}

	private void manageSummaryDialog ( FromServer order ) {
		VerticalPanel panel;
		DummyTextArea text;
		FromServerSavingDialog dialog;

		dialog = new FromServerSavingDialog ( order, "Invia Riassunto dell'Ordine" );

		panel = new VerticalPanel ();

		panel.add ( new HTML ( "<p>Qui puoi aggiungere un messaggio da allegare alle mail di notifica. Ovviamente puoi anche lasciare in bianco il campo qui sotto.</p>" +
				"<p>Clicca 'Salva' per spedire le mail.</p>" ) );

		text = new DummyTextArea ();
		text.addStyleName ( "top-spaced" );
		panel.add ( dialog.getPersonalizedWidget ( "mail_summary_text", text ) );

		dialog.setWidget ( panel );

		dialog.setCallback ( new FromServerFormCallbacks () {
			public boolean onSave ( FromServerRappresentationFull form ) {
				FromServer obj;

				obj = form.getValue ();
				obj.setDate ( "mail_summary_sent", new Date ( System.currentTimeMillis () ) );
				return true;
			}

			public void onSaved ( FromServerRappresentationFull form ) {
				FromServerForm f;

				f = ( FromServerForm ) main.retrieveFormById ( form.getValue ().getLocalID () );
				f.removeWidget ( "mail_summary" );
			}
		} );

		dialog.show ();
	}

	private void checkNewProductsAvailability ( final FromServerForm form ) {
		String order_identifier;
		FromServer ord;

		ord = form.getValue ();

		if ( ord.getInt ( "status" ) != Order.OPENED )
			return;

		if ( ord instanceof OrderAggregate )
			order_identifier = ord.getLocalID () + "&aggregate=true";
		else
			order_identifier = Integer.toString ( ord.getLocalID () );

		Utils.getServer ().rawGet ( "data_shortcuts.php?type=order_products_diff&order=" + order_identifier, new RequestCallback () {
			public void onError ( Request request, Throwable exception ) {
				Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );
			}

			public void onResponseReceived ( Request request, Response response ) {
				int num;
				JSONValue jsonObject;
				JSONArray products;
				FlexTable container;
				FromServer tmp;

				if ( response.getText () == "" ) {
					Utils.getServer ().dataArrived ();
					return;
				}

				try {
					jsonObject = JSONParser.parseStrict ( response.getText () );

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
								FromServer o;
								FromServer prod;
								OrderSummary summary;

								if ( row == 0 || cell != 2 )
									return;

								tab = ( FlexTable ) sender;
								hidden = ( Hidden ) tab.getWidget ( row, 0 );
								prod = Utils.getServer ().getObjectFromCache ( "Product", Integer.parseInt ( hidden.getName () ) );

								order = form.getValue ();

								if ( order instanceof OrderAggregate )
									o = orderFromAggregateByProduct ( order, prod );
								else
									o = order;

								products = o.getArray ( "products" );
								products.add ( prod );
								o.setArray ( "products", products );

								summary = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
								summary.reFill ( order );

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

	private FromServer orderFromAggregateByProduct ( FromServer aggregate, FromServer product ) {
		ArrayList orders;
		FromServer ord;

		orders = aggregate.getArray ( "orders" );

		for ( int i = 0; i < orders.size (); i++ ) {
			ord = ( FromServer ) orders.get ( i );
			if ( ord.getObject ( "supplier" ).equals ( product.getObject ( "supplier" ) ) )
				return ord;
		}

		return null;
	}

	private FromServerForm editableOrder ( FromServer ord ) {
		final FromServerForm ver;
		Supplier supplier;
		OrderDetails details;

		if ( ord.getBool ( "parent_aggregate" ) == true && filter.isVisible () == false )
			return null;

		/*
			Nel pannello di aggregazione lo metto sempre, anche
			se l'utente non e' referente
		*/
		aggregator.addElement ( ord );

		supplier = ( Supplier ) ord.getObject ( "supplier" );
		if ( supplier.iAmReference () == false )
			return null;

		ver = new FromServerForm ( ord );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );

		/*
			L'icona di editing la aggiungo sempre sebbene sia sottinteso che
			l'ordine e' editabile (altrimenti manco apparirebbe nella lista), per
			coerenza con gli altri pannelli
		*/
		ver.emblems ().activate ( "multiuser" );
		ver.emblems ().activate ( "status", ord.getInt ( "status" ) );

		details = new OrderDetails ();
		details.wireToForm ( ver );
		details.setValue ( ord );
		ver.add ( details );

		addOrderDetails ( ver );
		addSaveProducts ( ver );

		if ( ord.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false )
			ver.setVisible ( false );

		return ver;
	}

	private FromServerForm editableOrderAggregate ( FromServer ord ) {
		ArrayList orders;
		FromServer order;
		OrderAggregate aggregate;
		FromServerForm ver;
		OrderDetails details;

		aggregate = ( OrderAggregate ) ord;
		if ( aggregate.iAmReference () == false )
			return null;

		ver = new FromServerForm ( ord );
		ver.emblemsAttach ( Utils.getEmblemsCache ( "orders" ) );
		ver.emblems ().activate ( "multiuser" );
		ver.emblems ().activate ( "aggregate" );
		ver.emblems ().activate ( "status", ord.getInt ( "status" ) );

		ver.setCallback ( new FromServerFormCallbacks () {
			public void onSaved ( FromServerRappresentationFull form ) {
				ArrayList children;
				FromServerForm ver;
				FromServerRappresentationFull child;

				ver = ( FromServerForm ) form;
				ver.emblems ().activate ( "status", form.getValue ().getInt ( "status" ) );

				children = form.getChildren ();

				for ( int i = 0; i < children.size (); i++ ) {
					child = ( FromServerRappresentationFull ) children.get ( i );
					child.savingObject ();
				}
			}
		} );

		orders = ord.getArray ( "orders" );

		for ( int i = 0; i < orders.size (); i++ ) {
			order = ( FromServer ) orders.get ( i );
			details = new OrderDetails ();
			details.setValue ( order );
			ver.add ( details );
			ver.addChild ( details );
		}

		addOrderDetails ( ver );
		addSaveProducts ( ver );

		if ( ord.getInt ( "status" ) == Order.SHIPPED && OrdersHub.checkShippedOrdersStatus () == false )
			ver.setVisible ( false );

		return ver;
	}

	private Widget doFilterOptions () {
		filter = new OrdersHubWidget () {
			public void doFilter ( boolean show, Date start, Date end, FromServer supplier ) {
				/*
					TODO	Questo e' identico all'omonima
						funzione in DeliveryPanel
				*/
				boolean visible;
				ArrayList forms;
				FromServerForm form;
				FromServer ord;

				forms = main.collectForms ();

				for ( int i = 0; i < forms.size (); i++ ) {
					form = ( FromServerForm ) forms.get ( i );
					ord = form.getValue ();

					if ( show == true ) {
						visible = false;

						if ( ord.getDate ( "startdate" ).after ( start ) && ord.getDate ( "enddate" ).before ( end ) ) {
							if ( supplier == null ) {
								visible = true;
							}
							else {
								if ( ord instanceof Order && ord.getObject ( "supplier" ).getLocalID () == supplier.getLocalID () )
									visible = true;
								else if ( ord instanceof OrderAggregate && ( ( OrderAggregate ) ord ).hasSupplier ( supplier ) )
									visible = true;
							}
						}
					}
					else {
						visible = ( ord.getInt ( "status" ) != Order.SHIPPED );
					}

					form.setVisible ( visible );
				}
			}
		};

		filter.addStatusListener ( new StatusListener () {
			public void onStatusChange ( Widget sender, boolean status ) {
				aggregateToggle.setVisible ( status == false );
			}
		} );

		return filter;
	}

	private Widget doAggregationOptions () {
		HorizontalPanel box;

		box = new HorizontalPanel ();
		box.setStyleName ( "orders-hub-widget" );
		box.setVerticalAlignment ( HasVerticalAlignment.ALIGN_MIDDLE );

		aggregateToggle = new CheckBox ( "Modalità Aggregazione Ordini" );
		box.add ( aggregateToggle );

		aggregateToggle.addClickHandler ( new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				CheckBox self;

				self = ( CheckBox ) event.getSource ();

				if ( self.getValue () == true ) {
					container.showWidget ( 1 );
					filter.setVisible ( false );
				}
				else {
					container.showWidget ( 0 );
					filter.setVisible ( true );
				}
			}
		} );

		return box;
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
		CaptionPanel sframe;
		OrderSummary complete_list;

		if ( form == null )
			return null;

		complete_list = ( OrderSummary ) form.retriveInternalWidget ( "summary" );
		if ( complete_list != null )
			return null;

		sframe = new CaptionPanel ( "Stato Ordini" );

		complete_list = new OrderSummary ();
		form.setExtraWidget ( "summary", complete_list );
		complete_list.reFill ( form.getValue () );
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
		Utils.getServer ().testObjectReceive ( "OrderAggregate" );
		Utils.getServer ().testObjectReceive ( "Order" );
		Utils.getServer ().testObjectReceive ( "Supplier" );
		filter.doFilter ();
	}
}
