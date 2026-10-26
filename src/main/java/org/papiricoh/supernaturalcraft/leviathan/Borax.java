package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.function.Predicate;

/**
 * Borax (v0.25): the one thing in the world that burns a Leviathan. Thrown, sprayed or rubbed in, it reaches everything
 * {@link BoraxSensitive} it touches — a Leviathan burns ({@code borax_burn}: it stops knitting back) and sheds its disguise, a
 * severed head dies — and dries the goo it lands on ({@link #dry}). Harmless to everyone else.
 */
public final class Borax {

    private Borax() {
    }

    /** Dries the goo within {@code radius} of {@code at} (it only lies on Dick Roman's floor, so his arena does it). */
    public static void dry(ServerLevel level, Vec3 at, double radius) {
        org.papiricoh.supernaturalcraft.entity.boss.dick.DickRomanEntity.dryGoo(level, at, radius);
    }

    /** Borax lands in a splash of {@code radius} around {@code at}. @return how many sensitive things it touched */
    public static int splash(ServerLevel level, Vec3 at, double radius, @Nullable Entity owner) {
        int n = touch(level, new AABB(at, at).inflate(radius, radius / 2 + 1, radius), e -> e.position().distanceToSqr(at) <= radius * radius * 4, owner);
        dry(level, at, radius);
        org.papiricoh.supernaturalcraft.network.DickFxPayload.send(level, at, new org.papiricoh.supernaturalcraft.network.DickFxPayload(-1,
                org.papiricoh.supernaturalcraft.network.DickFxPayload.BORAX, (int) Math.round(radius * 10), n, at, 20));
        return n;
    }

    /** Borax reaches everything sensitive in {@code area} that passes {@code filter}. */
    public static int touch(ServerLevel level, AABB area, Predicate<Entity> filter, @Nullable Entity owner) {
        int n = 0;
        for (Entity e : level.getEntities((Entity) null, area, e -> e instanceof BoraxSensitive && e.isAlive() && filter.test(e))) {
            ((BoraxSensitive) e).onBorax(level, owner);
            n++;
        }
        return n;
    }

    /**
     * The common reaction of a Leviathan's flesh: it burns ({@link LeviathanRules#BORAX_BURN_TICKS}), takes
     * {@link LeviathanRules#BORAX_DAMAGE} of borax harm, and fizzes.
     */
    public static void burn(ServerLevel level, LivingEntity target, @Nullable Entity owner, float damage) {
        target.addEffect(new MobEffectInstance(AllMobEffects.BORAX_BURN, LeviathanRules.BORAX_BURN_TICKS, 0, false, true, true));
        if (damage > 0) target.hurt(AllDamageTypes.source(level, AllDamageTypes.BORAX, owner, owner), damage);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                12, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, target.blockPosition(), AllSounds.LEVIATHAN_BORAX_FIZZ.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    /** Whether {@code target} is burning with borax (it does not knit back). */
    public static boolean burning(LivingEntity target) {
        return target.hasEffect(AllMobEffects.BORAX_BURN);
    }
}
