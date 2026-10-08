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

    public static void init() {
    }
}
