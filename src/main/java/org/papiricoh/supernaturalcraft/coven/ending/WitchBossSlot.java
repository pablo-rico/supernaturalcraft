package org.papiricoh.supernaturalcraft.coven.ending;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaRelation;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

import java.util.function.Predicate;

/**
 * The witch boss "slot" of the progression (v0.21, P6; pure). There is one fight after Abaddon, in two variants: the choice a
 * hunter makes with the Book of the Damned ({@link RowenaRelation#book()}) decides which. {@link BossProgression} lists both
 * ({@code ROWENA} and {@code GRAND_WITCH}, both optional), each with its own entity, stats and kill advancement; this class is
 * the one place that says which of the two a hunter's road leads to and whether they have walked it.
 *
 * <ul>
 *   <li>{@code GIVEN}: Rowena is your ally; you fight the Grand Witch (her rite asks for {@link #GAVE}).</li>
 *   <li>{@code KEPT}: you keep the Book; you fight Rowena at her house (by using the Book there at night).</li>
 *   <li>{@code NONE}: neither yet.</li>
 * </ul>
 * In a fight with hunters of both choices, the boss is the summoner's; everyone in the arena gets that boss's credit.
 */
public final class WitchBossSlot {

    /** Advancements (in the mod's namespace) granted by code when the choice is made, and on the Mark's removal. */
    public static final String GAVE = "main/gave_the_book", KEPT = "main/kept_the_book", UNMARKED = "main/unmarked";

    private WitchBossSlot() {
    }

    /** The boss a hunter who chose {@code book} faces, or null before the choice. */
    public static @Nullable BossProgression.Boss bossFor(RowenaRelation.Book book) {
        return switch (book) {
            case GIVEN -> BossProgression.Boss.GRAND_WITCH;
            case KEPT -> BossProgression.Boss.ROWENA;
            case NONE -> null;
        };
    }

    /** Whether the hunter has beaten the witch boss of their road ({@code beaten}: advancement path → done). */
    public static boolean beaten(RowenaRelation.Book book, Predicate<String> beaten) {
        BossProgression.Boss b = bossFor(book);
        return b != null && beaten.test(b.advancement);
    }

    /** Whether either variant has been beaten (the slot's credit, whatever the road). */
    public static boolean slotBeaten(Predicate<String> beaten) {
        return beaten.test(BossProgression.Boss.ROWENA.advancement) || beaten.test(BossProgression.Boss.GRAND_WITCH.advancement);
    }

    /** Whether a hunter may summon {@code boss} (only their road's variant). */
    public static boolean maySummon(RowenaRelation.Book book, BossProgression.Boss boss) {
        return bossFor(book) == boss;
    }
}
