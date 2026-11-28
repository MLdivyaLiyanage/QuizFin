package com.perisic.fish.peripherals;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** Shared page background; loaded once and scaled proportionally to fill the panel. */
class BackgroundPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String RESOURCE =
            "/com/perisic/fish/peripherals/images/Soft pastel blue abstract waves.png";
    private static final BufferedImage IMAGE = loadImage();

    BackgroundPanel(LayoutManager layout) {
        super(layout);
        setBackground(Theme.BG);
    }

    private static BufferedImage loadImage() {
        try (InputStream input = BackgroundPanel.class.getResourceAsStream(RESOURCE)) {
            if (input != null) return ImageIO.read(input);
            // Support running compiled classes before resources have been copied.
            for (Path root : new Path[] {Path.of("QuizFin", "src"), Path.of("src"), Path.of(".")}) {
                Path file = root.resolve(RESOURCE.substring(1));
                if (Files.isRegularFile(file)) return ImageIO.read(file.toFile());
            }
        } catch (IOException ex) {
            System.err.println("Could not load QuizFin background: " + ex.getMessage());
        }
        return null; // Keep the theme colour if the optional image is unavailable.
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (IMAGE == null || getWidth() <= 0 || getHeight() <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.max((double) getWidth() / IMAGE.getWidth(),
                (double) getHeight() / IMAGE.getHeight());
        int width = (int) Math.ceil(IMAGE.getWidth() * scale);
        int height = (int) Math.ceil(IMAGE.getHeight() * scale);
        g.drawImage(IMAGE, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
        g.dispose();
    }
}
