package com.payroll.UI.pages;

import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Stack;
import javax.swing.JPanel;

public abstract class Page extends JPanel {

    protected final AppContext app;

    protected Page(AppContext app) {
        super(new Stack(20));
        setOpaque(false);
        this.app = app;
    }

    public void onShow() {
    }
}
