package com.perisic.fish.engine;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * Fish API client. A game consists of an image of fish and an integer: the
 * number of fish in the image (the solution).
 *
 * API: https://marcconrad.com/uob/fish/doc.php
 *
 * @author Marc Conrad (original), extended for QuizFin
 */
public class GameServer implements GameSource {

    private static final String FISH_API = "https://marcconrad.com/uob/fish/api.php?out=csv&base64=yes";
    private static final int ATTEMPTS = 3;

    /**
     * Retrieves a random game from the web site.
     *
     * @return a random game or null if a game cannot be loaded (network down,
     *         bad response, ...). Never throws.
     */
    @Override
    public Game getRandomGame() {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                String[] data = Http.get(FISH_API).trim().split(",");
                byte[] raw = Base64.getDecoder().decode(data[0].trim());
                int solution = Integer.parseInt(data[1].trim());
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
                if (img != null) {
                    return new Game(img, solution);
                }
            } catch (Exception e) {
                System.err.println("Fish API attempt " + attempt + " failed: " + e);
            }
        }
        return null;
    }
}
