package com.payroll.UI.kit;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.Map;

public class ResponsiveGrid implements LayoutManager2 {

    public static final Integer FULL = Integer.MAX_VALUE;

    private final int minColumnWidth;
    private final int maxColumns;
    private final int hgap;
    private final int vgap;
    private final Map<Component, Integer> spans = new HashMap<>();

    public ResponsiveGrid(int minColumnWidth, int maxColumns, int hgap, int vgap) {
        this.minColumnWidth = minColumnWidth;
        this.maxColumns = maxColumns;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        spans.put(comp, constraints instanceof Integer i ? i : 1);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        spans.put(comp, 1);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        spans.remove(comp);
    }

    public int columnsFor(int width) {
        if (width <= 0) {
            return maxColumns;
        }
        int cols = (width + hgap) / (minColumnWidth + hgap);
        return Math.max(1, Math.min(maxColumns, cols));
    }

    private int span(Component c, int cols) {
        int s = spans.getOrDefault(c, 1);
        return Math.max(1, Math.min(cols, s));
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return size(parent, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        return new Dimension(minColumnWidth + in.left + in.right, size(parent, false).height);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private Dimension size(Container parent, boolean preferred) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int cols = width > 0 ? columnsFor(width) : maxColumns;
            int colWidth = width > 0 ? (width - (cols - 1) * hgap) / cols : minColumnWidth;
            int height = 0;
            int rowHeight = 0;
            int used = 0;
            boolean first = true;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int s = span(c, cols);
                if (used + s > cols) {
                    height += rowHeight + vgap;
                    rowHeight = 0;
                    used = 0;
                }
                int w = colWidth * s + hgap * (s - 1);
                rowHeight = Math.max(rowHeight, heightFor(c, w, preferred));
                used += s;
                first = false;
            }
            if (!first) {
                height += rowHeight;
            }
            int prefWidth = width > 0 ? width : maxColumns * minColumnWidth + (maxColumns - 1) * hgap;
            return new Dimension(Math.min(prefWidth, minColumnWidth) + in.left + in.right,
                    height + in.top + in.bottom);
        }
    }

    static int heightFor(Component c, int width, boolean preferred) {
        if (c instanceof Container container && width > 0) {
            Dimension old = c.getSize();
            boolean resized = old.width != width;
            if (resized) {
                c.setSize(width, Math.max(1, old.height));
            }
            layoutTree(container);
            int h = (preferred ? c.getPreferredSize() : c.getMinimumSize()).height;
            if (resized) {
                c.setSize(old);
                layoutTree(container);
            }
            return h;
        }
        return (preferred ? c.getPreferredSize() : c.getMinimumSize()).height;
    }

    static void layoutTree(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container nested && child.isVisible()) {
                layoutTree(nested);
            }
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int cols = columnsFor(width);
            int colWidth = (width - (cols - 1) * hgap) / cols;
            int x0 = in.left;
            int y = in.top;
            int used = 0;
            int rowHeight = 0;
            java.util.List<Component> row = new java.util.ArrayList<>();
            java.util.List<Integer> rowSpans = new java.util.ArrayList<>();
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int s = span(c, cols);
                if (used + s > cols) {
                    placeRow(row, rowSpans, x0, y, colWidth, rowHeight);
                    y += rowHeight + vgap;
                    row.clear();
                    rowSpans.clear();
                    used = 0;
                    rowHeight = 0;
                }
                int w = colWidth * s + hgap * (s - 1);
                rowHeight = Math.max(rowHeight, heightFor(c, w, true));
                row.add(c);
                rowSpans.add(s);
                used += s;
            }
            placeRow(row, rowSpans, x0, y, colWidth, rowHeight);
        }
    }

    private void placeRow(java.util.List<Component> row, java.util.List<Integer> rowSpans, int x0, int y,
            int colWidth, int rowHeight) {
        int x = x0;
        for (int i = 0; i < row.size(); i++) {
            int s = rowSpans.get(i);
            int w = colWidth * s + hgap * (s - 1);
            row.get(i).setBounds(x, y, w, rowHeight);
            x += w + hgap;
        }
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0;
    }

    @Override
    public void invalidateLayout(Container target) {
    }
}
