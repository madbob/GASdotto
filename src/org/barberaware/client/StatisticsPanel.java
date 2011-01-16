/*  GASdotto
 *  Copyright (C) 2010/2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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
import com.google.gwt.core.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.json.client.*;
import com.google.gwt.visualization.client.*;
import com.google.gwt.visualization.client.Properties;
import com.google.gwt.visualization.client.visualizations.*;
import com.google.gwt.visualization.client.events.*;

import com.allen_sauer.gwt.log.client.Log;

public class StatisticsPanel extends GenericPanel {
	private VerticalPanel		main;

	private LinksDialog		usersFiles;
	private DateSelector		supplierStartDate;
	private DateSelector		supplierEndDate;
	private PieChart		graphByPrices;
	private PieChart.Options	graphByPricesOptions;
	private ColumnChart		graphByOrders;
	private ColumnChart.Options	graphByOrdersOptions;
	private DataTable		supplierData;
	private VerticalPanel		supplierDetails;

	private LinksDialog		productsFiles;
	private DateSelector		productStartDate;
	private DateSelector		productEndDate;
	private FromServerSelector	supplier;
	private ColumnChart		graphByProduct;
	private ColumnChart.Options	graphByProductOptions;
	private PieChart		graphByProductValue;
	private PieChart.Options	graphByProductValueOptions;
	private DataTable		productData;
	private VerticalPanel		productDetails;

	public StatisticsPanel () {
		super ();

		Date now;
		Date past;
		ChangeListener listener;
		HorizontalPanel hor;
		FlexTable layout;
		FlexTable.FlexCellFormatter formatter;
		CaptionPanel frame;
		FlexTable input;

		main = new VerticalPanel ();
		addTop ( main );

		frame = new CaptionPanel ( "Report per Utenti/Fornitori" );
		main.add ( frame );

		layout = new FlexTable ();
		formatter = layout.getFlexCellFormatter ();
		frame.setContentWidget ( layout );

		input = new FlexTable ();
		layout.setWidget ( 0, 0, input );

		listener = new ChangeListener () {
			public void onChange ( Widget sender ) {
				performUsersUpdate ();
			}
		};

		now = new Date ( System.currentTimeMillis () );
		supplierEndDate = new DateSelector ();
		supplierEndDate.setValue ( now );

		now = ( Date ) now.clone ();
		now.setYear ( now.getYear () - 1 );
		supplierStartDate = new DateSelector ();
		supplierStartDate.setValue ( now );

		supplierStartDate.addChangeListener ( listener );
		input.setWidget ( 0, 0, new Label ( "Dal" ) );
		input.setWidget ( 0, 1, supplierStartDate );

		supplierEndDate.addChangeListener ( listener );
		input.setWidget ( 1, 0, new Label ( "Al" ) );
		input.setWidget ( 1, 1, supplierEndDate );

		usersFiles = new LinksDialog ( "Scarica Statistiche" );
		input.setWidget ( 2, 0, usersFiles );
		input.getFlexCellFormatter ().setColSpan ( 2, 0, 2 );

		graphByPrices = new PieChart ();
		layout.setWidget ( 0, 1, graphByPrices );
		graphByPricesOptions = PieChart.Options.create ();
		graphByPricesOptions.setWidth ( 500 );
		graphByPricesOptions.setHeight ( 240 );
		graphByPricesOptions.set3D ( true );
		graphByPricesOptions.setLegend ( LegendPosition.NONE );
		graphByPricesOptions.setTitle ( "Somme Totali Pagate (€)" );

		graphByOrders = new ColumnChart ();
		layout.setWidget ( 1, 0, graphByOrders );
		formatter.setColSpan ( 1, 0, 2 );
		graphByOrdersOptions = ColumnChart.Options.create ();
		graphByOrdersOptions.setWidth ( 800 );
		graphByOrdersOptions.setHeight ( 240 );
		graphByOrdersOptions.set3D ( true );
		graphByOrdersOptions.setLegend ( LegendPosition.NONE );
		graphByOrdersOptions.setTitle ( "Numero Utenti con almeno un Ordine" );

		supplierDetails = new VerticalPanel ();
		supplierDetails.setStyleName ( "info-cell" );
		layout.setWidget ( 0, 2, supplierDetails );
		formatter.setRowSpan ( 0, 2, 2 );
		formatter.setVerticalAlignment ( 0, 2, HasVerticalAlignment.ALIGN_MIDDLE );
		supplierDetails.add ( new HTML ( "Clicca su un fornitore<br />per visualizzare qui i dettagli" ) );

		graphByPrices.addSelectHandler ( new SelectHandler () {
			public void onSelect ( SelectHandler.SelectEvent event ) {
				Selection sel;
				JsArray arr;

				arr = graphByPrices.getSelections ();
				sel = ( Selection ) arr.get ( 0 );
				graphByOrders.setSelections ( arr );
				detailsSupplier ( sel.getRow () );
			}
		} );

		graphByOrders.addSelectHandler ( new SelectHandler () {
			public void onSelect ( SelectHandler.SelectEvent event ) {
				Selection sel;
				JsArray arr;

				arr = graphByOrders.getSelections ();
				sel = ( Selection ) arr.get ( 0 );
				graphByPrices.setSelections ( arr );
				detailsSupplier ( sel.getRow () );
			}
		} );

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		frame = new CaptionPanel ( "Report per Prodotti/Fornitore" );
		main.add ( frame );

		layout = new FlexTable ();
		formatter = layout.getFlexCellFormatter ();
		frame.setContentWidget ( layout );

		hor = new HorizontalPanel ();
		layout.setWidget ( 0, 0, hor );
		formatter.setColSpan ( 0, 0, 2 );

		supplier = new FromServerSelector ( "Supplier", true, true, false );
		supplier.addChangeListener ( new ChangeListener () {
			public void onChange ( Widget sender ) {
				performProductsUpdate ();
			}
		} );
		hor.add ( new Label ( "Fornitore" ) );
		hor.add ( supplier );

		input = new FlexTable ();
		layout.setWidget ( 1, 0, input );

		now = new Date ( System.currentTimeMillis () );
		productEndDate = new DateSelector ();
		productEndDate.setValue ( now );

		listener = new ChangeListener () {
			public void onChange ( Widget sender ) {
				performProductsUpdate ();
			}
		};

		now = ( Date ) now.clone ();
		now.setYear ( now.getYear () - 1 );
		productStartDate = new DateSelector ();
		productStartDate.setValue ( now );

		productStartDate.addChangeListener ( listener );
		input.setWidget ( 0, 0, new Label ( "Dal" ) );
		input.setWidget ( 0, 1, productStartDate );

		productEndDate.addChangeListener ( listener );
		input.setWidget ( 1, 0, new Label ( "Al" ) );
		input.setWidget ( 1, 1, productEndDate );

		productsFiles = new LinksDialog ( "Scarica Statistiche" );
		input.setWidget ( 2, 0, productsFiles );
		input.getFlexCellFormatter ().setColSpan ( 2, 0, 2 );

		graphByProductValue = new PieChart ();
		layout.setWidget ( 1, 1, graphByProductValue );
		graphByProductValueOptions = PieChart.Options.create ();
		graphByProductValueOptions.setWidth ( 500 );
		graphByProductValueOptions.setHeight ( 240 );
		graphByProductValueOptions.set3D ( true );
		graphByProductValueOptions.setLegend ( LegendPosition.NONE );
		graphByProductValueOptions.setTitle ( "Valore dei Prodotti Ordinati (€)" );

		graphByProduct = new ColumnChart ();
		layout.setWidget ( 2, 0, graphByProduct );
		formatter.setColSpan ( 2, 0, 2 );
		graphByProductOptions = ColumnChart.Options.create ();
		graphByProductOptions.setWidth ( 800 );
		graphByProductOptions.setHeight ( 240 );
		graphByProductOptions.set3D ( true );
		graphByProductOptions.setLegend ( LegendPosition.NONE );
		graphByProductOptions.setTitle ( "Numero di Utenti che hanno Ordinato i Prodotti" );

		productDetails = new VerticalPanel ();
		productDetails.setStyleName ( "info-cell" );
		layout.setWidget ( 0, 2, productDetails );
		formatter.setRowSpan ( 0, 2, 3 );
		formatter.setVerticalAlignment ( 0, 2, HasVerticalAlignment.ALIGN_MIDDLE );
		productDetails.add ( new HTML ( "Clicca su un prodotto<br />per visualizzare qui i dettagli" ) );

		graphByProduct.addSelectHandler ( new SelectHandler () {
			public void onSelect ( SelectHandler.SelectEvent event ) {
				Selection sel;
				JsArray arr;

				arr = graphByProduct.getSelections ();
				sel = ( Selection ) arr.get ( 0 );
				graphByProductValue.setSelections ( arr );
				detailsProduct ( sel.getRow () );
			}
		} );

		graphByProductValue.addSelectHandler ( new SelectHandler () {
			public void onSelect ( SelectHandler.SelectEvent event ) {
				Selection sel;
				JsArray arr;

				arr = graphByProductValue.getSelections ();
				sel = ( Selection ) arr.get ( 0 );
				graphByProduct.setSelections ( arr );
				detailsProduct ( sel.getRow () );
			}
		} );
	}

	private void detailsSupplier ( int index ) {
		Label lab;

		if ( supplierDetails.getWidgetCount () == 1 ) {
			supplierDetails.remove ( 0 );

			for ( int i = 0; i < 3; i++ ) {
				lab = new Label ();
				supplierDetails.add ( lab );
				lab.setWordWrap ( false );
			}
		}

		lab = ( Label ) supplierDetails.getWidget ( 0 );
		lab.setText ( "Fornitore: " + supplierData.getValueString ( index, 0 ) );
		lab = ( Label ) supplierDetails.getWidget ( 1 );
		lab.setText ( "Importo versato: " + Double.toString ( supplierData.getValueDouble ( index, 1 ) ) + " €" );
		lab = ( Label ) supplierDetails.getWidget ( 2 );
		lab.setText ( "Utenti con almeno un ordine: " + Double.toString ( supplierData.getValueDouble ( index, 2 ) ) );
	}

	private void detailsProduct ( int index ) {
		Label lab;

		if ( productDetails.getWidgetCount () == 1 ) {
			productDetails.remove ( 0 );

			for ( int i = 0; i < 3; i++ ) {
				lab = new Label ();
				productDetails.add ( lab );
				lab.setWordWrap ( false );
			}
		}

		lab = ( Label ) productDetails.getWidget ( 0 );
		lab.setText ( "Prodotto: " + productData.getValueString ( index, 0 ) );
		lab = ( Label ) productDetails.getWidget ( 1 );
		lab.setText ( "Valore ordinato: " + Double.toString ( productData.getValueDouble ( index, 1 ) ) + " €" );
		lab = ( Label ) productDetails.getWidget ( 2 );
		lab.setText ( "Utenti con almeno un ordine: " + Double.toString ( productData.getValueDouble ( index, 2 ) ) );
	}

	private void populateUsersGraph ( JSONArray array ) {
		int num_items;
		double val;
		String supplier_name;
		JSONArray row;
		JSONString num;
		DataTable by_orders;
		DataTable by_price;

		num_items = array.size ();
		if ( num_items == 0 )
			return;

		supplierData = DataTable.create ();
		supplierData.addColumn ( AbstractDataTable.ColumnType.STRING, "fornitore" );
		supplierData.addColumn ( AbstractDataTable.ColumnType.NUMBER, "somma" );
		supplierData.addColumn ( AbstractDataTable.ColumnType.NUMBER, "utenti" );
		supplierData.addRows ( num_items );

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
			supplierData.setValue ( i, 0, supplier_name );

			num = row.get ( 1 ).isString ();
			if ( num != null ) {
				val = Double.parseDouble ( num.stringValue () );
				by_orders.setValue ( i, 1, val );
				supplierData.setValue ( i, 2, val );
			}

			num = row.get ( 2 ).isString ();
			if ( num != null ) {
				val = Double.parseDouble ( num.stringValue () );
				by_price.setValue ( i, 1, val );
				supplierData.setValue ( i, 1, val );
			}
		}

		graphByOrders.draw ( by_orders, graphByOrdersOptions );
		graphByPrices.draw ( by_price, graphByPricesOptions );
	}

	private void populateProductsGraph ( JSONArray array ) {
		int num_items;
		double val;
		String product_name;
		JSONArray row;
		JSONString num;
		DataTable by_users;
		DataTable by_value;

		num_items = array.size ();
		if ( num_items == 0 )
			return;

		productData = DataTable.create ();
		productData.addColumn ( AbstractDataTable.ColumnType.STRING, "prodotto" );
		productData.addColumn ( AbstractDataTable.ColumnType.NUMBER, "valore" );
		productData.addColumn ( AbstractDataTable.ColumnType.NUMBER, "utenti" );
		productData.addRows ( num_items );

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
			productData.setValue ( i, 0, product_name );

			num = row.get ( 1 ).isString ();
			if ( num != null ) {
				val = Double.parseDouble ( num.stringValue () );
				by_users.setValue ( i, 1, val );
				productData.setValue ( i, 2, val );
			}

			num = row.get ( 2 ).isString ();
			if ( num != null ) {
				val = Double.parseDouble ( num.stringValue () );
				by_value.setValue ( i, 1, val );
				productData.setValue ( i, 1, val );
			}
		}

		graphByProduct.draw ( by_users, graphByProductOptions );
		graphByProductValue.draw ( by_value, graphByProductValueOptions );
	}

	private String linkTemplate ( String data_type, String document_type, DateSelector start, DateSelector end, int extra ) {
		return "graph_data.php?type=" + data_type + "&document=" + document_type + "&extra=" + extra + "&graph=0&startdate=" +
			Utils.encodeDate ( start.getValue () ) + "&enddate=" + Utils.encodeDate ( end.getValue () );
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
		usersFiles.addLink ( "CSV", linkTemplate ( "users", "csv", supplierStartDate, supplierEndDate, -1 ) );
		usersFiles.addLink ( "PDF", linkTemplate ( "users", "pdf", supplierStartDate, supplierEndDate, -1 ) );
	}

	private void performUsersUpdate () {
		Date s;
		Date e;

		s = supplierStartDate.getValue ();
		e = supplierEndDate.getValue ();
		if ( s.after ( e ) ) {
			Utils.showNotification ( "La data di partenza è posteriore alla data di fine selezione" );
			Utils.graphicPulseWidget ( supplierStartDate );
			Utils.graphicPulseWidget ( supplierEndDate );
			return;
		}

		updateUsersLinks ();

		Utils.getServer ().rawGet ( linkTemplate ( "users", "visual", supplierStartDate, supplierEndDate, -1 ), new RequestCallback () {
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
		productsFiles.addLink ( "CSV", linkTemplate ( "products", "csv", productStartDate, productEndDate, supp.getLocalID () ) );
		productsFiles.addLink ( "PDF", linkTemplate ( "products", "pdf", productStartDate, productEndDate, supp.getLocalID () ) );
	}

	private void performProductsUpdate () {
		Date s;
		Date e;
		FromServer supp;

		s = productStartDate.getValue ();
		e = productEndDate.getValue ();
		if ( s.after ( e ) ) {
			Utils.showNotification ( "La data di partenza è posteriore alla data di fine selezione" );
			Utils.graphicPulseWidget ( productStartDate );
			Utils.graphicPulseWidget ( productEndDate );
			return;
		}

		supp = supplier.getValue ();
		if ( supp == null )
			return;

		updateProductsLinks ( supp );

		Utils.getServer ().rawGet ( linkTemplate ( "products", "visual", productStartDate, productEndDate, supp.getLocalID () ), new RequestCallback () {
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
