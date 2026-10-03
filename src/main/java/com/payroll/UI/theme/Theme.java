package com.payroll.UI.theme;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.UIManager;

public final class Theme {

    public static final Color PRIMARY = new Color(0xF0, 0x5A, 0x28);
    public static final Color PRIMARY_HOVER = new Color(0xD9, 0x4A, 0x1B);
    public static final Color PRIMARY_SOFT = new Color(0xFF, 0xF1, 0xEB);

    public static final Color BACKGROUND = new Color(0xF5, 0xF6, 0xF8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xE5, 0xE7, 0xEB);
    public static final Color TEXT = new Color(0x11, 0x18, 0x27);
    public static final Color TEXT_MUTED = new Color(0x6B, 0x72, 0x80);
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);
    public static final Color LINK = new Color(0x25, 0x63, 0xEB);

    public static final Color SUCCESS = new Color(0x15, 0x80, 0x3D);
    public static final Color SUCCESS_SOFT = new Color(0xDC, 0xFC, 0xE7);
    public static final Color WARNING = new Color(0xB4, 0x53, 0x09);
    public static final Color WARNING_SOFT = new Color(0xFE, 0xF3, 0xC7);
    public static final Color DANGER_SOFT = new Color(0xFE, 0xE2, 0xE2);
    public static final Color INFO = new Color(0x1D, 0x4E, 0xD8);
    public static final Color INFO_SOFT = new Color(0xDB, 0xEA, 0xFE);
    public static final Color NEUTRAL_SOFT = new Color(0xF3, 0xF4, 0xF6);
    public static final Color VIOLET = new Color(0x6D, 0x28, 0xD9);
    public static final Color VIOLET_SOFT = new Color(0xED, 0xE9, 0xFE);

    public static final int RADIUS = 4;
    public static final int CARD_RADIUS = 6;

    private static final String FONT = firstInstalled("Century Gothic", "Futura", "URW Gothic", "TeX Gyre Adventor");

    private static String firstInstalled(String... families) {
        java.util.Set<String> installed = java.util.Set.of(java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : families) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return FlatInterFont.FAMILY;
    }

    public static String fontFamily() {
        return FONT;
    }

    private Theme() {
    }

    public static void install() {
        FlatInterFont.install();
        UIManager.put("defaultFont", new Font(FONT, Font.PLAIN, 13));

        UIManager.put("@accentColor", hex(PRIMARY));
        UIManager.put("Component.arc", RADIUS);
        UIManager.put("Button.arc", RADIUS);
        UIManager.put("TextComponent.arc", RADIUS);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.focusColor", PRIMARY);
        UIManager.put("Component.focusedBorderColor", PRIMARY);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Button.borderColor", BORDER);
        UIManager.put("Button.default.background", PRIMARY);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.default.hoverBackground", PRIMARY_HOVER);
        UIManager.put("Button.default.focusedBackground", PRIMARY);
        UIManager.put("Button.default.borderWidth", 0);
        UIManager.put("Button.margin", new Insets(6, 14, 6, 14));
        UIManager.put("TextComponent.background", SURFACE);
        UIManager.put("TextField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.showRevealButton", true);
        UIManager.put("ComboBox.padding", new Insets(4, 8, 4, 8));
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", new Color(0xF0, 0xF1, 0xF4));
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.selectionInactiveBackground", PRIMARY_SOFT);
        UIManager.put("Table.selectionInactiveForeground", TEXT);
        UIManager.put("TableHeader.background", new Color(0xF9, 0xFA, 0xFB));
        UIManager.put("TableHeader.foreground", TEXT_MUTED);
        UIManager.put("TableHeader.separatorColor", BORDER);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("ScrollBar.thumbArc", 0);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("OptionPane.buttonMinimumWidth", 90);
        UIManager.put("TitlePane.unifiedBackground", true);

        FlatLightLaf.setup();
    }

    public static Font font(int style, float size) {
        return new Font(FONT, style, Math.round(size));
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
