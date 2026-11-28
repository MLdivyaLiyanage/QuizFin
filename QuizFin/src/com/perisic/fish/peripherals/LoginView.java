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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Resizable login presentation, separate from authentication and window lifecycle. */
final class LoginView extends BackgroundPanel {
    private static final long serialVersionUID = 1L;
    final JTextField txuser = new JTextField(18);
    final JPasswordField pass = new JPasswordField(18);
    final JCheckBox remember = new JCheckBox("Remember me");
    final RoundButton blogin = new RoundButton("Sign in");
    final RoundButton createAccount = new RoundButton("Create account");
    final JLabel loginSubtitle = Theme.label("Your next adventure starts here.", Theme.nunito(Font.PLAIN, 16), Theme.MUTED);
    final JTextArea loginFeedback = new JTextArea(2, 24);
    private final JPanel brand;
    private final JScrollPane formScroll;

    LoginView() {
        super(null);
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(1000, 680));
        brand = buildBrand();
        FormSurface surface = new FormSurface(buildForm());
        formScroll = new JScrollPane(surface);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(24);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        add(brand);
        add(formScroll);
    }

    @Override public void doLayout() {
        boolean twoColumns = getWidth() >= 860;
        int inset = twoColumns ? 16 : 0;
        int areaWidth = Math.min(1120, Math.max(0, getWidth() - inset * 2));
        int areaHeight = Math.min(740, getHeight());
        int x = (getWidth() - areaWidth) / 2;
        int y = (getHeight() - areaHeight) / 2;
        int brandWidth = twoColumns ? (int) (areaWidth * 0.42) : 0;
        brand.setVisible(twoColumns);
        brand.setBounds(x, y, brandWidth, areaHeight);
        formScroll.setBounds(x + brandWidth, y, areaWidth - brandWidth, areaHeight);
    }

    /** Centre a bounded-width card, and scroll vertically when the window is too short. */
    static final class FormSurface extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        private final JPanel card;
        FormSurface(JPanel card) {
            super(null);
            this.card = card;
            setOpaque(false);
            add(card);
        }
        @Override public Dimension getPreferredSize() {
            return new Dimension(488, Math.max(600, card.getPreferredSize().height + 48));
        }
        @Override public void doLayout() {
            int width = Math.min(440, Math.max(0, getWidth() - 48));
            int height = card.getPreferredSize().height;
            card.setBounds((getWidth() - width) / 2, Math.max(24, (getHeight() - height) / 2), width, height);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
        }
        @Override public int getScrollableUnitIncrement(Rectangle r, int axis, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int axis, int direction) { return Math.max(24, r.height - 24); }
    }

    /** The welcome artwork is a complete panel; preserve its aspect ratio and all its text. */
    private static JPanel buildBrand() {
        BufferedImage illustration = LoginGUI.loadIllustration();
        JPanel left = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                double scale = Math.min((double) getWidth() / illustration.getWidth(),
                        (double) getHeight() / illustration.getHeight());
                int width = Math.max(1, (int) Math.round(illustration.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(illustration.getHeight() * scale));
                g2.drawImage(illustration, (getWidth() - width) / 2, (getHeight() - height) / 2,
                        width, height, null);
                g2.dispose();
            }
        };
        left.setOpaque(false);
        left.getAccessibleContext().setAccessibleName("QuizFin welcome panel");
        left.getAccessibleContext().setAccessibleDescription(
                "QuizFin. Two games, one adventure. Build your knowledge and train your memory. "
                + "General Quiz: Think, Learn, Win. Fish Challenge: Remember, Match, Beat. "
                + "Learn, Remember, Beat your best.");
        return left;
    }

    private JPanel buildForm() {
        JPanel form = new RoundPanel(Color.WHITE, 36, new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 0, 3, 0);

        form.add(Theme.label("LET'S PLAY", Theme.nunito(Font.BOLD, 12), Theme.PRIMARY), c);
        c.insets = new Insets(8, 0, 4, 0);
        form.add(Theme.label("Welcome back!", Theme.display(34), Theme.TEXT), c);
        form.add(loginSubtitle, c);
        c.insets = new Insets(20, 0, 5, 0);
        JLabel usernameLabel = Theme.label("Username", Theme.nunito(Font.BOLD, 15), Theme.TEXT);
        usernameLabel.setLabelFor(txuser);
        form.add(usernameLabel, c);
        c.insets = new Insets(3, 0, 3, 0);
        txuser.setToolTipText("Enter the username you registered with");
        form.add(buildInput(txuser), c);
        c.insets = new Insets(16, 0, 5, 0);
        JLabel passwordLabel = Theme.label("Password", Theme.nunito(Font.BOLD, 15), Theme.TEXT);
        passwordLabel.setLabelFor(pass);
        form.add(passwordLabel, c);
        c.insets = new Insets(3, 0, 3, 0);
        form.add(buildInput(pass), c);
        remember.setOpaque(false);
        remember.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        remember.setFont(Theme.nunito(Font.PLAIN, 14));
        remember.setForeground(Theme.MUTED);
        remember.setToolTipText("Stay signed in on this device for up to 7 days, unless you log out");
        form.add(remember, c);
        loginFeedback.setEditable(false);
        loginFeedback.setFocusable(false);
        loginFeedback.setOpaque(false);
        loginFeedback.setLineWrap(true);
        loginFeedback.setWrapStyleWord(true);
        loginFeedback.setFont(Theme.nunitoSemiBold(14));
        loginFeedback.setForeground(Theme.RED);
        loginFeedback.getAccessibleContext().setAccessibleName("Sign in feedback");
        loginFeedback.setVisible(false);
        loginFeedback.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                loginFeedback.setVisible(loginFeedback.getDocument().getLength() > 0);
                form.revalidate();
                form.repaint();
            }
            @Override public void insertUpdate(DocumentEvent event) { update(); }
            @Override public void removeUpdate(DocumentEvent event) { update(); }
            @Override public void changedUpdate(DocumentEvent event) { update(); }
        });
        form.add(loginFeedback, c);
        c.insets = new Insets(18, 0, 0, 0);
        blogin.setFont(Theme.font(Font.BOLD, 16));
        blogin.setPreferredSize(new Dimension(340, 48));
        form.add(blogin, c);
        c.insets = new Insets(18, 0, 12, 0);
        JSeparator separator = new JSeparator();
        separator.setForeground(Theme.CHIP_BORDER);
        separator.setBackground(Color.WHITE);
        form.add(separator, c);
        c.insets = new Insets(0, 0, 6, 0);
        form.add(Theme.label("New to QuizFin?",
                Theme.nunito(Font.PLAIN, 14), Theme.MUTED), c);
        c.insets = new Insets(2, 0, 0, 0);
        createAccount.setPreferredSize(new Dimension(340, 44));
        createAccount.setColors(Color.WHITE, Theme.PRIMARY, Theme.PRIMARY);
        form.add(createAccount, c);
        // Keep keyboard-focused controls visible when the form needs scrolling.
        for (JComponent control : new JComponent[] {txuser, pass, remember, blogin, createAccount}) {
            control.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) {
                    control.scrollRectToVisible(new Rectangle(0, 0, control.getWidth(), control.getHeight()));
                }
            });
        }
        return form;
    }

    static JPanel buildInput(JTextField field) {
        field.setOpaque(false);
        field.setFont(Theme.nunito(Font.PLAIN, 16));
        field.setForeground(new Color(0x376681));
        field.setCaretColor(Theme.TEXT);
        field.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        JPanel input = new RoundPanel(new Color(0xF5F8FC), 14, new BorderLayout());
        input.setPreferredSize(new Dimension(340, 50));
        input.setMinimumSize(new Dimension(180, 50));
        input.add(field, BorderLayout.CENTER);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                input.setBorder(BorderFactory.createLineBorder(Theme.PRIMARY, 2, true));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                input.setBorder(BorderFactory.createLineBorder(Theme.CHIP_BORDER, 2, true));
            }
        });
        input.setBorder(BorderFactory.createLineBorder(Theme.CHIP_BORDER, 2, true));
        if (field instanceof JPasswordField password) {
            input.add(passwordVisibilityButton(password), BorderLayout.EAST);
        }
        return input;
    }

    private static JToggleButton passwordVisibilityButton(JPasswordField password) {
        char mask = password.getEchoChar();
        JToggleButton toggle = new JToggleButton(new PasswordEyeIcon(false));
        toggle.setText("Show");
        toggle.setFont(Theme.font(Font.PLAIN, 11));
        toggle.setForeground(Theme.NAV_ACTIVE);
        toggle.setSelectedIcon(new PasswordEyeIcon(true));
        toggle.setContentAreaFilled(false);
        toggle.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        toggle.setPreferredSize(new Dimension(78, 44));
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.getAccessibleContext().setAccessibleName("Show password");
        toggle.setToolTipText("Show password");
        toggle.addItemListener(e -> {
            boolean visible = toggle.isSelected();
            toggle.setText(visible ? "Hide" : "Show");
            password.setEchoChar(visible ? '\0' : mask);
            toggle.setToolTipText(visible ? "Hide password" : "Show password");
            toggle.getAccessibleContext().setAccessibleName(visible ? "Hide password" : "Show password");
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

}
