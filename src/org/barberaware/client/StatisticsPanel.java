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

	private LinksDialog		usersFiles;
	private DateSelector		startDate;
	private DateSelector		endDate;
	private ColumnChart		graphByOrders;
	private PieChart		graphByPrices;
	private ColumnChart.Options	graphByOrdersOptions;
	private PieChart.Options	graphByPricesOptions;

	private LinksDialog		productsFiles;
	private FromServerSelector	supplier;
	private ColumnChart		graphByProduct;
	private ColumnChart.Options	graphByProductOptions;
	private ColumnChart		graphByProductValue;
	private ColumnChart.Options	graphByProductValueOptions;

	public StatisticsPanel () {
		super ();

		Date now;
		Date past;
		ChangeListener listener;
		HorizontalPanel hor;
		VerticalPanel ver;
		CaptionPanel frame;
		FlexTable input;

		main = new VerticalPanel ();
		addTop ( main );

		frame = new CaptionPanel ( "Report per Utenti/Fornitori" );
		main.add ( frame );

		ver = new VerticalPanel ();
		frame.setContentWidget ( ver );

		hor = new HorizontalPanel ();
		ver.add ( hor );

		input = new FlexTable ();
		hor.add ( input );

		listener = new ChangeListener () {
			public void onChange ( Widget sender ) {
				performUsersUpdate ();
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

		usersFiles = new LinksDialog ( "Scarica Statistiche" );
		input.setWidget ( 3, 0, usersFiles );
		input.getFlexCellFormatter ().setColSpan ( 3, 0, 2 );

		graphByPrices = new PieChart ();
		hor.add ( graphByPrices );
		graphByPricesOptions = PieChart.Options.create ();
		graphByPricesOptions.setWidth ( 600 );
		graphByPricesOptions.setHeight ( 240 );
		graphByPricesOptions.set3D ( true );
		graphByPricesOptions.setLegend ( LegendPosition.NONE );
		graphByPricesOptions.setTitle ( "Somme Totali Pagate (€)" );

		graphByOrders = new ColumnChart ();
		ver.add ( graphByOrders );
		graphByOrdersOptions = ColumnChart.Options.create ();
		graphByOrdersOptions.setWidth ( 800 );
		graphByOrdersOptions.setHeight ( 240 );
		graphByOrdersOptions.set3D ( true );
		graphByOrdersOptions.setLegend ( LegendPosition.NONE );
		graphByOrdersOptions.setTitle ( "Numero Utenti con almeno un Ordine" );

		frame = new CaptionPanel ( "Report per Prodotti/Fornitore" );
		main.add ( frame );

		ver = new VerticalPanel ();
		frame.setContentWidget ( ver );

		input = new FlexTable ();
		ver.add ( input );

		supplier = new FromServerSelector ( "Supplier", true, true );
		supplier.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				performProductsUpdate ();
			}
		} );
		input.setWidget ( 1, 0, new Label ( "Fornitore" ) );
		input.setWidget ( 1, 1, supplier );

		productsFiles = new LinksDialog ( "Scarica Statistiche" );
		input.setWidget ( 1, 2, productsFiles );

		graphByProduct = new ColumnChart ();
		ver.add ( graphByProduct );
		graphByProductOptions = ColumnChart.Options.create ();
		graphByProductOptions.setWidth ( 800 );
		graphByProductOptions.setHeight ( 240 );
		graphByProductOptions.set3D ( true );
		graphByProductOptions.setLegend ( LegendPosition.NONE );
		graphByProductOptions.setTitle ( "Numero di Utenti che hanno Ordinato i Prodotti" );

		graphByProductValue = new ColumnChart ();
		ver.add ( graphByProductValue );
		graphByProductValueOptions = ColumnChart.Options.create ();
		graphByProductValueOptions.setWidth ( 800 );
		graphByProductValueOptions.setHeight ( 240 );
		graphByProductValueOptions.set3D ( true );
		graphByProductValueOptions.setLegend ( LegendPosition.NONE );
		graphByProductValueOptions.setTitle ( "Valore dei Prodotti Ordinati" );
	}

	private void populateUsersGraph ( JSONArray array ) {
		int num_items;
		String supplier_name;
		JSONArray row;
		JSONString num;
		DataTable by_orders;
		DataTable by_price;

		num_items = array.size ();

		by_orders = DataTable.create ();
		by_orders.addColumn ( AbstractDataTable.ColumnType.STRING, "fornitore" );
		by_orders.addColumn ( AbstractDataTable.ColumnType.NUMBER, "utenti" );
		by_orders.addRows ( num_items );

		by_price = DataTable.create ();
		by_price.addColumn ( AbstractDataTable.ColumnType.STRING, "fornitore" );
		by_price.addColumn ( AbstractDataTable.ColumnType.NUMBER, "somma" );
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

	private void populateProductsGraph ( JSONArray array ) {
		int num_items;
		String product_name;
		JSONArray row;
		JSONString num;
		DataTable by_users;
		DataTable by_value;

		num_items = array.size ();

		by_users = DataTable.create ();
		by_users.addColumn ( AbstractDataTable.ColumnType.STRING, "prodotto" );
		by_users.addColumn ( AbstractDataTable.ColumnType.NUMBER, "utenti" );
		by_users.addRows ( num_items );

		by_value = DataTable.create ();
		by_value.addColumn ( AbstractDataTable.ColumnType.STRING, "prodotto" );
		by_value.addColumn ( AbstractDataTable.ColumnType.NUMBER, "valore" );
		by_value.addRows ( num_items );

		for ( int i = 0; i < num_items; i++ ) {
			row = array.get ( i ).isArray ();

			product_name = row.get ( 0 ).isString ().stringValue ();

			by_users.setValue ( i, 0, product_name );
			by_value.setValue ( i, 0, product_name );

			num = row.get ( 1 ).isString ();
			if ( num != null )
				by_users.setValue ( i, 1, Double.parseDouble ( num.stringValue () ) );

			num = row.get ( 2 ).isString ();
			if ( num != null )
				by_value.setValue ( i, 1, Double.parseDouble ( num.stringValue () ) );
		}

		graphByProduct.draw ( by_users, graphByProductOptions );
		graphByProductValue.draw ( by_value, graphByProductValueOptions );
	}

	private String linkTemplate ( String data_type, String document_type, int extra ) {
		return "graph_data.php?type=" + data_type + "&document=" + document_type + "&extra=" + extra + "&graph=0&startdate=" +
			Utils.encodeDate ( startDate.getValue () ) + "&enddate=" + Utils.encodeDate ( endDate.getValue () );
	}

	private void notifyError ( Request request, Throwable exception ) {
		if ( exception instanceof RequestTimeoutException )
			Utils.showNotification ( "Timeout sulla connessione: accertarsi che il server sia raggiungibile" );
		else
			Utils.showNotification ( "Errore sulla connessione: accertarsi che il server sia raggiungibile" );

		Utils.getServer ().dataArrived ();
	}

	private void updateUsersLinks () {
		usersFiles.emptyBox ();
		usersFiles.addLink ( "CVS", linkTemplate ( "users", "csv", -1 ) );
		usersFiles.addLink ( "PDF", linkTemplate ( "users", "pdf", -1 ) );
	}

	private void performUsersUpdate () {
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

		updateUsersLinks ();

		Utils.getServer ().rawGet ( linkTemplate ( "users", "visual", -1 ), new RequestCallback () {
			public void onError ( Request request, Throwable exception ) {
				notifyError ( request, exception );
			}

			public void onResponseReceived ( Request request, Response response ) {
				JSONValue jsonObject;
				JSONObject obj;

				try {
					jsonObject = JSONParser.parse ( response.getText () );
					obj = jsonObject.isObject ();
					populateUsersGraph ( obj.get ( "data" ).isArray () );
				}
				catch ( com.google.gwt.json.client.JSONException e ) {
					Utils.showNotification ( "Ricevuti dati invalidi dal server" );
				}

				Utils.getServer ().dataArrived ();
			}
		} );
	}

	private void updateProductsLinks ( FromServer supp ) {
		productsFiles.emptyBox ();
		productsFiles.addLink ( "CVS", linkTemplate ( "products", "csv", supp.getLocalID () ) );
		productsFiles.addLink ( "PDF", linkTemplate ( "products", "pdf", supp.getLocalID () ) );
	}

	private void performProductsUpdate () {
		FromServer supp;

		supp = supplier.getValue ();
		updateProductsLinks ( supp );

		Utils.getServer ().rawGet ( linkTemplate ( "products", "visual", supp.getLocalID () ), new RequestCallback () {
			public void onError ( Request request, Throwable exception ) {
				notifyError ( request, exception );
			}

			public void onResponseReceived ( Request request, Response response ) {
				JSONValue jsonObject;
				JSONObject obj;

				try {
					jsonObject = JSONParser.parse ( response.getText () );
					obj = jsonObject.isObject ();
					populateProductsGraph ( obj.get ( "data" ).isArray () );
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
		performUsersUpdate ();
		performProductsUpdate ();
	}
}
