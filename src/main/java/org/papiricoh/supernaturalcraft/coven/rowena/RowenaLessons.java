package org.papiricoh.supernaturalcraft.coven.rowena;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * What Rowena teaches and sells, and at what price (v0.21, pure). She teaches the witchcraft bowl pages of {@link #PAGES}
 * (their tiers mirror the {@code bowl_spell_page} JSON; a JUnit check keeps them in step), lowest tier first, at most
 * {@link #OFFERED} at a time: only pages the hunter does not know yet and is ranked to learn (Witchcraft mastery), and some only
 * once she trusts them ({@link Page#minTrust}). Her Master's pages (tier 4, {@link #MASTER_PAGES}) only for a hunter who gave
 * her the Book of the Damned and saw the Grand Witch fall (P6: {@link #offer(Predicate, IntPredicate, int, boolean)}). Every
 * page bought earns {@link #TRUST_PER_PAGE} trust.
 */
public final class RowenaLessons {

    /** At most this many pages on offer at once (the dialogue's options, with "back" and "leave"). */
    public static final int OFFERED = 5;
    public static final int TRUST_PER_PAGE = 1;

    /** A price: emeralds, gold ingots, and a rarer reagent ({@code null} for none). */
    public record Price(int emeralds, int gold, String reagent, int reagentCount) {
    }

    /** A page she teaches: its id (path in the mod's namespace), tier, and the trust she asks before she will. */
    public record Page(String id, int tier, int minTrust) {
    }

    /** The price of a page of each tier (1-4). */
    public static Price price(int tier) {
        return switch (tier) {
            case 1 -> new Price(8, 0, null, 0);
            case 2 -> new Price(16, 2, "minecraft:amethyst_shard", 2);
            case 3 -> new Price(24, 4, "minecraft:ghast_tear", 1);
            default -> new Price(32, 8, "minecraft:echo_shard", 1);
        };
    }

    /** Her catalogue, in the order she offers it within a tier. */
    public static final List<Page> PAGES = List.of(
            new Page("curse_of_misfortune", 1, 0), new Page("locate_being", 1, 0), new Page("trace_the_familiar", 1, 0),
            new Page("concealment", 1, 0),
            new Page("break_hex", 2, 0), new Page("curse_of_pain", 2, 0), new Page("curse_of_frailty", 2, 0),
            new Page("hex_of_rot", 2, 1), new Page("binding_of_the_name", 2, 1), new Page("compel", 2, 1),
            new Page("ward_of_the_name", 2, 0), new Page("brew_hexed_ink", 2, 1), new Page("hex_bags", 2, 0),
            new Page("curse_of_silence", 3, 2), new Page("scrying", 3, 2), new Page("mirror_hex", 3, 3),
            new Page("familiar_bond", 3, 5));

    /**
     * Her Master's pages (tier 4, P6): what she teaches only an ally who gave her the Book and outlived the Grand Coven with her
     * ({@code unmark}: the spell she cast on them).
     */
    public static final List<Page> MASTER_PAGES = List.of(new Page("unmark", 4, 0));

    public static Page page(String id) {
        for (Page p : PAGES) if (p.id().equals(id)) return p;
        for (Page p : MASTER_PAGES) if (p.id().equals(id)) return p;
        return null;
    }

    /**
     * What she offers a hunter: pages they do not {@code know}, whose tier they {@code mayLearn}, she trusts them with
     * ({@code trust}); lowest tier first, at most {@link #OFFERED}.
     */
    public static List<String> offer(Predicate<String> know, IntPredicate mayLearn, int trust) {
        return offer(know, mayLearn, trust, false);
    }

    /** As {@link #offer(Predicate, IntPredicate, int)}, with her {@link #MASTER_PAGES} too when {@code master} (her ally, P6). */
    public static List<String> offer(Predicate<String> know, IntPredicate mayLearn, int trust, boolean master) {
        List<String> out = new ArrayList<>();
        List<Page> all = new ArrayList<>(PAGES);
        if (master) all.addAll(MASTER_PAGES);
        for (int tier = 1; tier <= 4; tier++) {
            for (Page p : all) {
                if (p.tier() != tier || know.test(p.id()) || !mayLearn.test(p.tier()) || trust < p.minTrust()) continue;
                out.add(p.id());
                if (out.size() >= OFFERED) return out;
            }
        }
        return out;
    }

    // --- her wares ---------------------------------------------------------------------------------------------------------

    /** Her wares: id → {emeralds, how many}. {@code taglocks}: blank taglocks; {@code hexed_ink}: Witchcraft-steeped ink. */
    public static final Map<String, int[]> WARES;

    static {
        Map<String, int[]> m = new LinkedHashMap<>();
        m.put("taglocks", new int[]{3, 3});
        m.put("hexed_ink", new int[]{6, 1});
        WARES = java.util.Collections.unmodifiableMap(m);
    }

    private RowenaLessons() {
    }
}
