package com.payroll.UI.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FormField extends JPanel {

    private final JComponent input;
    private final JLabel message;
    private String help;

    public FormField(String label, JComponent input) {
        this(label, input, null);
    }

    public FormField(String label, JComponent input, String help) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        this.input = input;
        this.help = help;
        JLabel title = Ui.label(label, Font.BOLD, 13, Theme.TEXT);
        title.setLabelFor(input);
        add(title, BorderLayout.NORTH);
        add(input, BorderLayout.CENTER);
        message = Ui.caption(help == null ? " " : help);
        add(message, BorderLayout.SOUTH);
        input.getAccessibleContext().setAccessibleName(label);
    }

    public void setHelp(String help) {
        this.help = help;
        if (error() == null) {
            message.setText(help == null ? " " : help);
        }
    }

    public JComponent input() {
        return input;
    }

    public void setError(String error) {
        if (error == null) {
            message.setText(help == null ? " " : help);
            message.setForeground(Theme.TEXT_MUTED);
            outline(null);
        } else {
            message.setText(error);
            message.setForeground(Theme.DANGER);
            outline(FlatClientProperties.OUTLINE_ERROR);
        }
    }

    public String error() {
        return Theme.DANGER.equals(message.getForeground()) ? message.getText() : null;
    }

    private void outline(Object value) {
        JComponent target = input;
        if (input instanceof javax.swing.JScrollPane sp && sp.getViewport().getView() instanceof JComponent v) {
            target = sp;
            v.putClientProperty(FlatClientProperties.OUTLINE, value);
        }
        target.putClientProperty(FlatClientProperties.OUTLINE, value);
        if (input instanceof DateField d) {
            d.setOutline(value);
        }
    }
}
