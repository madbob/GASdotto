/*  GASdotto
 *  Copyright (C) 2011 Roberto -MadBob- Guido <madbob@users.barberaware.org>
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

import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

import com.allen_sauer.gwt.log.client.Log;

public class TotalRow extends Composite implements FloatWidget {
	private PriceViewer	totalLabel;

	public TotalRow () {
		Label lab;
		FlexTable tot;
		PriceViewer total_label;
		FlexTable.FlexCellFormatter formatter;

		tot = new FlexTable ();
		tot.setWidth ( "100%" );
		initWidget ( tot );

		/*
			I vari elementi sono allineati come in ProductsUserSelection, per ottenere lo stesso effetto
			grafico rispetto alla barra dei totali inclusa in quel widget
		*/
		formatter = tot.getFlexCellFormatter ();

		tot.setWidget ( 0, 0, new HTML ( "<hr>" ) );
		tot.getFlexCellFormatter ().setColSpan ( 0, 0, 2 );

		tot.setWidget ( 1, 0, new Label ( "Totale" ) );
		formatter.setWidth ( 1, 0, "30%" );

		totalLabel = new PriceViewer ();
		totalLabel.setStyleName ( "bigger-text" );
		tot.setWidget ( 1, 1, totalLabel );
		formatter.setWidth ( 1, 1, "40%" );

		tot.setWidget ( 1, 2, new Label ( "" ) );
		formatter.setWidth ( 1, 2, "30%" );
	}

	/****************************************************************** FloatWidget */

	public void setVal ( float v ) {
		totalLabel.setVal ( v );
	}

	public float getVal () {
		return totalLabel.getVal ();
	}
}
