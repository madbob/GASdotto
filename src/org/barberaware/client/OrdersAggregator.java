/* GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <bob4job@gmail.com>
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

import com.allen_sauer.gwt.dnd.client.*;
import com.allen_sauer.gwt.dnd.client.drop.*;

public class OrdersAggregator extends Composite implements FromServerArray {
	private class DraggableOrder extends HTML {
		public FromServer	currentOrder;

		public DraggableOrder ( PickupDragController controller, FromServer order ) {
			setOrder ( order );
			controller.makeDraggable ( this );
		}

		public void setOrder ( FromServer order ) {
			String string;
			Date d;

			string = "<p>" + order.getObject ( "supplier" ).getString ( "name" ) + "<br />" +
					Utils.printableDateShort ( order.getDate ( "startdate" ) ) + " / " +
					Utils.printableDateShort ( order.getDate ( "enddate" ) );

			d = order.getDate ( "shippingdate" );
			if ( d != null )
				string += " / " + Utils.printableDateShort ( d );

			string += "</p>";

			setHTML ( string );
			currentOrder = order;
		}
	}

	private abstract class DroppablePanel extends VerticalPanel {
		public void setEngaged ( boolean engaged ) {
			if ( engaged )
				addStyleName ( "drop-engage" );
			else
				removeStyleName ( "drop-engage" );
		}

		public void eatWidget ( Widget order ) {
			add ( ( DraggableOrder ) order );
		}
	}

	private class DroppableOrders extends DroppablePanel {
		private PickupDragController	dragControl;

		public DroppableOrders ( PickupDragController controller ) {
			setStyleName ( "order-aggregate-box" );
			dragController.registerDropController ( new DropControl ( this ) );
			dragControl = controller;
		}
	}

	private class DroppableAggregate extends DroppablePanel {
		public FromServer		currentAggregate;
		private DropControl		dropControl;
		private PickupDragController	dragControl;
		private DroppableOrders		mainOrders;

		public DroppableAggregate ( PickupDragController controller, DroppableOrders panel, FromServer element ) {
			ArrayList orders;
			FromServer order;
			PushButton delbutton;

			setStyleName ( "order-aggregate-box" );
			dragControl = controller;
			mainOrders = panel;

			delbutton = new PushButton ( new Image ( "images/mini_delete.png" ), new ClickListener () {
				public void onClick ( Widget sender ) {
					removeMyself ();
				}
			} );
			add ( delbutton );
			setCellHorizontalAlignment ( delbutton, HasHorizontalAlignment.ALIGN_RIGHT );

			dropControl = new DropControl ( this );
			dragController.registerDropController ( dropControl );

			currentAggregate = element;
		}

		public DraggableOrder retrieveOrder ( FromServer element ) {
			DraggableOrder ord;

			for ( int i = 1; i < getWidgetCount (); i++ ) {
				ord = ( DraggableOrder ) getWidget ( i );
				if ( ord.currentOrder.equals ( element ) )
					return ord;
			}

			return null;
		}

		public void unaggregateOrders () {
			int rows;
			DraggableOrder ord;

			rows = getWidgetCount ();

			for ( int i = 1; i < rows; i++ ) {
				ord = ( DraggableOrder ) getWidget ( 1 );
				ordersList.add ( ord );
			}
		}

		public void setAggregate ( FromServer aggregate ) {
			ArrayList orders;
			FromServer order;

			for ( int i = 1; i < getWidgetCount (); i++ )
				remove ( 1 );

			orders = aggregate.getArray ( "orders" );

			if ( orders != null ) {
				for ( int i = 0; i < orders.size (); i++ ) {
					order = ( FromServer ) orders.get ( i );
					add ( new DraggableOrder ( dragControl, order ) );
				}
			}

			currentAggregate = aggregate;
		}

		public void removeMyself () {
			dragController.unregisterDropController ( dropControl );
			unaggregateOrders ();
			setVisible ( false );
		}
	}

	private class DropControl extends SimpleDropController {
		private DroppablePanel		target;

		public DropControl ( DroppablePanel bin ) {
			super ( bin );
			target = bin;
		}

		public void onDrop ( DragContext context ) {
			for ( Widget widget : context.selectedWidgets )
				target.eatWidget ( widget );
			super.onDrop ( context );
		}

		public void onEnter ( DragContext context ) {
			super.onEnter ( context );
			target.setEngaged ( true );
		}

		public void onLeave ( DragContext context ) {
			target.setEngaged ( false );
			super.onLeave ( context );
		}
	}

	private AbsolutePanel		main;
	private DroppableOrders		ordersList;
	private VerticalPanel		aggregationsList;
	private PickupDragController	dragController;
	private ArrayList		changeListeners;

	public OrdersAggregator () {
		HTML notice;
		VerticalPanel container;
		HorizontalPanel columns;
		ButtonsBar buttons;
		AddButton button;

		changeListeners = null;

		main = new AbsolutePanel ();
		main.setStyleName ( "size-extended" );
		initWidget ( main );

		dragController = new PickupDragController ( main, false );

		container = new VerticalPanel ();
		container.setWidth ( "100%" );
		main.add ( container );

		notice = new HTML ( "<p>Da questo pannello puoi unire diversi ordini in agglomerati, in modo che siano gestiti uniformemente come un ordine singolo all'interno dell'interfaccia. Essi continueranno a restare tra loro indipendenti, le modifiche riguardano solo l'aspetto grafico.</p><p>A sinistra appaiono gli ordini non aggregati, mentre a destra i diversi gruppi. Clicca e trascina i singoli ordini da una colonna all'altra. Clicca su \"Nuovo Aggregato\" per creare un nuovo insieme. Quando hai finito, clicca l'icona verde in fondo.</p>" );
		notice.setStyleName ( "small-text" );
		notice.addStyleName ( "main-panel-explain" );
		container.add ( notice );

		button = new AddButton ( "Nuovo Aggregato", new ClickListener () {
			public void onClick ( Widget sender ) {
				aggregationsList.insert ( new DroppableAggregate ( dragController, ordersList, new OrderAggregate () ), 0 );
			}
		} );
		container.add ( button );

		columns = new HorizontalPanel ();
		columns.setWidth ( "100%" );
		container.add ( columns );

		ordersList = new DroppableOrders ( dragController );
		columns.add ( ordersList );
		columns.setCellWidth ( ordersList, "40%" );

		aggregationsList = new VerticalPanel ();
		aggregationsList.setWidth ( "100%" );
		columns.add ( aggregationsList );
		columns.setCellWidth ( aggregationsList, "40%" );

		buttons = doButtonsBar ();
		container.add ( buttons );
		container.setCellHorizontalAlignment ( buttons, HasHorizontalAlignment.ALIGN_RIGHT );
	}

	private ButtonsBar doButtonsBar () {
		ButtonsBar panel;
		PushButton button;

		panel = new ButtonsBar ();

		button = new PushButton ( new Image ( "images/cancel.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				DroppableAggregate aggr;

				for ( int i = 0; i < aggregationsList.getWidgetCount (); i++ ) {
					aggr = ( DroppableAggregate ) aggregationsList.getWidget ( i );
					if ( aggr.currentAggregate.isValid () == false ) {
						aggr.removeMyself ();
						i--;
					}
					else {
						aggr.setAggregate ( aggr.currentAggregate );
					}
				}

				/*
					TODO	Qui ci sarebbero anche da rimettere in ordine gli ordini che sono
						stati spostati tra le colonne
				*/

				emitClose ();
			}
		} );
		panel.add ( button, "Annulla" );

		button = new PushButton ( new Image ( "images/confirm.png" ), new ClickListener () {
			public void onClick ( Widget sender ) {
				int num_orders;
				ArrayList elements;
				OrderAggregate aggr;
				DraggableOrder ord;
				DroppableAggregate ordaggr;

				elements = getElements ();

				for ( int i = 0; i < elements.size (); i++ ) {
					aggr = ( OrderAggregate ) elements.get ( i );
					num_orders = aggr.getArray ( "orders" ).size ();

					if ( num_orders == 0 || num_orders == 1 ) {
						/*
							Se l'aggregato viene eliminato con l'apposita icona gli
							elementi nella pagina vengono rimessi in ordine in quella
							situazione, ma se invalido un aggregato perche' ha un solo
							elemento all'interno me ne accorgo qui e dunque qui devo
							risistemare tutto
						*/
						if ( num_orders == 1 ) {
							ordaggr = retrieveOrderAggregateBox ( aggr );
							ordaggr.removeMyself ();
						}

						if ( aggr.isValid () )
							aggr.destroy ( null );
					}
					else {
						aggr.save ( new ServerResponse () {
							public void onComplete ( JSONValue response ) {
								int id;
								ArrayList orders;
								FromServer aggregate;
								Order child_ord;

								id = Integer.parseInt ( response.isString ().stringValue () );
								aggregate = Utils.getServer ().getObjectFromCache ( "OrderAggregate", id );
								orders = aggregate.getArray ( "orders" );

								for ( int a = 0; a < orders.size (); a++ ) {
									child_ord = ( Order ) orders.get ( a );
									child_ord.setBool ( "parent_aggregate", true );
									child_ord.save ( null );
								}
							}
						} );
					}
				}

				for ( int i = 0; i < ordersList.getWidgetCount (); i++ ) {
					ord = ( DraggableOrder ) ordersList.getWidget ( i );

					if ( ord.currentOrder.getBool ( "parent_aggregate" ) == true ) {
						ord.currentOrder.setBool ( "parent_aggregate", false );
						ord.currentOrder.save ( null );
					}
				}

				emitClose ();
			}
		} );
		panel.add ( button, "Salva" );

		return panel;
	}

	public boolean hasAggregatedOrder ( FromServer order ) {
		return false;
	}

	public void addChangeListener ( ChangeListener listener ) {
		if ( changeListeners == null )
			changeListeners = new ArrayList ();

		changeListeners.add ( listener );
	}

	private int retrieveOrderInOrders ( FromServer element ) {
		DraggableOrder ord;

		for ( int i = 0; i < ordersList.getWidgetCount (); i++ ) {
			ord = ( DraggableOrder ) ordersList.getWidget ( i );
			if ( ord.currentOrder.equals ( element ) )
				return i;
		}

		return -1;
	}

	private DraggableOrder retrieveOrderBox ( FromServer element ) {
		int index;
		DraggableOrder ord;
		DroppableAggregate aggr;

		index = retrieveOrderInOrders ( element );

		if ( index != -1 ) {
			return ( DraggableOrder ) ordersList.getWidget ( index );
		}
		else {
			for ( int i = 0; i < aggregationsList.getWidgetCount (); i++ ) {
				aggr = ( DroppableAggregate ) aggregationsList.getWidget ( i );
				ord = aggr.retrieveOrder ( element );
				if ( ord != null )
					return ord;
			}
		}

		return null;
	}

	private DroppableAggregate retrieveOrderAggregateBox ( FromServer element ) {
		DroppableAggregate aggr;

		for ( int i = 0; i < aggregationsList.getWidgetCount (); i++ ) {
			aggr = ( DroppableAggregate ) aggregationsList.getWidget ( i );
			if ( aggr.currentAggregate.equals ( element ) )
				return aggr;
		}

		return null;
	}

	private void emitClose () {
		ChangeListener listener;

		for ( int i = 0; i < changeListeners.size (); i++ ) {
			listener = ( ChangeListener ) changeListeners.get ( i );
			listener.onChange ( this );
		}
	}

	/****************************************************************** FromServerArray */

	public void addElement ( FromServer element ) {
		ArrayList orders;
		FromServer order;
		DroppableAggregate aggr_box;
		DraggableOrder order_box;

		if ( element.getInt ( "status" ) == Order.SHIPPED )
			return;

		if ( element.getType () == "Order" ) {
			if ( element.getInt ( "status" ) == Order.OPENED && retrieveOrderBox ( element ) == null )
				ordersList.add ( new DraggableOrder ( dragController, element ) );
		}
		else {
			aggr_box = retrieveOrderAggregateBox ( element );
			if ( aggr_box == null ) {
				aggr_box = new DroppableAggregate ( dragController, ordersList, element );
				aggregationsList.add ( aggr_box );
			}

			orders = element.getArray ( "orders" );
			for ( int i = 0; i < orders.size (); i++ ) {
				order = ( FromServer ) orders.get ( i );

				order_box = retrieveOrderBox ( order );
				if ( order_box == null )
					order_box = new DraggableOrder ( dragController, order );

				aggr_box.add ( order_box );
			}
		}
	}

	public void setElements ( ArrayList elements ) {
		FromServer element;

		/*
			Piuttosto che eliminare tutti gli handlers dal controller, faccio prima a
			sostituirlo con uno nuovo
		*/
		dragController = new PickupDragController ( main, true );

		ordersList.clear ();
		aggregationsList.clear ();

		for ( int i = 0; i < elements.size (); i++ ) {
			element = ( FromServer ) elements.get ( i );
			addElement ( element );
		}
	}

	public void removeElement ( FromServer element ) {
		int index;
		int rows;
		DraggableOrder ord;
		DroppableAggregate aggr;

		if ( element.getType () == "Order" ) {
			ord = retrieveOrderBox ( element );
			if ( ord != null )
				ord.removeFromParent ();
		}
		else {
			aggr = retrieveOrderAggregateBox ( element );
			if ( aggr != null ) {
				rows = aggr.getWidgetCount ();

				for ( int i = 1; i < rows; i++ ) {
					ord = ( DraggableOrder ) aggr.getWidget ( 1 );
					ordersList.add ( ord );
				}

				aggr.removeMyself ();
			}
		}
	}

	public ArrayList getElements () {
		int rows;
		ArrayList ret;
		ArrayList children;
		DraggableOrder ord;
		DroppableAggregate aggr;

		ret = new ArrayList ();

		for ( int i = 0; i < aggregationsList.getWidgetCount (); i++ ) {
			aggr = ( DroppableAggregate ) aggregationsList.getWidget ( i );
			children = new ArrayList ();
			rows = aggr.getWidgetCount ();

			if ( rows == 1 ) {
				aggr.removeFromParent ();
			}
			else {
				for ( int a = 1; a < rows; a++ ) {
					ord = ( DraggableOrder ) aggr.getWidget ( a );
					children.add ( ord.currentOrder );
				}
			}

			aggr.currentAggregate.setArray ( "orders", children );
			ret.add ( aggr.currentAggregate );
		}

		return ret;
	}

	public void refreshElement ( FromServer element ) {
		DraggableOrder ord;
		DroppableAggregate aggr;

		if ( element.getType () == "Order" ) {
			ord = retrieveOrderBox ( element );
			if ( ord != null )
				ord.setOrder ( element );
			else
				addElement ( element );
		}
		else {
			aggr = retrieveOrderAggregateBox ( element );
			if ( aggr != null )
				aggr.setAggregate ( element );
		}
	}
}
