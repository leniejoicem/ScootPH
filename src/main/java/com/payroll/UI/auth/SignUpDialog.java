package com.payroll.UI.auth;

import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.pages.parts.EmployeeEditor;
import com.payroll.UI.theme.Theme;
import com.payroll.service.AppServices;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class SignUpDialog extends JDialog {

    private final EmployeeEditor editor = new EmployeeEditor(false, 2);
    private final JButton create = Ui.primary("Create account");
    private boolean created;

    public SignUpDialog(Component parent, AppServices services) {
        super(SwingUtilities.getWindowAncestor(parent), "Create your account", ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.SURFACE);
        root.setBorder(Ui.padding(24));

        JPanel head = Ui.transparent(new BorderLayout(0, 4));
        head.add(Ui.title("Create your account"), BorderLayout.NORTH);
        head.add(Ui.muted("Tell us about yourself. HR will review your details and set up your pay."), BorderLayout.CENTER);
        root.add(head, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(new FormHolder(editor));
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        root.add(scroll, BorderLayout.CENTER);

        JButton cancel = Ui.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        root.add(Ui.actions(cancel, create), BorderLayout.SOUTH);
        create.addActionListener(e -> Async.run(this, () -> services.employees().signUp(editor.toForm()), id -> {
            created = true;
            dispose();
        }, editor::showFailure, create));

        setContentPane(root);
        setMinimumSize(new Dimension(420, 480));
        setSize(760, 760);
        setLocationRelativeTo(parent);

        create.setEnabled(false);
        Async.run(this, () -> new Object[]{services.employees().positions(), services.employees().statuses(),
                services.employees().directory()}, d -> {
            @SuppressWarnings("unchecked")
            var positions = (java.util.List<com.payroll.subdomain.EmployeePosition>) d[0];
            @SuppressWarnings("unchecked")
            var statuses = (java.util.List<com.payroll.subdomain.EmployeeStatus>) d[1];
            @SuppressWarnings("unchecked")
            var people = (java.util.List<com.payroll.domain.Person>) d[2];
            editor.setChoices(positions, statuses, people);
            create.setEnabled(true);
        });
    }

    public boolean wasCreated() {
        return created;
    }

    public static boolean open(Component parent, AppServices services) {
        SignUpDialog d = new SignUpDialog(parent, services);
        d.setVisible(true);
        if (d.wasCreated()) {
            Feedback.success(parent, "Account created.");
        }
        return d.wasCreated();
    }

    static final class FormHolder extends JPanel implements javax.swing.Scrollable {
        FormHolder(Component form) {
            super(new com.payroll.UI.kit.Stack(0));
            setOpaque(false);
            add(form);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return r.height - 48; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        @Override
        public Dimension getPreferredSize() {
            int width = getParent() != null ? getParent().getWidth() : 0;
            if (width > 0 && getWidth() != width) {
                setSize(width, Math.max(1, getHeight()));
            }
            return super.getPreferredSize();
        }
    }
}
