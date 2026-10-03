package com.payroll.UI.kit;

import com.payroll.UI.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

public class Badge extends JLabel {

    public Badge(String text) {
        setFont(Theme.font(Font.BOLD, 12));
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        setValue(text);
    }

    public final void setValue(String text) {
        setText(text == null ? "" : pretty(text));
        setForeground(colors(text)[0]);
    }

    static String pretty(String s) {
        if (s.equals(s.toUpperCase(Locale.ROOT)) && s.length() > 3) {
            return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT);
        }
        return s;
    }

    public static Color[] colors(String value) {
        String v = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return switch (v) {
            case "approved", "present", "enabled", "regular", "paid" ->
                new Color[]{Theme.SUCCESS, Theme.SUCCESS_SOFT};
            case "pending", "no time-out", "probationary" -> new Color[]{Theme.WARNING, Theme.WARNING_SOFT};
            case "declined", "absent", "disabled" -> new Color[]{Theme.DANGER, Theme.DANGER_SOFT};
            case "hr" -> new Color[]{Theme.INFO, Theme.INFO_SOFT};
            case "finance" -> new Color[]{Theme.SUCCESS, Theme.SUCCESS_SOFT};
            case "it" -> new Color[]{Theme.VIOLET, Theme.VIOLET_SOFT};
            case "employee" -> new Color[]{Theme.TEXT_MUTED, Theme.NEUTRAL_SOFT};
            default -> new Color[]{Theme.TEXT_MUTED, Theme.NEUTRAL_SOFT};
        };
    }

    public static TableCellRenderer renderer() {
        return new TableCellRenderer() {
            private final Badge badge = new Badge("");
            private final javax.swing.JPanel holder = new javax.swing.JPanel(
                    new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 10));

            {
                holder.add(badge);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                badge.setValue(value == null ? "" : value.toString());
                holder.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return holder;
            }
        };
    }
}
