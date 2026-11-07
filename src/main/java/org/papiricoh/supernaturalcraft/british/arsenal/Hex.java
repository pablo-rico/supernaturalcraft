package org.papiricoh.supernaturalcraft.british.arsenal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerTargets;
import org.papiricoh.supernaturalcraft.balance.WeaponDamage;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

/** The British hexes on the server (v0.41): the round's mark, the grenade's burst. Bosses only ever take their small damage. */
public final class Hex {

    private Hex() {
    }

    /** The enchanted round's hex on {@code e}: slowed and weakened for a moment. */
    public static void mark(LivingEntity e) {
        int t = ArsenalRules.hexTicks(ArsenalRules.ROUND_HEX_TICKS, BossDamage.isBoss(e));
        if (t <= 0) return;
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, ArsenalRules.ROUND_HEX_LEVEL));
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, ArsenalRules.ROUND_HEX_LEVEL));
    }

    /** Whether a hunter's hex may touch {@code e} ({@code owner} not a player: anything living but itself). */
    public static boolean target(@Nullable Entity owner, LivingEntity e) {
        if (e == owner || !e.isAlive()) return false;
        if (owner instanceof ServerPlayer p) return PowerTargets.foe(p, e, true);
        return true;
    }

    /** A hex grenade bursts at {@code at}: everything in it is slowed, weakened, blinded and a little hurt. */
    public static void burst(ServerLevel level, Vec3 at, @Nullable Entity direct, @Nullable Entity owner) {
        float r = ArsenalRules.GRENADE_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r, r / 2 + 1, r))) {
            if (!target(owner, e)) continue;
            float share = ArsenalRules.falloff(e.position().distanceTo(at), r);
            if (share <= 0) continue;
            boolean boss = BossDamage.isBoss(e);
            e.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, direct, owner), WeaponDamage.ability(ArsenalRules.GRENADE_DAMAGE) * share);
            int slow = ArsenalRules.hexTicks(ArsenalRules.GRENADE_SLOW_TICKS, boss);
            if (slow > 0) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ArsenalRules.GRENADE_WEAK_TICKS, 0));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ArsenalRules.GRENADE_BLIND_TICKS, 0));
            }
        }
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 60, r / 2, 0.6, r / 2, 0.05);
        level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.4, at.z, 30, r / 2, 0.3, r / 2, 0.02);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
