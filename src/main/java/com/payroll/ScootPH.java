package com.payroll;

import com.payroll.UI.auth.LoginWindow;
import com.payroll.UI.theme.Theme;
import javax.swing.SwingUtilities;

public final class ScootPH {

    private ScootPH() {
    }

    private static void setDockIcon() {
        try {
            if (java.awt.Taskbar.isTaskbarSupported()
                    && java.awt.Taskbar.getTaskbar().isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                java.awt.Taskbar.getTaskbar().setIconImage(com.payroll.UI.theme.Brand.mark(256).getImage());
            }
        } catch (UnsupportedOperationException | SecurityException e) {
        }
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "ScootPH");
        System.setProperty("apple.awt.application.appearance", "system");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            setDockIcon();
            LoginWindow.open(null);
        });
    }
}
