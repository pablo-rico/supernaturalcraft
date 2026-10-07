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

    public static void init() {
    }
}
