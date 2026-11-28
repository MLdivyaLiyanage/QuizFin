package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Account creation presentation and inline validation; persistence stays in AuthService. */
final class RegistrationView extends BackgroundPanel {
    private static final long serialVersionUID = 1L;
    final JTextField username = new JTextField(18);
    final JPasswordField password = new JPasswordField(18);
    final JPasswordField confirmation = new JPasswordField(18);
    final RoundButton submit = new RoundButton("Create account");
    final RoundButton cancel = new RoundButton("Back to sign in");
    final JTextArea feedback = new JTextArea(2, 24);
    final JLabel match = Theme.label(" ", Theme.nunito(Font.PLAIN, 12), Theme.MUTED);
    private final JPanel card;
    private boolean busy;

    RegistrationView(String initialUsername) {
        super(new BorderLayout());
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(560, 760));
        username.setText(initialUsername);
        card = new RoundPanel(Theme.CARD, 36, new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 6, 0);
        card.add(Theme.label("QUIZFIN", Font.BOLD, 12, Theme.PRIMARY), c);
        card.add(Theme.label("Create your account", Theme.display(27), Theme.TEXT), c);
        card.add(Theme.label("Play both games and keep your scores.", Font.PLAIN, 13, Theme.MUTED), c);
        addField(c, "Username", "3-20 letters, numbers or underscores.", username);
        addField(c, "Password", "Use at least 6 characters.", password);
        addField(c, "Confirm password", "Enter the same password again.", confirmation);
        c.insets = new Insets(5, 0, 0, 0);
        card.add(match, c);
        feedback.setEditable(false);
        feedback.setFocusable(false);
        feedback.setOpaque(false);
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);
        feedback.setFont(Theme.nunitoSemiBold(13));
        feedback.setForeground(Theme.RED);
        feedback.setVisible(false);
        feedback.getAccessibleContext().setAccessibleName("Account creation feedback");
        feedback.getDocument().addDocumentListener(listener(() -> {
            feedback.setVisible(feedback.getDocument().getLength() > 0);
            card.revalidate();
            card.repaint();
        }));
        c.insets = new Insets(8, 0, 0, 0);
        card.add(feedback, c);
        submit.setPreferredSize(new Dimension(340, 48));
        c.insets = new Insets(16, 0, 0, 0);
        card.add(submit, c);
        c.insets = new Insets(18, 0, 12, 0);
        card.add(new JSeparator(), c);
        c.insets = new Insets(0, 0, 7, 0);
        card.add(Theme.label("Already have an account?", Theme.nunito(Font.PLAIN, 12), Theme.MUTED), c);
        cancel.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.PRIMARY);
        cancel.setPreferredSize(new Dimension(340, 44));
        c.insets = new Insets(0, 0, 0, 0);
        card.add(cancel, c);

        JScrollPane scroll = new JScrollPane(new LoginView.FormSurface(card));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        username.getDocument().addDocumentListener(listener(() -> feedback.setText("")));
        Runnable passwordEdited = () -> {
            updateMatch();
            feedback.setText("");
        };
        password.getDocument().addDocumentListener(listener(passwordEdited));
        confirmation.getDocument().addDocumentListener(listener(passwordEdited));
        for (JComponent control : new JComponent[] {username, password, confirmation, submit, cancel}) {
            control.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) {
                    control.scrollRectToVisible(new Rectangle(0, 0, control.getWidth(), control.getHeight()));
                }
            });
        }
    }

    private void addField(GridBagConstraints c, String title, String hint, JTextField field) {
        JLabel label = Theme.label(title, Font.BOLD, 14, Theme.TEXT);
        label.setLabelFor(field);
        c.insets = new Insets(16, 0, 5, 0);
        card.add(label, c);
        c.insets = new Insets(0, 0, 0, 0);
        card.add(LoginView.buildInput(field), c);
        field.getAccessibleContext().setAccessibleDescription(hint);
        c.insets = new Insets(4, 0, 0, 0);
        card.add(Theme.label(hint, Theme.nunito(Font.PLAIN, 11), Theme.MUTED), c);
    }

    /** Immediate feedback; AuthService remains the authoritative validator. */
    boolean validateInput() {
        if (busy) return false;
        char[] secret = password.getPassword();
        char[] repeated = confirmation.getPassword();
        try {
            if (!username.getText().trim().matches("[A-Za-z0-9_]{3,20}"))
                return invalid(username, "Choose a username with 3-20 letters, numbers or underscores.");
            if (secret.length < 6)
                return invalid(password, "Use a password with at least 6 characters.");
            if (!Arrays.equals(secret, repeated))
                return invalid(confirmation, "The passwords do not match. Enter the same password in both fields.");
            feedback.setText("");
            return true;
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(repeated, '\0');
        }
    }

    private boolean invalid(JTextField field, String message) {
        feedback.setText(message);
        field.requestFocusInWindow();
        field.scrollRectToVisible(new Rectangle(0, 0, field.getWidth(), field.getHeight()));
        return false;
    }

    private void updateMatch() {
        char[] secret = password.getPassword();
        char[] repeated = confirmation.getPassword();
        try {
            if (repeated.length == 0) {
                match.setText(" ");
                match.setForeground(Theme.MUTED);
            } else {
                boolean equal = Arrays.equals(secret, repeated);
                match.setText(equal ? "Passwords match" : "Passwords do not match yet");
                match.setForeground(equal ? Theme.GREEN : Theme.MUTED);
            }
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(repeated, '\0');
        }
    }

    boolean isBusy() { return busy; }

    void setBusy(boolean value) {
        busy = value;
        username.setEnabled(!value);
        password.setEnabled(!value);
        confirmation.setEnabled(!value);
        submit.setEnabled(!value);
        cancel.setEnabled(!value);
        for (JPasswordField field : new JPasswordField[] {password, confirmation})
            for (java.awt.Component child : field.getParent().getComponents())
                if (child instanceof JToggleButton toggle) toggle.setEnabled(!value);
        submit.setText(value ? "Creating account..." : "Create account");
    }

    void clearPasswords() {
        password.setText("");
        confirmation.setText("");
        for (JPasswordField field : new JPasswordField[] {password, confirmation})
            for (java.awt.Component child : field.getParent().getComponents())
                if (child instanceof JToggleButton toggle) toggle.setSelected(false);
    }

    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }
}
