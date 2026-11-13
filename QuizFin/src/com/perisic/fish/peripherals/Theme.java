package com.perisic.fish.peripherals;

// AI-assisted secondary-text contrast update (OpenAI Codex, 2026-11-11). See SOURCES.md.

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Colours, fonts and tiny helpers shared by all screens (matches the Figma
 * design: navy sidebar, light-blue content, blue primary buttons).
 */
final class Theme {
    private static final Font FREDOKA_BOLD = loadAppFont("Fredoka-Bold.ttf");
    private static final Font POPPINS_REGULAR = loadAppFont("Poppins-Regular.ttf");
    private static final Font POPPINS_MEDIUM = loadAppFont("Poppins-Medium.ttf");
    private static final Font POPPINS_SEMIBOLD = loadAppFont("Poppins-SemiBold.ttf");
    private static final Font NUNITO_REGULAR = loadAppFont("Nunito-Regular.ttf");
    private static final Font NUNITO_SEMIBOLD = loadAppFont("Nunito-SemiBold.ttf");
    private static final Font NUNITO_BOLD = loadAppFont("Nunito-Bold.ttf");

    static {
        // Include standard Swing controls and dialogs in the application typography.
        UIDefaults defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet().toArray()) {
            Object value = defaults.get(key);
            if (value instanceof Font existing) {
                UIManager.put(key, new FontUIResource(font(existing.getStyle(), existing.getSize())));
            }
        }
        UIManager.put("OptionPane.messageFont", new FontUIResource(font(Font.PLAIN, 14)));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(font(Font.BOLD, 14)));
    }

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
    static final Color MUTED = new Color(0x50, 0x65, 0x7C);
    static final Color ORANGE = new Color(0xE8, 0x89, 0x2B);
    static final Color GREEN = new Color(0x1E, 0x9E, 0x5A);
    static final Color GREEN_BG = new Color(0xDD, 0xF4, 0xE7);
    static final Color RED = new Color(0xD6, 0x45, 0x45);
    static final Color RED_BG = new Color(0xFB, 0xE1, 0xE1);
    static final Color AVATAR = new Color(0x8F, 0xD8, 0xF0);

    private Theme() {
    }

    static Font font(int style, int size) {
        Font base = (style & Font.BOLD) != 0 ? POPPINS_SEMIBOLD : POPPINS_REGULAR;
        return base.deriveFont((float) size);
    }

    static Font display(int size) { return FREDOKA_BOLD.deriveFont((float) size); }
    static Font medium(int size) { return POPPINS_MEDIUM.deriveFont((float) size); }
    static Font nunito(int style, int size) {
        return ((style & Font.BOLD) != 0 ? NUNITO_BOLD : NUNITO_REGULAR).deriveFont((float) size);
    }
    static Font nunitoSemiBold(int size) { return NUNITO_SEMIBOLD.deriveFont((float) size); }

    private static Font loadAppFont(String filename) {
        String resource = "/com/perisic/fish/peripherals/fonts/" + filename;
        try (InputStream input = openFont(resource)) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, input);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException exception) {
            System.err.println("Could not load bundled font " + filename + ": " + exception.getMessage());
            return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        }
    }

    private static InputStream openFont(String resource) throws IOException {
        InputStream bundled = Theme.class.getResourceAsStream(resource);
        if (bundled != null) return bundled;
        // Support plain javac output from either the checkout or inner project root.
        for (Path sourceRoot : new Path[] { Path.of("QuizFin", "src"), Path.of("src") }) {
            Path fontPath = sourceRoot.resolve(resource.substring(1));
            if (Files.isRegularFile(fontPath)) return Files.newInputStream(fontPath);
        }
        throw new IOException("Font resource not found: " + resource);
    }

    static JLabel label(String text, int style, int size, Color color) {
        return label(text, font(style, size), color);
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** Keep navigation symbols readable when the text font lacks these glyphs. */
    static Icon symbolIcon(String symbol, Color color, int size) {
        return new Icon() {
            public int getIconWidth() { return size + 2; }
            public int getIconHeight() { return size + 2; }
            public void paintIcon(java.awt.Component component, java.awt.Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
                g.setColor(color);
                g.drawString(symbol, x, y + size);
                g.dispose();
            }
        };
    }

    /** Resolution-independent book, quiz and fish illustration from the reference. */
    static void paintBannerArt(Graphics2D g) {
        g.setColor(new Color(0xBEE6F3));
        g.fillRect(62, 4, 126, 118);
        g.fillOval(4, 116, 176, 18);
        g.setColor(new Color(0xFFD16B));
        g.fillOval(13, 16, 49, 49);
        g.setColor(new Color(0xFFF4D2));
        g.setStroke(new BasicStroke(4));
        g.drawOval(13, 16, 49, 49);
        g.setColor(TEXT);
        g.setFont(font(Font.BOLD, 34));
        g.drawString("?", 28, 51);
        g.setColor(new Color(0x6CD2E1));
        g.fillOval(114, 12, 51, 51);
        g.setColor(new Color(0xE9FCFF));
        g.setStroke(new BasicStroke(3));
        g.drawOval(114, 12, 51, 51);
        g.setColor(new Color(0x2484B5));
        g.fillArc(117, 36, 47, 28, 180, 180);
        g.fillRect(117, 42, 47, 17);
        g.setColor(Color.WHITE);
        g.drawOval(132, 30, 19, 19);
        g.drawLine(148, 46, 158, 56);
        g.setColor(new Color(0xFFD16B));
        g.fillOval(0, 31, 9, 9);
        g.fillOval(171, 20, 9, 9);
        g.setColor(Color.WHITE);
        g.fillOval(83, 44, 6, 6);
        g.setColor(new Color(0x7CCCE4));
        g.fillOval(81, 5, 8, 8);

        Path2D leftPage = new Path2D.Double();
        leftPage.moveTo(0, 78);
        leftPage.quadTo(38, 60, 79, 83);
        leftPage.lineTo(79, 124);
        leftPage.quadTo(39, 102, 0, 120);
        leftPage.closePath();
        Path2D rightPage = new Path2D.Double();
        rightPage.moveTo(79, 83);
        rightPage.quadTo(118, 60, 160, 78);
        rightPage.lineTo(160, 120);
        rightPage.quadTo(119, 102, 79, 124);
        rightPage.closePath();
        g.setColor(Color.WHITE);
        g.fill(leftPage);
        g.setColor(new Color(0xEAF7FE));
        g.fill(rightPage);
        g.setColor(new Color(0x519DCD));
        g.setStroke(new BasicStroke(1.7f));
        g.draw(leftPage);
        g.draw(rightPage);
        g.setColor(new Color(0x9CC8E4));
        g.setStroke(new BasicStroke(1.2f));
        for (int y = 88; y <= 112; y += 8) {
            Path2D lines = new Path2D.Double();
            lines.moveTo(10, y);
            lines.quadTo(38, y - 9, 68, y + 3);
            lines.moveTo(90, y + 3);
            lines.quadTo(119, y - 9, 150, y);
            g.draw(lines);
        }
    }

    /** Escapes text for use inside Swing HTML labels/buttons. */
    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
