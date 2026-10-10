package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * Naomi's rules that live outside her fight (v0.18): conditioning (a {@code CONDITIONED} hunter's blows on any angel land at
 * {@link NaomiBalance#CONDITIONED_FACTOR}; on Naomi herself the factor is in her own damage pipeline), the chair's straps (no
 * dismounting while strapped in, on either side), and blows on her console heard even where breaking is refused.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class NaomiEvents {

    private NaomiEvents() {
    }

    /** Whether {@code e} counts as an angel for conditioning: an angel by kin, or one of Heaven's great angels. */
    public static boolean angelic(Entity e) {
        if (Kin.isAngel(e)) return true;
        var t = e.getType();
        return t == AllEntities.ZACHARIAH.get() || t == AllEntities.MICHAEL.get() || t == AllEntities.RAPHAEL.get()
                || t == AllEntities.GABRIEL.get() || t == AllEntities.METATRON.get();
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof NaomiEntity || victim.level().isClientSide) return;
        if (event.getSource().getEntity() instanceof LivingEntity by && by.hasEffect(AllMobEffects.CONDITIONED) && angelic(victim)) {
            event.setAmount(event.getAmount() * NaomiBalance.CONDITIONED_FACTOR);
        }
    }

    /** Strapped in, a hunter cannot get up (unless they are dead or gone, or the chair is). */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof ReprogrammingChairEntity chair) || !chair.strapped()) return;
        Entity rider = event.getEntityMounting();
        if (chair.isRemoved() || !rider.isAlive() || rider.isRemoved()) return;
        if (rider instanceof net.minecraft.server.level.ServerPlayer sp && sp.hasDisconnected()) return;
        event.setCanceled(true);
    }

    /** A swing at the console, heard before anything that refuses breaking blocks in Heaven. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || !level.getBlockState(event.getPos()).is(AllBlocks.REPROGRAMMING_CONSOLE.get())) return;
        ReprogrammingConsoleBlock.struck(level, event.getPos(), p);
    }
}
