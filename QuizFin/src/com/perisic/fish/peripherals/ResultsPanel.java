package com.perisic.fish.peripherals;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Final score + performance message shown after a quiz or a fish challenge.
 */
class ResultPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    ResultPanel(int percent, String score, String detail, String primaryText, Runnable primary, String secondaryText,
                Runnable secondary) {
        super(new GridBagLayout());
        setOpaque(false);

        RoundPanel card = new RoundPanel(Theme.CARD, new GridBagLayout());
        card.setBorder(new EmptyBorder(34, 60, 34, 60));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(4, 0, 4, 0);

        javax.swing.JLabel verdict = Theme.label(verdict(percent), Font.BOLD, 24, Theme.TEXT);
        verdict.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(verdict, c);
        card.add(Theme.label("Final score", Font.PLAIN, 13, Theme.MUTED), c);
        javax.swing.JLabel big = Theme.label(score, Font.BOLD, 48, Theme.PRIMARY);
        card.add(big, c);
        card.add(Theme.label(detail, Font.PLAIN, 14, Theme.TEXT), c);

        c.gridwidth = 1;
        c.insets = new Insets(26, 6, 0, 6);
        RoundButton p = new RoundButton(primaryText);
        p.addActionListener(e -> primary.run());
        c.gridx = 0;
        card.add(p, c);
        RoundButton s = new RoundButton(secondaryText);
        s.setColors(Theme.CHIP_SEL, Theme.PRIMARY, Theme.TEXT);
        s.addActionListener(e -> secondary.run());
        c.gridx = 1;
        card.add(s, c);

        add(card);
    }

    private static String verdict(int pct) {
        if (pct >= 90)
            return "Outstanding!";
        if (pct >= 70)
            return "Great job!";
        if (pct >= 50)
            return "Good effort!";
        return "Keep practising!";
    }
}
