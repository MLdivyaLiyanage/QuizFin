package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.perisic.fish.auth.AuthException;
import com.perisic.fish.auth.Session;

/**
 * QuizFin login screen: login, "remember me" (session cookie) and
 * automatic login while that cookie is still valid.
 */
public class LoginGUI extends JFrame {

    private static final long serialVersionUID = -6921462126880570161L;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginGUI(new AppServices()));
    }

    private final transient AppServices services;
    private final LoginView view = new LoginView();
    private final JTextField txuser = view.txuser;
    private final JPasswordField pass = view.pass;
    private final JCheckBox remember = view.remember;
    private final RoundButton blogin = view.blogin;
    private final RoundButton createAccount = view.createAccount;
    private final JLabel loginSubtitle = view.loginSubtitle;
    private final JTextArea loginFeedback = view.loginFeedback;
    private boolean signingIn;

    LoginGUI(AppServices services) {
        super("QuizFin - Login");
        this.services = services;

        // "Cookie" login: a valid remember-me session skips the form.
        Session saved = services.auth.resume();
        if (saved != null) {
            openGame(saved);
            return;
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.Rectangle available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(720, available.width), Math.min(520, available.height)));
        setContentPane(view);
        pack();
        setSize(Math.min(getWidth(), available.width), Math.min(getHeight(), available.height));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // events
        blogin.addActionListener(e -> doLogin());
        createAccount.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(blogin); // Enter key logs in

        setVisible(true);
        txuser.requestFocusInWindow();
    }

    static BufferedImage loadIllustration() {
        String resource = "/com/perisic/fish/peripherals/images/QuizFin_Welcome_Panel.png";
        try (InputStream input = LoginGUI.class.getResourceAsStream(resource)) {
            BufferedImage image;
            if (input != null) {
                image = ImageIO.read(input);
            } else {
                // Also supports running javac output from the project directory.
                image = null;
                for (Path root : new Path[] { Path.of("QuizFin", "src"), Path.of("src"), Path.of(".") }) {
                    Path candidate = root.resolve(resource.substring(1));
                    if (Files.isRegularFile(candidate)) {
                        image = ImageIO.read(candidate.toFile());
                        break;
                    }
                }
            }
            if (image == null) {
                throw new IOException("Unsupported login illustration format");
            }
            return image;
        } catch (IOException ex) {
            BufferedImage fallback = new BufferedImage(420, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = fallback.createGraphics();
            g.setColor(Theme.SKY);
            g.fillRect(0, 0, 420, 400);
            g.translate(90, 90);
            Theme.paintBannerArt(g);
            g.dispose();
            return fallback;
        }
    }

    private static JPanel buildInput(JTextField field) {
        return LoginView.buildInput(field);
    }

    private void doLogin() {
        if (signingIn) return;
        String username = txuser.getText().trim();
        char[] secret = pass.getPassword();
        if (username.isEmpty() || secret.length == 0) {
            Arrays.fill(secret, '\0');
            loginFeedback.setText("Enter your username and password to continue.");
            (username.isEmpty() ? txuser : pass).requestFocusInWindow();
            return;
        }
        boolean keepSignedIn = remember.isSelected();
        signingIn = true;
        blogin.setEnabled(false);
        createAccount.setEnabled(false);
        txuser.setEnabled(false);
        pass.setEnabled(false);
        remember.setEnabled(false);
        blogin.setText("Signing in...");
        loginFeedback.setText("");
        new SwingWorker<Session, Void>() {
            @Override protected Session doInBackground() throws AuthException {
                try {
                    return services.auth.login(username, secret, keepSignedIn);
                } finally {
                    Arrays.fill(secret, '\0');
                }
            }
            @Override protected void done() {
                signingIn = false;
                blogin.setEnabled(true);
                createAccount.setEnabled(true);
                txuser.setEnabled(true);
                pass.setEnabled(true);
                remember.setEnabled(true);
                blogin.setText("Sign in");
                pass.setText("");
                if (!isDisplayable()) return;
                try {
                    openGame(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    loginFeedback.setText(cause instanceof AuthException ? cause.getMessage()
                            : "Sign in is unavailable. Please try again.");
                    pass.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void doRegister() {
        JDialog dialog = new JDialog(this, "Create your QuizFin account", true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTextField username = new JTextField(txuser.getText().trim(), 20);
        JPasswordField password = new JPasswordField(20);
        JPasswordField confirmation = new JPasswordField(20);

        JPanel surface = new JPanel(new BorderLayout());
        surface.setBackground(Theme.BG);
        surface.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CHIP_BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JPanel form = new RoundPanel(Theme.CARD, 36, new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        surface.add(form);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(Theme.label("QUIZFIN", Font.BOLD, 12, Theme.PRIMARY), c);
        c.insets = new Insets(5, 0, 0, 0);
        form.add(Theme.label("Create your account", Theme.display(28), Theme.TEXT), c);
        form.add(Theme.label("One account for both QuizFin games", Font.PLAIN, 13, Theme.MUTED), c);
        addRegistrationField(form, c, "Username", "3–20 letters, digits or underscores", username);
        addRegistrationField(form, c, "Password", "At least 6 characters", password);
        addRegistrationField(form, c, "Confirm password", null, confirmation);

        JTextArea error = new JTextArea(2, 0);
        error.setOpaque(false);
        error.setEditable(false);
        error.setFocusable(false);
        error.setLineWrap(true);
        error.setWrapStyleWord(true);
        error.setFont(Theme.nunitoSemiBold(13));
        error.setForeground(Theme.RED);
        error.getAccessibleContext().setAccessibleName("Account creation feedback");
        c.insets = new Insets(6, 0, 4, 0);
        form.add(error, c);

        RoundButton submit = new RoundButton("Create account");
        RoundButton cancel = new RoundButton("Cancel");
        cancel.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.PRIMARY);
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.add(submit, BorderLayout.CENTER);
        actions.add(cancel, BorderLayout.EAST);
        c.insets = new Insets(0, 0, 0, 0);
        form.add(actions, c);

        submit.addActionListener(e -> {
            char[] secret = password.getPassword();
            char[] repeated = confirmation.getPassword();
            boolean matching = Arrays.equals(secret, repeated);
            Arrays.fill(repeated, '\0');
            if (!matching) {
                Arrays.fill(secret, '\0');
                confirmation.setText("");
                error.setText("The passwords do not match. Please try again.");
                confirmation.requestFocusInWindow();
                return;
            }
            String name = username.getText().trim();
            submit.setEnabled(false);
            cancel.setEnabled(false);
            username.setEnabled(false);
            password.setEnabled(false);
            confirmation.setEnabled(false);
            submit.setText("Creating account...");
            error.setText("");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws AuthException {
                    try {
                        services.auth.register(name, secret);
                        return null;
                    } finally {
                        Arrays.fill(secret, '\0');
                    }
                }
                @Override protected void done() {
                    submit.setEnabled(true);
                    cancel.setEnabled(true);
                    username.setEnabled(true);
                    password.setEnabled(true);
                    confirmation.setEnabled(true);
                    submit.setText("Create account");
                    password.setText("");
                    confirmation.setText("");
                    try {
                        get();
                        txuser.setText(name);
                        pass.setText("");
                        loginSubtitle.setText("Account created. Sign in below.");
                        loginFeedback.setText("");
                        dialog.dispose();
                        SwingUtilities.invokeLater(pass::requestFocusInWindow);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        error.setText(cause instanceof AuthException ? cause.getMessage()
                                : "Could not create your account. Please try again.");
                        username.requestFocusInWindow();
                    }
                }
            }.execute();
        });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(submit);
        dialog.getRootPane().registerKeyboardAction(e -> { if (submit.isEnabled()) dialog.dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setContentPane(surface);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(username::requestFocusInWindow);
        dialog.setVisible(true);
        password.setText("");
        confirmation.setText("");
    }

    private static void addRegistrationField(JPanel form, GridBagConstraints c, String caption,
                                             String hint, JTextField field) {
        JLabel label = Theme.label(caption, Font.BOLD, 14, Theme.TEXT);
        label.setLabelFor(field);
        c.insets = new Insets(12, 0, 4, 0);
        form.add(label, c);
        c.insets = new Insets(0, 0, 0, 0);
        form.add(buildInput(field), c);
        if (hint != null) {
            field.getAccessibleContext().setAccessibleDescription(hint);
            c.insets = new Insets(3, 0, 0, 0);
            form.add(Theme.label(hint, Font.PLAIN, 12, Theme.MUTED), c);
        }
    }

    private void openGame(Session s) {
        new GameGUI(s, services).setVisible(true);
        dispose();
    }
}
