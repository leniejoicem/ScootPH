package com.payroll.UI.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.payroll.UI.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Card extends JPanel {

    private final JPanel body;
    private JLabel titleLabel;
    private JLabel subtitleLabel;

    public Card() {
        this(null, null);
    }

    public Card(String title) {
        this(title, null);
    }

    public Card(String title, String subtitle, JComponent... actions) {
        super(new BorderLayout(0, 16));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                new FlatLineBorder(new Insets(0, 0, 0, 0), Theme.BORDER, 1, Theme.CARD_RADIUS),
                Ui.padding(20)));
        putClientProperty(FlatClientProperties.STYLE, "arc: " + Theme.CARD_RADIUS);
        if (title != null) {
            JPanel header = Ui.transparent(new BorderLayout(12, 4));
            JPanel text = Ui.transparent(new BorderLayout(0, 2));
            titleLabel = Ui.heading(title);
            text.add(titleLabel, BorderLayout.NORTH);
            if (subtitle != null) {
                subtitleLabel = Ui.muted(subtitle);
                text.add(subtitleLabel, BorderLayout.CENTER);
            }
            header.add(text, BorderLayout.CENTER);
            if (actions.length > 0) {
                JPanel actionRow = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                for (JComponent a : actions) {
                    actionRow.add(a);
                }
                header.add(actionRow, BorderLayout.EAST);
            }
            super.add(header, BorderLayout.NORTH);
        }
        body = Ui.transparent(new BorderLayout());
        super.add(body, BorderLayout.CENTER);
    }

    public JPanel body() {
        return body;
    }

    public Card content(Component c) {
        body.removeAll();
        body.add(c, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
        return this;
    }

    public Card footer(Component c) {
        super.add(c, BorderLayout.SOUTH);
        return this;
    }

    public void setTitle(String title) {
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    public void setSubtitle(String subtitle) {
        if (subtitleLabel != null) {
            subtitleLabel.setText(subtitle);
        }
    }
}
