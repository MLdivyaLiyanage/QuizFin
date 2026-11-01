package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
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
        root.setPreferredSize(new Dimension(812, 578));
        root.add(buildBrand(), BorderLayout.WEST);
        root.add(buildForm(txuser, pass, remember, blogin), BorderLayout.CENTER);
        getContentPane().add(root, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);

        // events
        blogin.addActionListener(e -> doLogin());
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
        brand.add(Theme.label("QUIZFIN", Font.BOLD, 26, Theme.TEXT));
        brand.add(Box.createVerticalStrut(8));
        brand.add(Theme.label("Two games. One adventure.", Font.BOLD, 14, new Color(0x376681)));
        brand.add(Box.createVerticalStrut(10));
        brand.add(Theme.label("General knowledge + fish identification", Font.PLAIN, 10, Theme.MUTED));
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

    private static JPanel buildForm(JTextField txuser, JPasswordField pass,
                                    JCheckBox remember, RoundButton blogin) {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.BG);
        JPanel form = new RoundPanel(Color.WHITE, 36, new GridBagLayout());
        form.setPreferredSize(new Dimension(316, 400));
        form.setBorder(BorderFactory.createEmptyBorder(30, 30, 50, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 0, 3, 0);

        form.add(Theme.label("Welcome back!", Font.BOLD, 20, Theme.TEXT), c);
        form.add(Theme.label("Sign in to continue your adventure", Font.PLAIN, 10, Theme.MUTED), c);
        c.insets = new Insets(24, 0, 5, 0);
        form.add(Theme.label("Username", Font.BOLD, 10, Theme.TEXT), c);
        c.insets = new Insets(3, 0, 3, 0);
        form.add(buildInput(txuser), c);
        c.insets = new Insets(16, 0, 5, 0);
        form.add(Theme.label("Password", Font.BOLD, 10, Theme.TEXT), c);
        c.insets = new Insets(3, 0, 3, 0);
        form.add(buildInput(pass), c);
        remember.setOpaque(false);
        remember.setFont(Theme.font(Font.PLAIN, 10));
        remember.setForeground(Theme.MUTED);
        form.add(remember, c);
        c.insets = new Insets(10, 0, 0, 0);
        blogin.setFont(Theme.font(Font.BOLD, 10));
        blogin.setPreferredSize(new Dimension(254, 32));
        form.add(blogin, c);
        c.insets = new Insets(24, 0, 0, 0);
        JLabel footer = Theme.label("Ready to play? Your adventure awaits.", Font.PLAIN, 9, Theme.MUTED);
        footer.setHorizontalAlignment(JLabel.CENTER);
        form.add(footer, c);
        right.add(form);
        return right;
    }

    private static JPanel buildInput(JTextField field) {
        field.setOpaque(false);
        field.setFont(Theme.font(Font.PLAIN, 11));
        field.setForeground(new Color(0x376681));
        field.setCaretColor(Theme.TEXT);
        field.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JPanel input = new RoundPanel(new Color(0xF5F8FC), 14, new BorderLayout());
        input.setPreferredSize(new Dimension(254, 36));
        input.setMinimumSize(new Dimension(254, 36));
        input.add(field, BorderLayout.CENTER);
        return input;
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

    private void openGame(Session s) {
        new GameGUI(s, services).setVisible(true);
        dispose();
    }
}
