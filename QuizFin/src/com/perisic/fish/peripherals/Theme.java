package com.perisic.fish.peripherals;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;

/**
 * Colours, fonts and tiny helpers shared by all screens (matches the Figma
 * design: navy sidebar, light-blue content, blue primary buttons).
 */
final class Theme {
    static final Color NAVY = new Color(0x10, 0x2A, 0x4C);
    static final Color NAV_ACTIVE = new Color(0x24, 0x52, 0x7F);
    static final Color BG = new Color(0xEE, 0xF6, 0xFB);
    static final Color CARD = Color.WHITE;
    static final Color PRIMARY = new Color(0x15, 0x65, 0xD8);
    static final Color SKY = new Color(0xD6, 0xEE, 0xF9);
    static final Color CHIP = new Color(0xF3, 0xF8, 0xFC);
    static final Color CHIP_SEL = new Color(0xD9, 0xEA, 0xFD);
    static final Color CHIP_BORDER = new Color(0xDD, 0xE8, 0xF2);
    static final Color TEXT = new Color(0x12, 0x2B, 0x4A);
    static final Color MUTED = new Color(0x6B, 0x7F, 0x95);
    static final Color ORANGE = new Color(0xE8, 0x89, 0x2B);
    static final Color GREEN = new Color(0x1E, 0x9E, 0x5A);
    static final Color GREEN_BG = new Color(0xDD, 0xF4, 0xE7);
    static final Color RED = new Color(0xD6, 0x45, 0x45);
    static final Color RED_BG = new Color(0xFB, 0xE1, 0xE1);
    static final Color AVATAR = new Color(0x8F, 0xD8, 0xF0);

    private Theme() {
    }

    static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** Escapes text for use inside Swing HTML labels/buttons. */
    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
