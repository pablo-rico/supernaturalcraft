package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * SucroCorp's additive (v0.25): a sedated hunter is docile cattle — slow (the effect's modifier), unable to sprint (here), blind
 * to a Leviathan's borrowed face and beneath its notice. Milk washes it out (the effect's default cures), and so does holy water.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class Sedation {

    private Sedation() {
    }

    public static boolean sedated(LivingEntity entity) {
        return entity.hasEffect(AllMobEffects.SEDATED);
    }

    /** Holy water (or anything else that clears the head) washes it out. */
    public static void cure(LivingEntity entity) {
        entity.removeEffect(AllMobEffects.SEDATED);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.isSprinting() && sedated(p)) p.setSprinting(false);
    }
}
