package gui;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.util.Calendar;
import java.util.Date;
import javax.swing.event.CaretEvent;
import str.Str;
import util.Regex;

public class DateChooser extends JDateChooser {

    private static final long serialVersionUID = 1L;

    public DateChooser() {
        super(new JCalendar(null, null, true, true), null, null, new DateEditor());
    }

    public Calendar getTime() {
        Calendar calendar = getCalendar();
        if (calendar != null) {
            return (Calendar) calendar.clone();
        }
        String text = ((DateEditor) getDateEditor()).getYearText();
        if (text != null) {
            calendar = Calendar.getInstance(Str.locale());
            calendar.clear();
            calendar.set(Calendar.YEAR, Integer.parseInt(text));
            return calendar;
        }
        return null;
    }

    public static class DateEditor extends JTextFieldDateEditor {

        private static final long serialVersionUID = 1L;

        @Override
        public void focusLost(FocusEvent evt) {
            super.focusLost(evt);
            checkText();
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            checkText();
        }

        @Override
        public void setMaxSelectableDate(Date max) {
            super.setMaxSelectableDate(max);
            checkText();
        }

        @Override
        public void setMinSelectableDate(Date min) {
            super.setMinSelectableDate(min);
            checkText();
        }

        @Override
        public void setSelectableDateRange(Date min, Date max) {
            super.setSelectableDateRange(min, max);
            checkText();
        }

        @Override
        public void caretUpdate(CaretEvent evt) {
            super.caretUpdate(evt);
            checkText(darkGreen);
        }

        protected void checkText() {
            checkText(Color.BLACK);
        }

        protected void checkText(Color validForeground) {
            if (getYearText() != null) {
                setForeground(validForeground);
            }
        }

        public String getYearText() {
            String text = getText().trim();
            return Regex.isMatch(text, "\\d{4}+") ? text : null;
        }
    }
}
