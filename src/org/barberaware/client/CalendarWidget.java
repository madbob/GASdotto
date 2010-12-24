/*
Simple Calendar Widget for GWT
Copyright (C) 2006 Alexei Sokolov http://gwt.components.googlepages.com/

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA

*/

/*
	Modified by:
		GASdotto
		Copyright (C) 2009/2010 Roberto -MadBob- Guido <madbob@users.barberaware.org>
*/

package org.barberaware.client;

import java.util.*;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;

public class CalendarWidget extends DialogBox implements ClickListener, SavingDialog {
	private class NavBar extends Composite implements ClickListener {
		public DockPanel	bar = new DockPanel ();
		public Button		prevMonth = new Button ( "&lt;", this );
		public Button		nextMonth = new Button ( "&gt;", this );
		public NumberBox	year = null;
		public boolean		noYear = false;
		public HTML		title = new HTML ();

		private CalendarWidget calendar;

		public NavBar ( CalendarWidget calendar ) {
			this.calendar = calendar;

			setWidget ( bar );
			bar.setStyleName ( "navbar" );
			title.setStyleName ( "header" );

			HorizontalPanel prevButtons = new HorizontalPanel ();
			prevButtons.add ( prevMonth );

			HorizontalPanel nextButtons = new HorizontalPanel ();
			nextButtons.add ( nextMonth );

			bar.add ( prevButtons, DockPanel.WEST );
			bar.setCellHorizontalAlignment ( prevButtons, DockPanel.ALIGN_LEFT );
			bar.add ( nextButtons, DockPanel.EAST );
			bar.setCellHorizontalAlignment ( nextButtons, DockPanel.ALIGN_RIGHT );
			bar.add ( title, DockPanel.CENTER );
			bar.setVerticalAlignment ( DockPanel.ALIGN_MIDDLE );
			bar.setCellHorizontalAlignment ( title, HasAlignment.ALIGN_CENTER );
			bar.setCellVerticalAlignment ( title, HasAlignment.ALIGN_MIDDLE );
			bar.setCellWidth ( title, "100%" );
		}

		public void onClick ( Widget sender ) {
			if ( sender == prevMonth )
				calendar.prevMonth ();
			else if ( sender == nextMonth )
				calendar.nextMonth ();
		}

		public void yearSelectable ( boolean selectable ) {
			HorizontalPanel head;

			if ( selectable == true ) {
				year = new NumberBox ();
				year.setVal ( calendar.getYear () );

				head = new HorizontalPanel ();
				head.setHorizontalAlignment ( HasHorizontalAlignment.ALIGN_CENTER );
				head.setWidth ( "100%" );
				bar.remove ( title );
				head.add ( title );
				head.setCellVerticalAlignment ( title, HasVerticalAlignment.ALIGN_MIDDLE );
				head.add ( year );

				bar.add ( head, DockPanel.CENTER );
				bar.setCellHorizontalAlignment ( head, HasHorizontalAlignment.ALIGN_CENTER );
				bar.setCellVerticalAlignment ( head, HasVerticalAlignment.ALIGN_MIDDLE );
				bar.setCellWidth ( head, "100%" );

				year.addFocusListener ( new FocusListener () {
					public void onFocus ( Widget sender ) {
						/* dummy */
					}

					public void onLostFocus ( Widget sender ) {
						calendar.forceYear ( year.getVal () );
					}
				} );

				year.addKeyboardListener ( new KeyboardListener () {
					public void onKeyDown ( Widget sender, char keyCode, int modifiers ) {
						/* dummy */
					}

					public void onKeyUp ( Widget sender, char keyCode, int modifiers ) {
						calendar.forceYear ( year.getVal () );
					}

					public void onKeyPress ( Widget sender, char keyCode, int modifiers ) {
						/* dummy */
					}
				} );
			}
			else {
				year = null;
				bar.add ( title, DockPanel.CENTER );
				bar.setCellHorizontalAlignment ( title, HasAlignment.ALIGN_CENTER );
				bar.setCellVerticalAlignment ( title, HasAlignment.ALIGN_MIDDLE );
				bar.setCellWidth ( title, "100%" );
			}
		}

		public void setTitle ( int month, int year ) {
			if ( noYear == true ) {
				title.setText ( Utils.months [ month ] );
			}
			else {
				if ( this.year == null ) {
					title.setText ( Utils.months [ month ] + " " + year );
				}
				else {
					title.setText ( Utils.months [ month ] );
					this.year.setVal ( year );
				}
			}
		}
	}

	private static class CellHTML extends HTML {
		private int day;

		public CellHTML ( String text, int day ) {
			super ( text );
			this.day = day;
		}

		public int getDay () {
			return day;
		}
	}

	private NavBar		navbar;
	private DockPanel	outer;
	private Grid		grid;

	private Date		date = new Date();
	private Date		originalDate;
	private ArrayList	callbacks;

	public CalendarWidget () {
		Button exit;

		setText ( "Seleziona una Data" );

		exit = new Button ( "Annulla", new ClickListener () {
			public void onClick ( Widget sender ) {
				fireCallbacks ( 1 );
				hide ();
			}
		} );

		outer = new DockPanel ();
		navbar = new NavBar ( this );

		grid = new Grid ( 7, 7 ) {
			public boolean clearCell ( int row, int column ) {
				boolean retValue = super.clearCell ( row, column );
				Element td = getCellFormatter ().getElement ( row, column );
				DOM.setInnerHTML ( td, "" );
				return retValue;
			}
		};

		setWidget ( outer );
		grid.setStyleName ( "table" );
		grid.setCellSpacing ( 0 );
		outer.add ( navbar, DockPanel.NORTH );
		outer.add ( grid, DockPanel.CENTER );
		outer.add ( exit, DockPanel.SOUTH );
		drawCalendar ();
		addStyleName ( "CalendarWidget" );
	}

	private void fireCallbacks ( int mode ) {
		int i;
		int num;
		SavingDialogCallback call;

		if ( callbacks == null )
			return;

		num = callbacks.size ();

		if ( mode == 0 ) {
			for ( i = 0; i < num; i++ ) {
				call = ( SavingDialogCallback ) callbacks.get ( i );
				call.onSave ( this );
			}
		}
		else {
			if ( originalDate != null )
				date = originalDate;

			for ( i = 0; i < num; i++ ) {
				call = ( SavingDialogCallback ) callbacks.get ( i );
				call.onCancel ( this );
			}
		}
	}

	private void drawCalendar () {
		int year = getYear();
		int month = getMonth();
		int day = getDay();
		navbar.setTitle ( month, year );
		grid.getRowFormatter().setStyleName(0, "weekheader");
		for (int i = 0; i < Utils.days.length; i++) {
			grid.getCellFormatter().setStyleName(0, i, "days");
			grid.setText(0, i, Utils.days[i].substring(0, 3));
		}

		Date now = new Date();
		int sameDay = now.getDate();
		int today = (now.getMonth() == month && now.getYear()+1900 == year) ? sameDay : 0;

		/**
			TODO	L'inizio della settimana (domenica o lunedi) potrebbe essere
				settato in funzione della localizzazione dell'applicazione
		*/

		/*
			Date.getDay() ritorna 0 per domenica, ma devo far iniziare la settimana
			da lunedi
		*/
		int firstDay = new Date(year - 1900, month, 1).getDay() - 1;
		if ( firstDay < 0 )
			firstDay = 6;

		int numOfDays = getDaysInMonth(year, month);

		int j = 0;
		for (int i = 1; i < 7; i++) {
			for (int k = 0; k < 7; k++, j++) {
				int displayNum = (j - firstDay + 1);
				if (j < firstDay || displayNum > numOfDays) {
					grid.getCellFormatter().setStyleName(i, k, "empty");
					grid.setHTML(i, k, "&nbsp;");
				} else {
					HTML html = new CellHTML(
					"<span>" + String.valueOf(displayNum) + "</span>",
					displayNum);
					html.addClickListener(this);
					grid.getCellFormatter().setStyleName(i, k, "cell");
					if (displayNum == today) {
						grid.getCellFormatter().addStyleName(i, k, "today");
					} else if (displayNum == sameDay) {
						grid.getCellFormatter().addStyleName(i, k, "day");
					}
					grid.setWidget(i, k, html);
				}
			}
		}
	}

	private int getDaysInMonth(int year, int month) {
		switch (month) {
			case 1:
				if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0)
					return 29; // leap year
				else
					return 28;

			case 3:
			case 5:
			case 8:
			case 10:
				return 30;
			default:
				return 31;
		}
	}

	public void prevMonth() {
		int month = getMonth () - 1;

		if ( month < 0 ) {
			setDate ( getYear () - 1, 11, getDay () );
		}
		else {
			setMonth ( month );
			drawCalendar ();
		}
	}

	public void nextMonth() {
		int month = getMonth () + 1;

		if ( month > 11 ) {
			setDate ( getYear () + 1, 0, getDay () );
		}
		else {
			setMonth ( month );
			drawCalendar ();
		}
	}

	public void forceYear ( int year ) {
		setDate ( year, getMonth (), getDay () );
	}

	public void setDate(int year, int month, int day) {
		date = new Date(year - 1900, month, day);
		originalDate = ( Date ) date.clone ();
		drawCalendar();
	}

	public void setDate ( Date d ) {
		if ( d == null )
			return;

		setDate ( d.getYear () + 1900, d.getMonth (), d.getDate () );
	}

	private void setYear(int year) {
		date.setYear(year - 1900);
	}

	private void setMonth(int month) {
		date.setMonth(month);
	}

	public int getYear() {
		return 1900 + date.getYear();
	}

	public int getMonth() {
		return date.getMonth();
	}

	public int getDay() {
		return date.getDate();
	}

	public Date getDate() {
		return date;
	}

	public void onClick(Widget sender) {
		CellHTML cell = (CellHTML)sender;
		setDate(getYear(), getMonth(), cell.getDay());
		drawCalendar();
		fireCallbacks ( 0 );
		hide ();
	}

	public void yearSelectable ( boolean selectable ) {
		navbar.yearSelectable ( selectable );
		navbar.setTitle ( getMonth (), getYear () );
	}

	public void ignoreYear ( boolean ignore ) {
		navbar.noYear = ignore;
		navbar.setTitle ( getMonth (), getYear () );
	}

	/****************************************************************** SavingDialog */

	public void addCallback ( SavingDialogCallback callback ) {
		if ( callbacks == null )
			callbacks = new ArrayList ();
		callbacks.add ( callback );
	}

	public void removeCallback ( SavingDialogCallback callback ) {
		if ( callbacks == null )
			return;
		callbacks.remove ( callback );
	}
}
