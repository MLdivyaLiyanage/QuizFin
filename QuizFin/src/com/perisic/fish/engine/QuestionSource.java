package com.perisic.fish.engine;

import java.io.IOException;
import java.util.List;

/**
 * Where quiz questions come from. The GUI and QuizEngine only know this
 * interface, so Open Trivia DB can be swapped (or faked in tests) without
 * touching them - low coupling.
 */
public interface QuestionSource {
    List<Category> getCategories();

    List<Question> getQuestions(int amount, int categoryId, String difficulty) throws IOException;
}
