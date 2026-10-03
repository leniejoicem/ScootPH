package com.payroll.UI.kit;

import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public final class Async {

    public static volatile boolean synchronous = false;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "scootph-db");
        t.setDaemon(true);
        return t;
    });

    private Async() {
    }

    public static <T> void run(Component anchor, Supplier<T> work, Consumer<T> onSuccess) {
        run(anchor, work, onSuccess, null);
    }

    public static <T> void run(Component anchor, Supplier<T> work, Consumer<T> onSuccess,
            Consumer<Throwable> onError, JComponent... busy) {
        if (synchronous) {
            T result;
            try {
                result = work.get();
            } catch (RuntimeException e) {
                handle(anchor, onError, e);
                return;
            }
            onSuccess.accept(result);
            return;
        }
        setBusy(anchor, busy, true);
        WORKER.execute(() -> {
            T result;
            try {
                result = work.get();
            } catch (Throwable e) {
                SwingUtilities.invokeLater(() -> {
                    setBusy(anchor, busy, false);
                    handle(anchor, onError, e);
                });
                return;
            }
            SwingUtilities.invokeLater(() -> {
                setBusy(anchor, busy, false);
                onSuccess.accept(result);
            });
        });
    }

    public static void run(Component anchor, Runnable work, Runnable onSuccess, JComponent... busy) {
        run(anchor, () -> {
            work.run();
            return Boolean.TRUE;
        }, ignored -> onSuccess.run(), null, busy);
    }

    private static void handle(Component anchor, Consumer<Throwable> onError, Throwable e) {
        if (onError != null) {
            onError.accept(e);
        } else {
            Feedback.failure(anchor, e);
        }
    }

    private static void setBusy(Component anchor, JComponent[] busy, boolean on) {
        for (JComponent c : busy) {
            c.setEnabled(!on);
        }
        if (anchor != null) {
            anchor.setCursor(on ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
        }
    }
}
