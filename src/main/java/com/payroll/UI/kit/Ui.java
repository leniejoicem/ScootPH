package com.payroll.UI.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public final class Ui {

    public static final int GAP = 16;
    public static final int PAGE_PADDING = 24;

    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    private Ui() {
    }

    public static String peso(double amount) {
        return "₱" + MONEY.format(amount);
    }

    public static String amount(double amount) {
        return MONEY.format(amount);
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : date.format(DATE);
    }

    public static String date(java.util.Date date) {
        if (date == null) {
            return "—";
        }
        LocalDate local = date instanceof java.sql.Date d ? d.toLocalDate()
                : date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return date(local);
    }

    public static String day(LocalDate date) {
        return date == null ? "—" : date.format(DAY);
    }

    public static String time(LocalTime time) {
        return time == null ? "—" : time.format(TIME);
    }

    public static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    public static JLabel title(String text) {
        return label(text, Font.BOLD, 22, Theme.TEXT);
    }

    public static JLabel heading(String text) {
        return label(text, Font.BOLD, 15, Theme.TEXT);
    }

    public static JLabel text(String text) {
        return label(text, Font.PLAIN, 13, Theme.TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, Font.PLAIN, 13, Theme.TEXT_MUTED);
    }

    public static JLabel caption(String text) {
        return label(text, Font.PLAIN, 12, Theme.TEXT_MUTED);
    }

    public static JLabel overline(String text) {
        return label(text.toUpperCase(Locale.ROOT), Font.BOLD, 11, Theme.TEXT_MUTED);
    }

    public static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JTextArea paragraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(Theme.font(Font.PLAIN, 13));
        area.setForeground(Theme.TEXT_MUTED);
        return area;
    }

    public static JButton primary(String text) {
        JButton b = button(text, null);
        b.putClientProperty(FlatClientProperties.STYLE, "background: #F05A28; foreground: #ffffff; borderWidth: 0;"
                + "focusWidth: 0; hoverBackground: #D94A1B; pressedBackground: #C2410C; font: bold;"
                + "disabledBackground: #F7B49B; disabledText: #ffffff");
        return b;
    }

    public static JButton secondary(String text) {
        JButton b = button(text, null);
        b.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        return b;
    }

    public static JButton danger(String text) {
        JButton b = button(text, null);
        b.putClientProperty(FlatClientProperties.STYLE, "foreground: #B91C1C; borderColor: #FCA5A5; font: bold;"
                + "hoverBorderColor: #DC2626; focusedBorderColor: #DC2626");
        return b;
    }

    public static JButton ghost(String text) {
        JButton b = button(text, null);
        b.putClientProperty(FlatClientProperties.STYLE, "foreground: #374151; font: bold");
        return b;
    }

    public static JButton link(String text) {
        JButton b = new JButton(text);
        b.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(Theme.PRIMARY);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton button(String text, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setMargin(new Insets(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        return b;
    }

    public static JPanel row(int align, Component... items) {
        JPanel p = new JPanel(new WrapLayout(align, 8, 0));
        p.setOpaque(false);
        for (Component c : items) {
            p.add(c);
        }
        return p;
    }

    public static JPanel actions(Component... items) {
        return row(FlowLayout.RIGHT, items);
    }

    public static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static Border padding(int all) {
        return BorderFactory.createEmptyBorder(all, all, all, all);
    }

    public static Border padding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static JPanel stack(int gap, JComponent... items) {
        JPanel p = transparent(new Stack(gap));
        for (JComponent item : items) {
            p.add(item);
        }
        return p;
    }

    public static JPanel detail(String label, String value) {
        JPanel p = transparent(new BorderLayout(0, 2));
        p.add(caption(label), BorderLayout.NORTH);
        JLabel v = text(orDash(value));
        v.setFont(Theme.font(Font.BOLD, 13));
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    public static JLabel centered(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
}
