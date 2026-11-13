package com.perisic.fish.peripherals;

// AI-assisted implementation (OpenAI Codex, 2026-11-11). See SOURCES.md.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** Branded confirmation with an explicit, safe default action. */
final class ConfirmDialog {
    private ConfirmDialog() { }

    static boolean leaveChallenge(Window owner) {
        JDialog dialog = new JDialog(owner, "Leave challenge?", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        boolean[] confirmed = { false };
        JPanel content = leaveChallengeContent(leave -> {
            confirmed[0] = leave;
            dialog.dispose();
        });
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton((RoundButton) content.getClientProperty("keepPlaying"));
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> ((RoundButton) content.getClientProperty("keepPlaying")).requestFocusInWindow());
        dialog.setVisible(true);
        return confirmed[0];
    }

    /** Content is separate so its layout and actions can be checked without opening a window. */
    static JPanel leaveChallengeContent(Consumer<Boolean> choice) {
        JPanel surface = new JPanel(new BorderLayout());
        surface.setBackground(Theme.BG);
        surface.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CHIP_BORDER), new EmptyBorder(12, 12, 12, 12)));
        RoundPanel card = new RoundPanel(Theme.CARD, 28, new BorderLayout(0, 20));
        card.setBorder(new EmptyBorder(24, 26, 26, 26));
        surface.add(card);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(Theme.label("QUIZFIN  /  CHALLENGE", Font.BOLD, 12, Theme.PRIMARY), BorderLayout.WEST);
        RoundButton close = new RoundButton("×");
        close.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        close.setBorder(new EmptyBorder(0, 0, 0, 0));
        close.setPreferredSize(new Dimension(32, 32));
        close.setColors(Theme.CHIP, null, Theme.MUTED);
        close.setToolTipText("Close and keep playing (Esc)");
        close.getAccessibleContext().setAccessibleName("Close and keep playing");
        close.addActionListener(e -> choice.accept(false));
        heading.add(close, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
        JLabel badge = Theme.label("CHALLENGE IN PROGRESS", Font.BOLD, 11, new Color(0x8B510E));
        badge.setOpaque(true);
        badge.setBackground(new Color(0xFFF3DC));
        badge.setBorder(new EmptyBorder(7, 10, 7, 10));
        badge.setAlignmentX(0);
        message.add(badge);
        message.add(Box.createVerticalStrut(14));
        JLabel title = Theme.label("Leave this challenge?", Theme.display(27), Theme.TEXT);
        title.setAlignmentX(0);
        message.add(title);
        message.add(Box.createVerticalStrut(10));
        JTextArea description = new JTextArea("Your challenge is still in progress. If you leave now, this run's progress and score won't be saved.");
        description.setFont(Theme.nunito(Font.PLAIN, 15));
        description.setForeground(Theme.MUTED);
        description.setOpaque(false);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setAlignmentX(0);
        description.setSize(new Dimension(420, Short.MAX_VALUE));
        description.setPreferredSize(new Dimension(420, description.getPreferredSize().height));
        message.add(description);
        card.add(message, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        RoundButton keep = new RoundButton("Keep playing");
        keep.setFont(Theme.font(Font.BOLD, 14));
        keep.setPreferredSize(new Dimension(204, 46));
        keep.addActionListener(e -> choice.accept(false));
        RoundButton leave = new RoundButton("Leave challenge");
        leave.setFont(Theme.font(Font.BOLD, 14));
        leave.setColors(Theme.RED_BG, new Color(0xE8B8B8), new Color(0x9E2929));
        leave.addActionListener(e -> choice.accept(true));
        actions.add(keep);
        actions.add(leave);
        card.add(actions, BorderLayout.SOUTH);
        surface.putClientProperty("keepPlaying", keep);
        return surface;
    }
}
