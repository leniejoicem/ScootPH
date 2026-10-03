package com.payroll.UI.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.payroll.UI.theme.Theme;
import com.toedter.calendar.IDateEvaluator;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class DateField extends JPanel {

    private final JDateChooser chooser = new JDateChooser();

    public DateField() {
        super(new BorderLayout());
        setOpaque(false);
        chooser.setDateFormatString("MMM d, yyyy");
        JButton calendar = chooser.getCalendarButton();
        FlatSVGIcon icon = new FlatSVGIcon("ui/calendar.svg", 16, 16);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Theme.TEXT_MUTED));
        calendar.setIcon(icon);
        calendar.setText(null);
        calendar.setFocusable(false);
        calendar.setToolTipText("Open calendar");
        calendar.getAccessibleContext().setAccessibleName("Open calendar");
        calendar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        calendar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editor().putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Select a date");
        add(chooser, BorderLayout.CENTER);
    }

    public JComponent editor() {
        return (JComponent) chooser.getDateEditor().getUiComponent();
    }

    public LocalDate getDate() {
        Date d = chooser.getDate();
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setDate(LocalDate date) {
        chooser.setDate(date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public void setMinDate(LocalDate date) {
        chooser.setMinSelectableDate(date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public void setMaxDate(LocalDate date) {
        chooser.setMaxSelectableDate(date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public void disableWeekends() {
        chooser.getJCalendar().getDayChooser().addDateEvaluator(new IDateEvaluator() {
            @Override
            public boolean isInvalid(Date date) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                int d = c.get(Calendar.DAY_OF_WEEK);
                return d == Calendar.SATURDAY || d == Calendar.SUNDAY;
            }

            @Override public Color getInvalidForegroundColor() { return Color.GRAY; }
            @Override public Color getInvalidBackroundColor() { return null; }
            @Override public String getInvalidTooltip() { return "Leave is for weekdays only"; }
            @Override public boolean isSpecial(Date date) { return false; }
            @Override public Color getSpecialForegroundColor() { return null; }
            @Override public Color getSpecialBackroundColor() { return null; }
            @Override public String getSpecialTooltip() { return null; }
        });
    }

    public void addChangeListener(Runnable listener) {
        chooser.addPropertyChangeListener("date", e -> listener.run());
    }

    void setOutline(Object value) {
        editor().putClientProperty(FlatClientProperties.OUTLINE, value);
    }

    static boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        chooser.setEnabled(enabled);
    }
}
