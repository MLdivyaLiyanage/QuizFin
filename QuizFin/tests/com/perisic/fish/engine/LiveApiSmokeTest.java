package com.perisic.fish.engine;

import java.util.List;

/** Opt-in live HTTP check, separate from offline tests. Requires internet access.*/
public final class LiveApiSmokeTest {
    public static void main(String[] args) throws Exception {
        Game game = new GameServer().getRandomGame();
        if (game == null || game.getImage() == null || game.getSolution() < 0 || game.getSolution() > 9) {
            throw new AssertionError("Fish API did not return an image and an answer supported by the UI");
        }
        System.out.println("Fish API live check passed: decoded image and answer in 0-9 range.");
        List<Question> questions = new QuizServer().getQuestions(5, 9, "easy");
        if (questions.isEmpty()) throw new AssertionError("Trivia API returned no questions");
        for (Question question : questions) {
            if (question.text == null || question.text.isBlank() || question.options.size() != 4
                    || !question.options.contains(question.correct)) {
                throw new AssertionError("Trivia response is not a usable multiple-choice question");
            }
        }
        System.out.println("Trivia API live check passed: decoded multiple-choice questions.");
    }
}
