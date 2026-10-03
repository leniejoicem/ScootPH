package com.payroll.UI.kit;

import java.awt.Component;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

public class MonthPicker extends JComboBox<YearMonth> {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public MonthPicker(YearMonth newest, YearMonth oldest) {
        for (YearMonth m = newest; !m.isBefore(oldest); m = m.minusMonths(1)) {
            addItem(m);
        }
        setMaximumRowCount(12);
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof YearMonth m) {
                    setText(m.format(LABEL));
                }
                return this;
            }
        });
    }

    public YearMonth month() {
        return (YearMonth) getSelectedItem();
    }

    public boolean select(YearMonth month) {
        if (month == null || month.equals(month()) || ((javax.swing.DefaultComboBoxModel<YearMonth>) getModel()).getIndexOf(month) < 0) {
            return false;
        }
        setSelectedItem(month);
        return true;
    }
}
