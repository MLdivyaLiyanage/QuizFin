package com.perisic.fish.engine;

import java.awt.image.BufferedImage;
import java.util.List;

/** provenance is recorded in SOURCES.md. */
public final class GameFlowTest {
    public static void main(String[] args) {
        Question question = new Question("Test", "easy", "2 + 2?", "4", List.of("1", "2", "3"));
        QuizEngine quiz = new QuizEngine(List.of(question, question), "Hard");
        check(!quiz.unlocksFishChallenge(), "Incomplete quiz must not unlock fish");
        quiz.next();
        check(quiz.getNumber() == 1, "Cannot skip an unanswered question");
        check(quiz.submit("4"), "Correct quiz answer");
        check(!quiz.submit("4") && quiz.getScore() == 20, "Duplicate answer must not add points");
        quiz.next();
        check(!quiz.submit("1") && quiz.isFinished(), "Wrong final answer still completes quiz");
        check(quiz.unlocksFishChallenge(), "Completion unlocks matching fish rounds");
        check(new QuizEngine(List.of(question), "Easy").pointsPerQuestion() == 10, "Easy points");
        check(new QuizEngine(List.of(question), "Medium").pointsPerQuestion() == 15, "Medium points");

        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        int[] requests = {0};
        GameSource source = () -> { requests[0]++; return new Game(image, 3); };
        GameEngine fish = new GameEngine("player", source, 4);
        for (int round = 1; round <= 4; round++) {
            check(fish.nextGame() == image, "Injected Fish source provides image");
            fish.nextGame();
            check(requests[0] == round, "Unanswered round must not fetch twice");
            fish.startAnswering();
            GameEngine.AnswerResult result = fish.checkSolution(round == 4 ? 0 : 3);
            check(result.correct == (round != 4), "Correct and incorrect fish answers");
            check(result.base == (round == 4 ? 0 : 10), "Base points");
            check(result.streak == (round == 3 ? 5 : 0), "Third correct answer earns streak bonus");
            int score = fish.getScore();
            check(fish.checkSolution(3) == null && fish.getScore() == score, "One answer per fish round");
        }
        check(fish.isFinished() && fish.nextGame() == null, "No rounds after completion");
        check(fish.getCorrectCount() == 3 && fish.getBestStreak() == 3 && fish.getStreak() == 0,
                "Wrong answer resets streak and preserves best streak");
        GameEngine unavailable = new GameEngine("player", () -> null, 2);
        check(unavailable.nextGame() == null && unavailable.getRound() == 0, "API failure consumes no round");
        ScoreEntry combined = new ScoreEntry("player", quiz.getScore(), fish.getScore(), 0);
        check(combined.mode == ScoreEntry.Mode.QUIZ_AND_FISH
                && combined.score == quiz.getScore() + fish.getScore(), "Combined score");
        check(new ScoreEntry("player", null, 15, 0).mode == ScoreEntry.Mode.FISH_ONLY, "Standalone mode");
        System.out.println("GameFlowTest passed: quiz progression, fish rounds, scoring, retry, modes.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
