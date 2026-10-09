package org.papiricoh.supernaturalcraft.legacy.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Latin for the Men of Letters' generators (v0.17), pure: a small root lexicon turned into names ("Vinculum Umbrae") and
 * incantations ("exaudi nos ignis sanguinis"). Names are a bijection of the index within a hunter's sequence, so two of their
 * formulas (or rites) never share a name; past the lexicon a Roman numeral is added. Incantations are plain lowercase ASCII
 * letters and spaces, as the recitation screen expects.
 */
public final class LatinNames {

    /** Head nouns (nominative). */
    public static final List<String> HEADS = List.of("Vinculum", "Signum", "Clavis", "Ignis", "Lux", "Umbra", "Verbum", "Sigillum",
            "Murus", "Gladius", "Velum", "Oculus", "Manus", "Sagitta", "Nodus", "Flamma", "Scutum", "Fulmen", "Spiritus", "Lapis",
            "Ventus", "Carmen", "Corona", "Porta");

    /** Tails (genitive): what the head belongs to. */
    public static final List<String> TAILS = List.of("Umbrae", "Noctis", "Lucis", "Sanguinis", "Mortis", "Caeli", "Inferni", "Animae",
            "Tenebrarum", "Aurorae", "Silentii", "Tempestatis", "Ossium", "Salis", "Ferri", "Argenti", "Stellarum", "Lunae", "Solis",
            "Cineris", "Fidei", "Irae", "Memoriae", "Veritatis", "Daemonum", "Angelorum", "Spirituum", "Somni");

    /** Artifact forms in Latin (by {@code ArtifactData.form}). */
    public static final List<String> FORMS = List.of("ring:Anulus", "doll:Pupa", "mirror:Speculum", "watch:Horologium",
            "coin:Nummus", "book:Liber");

    /** Words an incantation opens with (calls). */
    public static final List<String> CALLS = List.of("exaudi", "veni", "aperi", "claude", "revela", "ligo", "solve", "voco",
            "obsecro", "impero", "custodi", "dimitte");

    /** Middle words. */
    public static final List<String> WORDS = List.of("nos", "me", "ignis", "aqua", "sanguis", "umbra", "lux", "terra", "spiritus",
            "verbum", "nomen", "via", "porta", "anima", "ossa", "sal", "ventus", "stella", "luna", "sol");

    /** Closing words. */
    public static final List<String> ENDS = List.of("in aeternum", "per sanguinem", "sub luna", "ante lucem", "in nomine patris",
            "contra tenebras", "et fiat", "amen", "nunc et semper", "per ignem");

    private LatinNames() {
    }

    /** How many names {@link #name} gives before repeating with a numeral. */
    public static int space() {
        return HEADS.size() * TAILS.size();
    }

    /**
     * The {@code index}-th name of a sequence keyed by {@code salt}: distinct for every index (a different numeral each lap of
     * the lexicon).
     */
    public static String name(long salt, int index) {
        int n = space();
        int lap = Math.max(0, index) / n;
        long a = coprimeStep(salt, n);
        long b = Math.floorMod(GenSeed.mix(salt), n);
        int k = (int) Math.floorMod(a * (Math.max(0, index) % n) + b, (long) n);
        String base = HEADS.get(k / TAILS.size()) + " " + TAILS.get(k % TAILS.size());
        return lap == 0 ? base : base + " " + roman(lap + 1);
    }

    /** An artifact's name: its form in Latin and a genitive ("Speculum Noctis"). */
    public static String artifactName(String form, long seed) {
        String head = "Res";
        for (String f : FORMS) if (f.startsWith(form + ":")) head = f.substring(form.length() + 1);
        GenSeed.Rng r = new GenSeed.Rng(seed ^ 0xA5A5A5A5L);
        return head + " " + r.pick(TAILS);
    }

    /** An incantation of {@code words} middle words (2–5) between a call and an ending. */
    public static String incantation(long seed, int words) {
        GenSeed.Rng r = new GenSeed.Rng(seed ^ 0x1CA17A71L);
        List<String> out = new ArrayList<>();
        out.add(r.pick(CALLS));
        int n = Math.max(1, Math.min(5, words));
        for (int i = 0; i < n; i++) {
            String w = r.pick(WORDS);
            if (!out.isEmpty() && out.get(out.size() - 1).equals(w)) w = WORDS.get((WORDS.indexOf(w) + 1) % WORDS.size());
            out.add(w);
        }
        out.add(r.pick(ENDS));
        return String.join(" ", out).toLowerCase(Locale.ROOT);
    }

    public static String roman(int n) {
        int[] v = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] s = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder b = new StringBuilder();
        int x = Math.max(1, n);
        for (int i = 0; i < v.length; i++) while (x >= v[i]) {
            b.append(s[i]);
            x -= v[i];
        }
        return b.toString();
    }

    /** A step in 1..n-1 coprime with n, from the salt. */
    private static long coprimeStep(long salt, int n) {
        long a = 1 + Math.floorMod(GenSeed.mix(salt ^ 0x77L), n - 1);
        while (gcd(a, n) != 1) a = a % (n - 1) + 1;
        return a;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}
