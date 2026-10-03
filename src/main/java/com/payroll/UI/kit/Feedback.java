package com.payroll.UI.kit;

import com.payroll.DAO.DataAccessException;
import com.payroll.service.ServiceException;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public final class Feedback {

    private static final Logger LOGGER = Logger.getLogger(Feedback.class.getName());

    public interface Handler {
        void success(Component anchor, String message);

        void error(Component anchor, String title, String message);

        void warn(Component anchor, String message);

        boolean confirm(Component anchor, String title, String message, String confirmText, boolean destructive);
    }

    public static final Handler UI = new Handler() {
        @Override
        public void success(Component anchor, String message) {
            Toast.show(anchor, message, Toast.Kind.SUCCESS);
        }

        @Override
        public void error(Component anchor, String title, String message) {
            JOptionPane.showMessageDialog(anchor, message, title, JOptionPane.WARNING_MESSAGE);
        }

        @Override
        public void warn(Component anchor, String message) {
            Toast.show(anchor, message, Toast.Kind.ERROR);
        }

        @Override
        public boolean confirm(Component anchor, String title, String message, String confirmText, boolean destructive) {
            Object[] options = {confirmText, "Cancel"};
            int choice = JOptionPane.showOptionDialog(anchor, message, title, JOptionPane.YES_NO_OPTION,
                    destructive ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            return choice == 0;
        }
    };

    public static volatile Handler handler = UI;

    private Feedback() {
    }

    public static void success(Component anchor, String message) {
        handler.success(anchor, message);
    }

    public static void warn(Component anchor, String message) {
        handler.warn(anchor, message);
    }

    public static void error(Component anchor, String message) {
        handler.error(anchor, "Something needs your attention", message);
    }

    public static boolean confirm(Component anchor, String title, String message, String confirmText) {
        return handler.confirm(anchor, title, message, confirmText, false);
    }

    public static boolean confirmDestructive(Component anchor, String title, String message, String confirmText) {
        return handler.confirm(anchor, title, message, confirmText, true);
    }

    public static void failure(Component anchor, Throwable error) {
        Throwable e = error instanceof java.util.concurrent.ExecutionException && error.getCause() != null
                ? error.getCause() : error;
        if (e instanceof ServiceException se) {
            handler.error(anchor, "Please check", se.getMessage());
        } else if (e instanceof DataAccessException) {
            LOGGER.log(Level.SEVERE, "Database error", e);
            handler.error(anchor, "Database problem",
                    "The database couldn't complete that request. Check your connection and try again.");
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected error", e);
            handler.error(anchor, "Unexpected error", "Something went wrong: " + e.getMessage());
        }
    }
}
