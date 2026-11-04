package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.MimicryPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The Leviathan Tablet's borrowed face (v0.36): while {@code MIMICRY} lasts, no mob but a boss takes its bearer for a foe
 * (and those who already did forget them), and everyone round them sees a villager. Striking anything ends it.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class Mimicry {

    private Mimicry() {
    }

    public static void start(LivingEntity who, int ticks) {
        who.addEffect(new MobEffectInstance(AllMobEffects.MIMICRY, ticks, 0, false, true, true));
        if (who.level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, who.getBoundingBox().inflate(32), m -> m.getTarget() == who && !m.getType().is(AllTags.Entities.BOSSES))) {
                m.setTarget(null);
            }
        }
        Send.trackingEntityAndSelf(who, new MimicryPayload(who.getId(), true));
    }

    public static void end(LivingEntity who) {
        if (who.hasEffect(AllMobEffects.MIMICRY)) who.removeEffect(AllMobEffects.MIMICRY);
        Send.trackingEntityAndSelf(who, new MimicryPayload(who.getId(), false));
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && target.hasEffect(AllMobEffects.MIMICRY) && !event.getEntity().getType().is(AllTags.Entities.BOSSES)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStrike(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(AllMobEffects.MIMICRY)) end(attacker);
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect().is(AllMobEffects.MIMICRY)) {
            Send.trackingEntityAndSelf(event.getEntity(), new MimicryPayload(event.getEntity().getId(), false));
        }
    }

    /** One who starts seeing a disguised player sees the disguise. */
    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player p && p.hasEffect(AllMobEffects.MIMICRY) && event.getEntity() instanceof ServerPlayer viewer) {
            Send.toPlayer(viewer, new MimicryPayload(p.getId(), true));
        }
    }
}
