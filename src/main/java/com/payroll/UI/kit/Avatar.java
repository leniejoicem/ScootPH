package com.payroll.UI.kit;

import com.payroll.UI.theme.Theme;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

public class Avatar extends JComponent {

    private String initials;
    private final int size;

    public Avatar(String name, int size) {
        this.size = size;
        setName(name);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        this.initials = initials(name);
        repaint();
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int d = Math.min(getWidth(), getHeight());
        int x = (getWidth() - d) / 2;
        int y = (getHeight() - d) / 2;
        g2.setColor(Theme.PRIMARY_SOFT);
        g2.fillOval(x, y, d, d);
        g2.setColor(Theme.PRIMARY);
        g2.setFont(Theme.font(Font.BOLD, Math.max(11, size * 2 / 5)));
        var fm = g2.getFontMetrics();
        g2.drawString(initials, x + (d - fm.stringWidth(initials)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
