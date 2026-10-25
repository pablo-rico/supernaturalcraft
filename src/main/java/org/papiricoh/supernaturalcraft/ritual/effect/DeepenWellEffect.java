package org.papiricoh.supernaturalcraft.ritual.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.DepthRules;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * A rite of the well ({@link DepthRules}): the Rite of Deepening (at the altar) widens the ritualist's mana, the Rite of the
 * Wellspring (in the bowl) quickens it. Each recipe is one {@code tier} of one {@code kind}; the rite refuses a tier that is
 * not the ritualist's next, a kind still resting, and a bar that is not full, since it takes all of it.
 */
public record DeepenWellEffect(Kind kind, int tier) implements RitualEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("deepen_well");
    public static final MapCodec<DeepenWellEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(DeepenWellEffect::kind),
            Codec.intRange(1, 3).fieldOf("tier").forGetter(DeepenWellEffect::tier)
    ).apply(i, DeepenWellEffect::new));

    /** Milestones granted as advancements ({@code main/well_<n>}); later ones still bring their title and light. */
    public static final int[] MILESTONE_ADVANCEMENTS = {10, 20, 30};
    /** The pillar and shockwave of a milestone, over the usual completion's. */
    public static final float MILESTONE_PILLAR = 24f, MILESTONE_WAVE = 12f;
    public static final int MILESTONE_TICKS = 80;

    public enum Kind implements StringRepresentable {
        DEEPENING(DepthRules.Kind.DEEPENING), WELLSPRING(DepthRules.Kind.WELLSPRING);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        public final DepthRules.Kind rules;

        Kind(DepthRules.Kind rules) {
            this.rules = rules;
        }

        @Override
        public String getSerializedName() {
            return rules.id();
        }
    }

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null) return Component.translatable("message.supernaturalcraft.well.no_one");
        ArcanaData data = ManaManager.get(ritualist);
        int done = data.rites(kind.rules);
        int wanted = DepthRules.tierFor(done);
        if (wanted != tier) {
            return Component.translatable("message.supernaturalcraft.well.wrong_tier", rite(), roman(wanted), done);
        }
        long left = data.nextRite(kind.rules) - WorldClock.day(level.getServer());
        if (left > 0) {
            long days = (left + DepthRules.TICKS_PER_DAY - 1) / DepthRules.TICKS_PER_DAY;
            return Component.translatable("message.supernaturalcraft.well.resting", rite(), days);
        }
        if (!ritualist.getAbilities().instabuild && data.mana() < data.maxMana() - 0.5f) {
            return Component.translatable("message.supernaturalcraft.well.not_full");
        }
        return null;
    }

    @Override
    public Float manaCost(ServerPlayer ritualist) {
        return ManaManager.get(ritualist).maxMana();
    }

    @Override
    public boolean perform(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        if (ritualist == null) return false;
        ArcanaData data = ManaManager.get(ritualist);
        // Checked again: two rites of a kind may have been started before either ended.
        if (DepthRules.tierFor(data.rites(kind.rules)) != tier) return false;
        int done = data.rites(kind.rules) + 1;
        data.setRites(kind.rules, done);
        data.setNextRite(kind.rules, WorldClock.day(level.getServer()) + DepthRules.restDays(done) * DepthRules.TICKS_PER_DAY);
        SNNetworking.syncArcana(ritualist);
        ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.well." + kind.getSerializedName(),
                done, Math.round(data.maxMana()), String.format(java.util.Locale.ROOT, "%.1f", data.regenPerTick() * 20f))
                .withStyle(ChatFormatting.AQUA), false);
        int total = data.rites(DepthRules.Kind.DEEPENING) + data.rites(DepthRules.Kind.WELLSPRING);
        if (DepthRules.milestone(total)) milestone(level, altar, ritualist, total);
        return true;
    }

    /** Every tenth rite: its title, a greater pillar of light, and (for the first few) an advancement. */
    private static void milestone(ServerLevel level, BlockPos altar, ServerPlayer ritualist, int total) {
        ritualist.displayClientMessage(Component.translatable("message.supernaturalcraft.well.milestone", total,
                Component.translatable(titleKey(DepthRules.title(total)))).withStyle(ChatFormatting.GOLD), false);
        MagicFx.ritualComplete(level, altar, School.ENOCHIAN, MILESTONE_PILLAR, MILESTONE_WAVE, MILESTONE_TICKS);
        level.playSound(null, altar, AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 1.2f, 0.6f);
        for (int at : MILESTONE_ADVANCEMENTS) {
            if (total >= at) ChorusRewards.award(ritualist, "main/well_" + at);
        }
    }

    /** {@code well.supernaturalcraft.title.<n>} (1–{@link DepthRules#TITLES}). */
    public static String titleKey(int title) {
        return "well.supernaturalcraft.title." + title;
    }

    private Component rite() {
        return Component.translatable("well.supernaturalcraft." + kind.getSerializedName());
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            default -> "III";
        };
    }
}
