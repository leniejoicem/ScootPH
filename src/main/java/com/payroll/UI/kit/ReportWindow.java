package com.payroll.UI.kit;

import java.awt.Component;
import java.awt.Window;
import javax.swing.WindowConstants;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

public final class ReportWindow {

    public static volatile java.util.function.BiConsumer<String, JasperPrint> opener = ReportWindow::open;

    private ReportWindow() {
    }

    public static void show(String title, JasperPrint print) {
        opener.accept(title, print);
    }

    private static void open(String title, JasperPrint print) {
        javax.swing.JFrame frame = new javax.swing.JFrame(title + " · ScootPH");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(new JRViewer(print));
        frame.setSize(900, 1000);
        Window active = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
        frame.setLocationRelativeTo(active);
        frame.setVisible(true);
    }

    public static void open(Component anchor, String title, java.util.function.Supplier<JasperPrint> report,
            javax.swing.JComponent... busy) {
        Async.run(anchor, report, print -> show(title, print), null, busy);
    }
}
