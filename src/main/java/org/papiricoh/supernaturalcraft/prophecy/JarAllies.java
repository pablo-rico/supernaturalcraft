package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a reader of the Leviathan Tablet's fourth chapter lets out of a Purgatory jar fights for them (v0.36) for
 * {@link #TICKS}: it never turns on a player, it goes for whatever hurt its master or for the nearest monster, and when its
 * time is up it goes back to Purgatory. Not saved: an ally does not survive a restart.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class JarAllies {

    public static final int TICKS = 1200, SEEK = 16, CHECK_TICKS = 10;

    private record Ally(UUID owner, ServerLevel level, long until) {
    }

    private static final Map<Mob, Ally> ALLIES = new ConcurrentHashMap<>();

    private JarAllies() {
    }

    public static void bind(Mob mob, Player owner) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        ALLIES.put(mob, new Ally(owner.getUUID(), level, level.getGameTime() + TICKS));
        mob.setTarget(null);
        mob.setPersistenceRequired();
    }

    public static boolean ally(Entity e) {
        return e instanceof Mob m && ALLIES.containsKey(m);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob m) || !ALLIES.containsKey(m)) return;
        LivingEntity t = event.getNewAboutToBeSetTarget();
        if (t instanceof Player || ally(t)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (ALLIES.isEmpty() || server.getTickCount() % CHECK_TICKS != 0) return;
        for (Iterator<Map.Entry<Mob, Ally>> it = ALLIES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Mob, Ally> e = it.next();
            Mob mob = e.getKey();
            Ally a = e.getValue();
            if (!mob.isAlive()) {
                it.remove();
                continue;
            }
            if (a.level().getGameTime() >= a.until()) {
                a.level().sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                mob.discard();
                it.remove();
                continue;
            }
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive() && !(target instanceof Player) && !ally(target)) continue;
            Player owner = Players.byUuid(a.level(), a.owner());
            LivingEntity foe = owner != null && owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive()
                    && !ally(owner.getLastHurtByMob()) ? owner.getLastHurtByMob() : null;
            if (foe == null) {
                foe = a.level().getNearestEntity(a.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(SEEK),
                        l -> l instanceof Enemy && l != mob && l.isAlive() && !ally(l)),
                        net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat(), mob, mob.getX(), mob.getY(), mob.getZ());
            }
            mob.setTarget(foe);
            if (foe == null && owner != null && mob.distanceToSqr(owner) > 36) mob.getNavigation().moveTo(owner, 1.1);
        }
    }
}
