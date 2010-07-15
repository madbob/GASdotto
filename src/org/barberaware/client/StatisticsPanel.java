/*  GASdotto
 *  Copyright (C) 2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;
import com.google.gwt.visualization.client.*;
import com.google.gwt.visualization.client.visualizations.*;

import com.allen_sauer.gwt.log.client.Log;

public class StatisticsPanel extends GenericPanel {
	private VerticalPanel		main;
	private LinksDialog		files;
	private DateSelector		startDate;
	private DateSelector		endDate;
	private PieChart		graphByOrders;
	private PieChart		graphByPrices;
	private PieChart.Options	graphByOrdersOptions;
	private PieChart.Options	graphByPricesOptions;

	public StatisticsPanel () {
		super ();

		Date now;
		Date past;
		ChangeListener listener;
		HorizontalPanel hor;
		CaptionPanel frame;
		FlexTable input;

		main = new VerticalPanel ();
		addTop ( main );

		hor = new HorizontalPanel ();
		main.add ( hor );

		frame = new CaptionPanel ( "Report per Utenti/Fornitori" );
		hor.add ( frame );

		input = new FlexTable ();
		frame.setContentWidget ( input );

		listener = new ChangeListener () {
			public void onChange ( Widget sender ) {
				performUpdate ();
			}
		};

		now = new Date ( System.currentTimeMillis () );
		endDate = new DateSelector ();
		endDate.setValue ( now );

		now = ( Date ) now.clone ();
		now.setYear ( now.getYear () - 1 );
		startDate = new DateSelector ();
		startDate.setValue ( now );

		startDate.addChangeListener ( listener );
		input.setWidget ( 1, 0, new Label ( "Dal" ) );
		input.setWidget ( 1, 1, startDate );

		endDate.addChangeListener ( listener );
		input.setWidget ( 2, 0, new Label ( "Al" ) );
		input.setWidget ( 2, 1, endDate );

		files = new LinksDialog ( "Scarica Statistiche" );
		input.setWidget ( 3, 0, files );
		input.getFlexCellFormatter ().setColSpan ( 3, 0, 2 );

		graphByOrders = new PieChart ();
		hor.add ( graphByOrders );
		graphByOrdersOptions = PieChart.Options.create ();
		graphByOrdersOptions.setWidth ( 300 );
		graphByOrdersOptions.setHeight ( 240 );
		graphByOrdersOptions.set3D ( true );
		graphByOrdersOptions.setLegend ( LegendPosition.NONE );
		graphByOrdersOptions.setTitle ( "Numero Utenti con almeno un Ordine" );

		graphByPrices = new PieChart ();
		hor.add ( graphByPrices );
		graphByPricesOptions = PieChart.Options.create ();
		graphByPricesOptions.setWidth ( 300 );
		graphByPricesOptions.setHeight ( 240 );
		graphByPricesOptions.set3D ( true );
		graphByPricesOptions.setLegend ( LegendPosition.NONE );
		graphByPricesOptions.setTitle ( "Somme Totali Pagate (€)" );
	}

	private void populateGraph ( JSONArray array ) {
		int num_items;
		String supplier_name;
		JSONArray row;
		JSONString num;
		DataTable by_orders;
		DataTable by_price;

		num_items = array.size ();

		by_orders = DataTable.create ();
		by_orders.addColumn ( AbstractDataTable.ColumnType.STRING, "Fornitore" );
		by_orders.addColumn ( AbstractDataTable.ColumnType.NUMBER, "Utenti che hanno ordinato" );
		by_orders.addRows ( num_items );

		by_price = DataTable.create ();
		by_price.addColumn ( AbstractDataTable.ColumnType.STRING, "Fornitore" );
		by_price.addColumn ( AbstractDataTable.ColumnType.NUMBER, "Somma totale (€)" );
		by_price.addRows ( num_items );

		for ( int i = 0; i < num_items; i++ ) {
			row = array.get ( i ).isArray ();

			supplier_name = row.get ( 0 ).isString ().stringValue ();

			by_orders.setValue ( i, 0, supplier_name );
			by_price.setValue ( i, 0, supplier_name );

			num = row.get ( 1 ).isString ();
			if ( num != null )
				by_orders.setValue ( i, 1, Double.parseDouble ( num.stringValue () ) );

			num = row.get ( 2 ).isString ();
			if ( num != null )
				by_price.setValue ( i, 1, Double.parseDouble ( num.stringValue () ) );
		}

		graphByOrders.draw ( by_orders, graphByOrdersOptions );
		graphByPrices.draw ( by_price, graphByPricesOptions );
	}

	private String linkTemplate ( String document_type ) {
		return "graph_data.php?document=" + document_type + "&graph=0&startdate=" + Utils.encodeDate ( startDate.getValue () ) + "&enddate=" + Utils.encodeDate ( endDate.getValue () );
	}

	private void updateLinks () {
		files.emptyBox ();
		files.addLink ( "CVS", linkTemplate ( "csv" ) );
		files.addLink ( "PDF", linkTemplate ( "pdf" ) );
	}

	private void performUpdate () {
		Date s;
		Date e;

		s = startDate.getValue ();
		e = endDate.getValue ();
		if ( s.after ( e ) ) {
			Utils.showNotification ( "La data di partenza è posteriore alla data di fine selezione" );
			Utils.graphicPulseWidget ( startDate );
			Utils.graphicPulseWidget ( endDate );
			return;
		}

		updateLinks ();

		Utils.getServer ().rawGet ( linkTemplate ( "visual" ), new RequestCallback () {
			public void onError ( Request request, Throwable exception ) {
				if ( exception instanceof RequestTimeoutException )
					Utils.showNotification ( "Timeout sulla connessione: accertarsi che il server sia raggiungibile" );
				else
					Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );

				Utils.getServer ().dataArrived ();
			}

			public void onResponseReceived ( Request request, Response response ) {
				JSONValue jsonObject;
				JSONObject obj;

				try {
					jsonObject = JSONParser.parse ( response.getText () );
					obj = jsonObject.isObject ();
					populateGraph ( obj.get ( "data" ).isArray () );
				}
				catch ( com.google.gwt.json.client.JSONException e ) {
					Utils.showNotification ( "Ricevuti dati invalidi dal server" );
				}

				Utils.getServer ().dataArrived ();
			}
		} );
	}

	/****************************************************************** GenericPanel */

	public String getName () {
		return "Statistiche";
	}

	public String getSystemID () {
		return "statistics";
	}

	public Image getIcon () {
		return new Image ( "images/path_stats.png" );
	}

	public void initView () {
		performUpdate ();
	}
}
