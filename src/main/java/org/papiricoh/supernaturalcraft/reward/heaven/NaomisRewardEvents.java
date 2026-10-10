package org.papiricoh.supernaturalcraft.reward.heaven;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.balance.DefenceEvents;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.UUID;

/**
 * Naomi's spoils at work (v0.18). The diadem: what it keeps off its wearer never takes, and a great enemy's blow loses
 * {@link NaomisDiademItem#AEGIS} more (after the Aegis of {@code DefenceEvents}). The drill: a reprogrammed creature never turns on
 * its wielder (nor is hurt by them), hunts what threatens them, strikes with the drill's Ascension, and is itself again when its
 * time runs out.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class NaomisRewardEvents {

    private NaomisRewardEvents() {
    }

    // --- the diadem ---------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (NaomisDiademItem.wards(event.getEffectInstance().getEffect()) && NaomisDiademItem.worn(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide || event.getNewDamage() <= 0) return;
        var src = event.getSource();
        if (!src.is(AllDamageTypes.DIVINE_WRATH) && !DefenceEvents.fromGreatEnemy(src)) return;
        if (NaomisDiademItem.worn(p)) event.setNewDamage(ProgressionScale.applyAegis(event.getNewDamage(), NaomisDiademItem.AEGIS));
    }

    // --- the drill ----------------------------------------------------------------------------------------------------------

    /** A reprogrammed creature never takes its master (nor anyone on their side of a fight) for a target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        UUID master = NaomisDrillItem.servant(event.getEntity());
        if (master != null && event.getNewAboutToBeSetTarget() instanceof Player p && p.getUUID().equals(master)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        // Master and servant do not hurt each other.
        UUID served = NaomisDrillItem.servant(victim);
        if (served != null && event.getSource().getEntity() instanceof Player p && p.getUUID().equals(served)) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof Mob mob) {
            UUID master = NaomisDrillItem.servant(mob);
            if (master == null) return;
            if (victim instanceof Player p && p.getUUID().equals(master)) {
                event.setCanceled(true);
                return;
            }
            float power = Math.max(1f, mob.getPersistentData().getFloat(NaomisDrillItem.POWER));
            event.setAmount(event.getAmount() * power);
        }
    }

    /** Every second while it serves: a target that threatens its master, or back to itself when its time is up. */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || mob.tickCount % 20 != 0) return;
        if (!mob.getPersistentData().hasUUID(NaomisDrillItem.ALLY)) return;
        UUID master = NaomisDrillItem.servant(mob);
        if (master == null) {
            NaomisDrillItem.forget(mob);
            return;
        }
        Player owner = level.getPlayerByUUID(master);
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive() || t == owner || NaomisDrillItem.servant(t) != null) {
            LivingEntity prey = prey(level, mob, owner);
            mob.setTarget(prey);
        }
        if (mob.tickCount % 40 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ(),
                    2, 0.15, 0.1, 0.15, 0.01);
        }
    }

    /** What it should hunt: whatever its master last struck or was struck by, else the nearest monster near them. */
    private static @Nullable LivingEntity prey(ServerLevel level, Mob mob, @Nullable Player owner) {
        if (owner != null) {
            LivingEntity last = owner.getLastHurtMob();
            if (last != null && last.isAlive() && last != mob && !BossDamage.isBoss(last) && NaomisDrillItem.servant(last) == null) return last;
            LivingEntity by = owner.getLastHurtByMob();
            if (by != null && by.isAlive() && by != mob && NaomisDrillItem.servant(by) == null) return by;
        }
        LivingEntity centre = owner != null ? owner : mob;
        LivingEntity best = null;
        double bestD = 16 * 16;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, centre.getBoundingBox().inflate(16),
                e -> e instanceof Enemy && e != mob && e.isAlive() && NaomisDrillItem.servant(e) == null && !BossDamage.isBoss(e))) {
            double d = e.distanceToSqr(centre);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** Test hook: what the diadem leaves of a great enemy's {@code amount}. */
    public static float diademAegis(float amount) {
        return ProgressionScale.applyAegis(amount, NaomisDiademItem.AEGIS);
    }
}
