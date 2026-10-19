package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.legacy.LegacyWeapons;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * Game rules of the v0.21 witchcraft's effects: a compelled creature never turns on its compeller, and a Silver Edge charge is
 * spent with each melee blow that lands.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class CurseEvents {

    private CurseEvents() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide || !mob.hasEffect(AllMobEffects.COMPELLED)) return;
        Player master = CurseEffects.Compelled.compeller(mob);
        if (master != null && event.getNewAboutToBeSetTarget() == master) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide || !LegacyWeapons.silverEdged(event.getSource())) return;
        spendSilverEdge((LivingEntity) event.getSource().getEntity());
    }

    /** One Silver Edge charge used: the effect drops a level, and goes when none is left. */
    public static void spendSilverEdge(LivingEntity attacker) {
        MobEffectInstance edge = attacker.getEffect(AllMobEffects.SILVER_EDGE);
        if (edge == null) return;
        attacker.removeEffect(AllMobEffects.SILVER_EDGE);
        if (edge.getAmplifier() > 0) {
            attacker.addEffect(new MobEffectInstance(AllMobEffects.SILVER_EDGE, edge.getDuration(), edge.getAmplifier() - 1,
                    edge.isAmbient(), edge.isVisible(), edge.showIcon()));
        }
    }
}
