package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.hunter.TrappedEffect;

public class AllMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, SupernaturalCraft.MODID);

    public static final DeferredHolder<MobEffect, TrappedEffect> TRAPPED =
            MOB_EFFECTS.register("trapped", TrappedEffect::new);

    public static final DeferredHolder<MobEffect, TrappedEffect> STUNNED =
            MOB_EFFECTS.register("stunned", () -> new TrappedEffect(0xE8D9A8, "stunned"));

    /** Spoken over by the Whispering Codex: takes a fifth more harm from everything. */
    public static final DeferredHolder<MobEffect, MobEffect> MARKED =
            MOB_EFFECTS.register("marked", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x6A2232) {
            });

    /** Azazel's smoke rides this creature: stronger, faster, and turned on whoever fights him. */
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.entity.boss.azazel.PossessedEffect> POSSESSED =
            MOB_EFFECTS.register("possessed", org.papiricoh.supernaturalcraft.entity.boss.azazel.PossessedEffect::new);

    /** A bowl spell hides its caster from demons, angels, hounds and spirits (never from bosses). */
    public static final DeferredHolder<MobEffect, MobEffect> CONCEALED =
            MOB_EFFECTS.register("concealed", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x5A6A7A) {
            });
    /** Sees what hides: ghosts, invisible hounds, hidden hex bags. */
    public static final DeferredHolder<MobEffect, MobEffect> SECOND_SIGHT =
            MOB_EFFECTS.register("second_sight", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xBFE3FF) {
            });
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.hex.JinxedEffect> JINXED =
            MOB_EFFECTS.register("jinxed", org.papiricoh.supernaturalcraft.hex.JinxedEffect::new);
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.hex.BleedingEffect> BLEEDING =
            MOB_EFFECTS.register("bleeding", org.papiricoh.supernaturalcraft.hex.BleedingEffect::new);
    /** A soul the crossroads collected: two hearts short and no mana coming back, for three days. */
    public static final DeferredHolder<MobEffect, MobEffect> SOULLESS =
            MOB_EFFECTS.register("soulless", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x2A2A30) {
            }.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,
                    SupernaturalCraft.asResource("effect.soulless"), -4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));

    /** Pestilence's plague: stacks, eats at you, stops you healing and takes away hearts while it lasts. */
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PlagueEffect> PLAGUE =
            MOB_EFFECTS.register("plague", org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence.PlagueEffect::new);

    // --- The Archangel Michael (v0.12) -----------------------------------------------------------
    /** Worn by Michael: the body is his for a while. */
    public static final DeferredHolder<MobEffect, MobEffect> VESSEL =
            MOB_EFFECTS.register("vessel", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0xBFE6FF) {
            });
    /** What his grace left behind when he let go: every blow against him lands twice as hard. */
    public static final DeferredHolder<MobEffect, MobEffect> GRACE_FAVOR =
            MOB_EFFECTS.register("grace_favor", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xFFE59A) {
            });
    /** Said no to him: the Host hunts you. */
    public static final DeferredHolder<MobEffect, MobEffect> HEAVENS_MARK =
            MOB_EFFECTS.register("heavens_mark", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0xE8D27A) {
            });

    // --- Heaven (v0.18) ----------------------------------------------------------------------------------------------------
    /** Reprogrammed by Naomi: blows against angels land weaker for a while. */
    public static final DeferredHolder<MobEffect, MobEffect> CONDITIONED =
            MOB_EFFECTS.register("conditioned", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0xD8F4FF) {
            });
    /** DENIED by Zachariah's stamp: no natural healing until the paperwork clears. */
    public static final DeferredHolder<MobEffect, MobEffect> PAPERWORK =
            MOB_EFFECTS.register("paperwork", () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0xE8D49A) {
            });

    // --- The v0.21 witchcraft ------------------------------------------------------------------------------------------------
    /** The Curse of Silence: no sigil may be cast and no allegiance power used while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> SILENCED =
            MOB_EFFECTS.register("silenced", () -> new org.papiricoh.supernaturalcraft.hex.CurseEffects.Marker(
                    net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x5A2752));
    /** The Curse of Pain: pulsing magic harm that, like poison, never kills. */
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.hex.CurseEffects.Agony> AGONY =
            MOB_EFFECTS.register("agony", org.papiricoh.supernaturalcraft.hex.CurseEffects.Agony::new);
    /** The Hex of Rot: food in the pack turns to rotten flesh. */
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.hex.CurseEffects.Rot> ROT =
            MOB_EFFECTS.register("rot", org.papiricoh.supernaturalcraft.hex.CurseEffects.Rot::new);
    /** The Ward of the Name: curses aimed at the bearer fail and turn on whoever spoke them. */
    public static final DeferredHolder<MobEffect, MobEffect> NAME_WARDED =
            MOB_EFFECTS.register("name_warded", () -> new org.papiricoh.supernaturalcraft.hex.CurseEffects.Shield(
                    net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xB23A8E));
    /** The Mirror Hex: the next curse aimed at the bearer goes back to its caster (and the mirror breaks). */
    public static final DeferredHolder<MobEffect, MobEffect> MIRRORED =
            MOB_EFFECTS.register("mirrored", () -> new org.papiricoh.supernaturalcraft.hex.CurseEffects.Shield(
                    net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xC9C2FF));
    /** Silver Edge (sigil): the next blows (amplifier + 1) count as silver. */
    public static final DeferredHolder<MobEffect, MobEffect> SILVER_EDGE =
            MOB_EFFECTS.register("silver_edge", () -> new org.papiricoh.supernaturalcraft.hex.CurseEffects.Marker(
                    net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0xB7C3CC));
    /** Compel (bowl): a creature follows and spares its compeller; a player is drawn toward the bowl. */
    public static final DeferredHolder<MobEffect, org.papiricoh.supernaturalcraft.hex.CurseEffects.Compelled> COMPELLED =
            MOB_EFFECTS.register("compelled", org.papiricoh.supernaturalcraft.hex.CurseEffects.Compelled::new);

    public static void init() {
    }
}
