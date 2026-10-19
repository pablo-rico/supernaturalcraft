package org.papiricoh.supernaturalcraft.magic;

import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.hunter.AmuletHelper;
import org.papiricoh.supernaturalcraft.magic.mastery.Mastery;
import org.papiricoh.supernaturalcraft.magic.mastery.MasteryRules;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * One rule for what magic costs (v0.21), the same for sigils, the bowl and the altar. Discounts add up and are capped at
 * {@link #MAX_DISCOUNT}: a human hunter saves {@code SNConfig.HUMAN_RITUAL_DISCOUNT} (15 % by default), the Hunter's Amulet
 * {@link #AMULET_DISCOUNT}, and mastery of the spell's school 5/10/15 % ({@link MasteryRules.Rank#discount}). What multiplies on
 * top stays apart: a scroll's half cost ({@code SpellCost.SCROLL_DISCOUNT}) and a catalyst's own multiplier. The bowl's longer
 * recitation for humans ({@code Allegiances.recitationScale}) is not a cost and is untouched.
 */
public final class ArcanaCost {

    /** Which kind of magic is being paid for. */
    public enum Tool {
        SIGIL, BOWL, RITUAL
    }

    /** The Hunter's Amulet steadies the hand: a tenth off every kind of magic. */
    public static final float AMULET_DISCOUNT = 0.10f;
    /** The most all discounts together may take off. */
    public static final float MAX_DISCOUNT = 0.40f;

    private ArcanaCost() {
    }

    /**
     * The pure rule (JUnit: {@code ArcanaCostTest}): the multiplier for a caster who is or is not human (saving
     * {@code humanDiscount}), wears the amulet or not, with {@code mastery} in the school.
     */
    public static float multiplier(boolean human, float humanDiscount, boolean amulet, MasteryRules.Rank mastery) {
        float off = (human ? Math.max(0, humanDiscount) : 0) + (amulet ? AMULET_DISCOUNT : 0) + mastery.discount;
        return 1f - Math.min(MAX_DISCOUNT, off);
    }

    /** What {@code player} pays for {@code tool} magic of {@code school}, as a multiplier of the base cost (0.6-1). */
    public static float discount(Player player, Tool tool, School school) {
        return multiplier(Allegiances.get(player).isHuman(), (float) SNConfig.HUMAN_RITUAL_DISCOUNT.getAsDouble(),
                AmuletHelper.isWearing(player), Mastery.get(player).rank(school));
    }

    /** {@code base} mana after {@link #discount}. */
    public static float apply(Player player, Tool tool, School school, float base) {
        return base * discount(player, tool, school);
    }
}
