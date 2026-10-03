package com.payroll.UI.kit;

import com.payroll.UI.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class Toast {

    public enum Kind { SUCCESS, ERROR, INFO }

    private Toast() {
    }

    public static void show(Component anchor, String message, Kind kind) {
        Window window = anchor == null ? null : SwingUtilities.getWindowAncestor(anchor);
        if (!(window instanceof javax.swing.RootPaneContainer rpc) || !window.isShowing()) {
            return;
        }
        JRootPane root = rpc.getRootPane();
        JLayeredPane layers = root.getLayeredPane();

        Color accent = switch (kind) {
            case SUCCESS -> Theme.SUCCESS;
            case ERROR -> Theme.DANGER;
            case INFO -> Theme.INFO;
        };
        JPanel toast = new JPanel(new BorderLayout(12, 0));
        toast.setBackground(Theme.SURFACE);
        toast.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, Theme.BORDER),
                        BorderFactory.createMatteBorder(0, 4, 0, 0, accent)),
                Ui.padding(12, 14)));
        JTextArea text = Ui.paragraph(message);
        text.setForeground(Theme.TEXT);
        toast.add(text, BorderLayout.CENTER);

        int width = Math.min(380, layers.getWidth() - 48);
        text.setSize(width - 70, Short.MAX_VALUE);
        int height = Math.max(52, text.getPreferredSize().height + 26);
        toast.setBounds(layers.getWidth() - width - 24, layers.getHeight() - height - 24, width, height);
        layers.add(toast, JLayeredPane.POPUP_LAYER);
        layers.revalidate();
        layers.repaint();

        Timer timer = new Timer(kind == Kind.ERROR ? 6000 : 3500, e -> {
            layers.remove(toast);
            layers.repaint();
        });
        timer.setRepeats(false);
        timer.start();
        toast.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                timer.stop();
                layers.remove(toast);
                layers.repaint();
            }
        });
    }
}
