package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The demon's powers of v0.20: strangle (Crossroads Demon), the hellfire burst (Prince), the wave of flame and the shadow
 * step (Knight), the Maw of Hell (King); Descend is a crossing ({@link Crossings}). What runs on (a strangled throat, a
 * burning wedge of ground, an open maw) is ticked with its caster from {@link #tick}; a shadow step's exposure is consumed by
 * the next blow ({@link #exposedBlow}) and swept server-wide.
 */
public final class DemonPowers {

    public static final int STRANGLE_TICKS = 60, STRANGLE_EVERY = 10;
    public static final float STRANGLE_DAMAGE = 2f, STRANGLE_LIFT = 1.5f, STRANGLE_MAX_HEALTH = 100f;
    public static final float BURST_DAMAGE = 8f, BURST_PUSH = 1.2f, BURST_LIFT = 0.4f;
    public static final int BURST_FIRE = 4;
    public static final int WAVE_TICKS = 100, WAVE_EVERY = 10, WAVE_FIRE = 5;
    /** The wave's wedge: half of the 120° the telegraph draws. */
    public static final double WAVE_HALF_ANGLE = 60;
    public static final float WAVE_DAMAGE = 6f, WAVE_BURN = 2f;
    public static final int EXPOSED_TICKS = 100;
    public static final float EXPOSED = 1.5f, STEP_BEHIND = 1.5f;
    public static final int MAW_TICKS = 100, MAW_EVERY = 10;
    public static final float MAW_REACH = 7f, MAW_CORE = 2.5f, MAW_PULL = 0.12f, MAW_DAMAGE = 3f;

    private record Throat(int entity, Vec3 anchor, long until) {
    }

    private record Wave(ServerLevel level, Vec3 from, float yaw, long until) {
    }

    private record Maw(ServerLevel level, Vec3 at, long until) {
    }

    private static final Map<UUID, Throat> STRANGLING = new ConcurrentHashMap<>();
    private static final Map<UUID, Wave> WAVES = new ConcurrentHashMap<>();
    private static final Map<UUID, Maw> MAWS = new ConcurrentHashMap<>();
    /** Entities a shadow step left exposed: until when (game time). */
    private static final Map<UUID, Long> EXPOSED_UNTIL = new ConcurrentHashMap<>();

    private DemonPowers() {
    }

    // --- strangle --------------------------------------------------------------------------------------------------------

    /** What a throat may be closed on: a foe, not a boss, nothing too great (a player of the other side, with PvP). */
    static boolean strangleable(ServerPlayer p, @Nullable LivingEntity t) {
        return t != null && PowerTargets.foe(p, t, false) && !PowerTargets.boss(t)
                && (t instanceof Player || t.getMaxHealth() <= STRANGLE_MAX_HEALTH);
    }

    static @Nullable ActivePowers.Result strangle(ServerPlayer p, @Nullable LivingEntity t) {
        if (!strangleable(p, t)) return null;
        ServerLevel level = p.serverLevel();
        STRANGLING.put(p.getUUID(), new Throat(t.getId(), t.position().add(0, STRANGLE_LIFT, 0), level.getGameTime() + STRANGLE_TICKS));
        level.playSound(null, t.blockPosition(), AllSounds.ALLEGIANCE_TELEKINESIS.get(), SoundSource.PLAYERS, 1f, 0.7f);
        return new ActivePowers.Result(t, t.position(), STRANGLE_TICKS);
    }

    /** Whom {@code p} is strangling, if anyone. */
    public static @Nullable LivingEntity strangled(ServerPlayer p) {
        Throat t = STRANGLING.get(p.getUUID());
        return t != null && p.serverLevel().getEntity(t.entity()) instanceof LivingEntity e ? e : null;
    }

    private static void tickStrangle(ServerPlayer p, ServerLevel level, Throat throat, long now) {
        if (!(level.getEntity(throat.entity()) instanceof LivingEntity e) || !e.isAlive() || !p.isAlive() || now >= throat.until()) {
            STRANGLING.remove(p.getUUID());
            return;
        }
        e.setDeltaMovement(throat.anchor().subtract(e.position()).scale(0.5));
        e.hurtMarked = true;
        e.fallDistance = 0;
        if (e instanceof Mob m) {
            m.setTarget(null);
            m.getNavigation().stop();
        }
        if ((throat.until() - now) % STRANGLE_EVERY == 0) {
            e.invulnerableTime = 0;
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, p), org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(STRANGLE_DAMAGE));
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), e.getX(), e.getEyeY() - 0.2, e.getZ(), 6, 0.15, 0.1, 0.15, 0.01);
        }
    }

    // --- hellfire burst --------------------------------------------------------------------------------------------------

    static ActivePowers.Result hellfireBurst(ServerPlayer p, int radius) {
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : PowerTargets.foesAround(p, p.position(), radius)) {
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, p), Ascension.vsBoss(p, e, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(BURST_DAMAGE)));
            e.igniteForSeconds(BURST_FIRE);
            if (PowerTargets.boss(e)) continue;
            Vec3 out = e.position().subtract(p.position()).multiply(1, 0, 1);
            out = out.lengthSqr() < 0.01 ? p.getLookAngle().multiply(1, 0, 1) : out;
            e.setDeltaMovement(out.normalize().scale(BURST_PUSH).add(0, BURST_LIFT, 0));
            e.hurtMarked = true;
        }
        for (int r = 1; r <= radius; r++) {
            int n = r * 8;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                level.sendParticles(AllParticles.HELLFIRE.get(), p.getX() + Math.cos(a) * r, p.getY() + 0.3, p.getZ() + Math.sin(a) * r, 1, 0.05, 0.1, 0.05, 0.01);
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
        return new ActivePowers.Result(null, p.position(), 10);
    }

    // --- wave of flame ---------------------------------------------------------------------------------------------------

    static ActivePowers.Result flameWave(ServerPlayer p, int reach) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        float yaw = p.getYRot();
        for (LivingEntity e : inWedge(p, level, from, yaw, reach)) {
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, p), Ascension.vsBoss(p, e, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(WAVE_DAMAGE)));
            e.igniteForSeconds(WAVE_FIRE);
        }
        TelegraphMarker.cone(level, from, yaw, reach, TelegraphMarker.RED, WAVE_TICKS);
        WAVES.put(p.getUUID(), new Wave(level, from, yaw, level.getGameTime() + WAVE_TICKS));
        Vec3 dir = LuciferAttacks.dirOf(yaw);
        for (int i = 1; i <= reach; i++) {
            Vec3 c = from.add(dir.scale(i));
            level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 6 + i, i * 0.25, 0.3, i * 0.25, 0.04);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.7f);
        return new ActivePowers.Result(null, from, WAVE_TICKS);
    }

    /** The foes in the 120° wedge {@code reach} long in front of {@code from}, facing {@code yaw}. */
    public static List<LivingEntity> inWedge(ServerPlayer p, ServerLevel level, Vec3 from, float yaw, double reach) {
        Vec3 dir = LuciferAttacks.dirOf(yaw);
        double cos = Math.cos(Math.toRadians(WAVE_HALF_ANGLE));
        return PowerTargets.foesAround(p, from, reach).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return (d < 0.8 || to.normalize().dot(dir) >= cos) && Math.abs(e.getY() - from.y) < 3;
        }).toList();
    }

    private static void tickWave(ServerPlayer p, Wave w, long now) {
        if (now >= w.until()) {
            WAVES.remove(p.getUUID());
            return;
        }
        if ((w.until() - now) % WAVE_EVERY != 0) return;
        Vec3 dir = LuciferAttacks.dirOf(w.yaw());
        for (int i = 1; i <= Power.FLAME_WAVE.range; i += 2) {
            Vec3 c = w.from().add(dir.scale(i));
            w.level().sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.1, c.z, 3, i * 0.3, 0.05, i * 0.3, 0.01);
        }
        for (LivingEntity e : inWedge(p, w.level(), w.from(), w.yaw(), Power.FLAME_WAVE.range)) {
            e.invulnerableTime = 0;
            e.hurt(AllDamageTypes.source(w.level(), AllDamageTypes.HELLFIRE, p), Ascension.vsBoss(p, e, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(WAVE_BURN)));
            e.igniteForSeconds(3);
        }
    }

    // --- shadow step -----------------------------------------------------------------------------------------------------

    static @Nullable ActivePowers.Result shadowStep(ServerPlayer p, @Nullable LivingEntity t) {
        if (t == null || !PowerTargets.foe(p, t, false)) return null;
        ServerLevel level = p.serverLevel();
        Vec3 at = behind(p, t);
        if (at == null) return null;
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), p.getX(), p.getY() + 1, p.getZ(), 24, 0.3, 0.6, 0.3, 0.03);
        float yaw = LuciferAttacks.yawTo(at, t.position());
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        p.moveTo(at.x, at.y, at.z, yaw, 0);
        p.teleportTo(level, at.x, at.y, at.z, yaw, 0);
        p.setYHeadRot(yaw);
        p.hurtMarked = true;
        expose(t, level.getGameTime() + EXPOSED_TICKS);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), at.x, at.y + 1, at.z, 24, 0.3, 0.6, 0.3, 0.03);
        level.playSound(null, BlockPos.containing(at), AllSounds.AZAZEL_SMOKE.get(), SoundSource.PLAYERS, 1f, 1.3f);
        return new ActivePowers.Result(t, at, EXPOSED_TICKS);
    }

    /** Where a step out of the shadow lands: {@link #STEP_BEHIND} blocks behind {@code t}'s back, on a spot the body fits. */
    static @Nullable Vec3 behind(ServerPlayer p, LivingEntity t) {
        Vec3 back = Vec3.directionFromRotation(0, t.getYRot());
        Vec3 base = t.position().subtract(back.scale(STEP_BEHIND + t.getBbWidth() / 2));
        for (int dy : new int[]{0, 1, -1, 2}) {
            Vec3 c = new Vec3(base.x, Math.floor(base.y) + dy, base.z);
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(p.position())))) return c;
        }
        return null;
    }

    public static void expose(LivingEntity e, long until) {
        EXPOSED_UNTIL.put(e.getUUID(), until);
    }

    public static boolean exposed(LivingEntity e) {
        Long until = EXPOSED_UNTIL.get(e.getUUID());
        return until != null && e.level().getGameTime() < until;
    }

    /** A blow lands on {@code victim}: if a shadow step left it exposed, it is the one that cuts deep. @return the multiplier */
    public static float exposedBlow(LivingEntity victim) {
        Long until = EXPOSED_UNTIL.remove(victim.getUUID());
        if (until == null || victim.level().getGameTime() >= until) return 1f;
        if (victim.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 16, 0.3, 0.3, 0.3, 0.2);
        }
        return EXPOSED;
    }

    // --- the Maw of Hell -------------------------------------------------------------------------------------------------

    static @Nullable ActivePowers.Result maw(ServerPlayer p, int range) {
        Vec3 at = PowerCaster.aimPoint(p, range);
        if (at == null) return null;
        ServerLevel level = p.serverLevel();
        MAWS.put(p.getUUID(), new Maw(level, at, level.getGameTime() + MAW_TICKS));
        TelegraphMarker.ring(level, at, MAW_REACH, TelegraphMarker.RED, MAW_TICKS);
        TelegraphMarker.circle(level, at, MAW_CORE, TelegraphMarker.RED, MAW_TICKS);
        level.playSound(null, BlockPos.containing(at), AllSounds.ALLEGIANCE_THRONE.get(), SoundSource.PLAYERS, 2f, 0.6f);
        return new ActivePowers.Result(null, at, MAW_TICKS);
    }

    /** One tick of an open maw: foes near are dragged in; every {@link #MAW_EVERY} ticks its core burns. @return how many it holds */
    public static int tickMaw(ServerPlayer p, ServerLevel level, Vec3 at, long left) {
        int n = 0;
        boolean burn = left % MAW_EVERY == 0;
        for (LivingEntity e : PowerTargets.foesAround(p, at, MAW_REACH)) {
            Vec3 in = at.subtract(e.position()).multiply(1, 0, 1);
            if (!PowerTargets.boss(e) && in.lengthSqr() > 0.04) {
                e.setDeltaMovement(e.getDeltaMovement().add(in.normalize().scale(MAW_PULL)));
                e.hurtMarked = true;
            }
            if (burn && e.position().distanceTo(at) <= MAW_CORE + e.getBbWidth() / 2) {
                e.invulnerableTime = 0;
                e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, p), Ascension.vsBoss(p, e, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(MAW_DAMAGE)));
            }
            n++;
        }
        if (left % 2 == 0) {
            double a = left * 0.35;
            for (int arm = 0; arm < 3; arm++) {
                double b = a + arm * Math.PI * 2 / 3;
                double r = MAW_REACH * ((left % 20) / 20.0);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x + Math.cos(b) * r, at.y + 0.2, at.z + Math.sin(b) * r, 1, 0, 0.05, 0, 0.01);
                level.sendParticles(AllParticles.HELLFIRE.get(), at.x + Math.cos(b) * r * 0.5, at.y + 0.3, at.z + Math.sin(b) * r * 0.5, 1, 0, 0.1, 0, 0.02);
            }
        }
        return n;
    }

    // --- ticking ---------------------------------------------------------------------------------------------------------

    static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        long now = p.serverLevel().getGameTime();
        Throat throat = STRANGLING.get(id);
        if (throat != null) tickStrangle(p, p.serverLevel(), throat, now);
        Wave wave = WAVES.get(id);
        if (wave != null) tickWave(p, wave, now);
        Maw maw = MAWS.get(id);
        if (maw != null) {
            if (now >= maw.until() || !p.isAlive()) MAWS.remove(id);
            else tickMaw(p, maw.level(), maw.at(), maw.until() - now);
        }
    }

    /** Exposures nobody struck run out (once a second, for the whole server). */
    public static void sweep(MinecraftServer server) {
        if (EXPOSED_UNTIL.isEmpty() || server.overworld().getGameTime() % 20 != 0) return;
        long now = server.overworld().getGameTime();
        EXPOSED_UNTIL.values().removeIf(until -> until <= now);
    }

    static void forget(UUID player) {
        STRANGLING.remove(player);
        WAVES.remove(player);
        MAWS.remove(player);
    }
}
