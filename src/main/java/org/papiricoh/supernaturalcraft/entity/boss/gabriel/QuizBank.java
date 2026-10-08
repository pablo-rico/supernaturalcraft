package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import java.util.List;
import java.util.Random;

/**
 * The game show's questions (pure, v0.14): about the mod's own lore, three answers each, one of them right. The text lives in
 * lang ({@code quiz.supernaturalcraft.gabriel.<id>.q}, {@code .a0}..{@code .a2}, written by {@code GabrielLang} with
 * answer 0 always the right one); each round the answers are dealt to the three platforms in a random order
 * ({@link #deal}), so the right platform changes. {@code QuizBankTest} checks every question has its text.
 */
public final class QuizBank {

    /** One question: {@code id} names its lang keys; answer 0 is the right one. */
    public record Question(String id) {
        public String key() {
            return "quiz.supernaturalcraft.gabriel." + id + ".q";
        }

        public String answerKey(int answer) {
            return "quiz.supernaturalcraft.gabriel." + id + ".a" + answer;
        }
    }

    /** Every question, in no order (ids are lang keys: never reuse one). */
    public static final List<Question> QUESTIONS = List.of(
            new Question("colt_bullets"), new Question("salt_line"), new Question("yellow_eyes"), new Question("colt_rounds"),
            new Question("first_demon"), new Question("last_seal"), new Question("cage_key"), new Question("cracked_key"),
            new Question("lucifer_last_face"), new Question("darkness_called"), new Question("amara_wells"), new Question("hymn_notes"),
            new Question("chorus_weather"), new Question("metatron_name"), new Question("metatron_word"), new Question("michael_asks"),
            new Question("michael_true_form"), new Question("host_captain"), new Question("war_parry"), new Question("famine_food"),
            new Question("pestilence_flies"), new Question("death_clock"), new Question("death_rings"), new Question("ghost_rest"),
            new Question("hellhound_sight"), new Question("azazel_trap"), new Question("deal_sealed"), new Question("deal_due"),
            new Question("angel_fire"), new Question("author_home"), new Question("hex_bag"), new Question("starting_mana"),
            new Question("lilith_light"), new Question("human_free_will"));

    private QuizBank() {
    }

    /**
     * Deals a question's three answers to the three platforms.
     *
     * @return {@code platform -> answer}: {@code result[p]} is the answer shown on platform {@code p}; the right one is the
     * platform holding answer 0
     */
    public static int[] deal(Random random) {
        int[] order = {0, 1, 2};
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        return order;
    }

    /** @return the platform holding the right answer in a {@link #deal} */
    public static int rightPlatform(int[] dealt) {
        for (int p = 0; p < dealt.length; p++) if (dealt[p] == 0) return p;
        throw new IllegalArgumentException("no right answer dealt");
    }

    /** Packs a deal ({@code dealt[p]} = the answer on platform p) into one int, for {@code GabrielFxPayload.QUIZ}. */
    public static int pack(int[] dealt) {
        return dealt[0] + 3 * dealt[1] + 9 * dealt[2];
    }

    public static int[] unpack(int packed) {
        return new int[]{packed % 3, packed / 3 % 3, packed / 9 % 3};
    }

    /** @return a question other than {@code last} (any, if the bank has only one) */
    public static Question pick(Random random, Question last) {
        if (QUESTIONS.size() == 1) return QUESTIONS.get(0);
        Question q;
        do {
            q = QUESTIONS.get(random.nextInt(QUESTIONS.size()));
        } while (q.equals(last));
        return q;
    }
}
