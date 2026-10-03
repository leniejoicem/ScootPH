package com.payroll.UI.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.payroll.UI.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatCard extends JPanel {

    private final JLabel value;
    private final JLabel caption;

    public StatCard(String label, String value, String caption) {
        super(new BorderLayout(0, 4));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                new FlatLineBorder(new Insets(0, 0, 0, 0), Theme.BORDER, 1, Theme.CARD_RADIUS),
                Ui.padding(16, 18)));
        putClientProperty(FlatClientProperties.STYLE, "arc: " + Theme.CARD_RADIUS);
        add(Ui.caption(label), BorderLayout.NORTH);
        this.value = Ui.label(value, Font.BOLD, 24, Theme.TEXT);
        add(this.value, BorderLayout.CENTER);
        this.caption = Ui.caption(caption == null ? " " : caption);
        add(this.caption, BorderLayout.SOUTH);
    }

    public void setValue(String text) {
        value.setText(text);
    }

    public void setCaption(String text) {
        caption.setText(text == null ? " " : text);
    }
}
