package com.perisic.fish.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Open Trivia DB client (https://opentdb.com/api_config.php). No API key
 * needed. Responses are requested Base64-encoded so HTML entities are never an
 * issue.
 */
public class QuizServer implements QuestionSource {

    private static final String BASE = "https://opentdb.com/";

    /** Categories from OpenTDB; falls back to General Knowledge if offline. */
    @Override
    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        try {
            Map<String, Object> root = Json.obj(Json.parse(Http.get(BASE + "api_category.php")));
            for (Object o : Json.arr(root.get("trivia_categories"))) {
                Map<String, Object> c = Json.obj(o);
                list.add(new Category(((Number) c.get("id")).intValue(), (String) c.get("name")));
            }
        } catch (Exception e) {
            System.err.println("Could not load categories: " + e);
        }
        if (list.isEmpty()) {
            list.add(new Category(9, "General Knowledge"));
        }
        return list;
    }

    /**
     * Fetches multiple-choice questions (blocking - call off the UI thread).
     *
     * @param amount     number of questions wanted
     * @param categoryId OpenTDB category id (0 = any)
     * @param difficulty easy, medium or hard
     * @throws IOException if questions cannot be loaded
     */
    @Override
    public List<Question> getQuestions(int amount, int categoryId, String difficulty) throws IOException {
        int n = amount;
        for (int attempt = 0; attempt < 5; attempt++) {
            String url = BASE + "api.php?amount=" + n + "&type=multiple&encode=base64&difficulty="
                    + difficulty.toLowerCase() + (categoryId > 0 ? "&category=" + categoryId : "");
            Map<String, Object> root;
            try {
                root = Json.obj(Json.parse(Http.get(url)));
            } catch (Http.HttpException e) {
                if (e.code == 429) { // rate limit: 1 request / 5 s per IP
                    pause();
                    continue;
                }
                throw e;
            } catch (RuntimeException e) {
                throw new IOException("Unexpected response from Open Trivia DB", e);
            }

            int code = ((Number) root.get("response_code")).intValue();
            if (code == 0) {
                return parse(Json.arr(root.get("results")));
            } else if (code == 5) { // rate limit
                pause();
            } else if (code == 1 && n > 1) { // not enough questions for this combination
                n = Math.max(1, n / 2);
                pause();
            } else {
                throw new IOException("Open Trivia DB has no questions for that category/difficulty (code " + code + ").");
            }
        }
        throw new IOException("Could not load questions from Open Trivia DB. Please try again.");
    }

    private static void pause() throws IOException {
        try {
            Thread.sleep(5500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted");
        }
    }

    private static List<Question> parse(List<Object> results) {
        List<Question> out = new ArrayList<>();
        for (Object o : results) {
            Map<String, Object> q = Json.obj(o);
            List<String> wrong = new ArrayList<>();
            for (Object w : Json.arr(q.get("incorrect_answers"))) {
                wrong.add(dec((String) w));
            }
            out.add(new Question(dec((String) q.get("category")), dec((String) q.get("difficulty")),
                    dec((String) q.get("question")), dec((String) q.get("correct_answer")), wrong));
        }
        return out;
    }

    private static String dec(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }
}
