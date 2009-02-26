/*
Simple Calendar Widget for GWT
Copyright (C) 2006 Alexei Sokolov http://gwt.components.googlepages.com/
Copyright (C) 2009 Roberto -MadBob- Guido <madbob@users.barberaware.org>

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

package org.barberaware.client;

import java.util.Date;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.Widget;

public class CalendarWidget extends Composite
    implements ClickListener, SourcesChangeEvents {

  private class NavBar extends Composite implements ClickListener {

    public DockPanel bar = new DockPanel();
    public Button prevMonth = new Button("&lt;", this);
    public Button nextMonth = new Button("&gt;", this);
    public HTML title = new HTML();

    private CalendarWidget calendar;

    public NavBar(CalendarWidget calendar) {
      this.calendar = calendar;

      setWidget(bar);
      bar.setStyleName("navbar");
      title.setStyleName("header");

      HorizontalPanel prevButtons = new HorizontalPanel();
      prevButtons.add(prevMonth);

      HorizontalPanel nextButtons = new HorizontalPanel();
      nextButtons.add(nextMonth);

      bar.add(prevButtons, DockPanel.WEST);
      bar.setCellHorizontalAlignment(prevButtons, DockPanel.ALIGN_LEFT);
      bar.add(nextButtons, DockPanel.EAST);
      bar.setCellHorizontalAlignment(nextButtons, DockPanel.ALIGN_RIGHT);
      bar.add(title, DockPanel.CENTER);
      bar.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
      bar.setCellHorizontalAlignment(title, HasAlignment.ALIGN_CENTER);
      bar.setCellVerticalAlignment(title, HasAlignment.ALIGN_MIDDLE);
      bar.setCellWidth(title, "100%");
    }

    public void onClick(Widget sender) {
      if (sender == prevMonth) {
        calendar.prevMonth();
      } else if (sender == nextMonth) {
        calendar.nextMonth();
      }
    }
  }

  private static class CellHTML extends HTML {
    private int day;

    public CellHTML(String text, int day) {
      super(text);
      this.day = day;
    }

    public int getDay() {
      return day;
    }
  }

  private NavBar navbar;
  private DockPanel outer;
  private Grid grid;

  private Date date = new Date();
  private Date originalDate;

  private ChangeListenerCollection changeListeners;

  public CalendarWidget() {
    Button exit;

    exit = new Button ( "Annulla", new ClickListener () {
      public void onClick ( Widget sender ) {
        if (changeListeners != null) {
          date = originalDate;
          changeListeners.fireChange(null);
        }
      }
    } );

    outer = new DockPanel();
    navbar = new NavBar(this);

    grid = new Grid(6, 7) {
      public boolean clearCell(int row, int column) {
        boolean retValue = super.clearCell(row, column);

        Element td = getCellFormatter().getElement(row, column);
        DOM.setInnerHTML(td, "");
        return retValue;
      }
    };

    setWidget(outer);
    grid.setStyleName("table");
    grid.setCellSpacing(0);
    outer.add(navbar, DockPanel.NORTH);
    outer.add(grid, DockPanel.CENTER);
    outer.add(exit, DockPanel.SOUTH);
    drawCalendar();
    setStyleName("CalendarWidget");
  }

  private void drawCalendar() {
    int year = getYear();
    int month = getMonth();
    int day = getDay();
    setHeaderText(year, month);
    grid.getRowFormatter().setStyleName(0, "weekheader");
    for (int i = 0; i < Utils.days.length; i++) {
      grid.getCellFormatter().setStyleName(0, i, "days");
      grid.setText(0, i, Utils.days[i].substring(0, 3));
    }

    Date now = new Date();
    int sameDay = now.getDate();
    int today = (now.getMonth() == month && now.getYear()+1900 == year) ? sameDay : 0;

    int firstDay = new Date(year - 1900, month, 1).getDay();
    int numOfDays = getDaysInMonth(year, month);

    int j = 0;
    for (int i = 1; i < 6; i++) {
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

  protected void setHeaderText(int year, int month) {
    navbar.title.setText(Utils.months[month] + " " + year);
  }

  private int getDaysInMonth(int year, int month) {
    switch (month) {
      case 1:
        if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0)
          return 29; // leap year
        else
          return 28;
      case 3:
        return 30;
      case 5:
        return 30;
      case 8:
        return 30;
      case 10:
        return 30;
      default:
        return 31;
    }
  }

  public void prevMonth() {
    int month = getMonth() - 1;
    if (month < 0) {
      setDate(getYear() - 1, 11, getDay());
    } else {
      setMonth(month);
    }
    drawCalendar();
  }

  public void nextMonth() {
    int month = getMonth() + 1;
    if (month > 11) {
      setDate(getYear() + 1, 0, getDay());
    } else {
      setMonth(month);
    }
    drawCalendar();
  }

  public void setDate(int year, int month, int day) {
    date = new Date(year - 1900, month, day);
    originalDate = ( Date ) date.clone ();
    drawCalendar();
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
    if (changeListeners != null) {
      changeListeners.fireChange(this);
    }
  }

  public void addChangeListener(ChangeListener listener) {
    if (changeListeners == null)
      changeListeners = new ChangeListenerCollection();
    changeListeners.add(listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    if (changeListeners != null)
      changeListeners.remove(listener);
  }
}
