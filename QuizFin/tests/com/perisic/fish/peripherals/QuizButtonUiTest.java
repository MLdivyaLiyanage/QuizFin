package com.perisic.fish.peripherals;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import com.perisic.fish.engine.Question;
import com.perisic.fish.engine.QuizEngine;

/** Headless regression checks for the reported button outlines and quiz footer.*/
public final class QuizButtonUiTest {
    private static final class FocusedButton extends RoundButton {
        FocusedButton() { super("Dashboard"); }
        @Override public boolean hasFocus() { return true; }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                checkFocus();
                checkQuiz(800, 610, "quiz-compact.png");
                checkQuiz(1100, 660, "quiz-wide.png");
                System.out.println("QuizButtonUiTest passed: mouse/keyboard focus, stable answer colours, answer actions, visible footer at two sizes.");
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
    }

    private static void checkFocus() {
        FocusedButton button = new FocusedButton();
        button.setSize(220, 48);
        focus(button, FocusEvent.Cause.MOUSE_EVENT);
        BufferedImage mouse = render(button);
        focus(button, FocusEvent.Cause.TRAVERSAL_FORWARD);
        BufferedImage keyboard = render(button);
        check(!same(mouse, keyboard), "Keyboard focus indicator must remain visible");
        for (int y = 8; y < mouse.getHeight() - 8; y++) {
            for (int x = 4; x < mouse.getWidth() - 4; x++) {
                check(mouse.getRGB(x, y) == keyboard.getRGB(x, y), "Focus must not draw any inner outline");
            }
        }
        for (var listener : button.getMouseListeners()) {
            if (listener.getClass().getName().startsWith(RoundButton.class.getName() + "$"))
                listener.mousePressed(new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 0, 0, 5, 5, 1, false));
        }
        check(same(mouse, render(button)), "Mouse click must clear the extra focus outline");
        button.setColors(Theme.GREEN_BG, Theme.GREEN, Theme.TEXT);
        button.setInteractionFeedback(false);
        BufferedImage scored = render(button);
        for (var listener : button.getMouseListeners()) {
            if (listener.getClass().getName().startsWith(RoundButton.class.getName() + "$"))
                listener.mouseEntered(new MouseEvent(button, MouseEvent.MOUSE_ENTERED, 0, 0, 5, 5, 0, false));
        }
        check(same(scored, render(button)), "Hover must not change locked answer colours");
    }

    private static void focus(Component button, FocusEvent.Cause cause) {
        FocusEvent event = new FocusEvent(button, FocusEvent.FOCUS_GAINED, false, null, cause);
        for (var listener : button.getFocusListeners()) listener.focusGained(event);
    }

    private static void checkQuiz(int width, int height, String filename) throws Exception {
        Question question = new Question("General Knowledge", "easy",
                "Which of these Marvel games was released on the PlayStation 2?", "Spider-Man 2",
                List.of("Howard the Duck", "Silver Surfer", "Wolverine: Adamantium's Revenge"));
        QuizEngine engine = new QuizEngine(List.of(question, question), "Easy");
        QuizPanel page = new QuizPanel(null, engine);
        page.setSize(width, height);
        layout(page);
        List<RoundButton> buttons = new ArrayList<>();
        collect(page, buttons);
        RoundButton next = buttons.stream().filter(b -> b.getText().equals("Next Question")).findFirst().orElseThrow();
        Rectangle bounds = SwingUtilities.convertRectangle(next.getParent(), next.getBounds(), page);
        check(bounds.y + bounds.height < height && bounds.x + bounds.width <= width,
                "Next button must fit within the visible quiz");
        check(!next.isEnabled(), "Cannot continue before answering");
        RoundButton correct = buttons.stream().filter(b -> b.getText().contains("Spider-Man 2")).findFirst().orElseThrow();
        correct.doClick(0);
        check(engine.getScore() == 10 && next.isEnabled(), "Answer must score and enable Next");
        for (RoundButton button : buttons) if (button != next) button.doClick(0);
        check(engine.getScore() == 10, "Further clicks cannot rescore the question");
        layout(page);
        Path output = Path.of("target", "button-preview");
        Files.createDirectories(output);
        ImageIO.write(render(page), "png", output.resolve(filename).toFile());
        next.doClick(0);
        check(engine.getNumber() == 2 && !next.isEnabled(), "Next opens a fresh unanswered question");
    }

    private static void collect(Container parent, List<RoundButton> buttons) {
        for (Component child : parent.getComponents()) {
            if (child instanceof RoundButton button) buttons.add(button);
            else if (child instanceof Container container) collect(container, buttons);
        }
    }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
    }
    private static BufferedImage render(Component component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Theme.BG);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        component.printAll(g);
        g.dispose();
        return image;
    }
    private static boolean same(BufferedImage first, BufferedImage second) {
        for (int y = 0; y < first.getHeight(); y++) for (int x = 0; x < first.getWidth(); x++)
            if (first.getRGB(x, y) != second.getRGB(x, y)) return false;
        return true;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
