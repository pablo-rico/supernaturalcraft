package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.BossTwists;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.network.CastPowerPayload;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Casting a power from the wheel ({@link CastPowerPayload}). The server checks everything again ({@link PowerRules#canCast}
 * with the configured cost, the cooldowns kept here and the suppression of the Author's arena), resolves the target (the
 * one the client saw if it is alive, in range and in sight, else whatever is under the caster's gaze) and runs the power
 * ({@link ActivePowers}). Refusals go back as {@link AllegianceFxPayload#DENIED} (arg = {@link PowerRules.Verdict} ordinal);
 * a cast goes to everyone near as {@link AllegianceFxPayload#CAST} (arg = power ordinal, arg2 = target entity id or -1,
 * point = where it landed). Only a power that took effect is paid for and starts its cooldown.
 */
public final class PowerCaster {

    /** Ticks a caster's eyes show after a cast (black/yellow/red for a demon, light for an angel). */
    public static final int EYES_TICKS = 40;

    private static final Map<UUID, EnumMap<Power, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> EYES_UNTIL = new ConcurrentHashMap<>();

    private PowerCaster() {
    }

    /** From the network. */
    public static void cast(ServerPlayer player, CastPowerPayload payload) {
        cast(player, Power.byOrdinal(payload.power()), payload.target());
    }

    /** @return the verdict ({@code OK} if the power took effect) */
    public static PowerRules.Verdict cast(ServerPlayer player, @Nullable Power power, int targetId) {
        if (power == null) return deny(player, PowerRules.Verdict.NOT_YOURS);
        // The second half of a two-step power costs nothing more: telekinesis hurls what it holds.
        if (power == Power.TELEKINESIS && ActivePowers.holding(player)) {
            ActivePowers.hurl(player);
            return PowerRules.Verdict.OK;
        }
        Allegiance a = Allegiances.get(player);
        float cost = cost(power);
        PowerRules.Verdict v = PowerRules.canCast(a, power, cooldownLeft(player, power), suppressed(player));
        if (v == PowerRules.Verdict.NO_ESSENCE && a.essence() >= cost) v = PowerRules.Verdict.OK;
        if (v == PowerRules.Verdict.OK && a.essence() < cost) v = PowerRules.Verdict.NO_ESSENCE;
        // Held by a devil's trap, salt or holy fire: no smoking out, no blinking away.
        if (v == PowerRules.Verdict.OK && (power == Power.SMOKE || power == Power.TELEPORT)
                && player.hasEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.TRAPPED)) v = PowerRules.Verdict.SUPPRESSED;
        if (v != PowerRules.Verdict.OK) return deny(player, v);
        LivingEntity target = target(player, targetId, power.range);
        ActivePowers.Result done = ActivePowers.perform(player, power, target);
        if (done == null) return deny(player, PowerRules.Verdict.NO_TARGET);
        Allegiances.update(player, Allegiances.get(player).addEssence(-cost));
        startCooldown(player, power);
        showEyes(player);
        AllegianceFx.around(player, AllegianceFxPayload.CAST, power.ordinal(), done.target() == null ? -1 : done.target().getId(),
                done.point(), done.duration());
        return PowerRules.Verdict.OK;
    }

    static PowerRules.Verdict deny(ServerPlayer player, PowerRules.Verdict v) {
        AllegianceFx.toSelf(player, AllegianceFxPayload.DENIED, v.ordinal(), 0, player.position(), 0);
        return v;
    }

    /** Powers are nullified in Chuck's arena. */
    public static boolean suppressed(ServerPlayer player) {
        return BossTwists.suppressed(player);
    }

    public static float cost(Power power) {
        return (float) (power.cost * SNConfig.POWER_COST_MULTIPLIER.get());
    }

    public static int cooldown(Power power) {
        return (int) Math.round(power.cooldown * SNConfig.POWER_COOLDOWN_MULTIPLIER.get());
    }

    public static int cooldownLeft(ServerPlayer player, Power power) {
        EnumMap<Power, Long> m = COOLDOWNS.get(player.getUUID());
        Long until = m == null ? null : m.get(power);
        return until == null ? 0 : (int) Math.max(0, until - player.serverLevel().getGameTime());
    }

    public static void startCooldown(ServerPlayer player, Power power) {
        int ticks = cooldown(power);
        if (ticks <= 0) return;
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new EnumMap<>(Power.class)).put(power, player.serverLevel().getGameTime() + ticks);
    }

    /** Clears every cooldown and anything a power left running (logout, a change of side, the test command). */
    public static void forget(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
        EYES_UNTIL.remove(player.getUUID());
        ActivePowers.forget(player);
    }

    static void showEyes(ServerPlayer player) {
        EYES_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + EYES_TICKS);
        Allegiances.setFlag(player, Allegiances.EYES, true);
    }

    /** Every tick: the eyes fade, whatever a power holds moves on. */
    public static void tick(ServerPlayer player) {
        Long until = EYES_UNTIL.get(player.getUUID());
        if (until != null && player.serverLevel().getGameTime() >= until) {
            EYES_UNTIL.remove(player.getUUID());
            Allegiances.setFlag(player, Allegiances.EYES, false);
        }
        ActivePowers.tick(player);
    }

    /**
     * The target: the one the client saw if it still holds (alive, within {@code range} + 2, in sight), else the first
     * living thing along the caster's gaze. Null for self-cast powers ({@code range} 0) or nothing found.
     */
    static @Nullable LivingEntity target(ServerPlayer player, int targetId, int range) {
        if (range <= 0) return null;
        if (targetId >= 0 && player.serverLevel().getEntity(targetId) instanceof LivingEntity e && e != player && e.isAlive()
                && e.distanceTo(player) <= range + 2 && player.hasLineOfSight(e)) {
            return e;
        }
        return gaze(player, range);
    }

    /** The first living thing along {@code player}'s gaze, within {@code range}. */
    public static @Nullable LivingEntity gaze(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && e != player, range * range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity l) || !player.hasLineOfSight(l)) return null;
        return l;
    }

    static boolean isOwnerOf(ServerPlayer player, Entity e) {
        return e.getUUID().equals(player.getUUID());
    }
}
