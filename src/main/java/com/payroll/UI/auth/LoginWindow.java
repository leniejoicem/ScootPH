package com.payroll.UI.auth;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.AppContext;
import com.payroll.UI.Dashboard;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Form;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.domain.IT;
import com.payroll.service.AppServices;
import com.payroll.service.AuthService;
import com.payroll.service.AuthService.LoginResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class LoginWindow extends JFrame {

    public static volatile Supplier<AppServices> connector = () -> {
        try {
            return AppServices.connect();
        } catch (java.sql.SQLException e) {
            throw new com.payroll.DAO.DataAccessException("Could not connect", e);
        }
    };

    private AppServices services;
    private final CardLayout steps = new CardLayout();
    private final JPanel stepPanel = new JPanel(steps);
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JTextField code = new JTextField();
    private final Form credentials = new Form(1);
    private final Form secondFactor = new Form(1);
    private final JButton signIn = Ui.primary("Sign in");
    private final JButton verify = Ui.primary("Verify");
    private final JPanel banner = new JPanel(new BorderLayout(10, 0));
    private final JTextArea bannerText = Ui.paragraph("");
    private final JButton retry = Ui.link("Retry");
    private IT pending;
    private boolean connectionError;
    private boolean handedOff;

    public LoginWindow(String message) {
        super("Sign in · ScootPH");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setIconImages(com.payroll.UI.theme.Brand.windowIcons());

        BrandPanel brand = new BrandPanel();
        JPanel formSide = new JPanel(new GridBagLayout());
        formSide.setBackground(Theme.SURFACE);
        formSide.add(buildForm());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(brand, BorderLayout.WEST);
        getContentPane().add(formSide, BorderLayout.CENTER);
        setMinimumSize(new Dimension(420, 600));
        setSize(1040, 680);
        setLocationRelativeTo(null);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                brand.setVisible(getWidth() >= 860);
                brand.setPreferredSize(new Dimension(Math.max(380, getWidth() * 9 / 20), 0));
                getContentPane().revalidate();
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (!handedOff && services != null) {
                    services.close();
                }
            }
        });
        if (message != null) {
            showBanner(message, false);
        }
        connect();
    }

    public static void open(String message) {
        new LoginWindow(message).setVisible(true);
    }

    private JPanel buildForm() {
        JPanel column = Ui.transparent(new BorderLayout(0, 24));
        column.setPreferredSize(new Dimension(380, 520));

        JPanel head = Ui.transparent(new BorderLayout(0, 6));
        JLabel mark = com.payroll.UI.theme.Brand.wordmark(20);
        mark.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        head.add(mark, BorderLayout.NORTH);
        JPanel titles = Ui.transparent(new BorderLayout(0, 4));
        titles.add(Ui.label("Sign in", Font.BOLD, 26, Theme.TEXT), BorderLayout.NORTH);
        titles.add(Ui.muted("Use your ScootPH username and password."), BorderLayout.CENTER);
        head.add(titles, BorderLayout.CENTER);
        column.add(head, BorderLayout.NORTH);

        bannerText.setName("banner");
        bannerText.getAccessibleContext().setAccessibleName("Sign-in message");
        banner.setOpaque(true);
        banner.setBorder(Ui.padding(10, 12));
        banner.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        banner.add(bannerText, BorderLayout.CENTER);
        banner.add(retry, BorderLayout.EAST);
        banner.setVisible(false);
        retry.setVisible(false);
        retry.addActionListener(e -> connect());

        stepPanel.setOpaque(false);
        stepPanel.add(credentialsStep(), "credentials");
        stepPanel.add(codeStep(), "code");
        column.add(Ui.stack(16, banner, stepPanel), BorderLayout.CENTER);
        return column;
    }

    private JPanel credentialsStep() {
        username.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Your username");
        password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Your password");
        password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        credentials.add("username", new FormField("Username", username));
        credentials.add("password", new FormField("Password", password));

        JButton forgot = Ui.link("Forgot password?");
        forgot.addActionListener(e -> {
            if (services != null) {
                ForgotPasswordDialog.open(this, services);
            }
        });
        JButton create = Ui.link("Create an account");
        create.addActionListener(e -> {
            if (services != null && SignUpDialog.open(this, services)) {
                showBanner("Your account is ready. Sign in with your new username.", false);
            }
        });

        signIn.setPreferredSize(new Dimension(0, 42));
        signIn.addActionListener(e -> signIn());
        getRootPane().setDefaultButton(signIn);

        JPanel forgotRow = Ui.row(FlowLayout.RIGHT, forgot);
        JPanel createRow = Ui.row(FlowLayout.CENTER, Ui.muted("New to ScootPH?"), create);
        return Ui.stack(12, credentials, forgotRow, signIn, createRow);
    }

    private JPanel codeStep() {
        code.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "123 456");
        code.setFont(Theme.font(Font.BOLD, 20));
        code.setHorizontalAlignment(SwingConstants.CENTER);
        secondFactor.add("code", new FormField("Authentication code", code,
                "Open your authenticator app and enter the 6-digit code"));
        verify.setPreferredSize(new Dimension(0, 42));
        verify.addActionListener(e -> verifyCode());
        JButton back = Ui.link("Use a different account");
        back.addActionListener(e -> backToCredentials());
        JLabel title = Ui.heading("Two-step verification");
        return Ui.stack(12, title, secondFactor, verify, Ui.row(FlowLayout.CENTER, back));
    }

    private void connect() {
        signIn.setEnabled(false);
        retry.setVisible(false);
        Async.run(this, () -> connector.get(), s -> {
            services = s;
            signIn.setEnabled(true);
            if (connectionError) {
                connectionError = false;
                banner.setVisible(false);
            }
        }, error -> {
            connectionError = true;
            showBanner("Can't reach the database. Check that PostgreSQL is running and config/db.properties "
                    + "is correct.", true);
            retry.setVisible(true);
        });
    }

    private void signIn() {
        credentials.clearErrors();
        if (username.getText().isBlank() || password.getPassword().length == 0) {
            credentials.showErrors(username.getText().isBlank()
                    ? Map.of("username", "Enter your username") : Map.of("password", "Enter your password"));
            return;
        }
        if (services == null) {
            connect();
            return;
        }
        String user = username.getText();
        char[] pw = password.getPassword();
        Async.run(this, () -> services.auth().login(user, pw), this::handle, null, signIn);
    }

    private void handle(LoginResult result) {
        switch (result.outcome()) {
            case SUCCESS -> openDashboard(result.account());
            case TWO_FACTOR_REQUIRED -> {
                pending = result.account();
                banner.setVisible(false);
                code.setText("");
                steps.show(stepPanel, "code");
                getRootPane().setDefaultButton(verify);
                code.requestFocusInWindow();
            }
            case LOCKED -> {
                backToCredentials();
                long minutes = Math.max(1, (result.retryAfter().toSeconds() + 59) / 60);
                showBanner("Too many failed attempts. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes."),
                        true);
            }
            default -> {
                showBanner("The username or password is incorrect.", true);
                password.setText("");
                password.requestFocusInWindow();
            }
        }
    }

    private void verifyCode() {
        secondFactor.clearErrors();
        IT account = pending;
        String value = code.getText();
        Async.run(this, () -> services.auth().verifyLoginCode(account, value), result -> {
            if (result.outcome() == AuthService.Outcome.SUCCESS) {
                openDashboard(result.account());
            } else if (result.outcome() == AuthService.Outcome.LOCKED) {
                handle(result);
            } else {
                secondFactor.showErrors(Map.of("code", "That code didn't work. Enter the newest code."));
                code.selectAll();
            }
        }, null, verify);
    }

    private void backToCredentials() {
        pending = null;
        steps.show(stepPanel, "credentials");
        getRootPane().setDefaultButton(signIn);
    }

    private void openDashboard(IT account) {
        password.setText("");
        Dashboard dashboard = new Dashboard(new AppContext(services, account));
        handedOff = true;
        dashboard.setVisible(true);
        dispose();
    }

    private void showBanner(String text, boolean error) {
        retry.setVisible(error && connectionError);
        bannerText.setText(text);
        bannerText.setForeground(error ? Theme.DANGER : Theme.INFO);
        banner.setBackground(error ? Theme.DANGER_SOFT : Theme.INFO_SOFT);
        banner.setVisible(true);
        banner.revalidate();
    }

    private static final class BrandPanel extends JPanel {

        BrandPanel() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(460, 0));
            setBackground(new Color(0xE0, 0x52, 0x22));
            JPanel text = new JPanel(new BorderLayout(0, 10));
            text.setOpaque(false);
            text.setBorder(BorderFactory.createEmptyBorder(0, 44, 52, 44));
            JLabel name = Ui.label("ScootPH", Font.BOLD, 40, Color.WHITE);
            name.setIcon(com.payroll.UI.theme.Brand.scooter(64, Color.WHITE));
            name.setIconTextGap(14);
            text.add(name, BorderLayout.NORTH);
            add(text, BorderLayout.SOUTH);
        }
    }
}
