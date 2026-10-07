package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import net.minecraft.core.Holder;

/**
 * Game rules of the hex bags: a curse bag carried by someone other than its maker, or found in a
 * chest; demons that cannot find a protection bag's holder unless provoked; and the bag's immunity
 * to the curses' jinx and bleeding.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class HexEvents {

    private HexEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % HexBags.PULSE != 0) return;
        HexBags.afflictCarrier(player, true);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!event.getEntity().level().isClientSide) HexBags.afflictOpener(event.getEntity(), event.getContainer());
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob demon && event.getNewAboutToBeSetTarget() != null
                && !demon.level().isClientSide && !HexBags.mayTarget(demon, event.getNewAboutToBeSetTarget())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** A demon already hunting a holder (its provocation run out) gives up the hunt. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.tickCount % 20 != 0 || !(mob.getTarget() instanceof Player target)) return;
        if (!mob.level().isClientSide && !HexBags.mayTarget(mob, target)) mob.setTarget(null);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Mob demon && event.getSource().getEntity() instanceof Player
                && HexBags.wardedOff(demon)) {
            HexBags.provoke(demon, HexBags.PROVOKE_TICKS);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide) return;
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if ((effect.is(AllMobEffects.JINXED.getKey()) || effect.is(AllMobEffects.BLEEDING.getKey())) && HexBags.isProtected(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
