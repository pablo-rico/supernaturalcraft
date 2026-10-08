package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.papiricoh.supernaturalcraft.allegiance.power.ActivePowers;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.entity.allegiance.HostAllyEntity;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.BoundHellhoundEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.UUID;

/**
 * How creatures react to a sworn player (v0.13), by events only:
 * <ul>
 *   <li>the same side never turns on them ({@link LivingChangeTargetEvent} cancelled): demons and hellhounds on a demon,
 *   the Host on an angel; unless the player struck first (a King of Hell is obeyed even then). Bosses fight on, as do
 *   Michael's own soldiers and the hounds come to collect a debt;</li>
 *   <li>ghosts leave angels alone; a ridden mob never turns on its rider;</li>
 *   <li>the other side hunts them: demons get a goal for angel players, angels one for demon players
 *   ({@link EntityJoinLevelEvent});</li>
 *   <li>villagers flee a demon whose eyes show.</li>
 * </ul>
 */
public final class MobReactions {

    /** Villagers within this many blocks of a demon showing their eyes panic. */
    public static final double VILLAGER_FEAR = 10;

    private MobReactions() {
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide || !(event.getNewAboutToBeSetTarget() instanceof Player p)) return;
        if (ignores(mob, p)) event.setCanceled(true);
    }

    /** Whether {@code mob} leaves {@code p} alone. */
    public static boolean ignores(LivingEntity mob, Player p) {
        if (mob.getType().is(AllTags.Entities.BOSSES)) return false;
        if (ActivePowers.rides(p, mob)) return true;
        if (mob instanceof HostAngelEntity h && !(mob instanceof HostAllyEntity) && h.master() != null) return false;
        if (mob instanceof HellhoundEntity hound && p.getUUID().equals(hound.quarry())) return false;
        if (mob.getType().is(AllTags.Entities.SPIRITS) && Kin.isAngel(p)) return true;
        if (!Kin.sameSide(mob, p)) return false;
        boolean provoked = mob.getLastHurtByMob() == p;
        return !provoked || p instanceof ServerPlayer sp
                && PowerRules.passiveActive(Allegiances.get(sp), Power.DOMINION, PowerCaster.suppressed(sp));
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Monster mob)) return;
        if (mob.getType().is(AllTags.Entities.BOSSES) || mob instanceof HostAllyEntity || mob instanceof BoundHellhoundEntity
                || mob instanceof RivalHunterEntity || mob instanceof CrossroadsDemonEntity) return;
        if (Kin.isDemon(mob)) {
            mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, 10, true, false, Kin::isAngel));
        } else if (Kin.isAngel(mob)) {
            mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, 10, true, false, Kin::isDemon));
        }
    }

    /** Villagers near a demon whose eyes show run from them. @return how many */
    public static int frightenVillagers(ServerPlayer demon) {
        int n = 0;
        for (Villager v : demon.serverLevel().getEntitiesOfClass(Villager.class, demon.getBoundingBox().inflate(VILLAGER_FEAR))) {
            v.getBrain().setMemoryWithExpiry(MemoryModuleType.NEAREST_HOSTILE, demon, 100);
            n++;
        }
        return n;
    }

    /** Whether some player rides {@code mob}. */
    public static boolean ridden(LivingEntity mob) {
        UUID rider = ActivePowers.rider(mob);
        return rider != null;
    }
}
