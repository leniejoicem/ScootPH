package com.payroll.UI.kit;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

public class Stack implements LayoutManager {

    private final int gap;

    public Stack(int gap) {
        this.gap = gap;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int height = 0;
            int maxWidth = 0;
            boolean first = true;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                height += (first ? 0 : gap) + ResponsiveGrid.heightFor(c, width, true);
                maxWidth = Math.max(maxWidth, c.getMinimumSize().width);
                first = false;
            }
            return new Dimension(maxWidth + in.left + in.right, height + in.top + in.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int y = in.top;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int h = ResponsiveGrid.heightFor(c, width, true);
                c.setBounds(in.left, y, width, h);
                y += h + gap;
            }
        }
    }
}
