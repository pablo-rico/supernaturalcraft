package org.papiricoh.supernaturalcraft.allegiance.power;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.ServerScheduler;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
/**
 * The angel's powers of v0.20: the memory wipe (Lesser Angel), holy chains (Seraph), the snap (Archangel) and the pillar of
 * judgment (General); Ascend is a crossing ({@link Crossings}). Each returns what {@link ActivePowers#perform} returns.
 */
public final class AngelPowers {

    /** Ticks a wiped mob cannot set its mind on the caster again; a player's daze. */
    public static final int FORGET_TICKS = 200, DAZE_TICKS = 100, DAZE_BLIND_TICKS = 40;
    /** Persistent data on a wiped mob: whom it forgot, and until when. */
    public static final String FORGETS = "sn_forgets", FORGETS_UNTIL = "sn_forgets_until";
    public static final int CHAIN_TICKS = 80;
    /** Snap: the lesser burst (max health up to {@link #SNAP_LESSER_HEALTH}), the rest are hurt; a boss barely. */
    public static final float SNAP_LESSER = 40f, SNAP_OTHER = 15f, SNAP_PLAYER = 8f, SNAP_BOSS = 6f, SNAP_LESSER_HEALTH = 100f;
    public static final int PILLAR_WARN = 30;
    public static final float PILLAR_RADIUS = 3f, PILLAR_HEIGHT = 12f, PILLAR_DAMAGE = 30f, PILLAR_DEMON = 1.5f, PILLAR_BOSS = 15f;

    private AngelPowers() {
    }

    // --- memory wipe -----------------------------------------------------------------------------------------------------

    /** Everything near forgets whom it fought, and cannot set its mind on the caster for a while. @return null if nothing heard */
    static @Nullable ActivePowers.Result memoryWipe(ServerPlayer p, int radius) {
        ServerLevel level = p.serverLevel();
        long until = level.getGameTime() + FORGET_TICKS;
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                e -> e != p && e.isAlive() && e.distanceTo(p) <= radius && !PowerTargets.boss(e))) {
            if (e instanceof Mob m) {
                forget(m, p, until);
                n++;
            } else if (e instanceof Player q && PowerTargets.foe(p, q, false)) {
                q.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DAZE_TICKS, 0, false, false), p);
                q.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DAZE_BLIND_TICKS, 0, false, false), p);
                n++;
            } else {
                continue;
            }
            level.sendParticles(AllParticles.GRACE.get(), e.getX(), e.getEyeY() + 0.3, e.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
        }
        if (n == 0) return null;
        level.playSound(null, p.blockPosition(), AllSounds.ALLEGIANCE_RADIO.get(), SoundSource.PLAYERS, 1f, 0.6f);
        return new ActivePowers.Result(null, p.position(), FORGET_TICKS);
    }

    /** {@code mob} forgets whom it fought and is angry at no one; it cannot target {@code p} until {@code until}. */
    public static void forget(Mob mob, ServerPlayer p, long until) {
        CompoundTag data = mob.getPersistentData();
        data.putUUID(FORGETS, p.getUUID());
        data.putLong(FORGETS_UNTIL, until);
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.setLastHurtByPlayer(null);
        mob.getNavigation().stop();
        if (mob instanceof NeutralMob angry) angry.stopBeingAngry();
        var brain = mob.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if (brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) brain.eraseMemory(MemoryModuleType.ANGRY_AT);
    }

    /** Whether {@code mob} has forgotten {@code p} (a wiped memory not yet come back). */
    public static boolean forgets(LivingEntity mob, Player p) {
        CompoundTag data = mob.getPersistentData();
        if (!data.hasUUID(FORGETS) || !data.getUUID(FORGETS).equals(p.getUUID())) return false;
        if (mob.level().getGameTime() < data.getLong(FORGETS_UNTIL)) return true;
        data.remove(FORGETS);
        data.remove(FORGETS_UNTIL);
        return false;
    }

    // --- holy chains -----------------------------------------------------------------------------------------------------

    /** Chains of light bind what the caster looks at; a boss is only slowed. */
    static @Nullable ActivePowers.Result holyChains(ServerPlayer p, @Nullable LivingEntity t) {
        if (t == null || !PowerTargets.foe(p, t, false)) return null;
        ServerLevel level = p.serverLevel();
        if (PowerTargets.boss(t)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHAIN_TICKS, 1, false, true), p);
        else t.addEffect(new MobEffectInstance(AllMobEffects.STUNNED, CHAIN_TICKS, 0, false, true), p);
        TelegraphMarker.ring(level, t.position(), Math.max(0.8f, t.getBbWidth()), TelegraphMarker.GOLD, CHAIN_TICKS);
        level.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 24, t.getBbWidth() / 2, t.getBbHeight() / 2,
                t.getBbWidth() / 2, 0.01);
        level.playSound(null, t.blockPosition(), AllSounds.UNCAGED_CHAINS.get(), SoundSource.PLAYERS, 1f, 1.4f);
        return new ActivePowers.Result(t, t.position(), CHAIN_TICKS);
    }

    // --- snap ------------------------------------------------------------------------------------------------------------

    /** A snap of the fingers: the lesser foes near burst, the greater are hurt. @return null if there was no foe near */
    static @Nullable ActivePowers.Result snap(ServerPlayer p, int radius) {
        ServerLevel level = p.serverLevel();
        var foes = PowerTargets.foesAround(p, p.position(), radius);
        if (foes.isEmpty()) return null;
        for (LivingEntity e : foes) {
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.GRACE, p), Ascension.vsBoss(p, e, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(snapDamage(e))));
            level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FIREWORK, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        }
        level.playSound(null, p.blockPosition(), AllSounds.GABRIEL_SNAP.get(), SoundSource.PLAYERS, 2f, 1.1f);
        return new ActivePowers.Result(null, p.position(), 10);
    }

    public static float snapDamage(LivingEntity e) {
        if (PowerTargets.boss(e)) return SNAP_BOSS;
        if (e instanceof Player) return SNAP_PLAYER;
        return e.getMaxHealth() <= SNAP_LESSER_HEALTH ? SNAP_LESSER : SNAP_OTHER;
    }

    // --- pillar of judgment ----------------------------------------------------------------------------------------------

    /** A golden circle where the caster looks; {@link #PILLAR_WARN} ticks later the pillar falls. */
    static @Nullable ActivePowers.Result judgmentPillar(ServerPlayer p, int range) {
        Vec3 at = PowerCaster.aimPoint(p, range);
        if (at == null) return null;
        ServerLevel level = p.serverLevel();
        TelegraphMarker.circle(level, at, PILLAR_RADIUS, TelegraphMarker.GOLD, PILLAR_WARN);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), AllSounds.ALLEGIANCE_SMITE.get(), SoundSource.PLAYERS, 1f, 0.6f);
        ServerScheduler.schedule(PILLAR_WARN, () -> pillar(p, level, at));
        return new ActivePowers.Result(null, at, PILLAR_WARN);
    }

    /** The pillar falls on {@code at}. @return how many it struck */
    public static int pillar(ServerPlayer p, ServerLevel level, Vec3 at) {
        int n = 0;
        for (LivingEntity e : PowerTargets.foesAround(p, at, PILLAR_RADIUS + PILLAR_HEIGHT)) {
            Vec3 d = e.position().subtract(at);
            if (d.x * d.x + d.z * d.z > PILLAR_RADIUS * PILLAR_RADIUS || d.y < -1 || d.y > PILLAR_HEIGHT) continue;
            float dmg = PowerTargets.boss(e) ? PILLAR_BOSS : Kin.isDemon(e) ? PILLAR_DAMAGE * PILLAR_DEMON : PILLAR_DAMAGE;
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.JUDGMENT, p), Ascension.vsBoss(p, e, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(dmg)));
            if (Kin.isDemon(e)) e.igniteForSeconds(4);
            n++;
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            Vec3 c = at.add(Math.cos(a) * PILLAR_RADIUS * 0.6, 0, Math.sin(a) * PILLAR_RADIUS * 0.6);
            org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.column(level, c, ParticleTypes.END_ROD, PILLAR_HEIGHT + 4, 16);
        }
        org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.column(level, at, AllParticles.GRACE.get(), PILLAR_HEIGHT + 8, 24);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), AllSounds.ALLEGIANCE_TRUE_FORM.get(), SoundSource.PLAYERS, 2f, 1.3f);
        return n;
    }
}
