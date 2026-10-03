package com.payroll.UI;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.auth.LoginWindow;
import com.payroll.UI.kit.Avatar;
import com.payroll.UI.kit.Badge;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.pages.AccessPage;
import com.payroll.UI.pages.AttendancePage;
import com.payroll.UI.pages.CompensationPage;
import com.payroll.UI.pages.EmployeesPage;
import com.payroll.UI.pages.HomePage;
import com.payroll.UI.pages.LeaveApprovalsPage;
import com.payroll.UI.pages.LeavePage;
import com.payroll.UI.pages.MyPayslipsPage;
import com.payroll.UI.pages.Page;
import com.payroll.UI.pages.PayrollPage;
import com.payroll.UI.pages.ProfilePage;
import com.payroll.UI.theme.Theme;
import com.payroll.security.AccessControl;
import com.payroll.security.Permission;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public class Dashboard extends JFrame {

    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(15);
    private static final int SIDEBAR_WIDTH = 248;

    private final AppContext app;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final JPanel navList = new JPanel();
    private final ButtonGroup navGroup = new ButtonGroup();
    private final Map<String, Section> sections = new LinkedHashMap<>();
    private final java.util.List<JComponent> expandedOnly = new java.util.ArrayList<>();
    private final JLabel crumbGroup = new JLabel();
    private final JLabel crumbTitle = new JLabel();
    private final JLabel date = new JLabel();
    private long lastActivity = System.currentTimeMillis();
    private final Timer idleTimer;
    private final AWTEventListener activityListener = e -> lastActivity = System.currentTimeMillis();
    private boolean closed;

    private record Section(String title, String group, Permission permission, NavItem item, Page page,
            JScrollPane scroller) {
    }

    public Dashboard(AppContext app) {
        AccessControl.requireAny(app.account(), Permission.VIEW_OWN_PROFILE);
        this.app = app;
        app.setNavigator(this::showSection);

        setTitle("ScootPH");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdown();
                dispose();
                System.exit(0);
            }
        });
        setIconImages(com.payroll.UI.theme.Brand.windowIcons());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);
        getContentPane().add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);
        main.add(buildTopBar(), BorderLayout.NORTH);
        content.setBackground(Theme.BACKGROUND);
        main.add(content, BorderLayout.CENTER);
        getContentPane().add(main, BorderLayout.CENTER);

        buildSections();

        sizeToScreen();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyResponsiveLayout();
            }
        });
        showSection("Home");

        if (!GraphicsEnvironment.isHeadless()) {
            Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                    AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
        idleTimer = new Timer(30_000, e -> {
            if (System.currentTimeMillis() - lastActivity > IDLE_TIMEOUT.toMillis()) {
                signOut("You were signed out after " + IDLE_TIMEOUT.toMinutes() + " minutes of inactivity.");
            }
        });
        idleTimer.start();
    }

    private void buildSections() {
        addGroup("Workspace");
        addSection("Workspace", "Home", "home", Permission.VIEW_OWN_PROFILE, HomePage::new);
        addSection("Workspace", "My Profile", "user", Permission.VIEW_OWN_PROFILE, ProfilePage::new);
        addSection("Workspace", "My Payslips", "wallet", Permission.VIEW_OWN_PAYROLL, MyPayslipsPage::new);
        addSection("Workspace", "Leave", "calendar-plus", Permission.REQUEST_LEAVE, LeavePage::new);

        if (AccessControl.hasAnyPermission(app.account(), Permission.MANAGE_EMPLOYEES,
                Permission.VIEW_EMPLOYEE_ATTENDANCE, Permission.APPROVE_LEAVE)) {
            addGroup("Human Resources");
            addSection("Human Resources", "Employees", "users", Permission.MANAGE_EMPLOYEES, EmployeesPage::new);
            addSection("Human Resources", "Attendance", "clock", Permission.VIEW_EMPLOYEE_ATTENDANCE, AttendancePage::new);
            addSection("Human Resources", "Leave Approvals", "calendar-check", Permission.APPROVE_LEAVE, LeaveApprovalsPage::new);
        }
        if (AccessControl.hasAnyPermission(app.account(), Permission.VIEW_EMPLOYEE_PAYROLL, Permission.MANAGE_PAYROLL)) {
            addGroup("Finance");
            addSection("Finance", "Payroll", "receipt", Permission.VIEW_EMPLOYEE_PAYROLL, PayrollPage::new);
            addSection("Finance", "Compensation", "banknote", Permission.MANAGE_PAYROLL, CompensationPage::new);
        }
        if (AccessControl.hasPermission(app.account(), Permission.MANAGE_USER_ROLES)) {
            addGroup("Administration");
            addSection("Administration", "User Access", "shield", Permission.MANAGE_USER_ROLES, AccessPage::new);
        }
        navList.add(Box.createVerticalGlue());
    }

    private void addGroup(String name) {
        JLabel label = Ui.overline(name);
        label.setBorder(BorderFactory.createEmptyBorder(navList.getComponentCount() == 0 ? 4 : 18, 12, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        navList.add(label);
        expandedOnly.add(label);
    }

    private void addSection(String group, String title, String icon, Permission permission,
            Function<AppContext, Page> factory) {
        if (!AccessControl.hasPermission(app.account(), permission)) {
            return;
        }
        Page page = factory.apply(app);
        JScrollPane scroller = scroller(page);
        content.add(scroller, title);
        NavItem item = new NavItem(title, icon);
        item.addActionListener(e -> showSection(title));
        navGroup.add(item);
        navList.add(item);
        navList.add(Box.createVerticalStrut(2));
        sections.put(title, new Section(title, group, permission, item, page, scroller));
    }

    public void showSection(String title) {
        Section section = sections.get(title);
        if (section == null || !AccessControl.hasPermission(app.account(), section.permission())) {
            Feedback.error(this, "You don't have access to that page.");
            return;
        }
        section.item().setSelected(true);
        sections.values().forEach(s -> s.item().refreshIcon());
        crumbGroup.setText(section.group() + "  /  ");
        crumbTitle.setText(title);
        cardLayout.show(content, title);
        section.scroller().getVerticalScrollBar().setValue(0);
        section.page().onShow();
    }

    public Page page(String title) {
        Section s = sections.get(title);
        return s == null ? null : s.page();
    }

    public java.util.Set<String> pageTitles() {
        return sections.keySet();
    }

    private JComponent buildSidebar() {
        sidebar.setBackground(Theme.SURFACE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        brand.add(com.payroll.UI.theme.Brand.wordmark(18));
        sidebar.add(brand, BorderLayout.NORTH);

        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JScrollPane navScroll = new JScrollPane(navList);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(buildUserCard(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JComponent buildUserCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 12)));
        card.add(new Avatar(app.displayName(), 36), BorderLayout.WEST);

        JPanel who = new JPanel(new BorderLayout(0, 2));
        who.setOpaque(false);
        JLabel name = Ui.label(app.displayName(), Font.BOLD, 13, Theme.TEXT);
        name.setToolTipText(app.displayName());
        who.add(name, BorderLayout.NORTH);
        who.add(Ui.caption(app.role().getDisplayName() + "  ·  @" + app.account().getEmpUserName()), BorderLayout.CENTER);
        card.add(who, BorderLayout.CENTER);
        expandedOnly.add(who);

        JButton logout = textButton("Sign out", "Sign out of ScootPH");
        logout.setHorizontalAlignment(SwingConstants.LEFT);
        logout.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        logout.addActionListener(e -> {
            if (Feedback.confirm(this, "Sign out", "Sign out of ScootPH?", "Sign out")) {
                signOut(null);
            }
        });
        who.add(logout, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 20)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        crumbGroup.setFont(Theme.font(Font.PLAIN, 14));
        crumbGroup.setForeground(Theme.TEXT_MUTED);
        crumbTitle.setFont(Theme.font(Font.BOLD, 14));
        crumbTitle.setForeground(Theme.TEXT);
        JPanel crumbs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        crumbs.setOpaque(false);
        crumbs.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 0));
        crumbs.add(crumbGroup);
        crumbs.add(crumbTitle);
        left.add(crumbs);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        right.setOpaque(false);
        date.setText(Ui.date(java.time.LocalDate.now()));
        date.setFont(Theme.font(Font.PLAIN, 13));
        date.setForeground(Theme.TEXT_MUTED);
        right.add(date);
        right.add(new Badge(app.role().getDisplayName()));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane scroller(Page page) {
        ResponsivePage holder = new ResponsivePage();
        holder.setBackground(Theme.BACKGROUND);
        holder.setBorder(Ui.padding(Ui.PAGE_PADDING));
        holder.add(page, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    private void applyResponsiveLayout() {
        int width = getContentPane().getWidth() > 0 ? getContentPane().getWidth() : getWidth();
        date.setVisible(width >= 900);
    }

    private void sizeToScreen() {
        Rectangle screen = GraphicsEnvironment.isHeadless() ? new Rectangle(1440, 900)
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(640, screen.width), Math.min(480, screen.height)));
        setSize(Math.min(1440, (int) (screen.width * 0.92)), Math.min(900, (int) (screen.height * 0.92)));
        setLocationRelativeTo(null);
        if (screen.width < 1500) {
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        }
        applyResponsiveLayout();
    }

    private void signOut(String message) {
        shutdown();
        dispose();
        LoginWindow.open(message);
    }

    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        idleTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        releaseSession();
    }

    protected void releaseSession() {
        app.services().close();
    }

    private static JButton textButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.BOLD, 12));
        button.setForeground(Theme.TEXT_MUTED);
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static final class NavItem extends JToggleButton {

        NavItem(String title, String iconName) {
            super(title);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setHorizontalAlignment(SwingConstants.LEFT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setContentAreaFilled(false);
            setBorderPainted(true);
            getAccessibleContext().setAccessibleName(title);
            refreshIcon();
        }

        void refreshIcon() {
            boolean on = isSelected();
            setFont(Theme.font(on ? Font.BOLD : Font.PLAIN, 14));
            setForeground(on ? Theme.TEXT : new java.awt.Color(0x4B, 0x55, 0x63));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, on ? Theme.PRIMARY : Theme.SURFACE),
                    BorderFactory.createEmptyBorder(8, 12, 8, 8)));
        }

        void setCollapsed(boolean collapsed) {
        }
    }

    private static final class ResponsivePage extends JPanel implements Scrollable {
        ResponsivePage() {
            super(new com.payroll.UI.kit.Stack(0));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height - 48 : visibleRect.width - 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
        }

        @Override
        public Dimension getPreferredSize() {
            int width = getParent() != null ? getParent().getWidth() : 0;
            if (width > 0 && getWidth() != width) {
                setSize(width, Math.max(1, getHeight()));
            }
            return super.getPreferredSize();
        }
    }
}
