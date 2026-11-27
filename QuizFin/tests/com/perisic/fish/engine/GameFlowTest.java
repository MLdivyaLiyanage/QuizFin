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
        int[] invalidRequests = {0};
        GameEngine malformed = new GameEngine("player", () ->
                new Game(image, invalidRequests[0]++ == 0 ? 10 : 3), 1);
        check(malformed.nextGame() == null && malformed.getRound() == 0,
                "An unanswerable puzzle must not consume a round");
        check(malformed.nextGame() == image && malformed.getRound() == 1,
                "A valid retry starts the same round");
        check(malformed.checkSolution(-1) == null && malformed.checkSolution(10) == null
                && !malformed.isFinished(), "Invalid input must leave the round available");
        check(malformed.checkSolution(3).correct && malformed.isFinished(), "Valid input after rejected input");

        GameEngine journey = new GameEngine("player", source, 10);
        for (int round = 1; round <= 10; round++) {
            journey.nextGame();
            int seconds = round <= 4 ? 5 : round <= 7 ? 4 : 3;
            String stage = round <= 4 ? "Shallows" : round <= 7 ? "Reef" : "Deep sea";
            check(journey.getMemorySeconds() == seconds && journey.getMemoryStageName().equals(stage),
                    "Preview and stage must follow the ten-round journey");
            journey.checkSolution(3);
        }
        for (int count : new int[]{1, 2, 5, 15}) {
            GameEngine varied = new GameEngine("player", source, count);
            int previousSeconds = 5;
            for (int round = 1; round <= count; round++) {
                varied.nextGame();
                check(varied.getMemorySeconds() >= 3 && varied.getMemorySeconds() <= previousSeconds,
                        "Preview must shorten safely for every supported challenge length");
                previousSeconds = varied.getMemorySeconds();
                varied.checkSolution(3);
            }
        }
        ScoreEntry combined = new ScoreEntry("player", quiz.getScore(), fish.getScore(), 0);
        check(combined.mode == ScoreEntry.Mode.QUIZ_AND_FISH
                && combined.score == quiz.getScore() + fish.getScore(), "Combined score");
        check(new ScoreEntry("player", null, 15, 0).mode == ScoreEntry.Mode.FISH_ONLY, "Standalone mode");
        System.out.println("GameFlowTest passed: quiz progression, memory stages, scoring, invalid puzzles/input, retry, modes.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
