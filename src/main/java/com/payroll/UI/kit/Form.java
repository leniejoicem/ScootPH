package com.payroll.UI.kit;

import com.payroll.service.ServiceException;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class Form extends JPanel {

    private final Map<String, FormField> fields = new LinkedHashMap<>();

    public Form(int columns) {
        super(new ResponsiveGrid(240, columns, 16, 4));
        setOpaque(false);
    }

    public FormField add(String key, FormField field) {
        return add(key, field, 1);
    }

    public FormField add(String key, FormField field, int span) {
        fields.put(key, field);
        super.add(field, Integer.valueOf(span));
        return field;
    }

    public void section(String title) {
        JPanel holder = Ui.transparent(new java.awt.BorderLayout());
        holder.setBorder(javax.swing.BorderFactory.createEmptyBorder(getComponentCount() == 0 ? 0 : 12, 0, 4, 0));
        holder.add(Ui.overline(title));
        super.add(holder, ResponsiveGrid.FULL);
    }

    public FormField field(String key) {
        return fields.get(key);
    }

    public void clearErrors() {
        fields.values().forEach(f -> f.setError(null));
    }

    public boolean showErrors(Map<String, String> errors) {
        clearErrors();
        FormField first = null;
        for (var e : errors.entrySet()) {
            FormField f = fields.get(e.getKey());
            if (f != null) {
                f.setError(e.getValue());
                if (first == null) {
                    first = f;
                }
            }
        }
        if (first != null) {
            JComponent input = first.input();
            Component focus = input instanceof javax.swing.JScrollPane sp ? sp.getViewport().getView() : input;
            focus.requestFocusInWindow();
            input.scrollRectToVisible(input.getBounds());
            return true;
        }
        return false;
    }

    public void showFailure(Throwable error) {
        if (error instanceof ServiceException se && !se.getFieldErrors().isEmpty() && showErrors(se.getFieldErrors())) {
            Feedback.warn(this, se.getMessage());
            return;
        }
        clearErrors();
        Feedback.failure(this, error);
    }

    public Map<String, FormField> fields() {
        return fields;
    }
}
