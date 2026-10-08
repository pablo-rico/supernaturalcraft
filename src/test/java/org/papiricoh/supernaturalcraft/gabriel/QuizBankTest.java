package org.papiricoh.supernaturalcraft.gabriel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.QuizBank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The game show's questions (v0.14): enough of them, unique, each with its text and three different answers in the lang. */
class QuizBankTest {

    private static final Path LANG = Path.of("src/generated/resources/assets/supernaturalcraft/lang/en_us.json");

    @Test
    void aBankOfAboutThirtyUniqueQuestions() {
        assertTrue(QuizBank.QUESTIONS.size() >= 30, "about thirty questions, has " + QuizBank.QUESTIONS.size());
        Set<String> ids = new HashSet<>();
        for (QuizBank.Question q : QuizBank.QUESTIONS) {
            assertTrue(ids.add(q.id()), "question id used twice: " + q.id());
            assertTrue(q.id().matches("[a-z0-9_]+"), "a lang-safe id: " + q.id());
        }
    }

    @Test
    void everyQuestionHasItsTextAndThreeDifferentAnswers() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        for (QuizBank.Question q : QuizBank.QUESTIONS) {
            assertTrue(lang.has(q.key()) && !lang.get(q.key()).getAsString().isBlank(), "no text for " + q.key());
            Set<String> answers = new HashSet<>();
            for (int a = 0; a < 3; a++) {
                String key = q.answerKey(a);
                assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), "no text for " + key);
                assertTrue(answers.add(lang.get(key).getAsString().toLowerCase()), q.id() + ": two answers read the same");
            }
            assertFalse(lang.has("quiz.supernaturalcraft.gabriel." + q.id() + ".a3"), q.id() + ": three answers, three platforms");
        }
    }

    @Test
    void theNextQuestionIsNeverTheLastOne() {
        Random r = new Random(3);
        QuizBank.Question last = QuizBank.QUESTIONS.getFirst();
        for (int i = 0; i < 200; i++) {
            QuizBank.Question next = QuizBank.pick(r, last);
            assertNotEquals(last, next);
            last = next;
        }
        int[] dealt = QuizBank.deal(r);
        assertEquals(0, dealt[QuizBank.rightPlatform(dealt)]);
    }
}
