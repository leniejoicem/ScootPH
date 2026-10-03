package com.payroll.UI.kit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PageHeader extends JPanel {

    private final JLabel title;
    private final JLabel subtitle;

    public PageHeader(String title, String subtitle, JComponent... actions) {
        super(new ResponsiveGrid(320, 2, 16, 8));
        setOpaque(false);
        JPanel text = Ui.transparent(new BorderLayout(0, 4));
        this.title = Ui.title(title);
        text.add(this.title, BorderLayout.NORTH);
        this.subtitle = Ui.muted(subtitle == null ? "" : subtitle);
        text.add(this.subtitle, BorderLayout.CENTER);
        add(text);
        JPanel right = Ui.row(FlowLayout.RIGHT, actions);
        JPanel holder = Ui.transparent(new BorderLayout());
        holder.add(right, BorderLayout.NORTH);
        add(holder);
    }

    public void setTitle(String text) {
        title.setText(text);
    }

    public void setSubtitle(String text) {
        subtitle.setText(text);
    }
}
