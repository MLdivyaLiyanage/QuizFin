package com.perisic.fish.peripherals;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

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
    private final JTextField txuser = new JTextField(18);
    private final JPasswordField pass = new JPasswordField(18);
    private final JCheckBox remember = new JCheckBox("Remember me");
    private final RoundButton blogin = new RoundButton("Login");
    private final RoundButton createAccount = new RoundButton("Create account");
    private final JLabel loginSubtitle = Theme.label("Sign in to continue your adventure", Font.PLAIN, 13, Theme.MUTED);

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
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(960, 640));
        root.add(buildBrand(), BorderLayout.WEST);
        root.add(buildForm(), BorderLayout.CENTER);
        getContentPane().add(root, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);

        // events
        blogin.addActionListener(e -> doLogin());
        createAccount.addActionListener(e -> doRegister());
        getRootPane().setDefaultButton(blogin); // Enter key logs in

        setVisible(true);
        txuser.requestFocusInWindow();
    }

    private static JPanel buildBrand() {
        BufferedImage illustration = loadIllustration();
        JPanel left = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(illustration.getRGB(200, 0)));
                g2.fillOval(145, 77, 290, 290);
                g2.setColor(new Color(illustration.getRGB(0, 300)));
                g2.fillOval(-110, 406, 267, 267);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int imageHeight = getWidth() * illustration.getHeight() / illustration.getWidth();
                g2.drawImage(illustration, 0, 170, getWidth(), imageHeight, null);
                g2.dispose();
            }
        };
        left.setPreferredSize(new Dimension(420, 578));
        left.setBackground(new Color(illustration.getRGB(0, 0)));
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(46, 48, 0, 20));
        brand.add(Theme.label("QUIZFIN", Theme.display(34), Theme.TEXT));
        brand.add(Box.createVerticalStrut(8));
        brand.add(Theme.label("Two games. One adventure.", Font.PLAIN, 15, new Color(0x376681)));
        brand.add(Box.createVerticalStrut(10));
        brand.add(Theme.label("General knowledge + fish identification", Font.PLAIN, 13, Theme.MUTED));
        left.add(brand, BorderLayout.NORTH);
        return left;
    }

    static BufferedImage loadIllustration() {
        String resource = "/com/perisic/fish/peripherals/images/login-illustration.png";
        try (InputStream input = LoginGUI.class.getResourceAsStream(resource)) {
            BufferedImage image;
            if (input != null) {
                image = ImageIO.read(input);
            } else {
                // Also supports running javac output from the project directory.
                try (InputStream file = Files.newInputStream(Path.of(resource.substring(1)))) {
                    image = ImageIO.read(file);
                }
            }
            if (image == null) {
                throw new IOException("Unsupported login illustration format");
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load the login illustration", ex);
        }
    }

    private JPanel buildForm() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.BG);
        JPanel form = new RoundPanel(Color.WHITE, 36, new GridBagLayout());
        form.setPreferredSize(new Dimension(400, 470));
        form.setBorder(BorderFactory.createEmptyBorder(30, 30, 50, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 0, 3, 0);

        form.add(Theme.label("Welcome back!", Theme.display(28), Theme.TEXT), c);
        form.add(loginSubtitle, c);
        c.insets = new Insets(24, 0, 5, 0);
        form.add(Theme.label("Username", Font.BOLD, 14, Theme.TEXT), c);
        c.insets = new Insets(3, 0, 3, 0);
        form.add(buildInput(txuser), c);
        c.insets = new Insets(16, 0, 5, 0);
        form.add(Theme.label("Password", Font.BOLD, 14, Theme.TEXT), c);
        c.insets = new Insets(3, 0, 3, 0);
        form.add(buildInput(pass), c);
        remember.setOpaque(false);
        remember.setFont(Theme.font(Font.PLAIN, 13));
        remember.setForeground(Theme.MUTED);
        form.add(remember, c);
        c.insets = new Insets(10, 0, 0, 0);
        blogin.setFont(Theme.font(Font.BOLD, 16));
        blogin.setPreferredSize(new Dimension(340, 44));
        form.add(blogin, c);
        c.insets = new Insets(10, 0, 0, 0);
        createAccount.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.PRIMARY);
        createAccount.setPreferredSize(new Dimension(340, 40));
        form.add(createAccount, c);
        right.add(form);
        return right;
    }

    private static JPanel buildInput(JTextField field) {
        field.setOpaque(false);
        field.setFont(Theme.font(Font.PLAIN, 15));
        field.setForeground(new Color(0x376681));
        field.setCaretColor(Theme.TEXT);
        field.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JPanel input = new RoundPanel(new Color(0xF5F8FC), 14, new BorderLayout());
        input.setPreferredSize(new Dimension(340, 44));
        input.setMinimumSize(new Dimension(340, 44));
        input.add(field, BorderLayout.CENTER);
        if (field instanceof JPasswordField password) {
            input.add(passwordVisibilityButton(password), BorderLayout.EAST);
        }
        return input;
    }

    private static JToggleButton passwordVisibilityButton(JPasswordField password) {
        char mask = password.getEchoChar();
        JToggleButton toggle = new JToggleButton(new PasswordEyeIcon(false));
        toggle.setSelectedIcon(new PasswordEyeIcon(true));
        toggle.setContentAreaFilled(false);
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setPreferredSize(new Dimension(44, 44));
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.getAccessibleContext().setAccessibleName("Show password");
        toggle.setToolTipText("Show password");
        toggle.addItemListener(e -> {
            boolean visible = toggle.isSelected();
            password.setEchoChar(visible ? '\0' : mask);
            toggle.setToolTipText(visible ? "Hide password" : "Show password");
        });
        return toggle;
    }

    private record PasswordEyeIcon(boolean crossed) implements Icon {
        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.NAV_ACTIVE);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D eye = new Path2D.Float();
            eye.moveTo(1, 10);
            eye.curveTo(6, 2, 14, 2, 19, 10);
            eye.curveTo(14, 18, 6, 18, 1, 10);
            eye.closePath();
            g.draw(eye);
            g.drawOval(7, 7, 6, 6);
            if (crossed) {
                g.drawLine(3, 3, 17, 17);
            }
            g.dispose();
        }
    }

    private void doLogin() {
        try {
            Session s = services.auth.login(txuser.getText(), pass.getPassword(), remember.isSelected());
            openGame(s);
        } catch (AuthException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            pass.setText("");
            txuser.requestFocus();
        }
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
            try {
                if (!Arrays.equals(secret, repeated)) {
                    throw new AuthException("The passwords do not match.");
                }
                services.auth.register(username.getText(), secret);
                txuser.setText(username.getText().trim());
                pass.setText("");
                loginSubtitle.setText("Account created. Enter your password to log in.");
                dialog.dispose();
                SwingUtilities.invokeLater(pass::requestFocusInWindow);
            } catch (AuthException ex) {
                error.setText(ex.getMessage());
                username.requestFocusInWindow();
            } finally {
                Arrays.fill(secret, '\0');
                Arrays.fill(repeated, '\0');
                password.setText("");
                confirmation.setText("");
            }
        });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(submit);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
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
