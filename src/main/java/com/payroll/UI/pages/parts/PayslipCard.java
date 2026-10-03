package com.payroll.UI.pages.parts;

import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.service.Payslip;
import com.payroll.service.PayrollService;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PayslipCard extends Card {

    private final JPanel lines = Ui.transparent(new GridBagLayout());
    private int row;

    public PayslipCard() {
        super("Payslip", "Select a month");
        content(lines);
        showMessage("Choose a month to see the payslip.");
    }

    public void showMessage(String message) {
        setSubtitle(" ");
        lines.removeAll();
        row = 0;
        JLabel l = Ui.muted(message);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 0, 0);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weighty = 1;
        lines.add(l, c);
        refresh();
    }

    public void show(Payslip p) {
        setTitle("Payslip · " + PayrollService.monthLabel(p.period()));
        setSubtitle(p.employeeName() + " · " + Ui.orDash(p.position()) + " · " + p.daysWorked() + " days, "
                + p.hoursWorkedLabel() + " hours");
        lines.removeAll();
        row = 0;
        section("Earnings");
        line("Basic salary", p.basicSalary(), false);
        line("Rice subsidy", p.riceSubsidy(), false);
        line("Phone allowance", p.phoneAllowance(), false);
        line("Clothing allowance", p.clothingAllowance(), false);
        line("Gross pay", p.grossPay(), true);
        section("Contributions");
        line("SSS", -p.sss(), false);
        line("PhilHealth", -p.philHealth(), false);
        line("Pag-IBIG", -p.pagIbig(), false);
        line("Total contributions", -p.totalContributions(), true);
        section("Tax");
        line("Taxable income", p.taxableIncome(), false);
        line("Withholding tax", -p.withholdingTax(), true);
        net(p.netPay());
        refresh();
    }

    private void section(String title) {
        GridBagConstraints c = base();
        c.gridwidth = 2;
        c.insets = new Insets(row == 0 ? 0 : 14, 0, 6, 0);
        lines.add(Ui.overline(title), c);
        row++;
    }

    private void line(String label, double amount, boolean total) {
        GridBagConstraints c = base();
        c.weightx = 1;
        c.insets = new Insets(3, 0, 3, 12);
        JLabel l = total ? Ui.label(label, Font.BOLD, 13, Theme.TEXT) : Ui.text(label);
        lines.add(l, c);
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(3, 0, 3, 0);
        JLabel v = total ? Ui.label(money(amount), Font.BOLD, 13, Theme.TEXT) : Ui.text(money(amount));
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        lines.add(v, c);
        row++;
    }

    private void net(double amount) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Theme.PRIMARY_SOFT);
        box.setBorder(Ui.padding(14, 16));
        box.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 12");
        box.add(Ui.label("Net pay", Font.BOLD, 15, Theme.TEXT), BorderLayout.WEST);
        box.add(Ui.label(Ui.peso(amount), Font.BOLD, 22, Theme.PRIMARY), BorderLayout.EAST);
        GridBagConstraints c = base();
        c.gridwidth = 2;
        c.insets = new Insets(16, 0, 0, 0);
        lines.add(box, c);
        row++;
    }

    private static String money(double amount) {
        return amount < 0 ? "− " + Ui.peso(-amount) : Ui.peso(amount);
    }

    private GridBagConstraints base() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void refresh() {
        lines.revalidate();
        lines.repaint();
    }
}
