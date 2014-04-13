/*  GASdotto
 *  Copyright (C) 2009/2013 Roberto -MadBob- Guido <bob4job@gmail.com>
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

public class DeliverySummary extends Composite {
	private VerticalPanel		main;

	private String			identifier;
	private int			numOrders;
	private int []			statusOrder		= { 0, 2, 3, 1 };

	public DeliverySummary () {
		main = new VerticalPanel ();
		initWidget ( main );
		main.setWidth ( "100%" );

		identifier = Utils.randomString ();
		numOrders = 0;
		cleanUp ();
	}

	public void addOrder ( OrderUserInterface uorder ) {
		final FromServerForm row;
		FromServerForm existing;
		FromServer uord;
		FromServer user;
		FromServer prevmap;
		OrderUserAggregate aggregate;
		HorizontalPanel informations;
		CustomCaptionPanel frame;
		UserDetailsInOrder user_frame;
		ProductsDeliveryTable products;

		if ( numOrders == 0 && main.getWidgetCount () > 0 )
			main.remove ( 0 );

		uord = ( FromServer ) uorder;

		if ( uord.getRelatedInfo ( "DeliverySummary" ) != null )
			return;

		row = new FromServerForm ( uord, FromServerForm.NOT_EDITABLE );
		row.addStyleName ( "subform" );
		row.emblemsAttach ( Utils.getEmblemsCache ( "delivery" ) );

		/**
			TODO	Sarebbe assai meglio avere due modalita' per il pannello consegne, selezionabili in
				cima: una per la fase di "prezzatura" (quando si compilano i form e si salvano
				semplicemente) ed una per la fase di "consegna" vera e propria. Questo per mostrare
				solo i tasti funzione utili in quel dato contesto, anziche' la sfilza presente adesso
		*/

		row.addBottomButton ( "images/save.png", "Salva<br/>Informazioni", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				row.getValue ().setInt ( "status", OrderUser.SAVED );
				commonActionsOnEdit ( row );
			}
		} );

		row.addBottomButton ( "images/confirm.png", "Consegna<br/>Completata", new ClickHandler () {
			public void onClick ( ClickEvent event ) {
				boolean pass;
				float payed;
				float topay;
				ArrayList products;
				FromServer uorder;
				FromServer supp;
				FromServer payment;

				pass = true;
				payment = null;
				topay = 0;

				uorder = row.getCurrentValue ();
				products = uorder.getArray ( "allproducts" );

				/*
					TODO	In caso di ordini aggregati, se si apre il dialog per forzare il
						pagamento ma si clicca su Annulla, i BankMovement dei sotto-ordini
						vengono comunque settati e ricliccando su "Salva" risultano essere
						gia' pagati per la somma precedentemente settata come default
				*/

				if ( products != null && Session.getGAS ().getBool ( "use_bank" ) == true ) {
					topay = ProductUser.sumProductUserArray ( products, "delivered" );
					payment = uorder.getObject ( "payment_event" );

					if ( payment == null ) {
						pass = false;
					}
					else {
						payed = payment.getFloat ( "amount" );
						if ( payed == 0 || payed != topay )
							pass = false;
					}
				}

				if ( pass == true ) {
					row.getValue ().setInt ( "status", OrderUser.COMPLETE_DELIVERY );
					commonActionsOnEdit ( row );
				}
				else {
					BankMovementDialog dialog;

					dialog = new BankMovementDialog ();

					if ( uorder instanceof OrderUser ) {
						supp = uorder.getObject ( "baseorder" ).getObject ( "supplier" );
						dialog.setDefaultTargetUser ( uorder.getObject ( "baseuser" ) );
						dialog.setDefaultTargetSupplier ( supp );
						dialog.setDefaultNote ( "Pagamento ordine a " + supp.getString ( "name" ) );
					}

					/*
						Questo viene forzato per limitare casini nella prima fase di
						introduzione della funzione di Gestione Cassa, sara' prossimamente da
						eliminare
					*/
					dialog.setDefaultMethod ( BankMovement.BY_CASH );

					dialog.setDefaultAmount ( topay );
					dialog.showCro ( false );
					dialog.setDefaultType ( BankMovement.ORDER_USER_PAYMENT );
					dialog.setDefaultDate ( new Date ( System.currentTimeMillis () ) );
					dialog.setEditable ( false );

					dialog.addCallback ( new SavingDialogCallback () {
						public void onSave ( SavingDialog dialog ) {
							BankMovementCellViewer payment;

							/*
								Assegno il BankMovement appena creato al widget che, nel form,
								rappresenta il movimento dell'ordine in oggetto. Verra' poi
								assegnato all'ordine effettivo in fase di ricostruzione dello
								stesso per il salvataggio
							*/
							payment = ( BankMovementCellViewer ) row.retriveInternalWidget ( "payment_event" );
							payment.setValue ( ( ( BankMovementComponent ) dialog ).getValue () );

							row.getValue ().setInt ( "status", OrderUser.COMPLETE_DELIVERY );
							commonActionsOnEdit ( row );
						}
					} );

					dialog.setValue ( payment );
					dialog.show ();
				}
			}
		} );

		informations = new HorizontalPanel ();
		informations.setWidth ( "100%" );
		row.add ( informations );

		/* utente */

		user = uord.getObject ( "baseuser" );

		user_frame = new UserDetailsInOrder ( user );
		informations.add ( user_frame );
		informations.setCellWidth ( user_frame, "50%" );

		/* ordine */

		frame = new CustomCaptionPanel ( "Informazioni Ordine" );
		informations.add ( frame );
		informations.setCellWidth ( frame, "50%" );

		frame.addPair ( "Ultima modifica", row.getPersonalizedWidget ( "deliverydate", new DateViewer () ) );
		frame.addPair ( "Effettuata da", row.getPersonalizedWidget ( "deliveryperson", new NameLabelWidget () ) );

		if ( Session.getGAS ().getBool ( "use_bank" ) == true )
			frame.addPair ( "Pagamento", row.getPersonalizedWidget ( "payment_event", new BankMovementCellViewer () ) );

		/* prodotti */

		if ( uorder instanceof OrderUser ) {
			products = new ProductsDeliveryTable ();
			products.setValue ( ( OrderUser ) uorder );
			row.add ( products );
			row.addChild ( products );
		}
		else if ( uorder instanceof OrderUserAggregate ) {
			addAllSubOrders ( uord, row );
		}

		row.setCallback ( new FromServerFormCallbacks () {
			public String getName ( FromServerRappresentationFull form ) {
				return form.getValue ().getObject ( "baseuser" ).getString ( "name" );
			}
		} );

		/*
			Queste callback servono nel caso un sotto-ordine venga aggiunto o rimosso successivamente
			dall'aggregato corrente. Quelli che gia' esistono all'atto della creazione del pannello sono
			gestiti in addAllSubOrders()
		*/
		row.setCallback ( new FromServerRappresentationCallbacks () {
			public FromServerRappresentation onAddChild ( FromServerRappresentation form, FromServer child ) {
				float tot;
				ProductsDeliveryTable details;
				ProductsDeliveryTable iter;
				FromServerForm f;

				details = new ProductsDeliveryTable ();
				details.setValue ( child );
				child.addRelatedInfo ( "DeliverySummary", details );

				f = ( FromServerForm ) form;

				if ( f.getChildren ().size () > 0 ) {
					tot = currentTotalInForm ( f ) + ( ( OrderUser ) child ).getDeliveredPriceWithFriends ( false );
					addGranTotal ( f, tot );
					f.insert ( details, f.getWidgetCount () - 3 );
				}
				else {
					f.add ( details );
				}

				attachPrivateTotalCallback ( details );
				return details;
			}

			public boolean onRemoveChild ( FromServerRappresentation form ) {
				form.removeFromParent ();
				return true;
			}
		} );

		checkPay ( user, row );

		setStatusIcon ( row );
		setCreditIcon ( row );
		main.insert ( row, getSortedIndex ( uord ) );
		numOrders += 1;

		user.addRelatedInfo ( "DeliverySummary" + identifier, row );
		uord.addRelatedInfo ( "DeliverySummary", row );
	}

	public void modOrder ( OrderUserInterface uorder ) {
		FromServer uord;
		FromServerRappresentation widget;
		FromServerForm form;

		uord = ( FromServer ) uorder;

		widget = ( FromServerRappresentation ) uord.getRelatedInfo ( "DeliverySummary" );
		if ( widget != null ) {
			widget.setValue ( uord );

			if ( widget instanceof FromServerForm ) {
				form = ( FromServerForm ) widget;
				main.insert ( form, getSortedIndex ( uord ) );
				setStatusIcon ( form );
			}
		}
	}

	public void delOrder ( OrderUserInterface uorder ) {
		FromServer uord;
		FromServerRappresentation widget;
		FromServerRappresentation form;

		uord = ( FromServer ) uorder;
		form = null;

		widget = ( FromServerRappresentation ) uord.getRelatedInfo ( "DeliverySummary" );
		if ( widget != null ) {
			if ( widget instanceof FromServerForm ) {
				numOrders -= 1;
				cleanUp ();
			}
			else {
				form = widget.getRappresentationParent ();
			}

			widget.invalidate ();
			uord.delRelatedInfo ( "DeliverySummary" );

			if ( form != null && form.getChildren ().size () == 0 ) {
				form.invalidate ();
				numOrders -= 1;
				cleanUp ();
			}
		}
	}

	public void delUser ( FromServer user ) {
		FromServerForm form;

		form = ( FromServerForm ) user.getRelatedInfo ( "DeliverySummary" + identifier );
		if ( form != null ) {
			form.invalidate ();
			user.delRelatedInfo ( "DeliverySummary" + identifier );
		}
	}

	private float currentTotalInForm ( FromServerRappresentationFull form ) {
		float tot;
		ArrayList children;
		ProductsDeliveryTable iter;

		tot = 0;
		children = form.getChildren ();

		for ( int i = 0; i < children.size (); i++ ) {
			iter = ( ProductsDeliveryTable ) children.get ( i );
			tot += iter.getTotal ();
		}

		return tot;
	}

	private void addAllSubOrders ( FromServer uorder, FromServerForm row ) {
		float tot;
		ArrayList uorders;
		FromServer uord;
		ProductsDeliveryTable table;

		tot = 0;
		uorders = uorder.getArray ( "orders" );

		for ( int i = 0; i < uorders.size (); i++ ) {
			uord = ( FromServer ) uorders.get ( i );

			table = new ProductsDeliveryTable ();
			table.setValue ( uord );
			table.addStyleName ( "bottom-spaced" );

			tot += table.getTotal ();

			row.add ( table );
			row.addChild ( table );

			attachPrivateTotalCallback ( table );
			uord.addRelatedInfo ( "DeliverySummary", table );
		}

		if ( uorders.size () > 1 )
			addGranTotal ( row, tot );
	}

	/*
		Questo viene usato solo in caso di ordini aggregati, permette
		di visualizzare il totale complessivo di tutti gli ordini
	*/
	private void addGranTotal ( FromServerForm row, float tot ) {
		Widget ext;
		Label label;
		PriceViewer total;
		HorizontalPanel bottom;

		ext = row.retriveInternalWidget ( "total" );

		if ( ext == null ) {
			row.add ( new HTML ( "<hr>" ) );

			bottom = new HorizontalPanel ();
			bottom.setWidth ( "100%" );
			row.add ( bottom );

			label = new Label ( "" );
			bottom.add ( label );
			bottom.setCellWidth ( label, "50%" );

			label = new Label ( "Totale Complessivo" );
			bottom.add ( label );
			bottom.setCellWidth ( label, "10%" );
			bottom.setCellHorizontalAlignment ( label, HasHorizontalAlignment.ALIGN_LEFT );
			bottom.setCellVerticalAlignment ( label, HasVerticalAlignment.ALIGN_MIDDLE );

			total = new PriceViewer ();
			bottom.add ( total );
			bottom.setCellWidth ( total, "25%" );
			bottom.setCellHorizontalAlignment ( total, HasHorizontalAlignment.ALIGN_LEFT );
			row.setExtraWidget ( "total", total );
			total.setStyleName ( "bigger-text" );

			row.setCallback ( new FromServerFormCallbacks () {
				/*
					Per sistemare il valore del totale complessivo quando le modifiche nel
					FromServerForm sono annullate
				*/
				public void onReset ( FromServerRappresentationFull form ) {
					PriceViewer total;

					total = ( PriceViewer ) form.retriveInternalWidget ( "total" );
					total.setVal ( currentTotalInForm ( form ) );
				}

				/*
					Questo e' perche' altrimenti l'implementazione di default sovrascrive la
					callback esatta (implementata in addOrder())
				*/
				public String getName ( FromServerRappresentationFull form ) {
					return null;
				}
			} );
		}
		else {
			total = ( PriceViewer ) ext;
		}

		total.setVal ( tot );
	}

	private void attachPrivateTotalCallback ( ProductsDeliveryTable details ) {
		/*
			Reminder: qui vanno tutte le operazioni che necessitano
			del totale dell'ordine completo, poiche' in questo
			pannello ce ne possono essere diversi (nel caso di
			ordini aggregati)
		*/

		details.addDomHandler ( new ChangeHandler () {
			public void onChange ( ChangeEvent event ) {
				float tot;
				ArrayList children;
				Widget tmp;
				FromServerRappresentation parent;
				ProductsDeliveryTable det;
				PriceViewer view;

				tot = 0;
				det = ( ProductsDeliveryTable ) event.getSource ();
				parent = det.getRappresentationParent ();

				tmp = parent.retriveInternalWidget ( "total" );
				if ( tmp == null )
					return;

				children = parent.getChildren ();

				for ( int i = 0; i < children.size (); i++ ) {
					det = ( ProductsDeliveryTable ) children.get ( i );
					tot += det.getTotal ();
				}

				view = ( PriceViewer ) tmp;
				view.setVal ( tot );
			}
		}, ChangeEvent.getType () );
	}

	private void commonActionsOnEdit ( FromServerForm row ) {
		DateViewer deliverydate;
		NameLabelWidget deliveryperson;
		FromServer uorder;

		uorder = row.getValue ();

		deliverydate = ( DateViewer ) row.retriveInternalWidget ( "deliverydate" );
		deliverydate.setValue ( new Date ( System.currentTimeMillis () ) );

		deliveryperson = ( NameLabelWidget ) row.retriveInternalWidget ( "deliveryperson" );
		deliveryperson.setValue ( Session.getUser () );

		main.insert ( row, getSortedIndex ( uorder ) );
		row.savingObject ();
		row.open ( false );
		setStatusIcon ( row );
	}

	private void setStatusIcon ( FromServerForm form ) {
		EmblemsBar bar;

		bar = form.emblems ();
		bar.activate ( "status", form.getValue ().getInt ( "status" ) );
	}

	private void setCreditIcon ( FromServerForm form ) {
		if ( Session.getGAS ().getBool ( "use_bank" ) == false )
			return;

		float balance;
		float price;
		EmblemsBar bar;
		FromServer user;
		FromServer order;
		FromServer deposit;

		bar = form.emblems ();
		order = form.getValue ();
		user = order.getObject ( "baseuser" );
		price = ( ( OrderUserInterface ) order ).getTotalPriceWithFriends ();
		balance = user.getFloat ( "current_balance" );

		if ( order.getInt ( "status" ) == OrderUser.COMPLETE_DELIVERY ) {
			bar.activate ( "credit", 0 );
		}
		else if ( balance >= price ) {
			bar.activate ( "credit", 1 );
		}
		else {
			deposit = user.getObject ( "deposit" );

			if ( deposit != null && ( ( deposit.getFloat ( "amount" ) + balance ) >= price ) )
				bar.activate ( "credit", 2 );
			else
				bar.activate ( "credit", 3 );
		}
	}

	private int getSortedIndex ( FromServer order ) {
		int i;
		int status_iter;
		int status_to_place;
		FromServerForm row;
		FromServer u_iter;
		FromServer o_iter;
		String name_iter;
		String name_to_place;

		name_to_place = order.getObject ( "baseuser" ).getString ( "name" );
		status_to_place = order.getInt ( "status" );

		for ( i = 0; i < main.getWidgetCount (); i++ ) {
			row = ( FromServerForm ) main.getWidget ( i );

			o_iter = row.getValue ();
			if ( o_iter == null )
				continue;

			status_iter = o_iter.getInt ( "status" );

			if ( statusOrder [ status_iter ] > statusOrder [ status_to_place ] ) {
				return i;
			}
			else if ( statusOrder [ status_iter ] == statusOrder [ status_to_place ] ) {
				u_iter = o_iter.getObject ( "baseuser" );
				name_iter = u_iter.getString ( "name" );

				if ( name_iter.compareTo ( name_to_place ) > 0 )
					return i;
			}
		}

		return i;
	}

	private void checkPay ( FromServer u, FromServerForm form ) {
		if ( Session.getGAS ().getBool ( "payments" ) == true ) {
			User user;
			user = ( User ) u;
			user.checkUserPaying ( form );
		}
	}

	private void cleanUp () {
		if ( numOrders < 0 )
			numOrders = 0;

		if ( numOrders == 0 )
			main.add ( new Label ( "Non sono stati avanzati ordini" ) );
	}
}

