package com.payroll.UI.kit;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.payroll.UI.theme.Theme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

public class DataTable<T> extends JPanel {

    public enum Kind { TEXT, NUMBER, MONEY, DATE, DAY, TIME, BADGE }

    public record Column<T>(String header, Function<T, Object> value, Kind kind, int minWidth) {
        public static <T> Column<T> text(String header, Function<T, Object> value, int minWidth) {
            return new Column<>(header, value, Kind.TEXT, minWidth);
        }

        public static <T> Column<T> of(String header, Function<T, Object> value, Kind kind, int minWidth) {
            return new Column<>(header, value, kind, minWidth);
        }
    }

    private final List<Column<T>> columns;
    private final List<T> rows = new ArrayList<>();
    private final Model model = new Model();
    private final JTable table;
    private final TableRowSorter<Model> sorter;
    private final JTextField search;
    private final CardLayout cards = new CardLayout();
    private final JPanel views = new JPanel(cards);
    private final JLabel emptyTitle;
    private final JLabel emptyText;
    private final JScrollPane scroll;
    private String emptyDefaultTitle = "Nothing here yet";
    private String emptyDefaultText = "";

    @SafeVarargs
    public DataTable(String searchPlaceholder, Column<T>... columns) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        this.columns = List.of(columns);

        table = new JTable(model) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() != null && getParent().getWidth() >= minimumTableWidth();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new java.awt.Color(0xF0, 0xF1, 0xF4));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty(FlatClientProperties.STYLE, "selectionBackground: #FFF1EB; selectionForeground: #111827;"
                + "selectionInactiveBackground: #FFF1EB; selectionInactiveForeground: #111827");

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        for (int i = 0; i < columns.length; i++) {
            Column<T> c = columns[i];
            var tc = table.getColumnModel().getColumn(i);
            tc.setMinWidth(Math.max(56, (int) (c.minWidth() * 0.6)));
            tc.setPreferredWidth(c.minWidth());
            tc.setCellRenderer(c.kind() == Kind.BADGE ? Badge.renderer() : new Cells(c.kind()));
            sorter.setComparator(i, comparator(c.kind()));
        }

        scroll = new JScrollPane(table);
        scroll.setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), Theme.BORDER, 1, Theme.RADIUS));
        scroll.getViewport().setBackground(Theme.SURFACE);
        visibleRows(8);

        JPanel empty = new JPanel(new java.awt.GridBagLayout());
        empty.setBackground(Theme.SURFACE);
        empty.setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), Theme.BORDER, 1, Theme.RADIUS));
        emptyTitle = Ui.heading("Nothing here yet");
        emptyText = Ui.muted("");
        JPanel emptyText2 = Ui.transparent(new BorderLayout(0, 4));
        emptyTitle.setHorizontalAlignment(SwingConstants.CENTER);
        emptyText.setHorizontalAlignment(SwingConstants.CENTER);
        emptyText2.add(emptyTitle, BorderLayout.CENTER);
        emptyText2.add(emptyText, BorderLayout.SOUTH);
        empty.add(emptyText2);

        views.setOpaque(false);
        views.add(scroll, "table");
        views.add(empty, "empty");
        add(views, BorderLayout.CENTER);

        if (searchPlaceholder != null) {
            search = new JTextField();
            search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, searchPlaceholder);
            search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
            });
            JPanel top = Ui.transparent(new BorderLayout());
            top.add(search, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);
        } else {
            search = null;
        }
        setEmptyMessage("Nothing here yet", "");
        refreshEmptyState();
    }

    private int minimumTableWidth() {
        int sum = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            sum += table.getColumnModel().getColumn(i).getMinWidth();
        }
        return sum;
    }

    public JTable table() {
        return table;
    }

    public JTextField searchField() {
        return search;
    }

    public void visibleRows(int count) {
        table.setPreferredScrollableViewportSize(new Dimension(420, table.getRowHeight() * count));
    }

    public void setEmptyMessage(String title, String text) {
        emptyDefaultTitle = title;
        emptyDefaultText = text;
        refreshEmptyState();
    }

    public void setRows(List<T> data) {
        T selected = selected().orElse(null);
        rows.clear();
        rows.addAll(data);
        model.fireTableDataChanged();
        if (selected != null) {
            select(selected::equals);
        }
        refreshEmptyState();
    }

    public List<T> rows() {
        return List.copyOf(rows);
    }

    public int visibleRowCount() {
        return table.getRowCount();
    }

    public Optional<T> selected() {
        int view = table.getSelectedRow();
        return view < 0 ? Optional.empty() : Optional.of(rows.get(table.convertRowIndexToModel(view)));
    }

    public boolean select(Predicate<T> match) {
        for (int i = 0; i < rows.size(); i++) {
            if (match.test(rows.get(i))) {
                int view = table.convertRowIndexToView(i);
                if (view >= 0) {
                    table.setRowSelectionInterval(view, view);
                    table.scrollRectToVisible(table.getCellRect(view, 0, true));
                    return true;
                }
            }
        }
        return false;
    }

    public void clearSelection() {
        table.clearSelection();
    }

    public void onSelect(Consumer<T> listener) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selected().ifPresent(listener);
            }
        });
    }

    public void onOpen(Consumer<T> listener) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selected().ifPresent(listener);
                }
            }
        });
    }

    private void applyFilter() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        Object v = entry.getValue(i);
                        if (v != null && display(v, columns.get(i).kind()).toLowerCase(Locale.ROOT).contains(q)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        refreshEmptyState();
    }

    private void refreshEmptyState() {
        boolean filtered = search != null && !search.getText().isBlank();
        if (filtered && !rows.isEmpty()) {
            emptyTitle.setText("No matches");
            emptyText.setText("Nothing matches “" + search.getText().trim() + "”.");
        } else {
            emptyTitle.setText(emptyDefaultTitle);
            emptyText.setText(emptyDefaultText);
        }
        cards.show(views, table.getRowCount() == 0 ? "empty" : "table");
    }

    static String display(Object v, Kind kind) {
        if (v == null) {
            return "";
        }
        return switch (kind) {
            case MONEY -> v instanceof Number n ? Ui.peso(n.doubleValue()) : v.toString();
            case DATE -> v instanceof LocalDate d ? Ui.date(d) : v instanceof java.util.Date d ? Ui.date(d) : v.toString();
            case DAY -> v instanceof LocalDate d ? Ui.day(d) : v.toString();
            case TIME -> v instanceof LocalTime t ? Ui.time(t) : v.toString();
            default -> v.toString();
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparator<Object> comparator(Kind kind) {
        return (a, b) -> {
            if (a == null || b == null) {
                return a == null ? (b == null ? 0 : -1) : 1;
            }
            if (a instanceof Comparable ca && a.getClass().isInstance(b)) {
                return ca.compareTo(b);
            }
            return a.toString().compareToIgnoreCase(b.toString());
        };
    }

    private final class Model extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).header();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return columns.get(column).value().apply(rows.get(row));
        }
    }

    private static final class Cells extends DefaultTableCellRenderer {
        private final Kind kind;

        Cells(Kind kind) {
            this.kind = kind;
            setBorder(Ui.padding(0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, display(value, kind), isSelected, false, row, column);
            setBorder(Ui.padding(0, 12));
            setHorizontalAlignment(kind == Kind.MONEY || kind == Kind.NUMBER ? SwingConstants.RIGHT : SwingConstants.LEFT);
            String text = getText();
            setForeground(text.isEmpty() || "—".equals(text) ? Theme.TEXT_MUTED : Theme.TEXT);
            if (text.isEmpty()) {
                setText("—");
            }
            return this;
        }
    }
}
