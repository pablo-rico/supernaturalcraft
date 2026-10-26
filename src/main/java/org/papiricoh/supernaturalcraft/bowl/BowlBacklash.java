package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A spell gone wrong: a mix nothing answers to, a botched or abandoned recitation. The bowl
 * belches black smoke, the caster takes {@link #DAMAGE} and one short affliction, and everything
 * in the bowl is lost. The caster is always told why ({@link Reason}, and for a wrong mix the
 * nearest spell they know and what was off: {@link #wrongMix}).
 */
public final class BowlBacklash {

    public static final float DAMAGE = 3;
    /** The implosion's reach (blocks) and how long it plays (v0.21 effect). */
    public static final float STRENGTH = 2.5f;
    public static final int TICKS = 24;

    /** Why a spell turned on its caster; each has its line, {@code message.supernaturalcraft.bowl.backlash.<id>}. */
    public enum Reason {
        /** No recipe answers to the mix, and none the caster knows is close. */
        NO_SPELL,
        /** Close to a spell the caster knows, but something is missing or extra ({@link #wrongMix}). */
        NEAR_MISS,
        /** The recitation ran out of time. */
        TOO_SLOW,
        /** It ran out of time mostly through mistakes. */
        TYPOS,
        /** The caster stopped reciting (closed the screen) with time to spare. */
        ABANDONED,
        /** The caster walked away from the bowl. */
        WANDERED,
        /** No answer ever came back before the deadline. */
        SILENT,
        /** A claimed success too fast to have been typed. */
        FORGED,
        /** A claimed success after the deadline. */
        LATE;

        public String key() {
            return "message.supernaturalcraft.bowl.backlash." + name().toLowerCase(java.util.Locale.ROOT);
        }

        public Component line() {
            return Component.translatable(key());
        }
    }

    private BowlBacklash() {
    }

    /**
     * Why a mix nothing answers to blew up: the nearest spell {@code caster} knows and what was missing or extra
     * ({@link MixDiagnosis}), or just {@link Reason#NO_SPELL}. Only spells already learned are named, so a backlash
     * never teaches a recipe.
     */
    public static Component wrongMix(ServerLevel level, ServerPlayer caster, BowlContents contents) {
        org.papiricoh.supernaturalcraft.magic.mana.ArcanaData arcana = org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(caster);
        List<ItemStack> stacks = contents.stacks();
        List<BowlLiquid> liquids = contents.liquidKinds();
        int namedBlood = BowlInput.of(contents).namedBlood();
        RecipeHolder<BowlSpellRecipe> best = null;
        MixDiagnosis.Gap bestGap = null;
        List<ItemStack> bestItems = List.of();
        for (RecipeHolder<BowlSpellRecipe> holder : BowlSpells.all(level.getRecipeManager())) {
            if (!BowlSpells.knows(arcana, holder)) continue;
            BowlSpellRecipe r = holder.value();
            // As BowlSpellRecipe#matches: target-only things and an aimed spell's named blood are not part of the mix.
            List<ItemStack> items = stacks.stream().filter(s -> !PersonalComponent.isTargetOnly(s)
                    || r.ingredients().stream().anyMatch(i -> i.test(s))).toList();
            List<BowlLiquid> have = new ArrayList<>(liquids);
            if (r.requiresTarget() && namedBlood > 0
                    && Collections.frequency(have, BowlLiquid.BLOOD) > Collections.frequency(r.liquids(), BowlLiquid.BLOOD)) {
                have.remove(BowlLiquid.BLOOD);
            }
            MixDiagnosis.Gap gap = MixDiagnosis.compare(r.liquids(), have, r.ingredients(), items, Ingredient::test);
            if (!gap.near(r.liquids().size() + r.ingredients().size())) continue;
            if (bestGap == null || gap.size() < bestGap.size() || gap.size() == bestGap.size() && gap.matched() > bestGap.matched()) {
                best = holder;
                bestGap = gap;
                bestItems = items;
            }
        }
        if (best == null) return Reason.NO_SPELL.line();
        BowlSpellRecipe r = best.value();
        Component spell = Component.translatable(BowlSpells.nameKey(r.spell(best.id())));
        List<Component> missing = new ArrayList<>();
        bestGap.missingLiquids().forEach(l -> missing.add(Component.translatable(SpellBowlItem.liquidKey(l))));
        for (int i : bestGap.missingIngredients()) {
            ItemStack[] options = r.ingredients().get(i).getItems();
            missing.add(options.length == 0 ? Component.literal("?") : options[0].getHoverName());
        }
        List<Component> extra = new ArrayList<>();
        bestGap.extraLiquids().forEach(l -> extra.add(Component.translatable(SpellBowlItem.liquidKey(l))));
        for (int i : bestGap.extraItems()) extra.add(bestItems.get(i).getHoverName());
        String key = Reason.NEAR_MISS.key();
        if (!bestGap.holdsSomethingExtra()) return Component.translatable(key + ".missing", spell, list(missing));
        if (!bestGap.lacksSomething()) return Component.translatable(key + ".extra", spell, list(extra));
        return Component.translatable(key + ".both", spell, list(missing), list(extra));
    }

    /** "a", "a and b", "a, b and c", with repeats folded into "a ×2". */
    static Component list(List<Component> parts) {
        Map<String, Component> first = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Component c : parts) {
            String k = c.getString();
            first.putIfAbsent(k, c);
            counts.merge(k, 1, Integer::sum);
        }
        List<Component> folded = new ArrayList<>();
        first.forEach((k, c) -> folded.add(counts.get(k) > 1 ? c.copy().append(" \u00d7" + counts.get(k)) : c));
        MutableComponent out = Component.empty();
        for (int i = 0; i < folded.size(); i++) {
            if (i > 0) out.append(i == folded.size() - 1 ? Component.translatable("message.supernaturalcraft.bowl.backlash.and")
                    : Component.literal(", "));
            out.append(folded.get(i));
        }
        return out;
    }

    /** Sets off the backlash at {@code bowl}, telling {@code caster} (null if gone) {@code why}. Empties and unlights the bowl. */
    public static void trigger(SpellBowlBlockEntity bowl, @Nullable ServerPlayer caster, Component why) {
        if (!(bowl.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = bowl.getBlockPos();
        Vec3 c = Vec3.atBottomCenterOf(pos).add(0, 0.35, 0);
        // Smoke, implosion and sound are the client's BOWL_BACKLASH (v0.21).
        net.minecraft.resources.ResourceLocation recipe = bowl.sessionRecipe();
        School school = recipe == null ? School.WITCHCRAFT : level.getRecipeManager().byKey(recipe)
                .map(h -> h.value() instanceof BowlSpellRecipe r ? r.school() : School.WITCHCRAFT).orElse(School.WITCHCRAFT);
        int seal = recipe == null ? -1 : org.papiricoh.supernaturalcraft.magic.MagicAssets.sealIndex(recipe.getPath().substring(recipe.getPath().lastIndexOf('/') + 1));
        MagicFx.bowlBacklash(level, school, c.add(0, 0.25, 0), caster, STRENGTH, seal, TICKS);
        if (caster != null && caster.isAlive()) {
            caster.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, null), DAMAGE);
            Holder<MobEffect> effect;
            int ticks;
            switch (level.random.nextInt(3)) {
                case 0 -> {
                    effect = MobEffects.BLINDNESS;
                    ticks = 60;
                }
                case 1 -> {
                    effect = MobEffects.CONFUSION;
                    ticks = 120;
                }
                default -> {
                    effect = MobEffects.WEAKNESS;
                    ticks = 200;
                }
            }
            caster.addEffect(new MobEffectInstance(effect, ticks, 0));
            caster.displayClientMessage(Component.translatable("message.supernaturalcraft.bowl.backlash").withStyle(ChatFormatting.DARK_RED), true);
            // The reason goes to chat, where it stays long enough to read.
            caster.sendSystemMessage(Component.translatable("message.supernaturalcraft.bowl.backlash").withStyle(ChatFormatting.DARK_RED)
                    .append(" ").append(why.copy().withStyle(ChatFormatting.RED)));
        }
        bowl.backlashed(why);
        bowl.extinguish(BowlContents.EMPTY);
    }
}
