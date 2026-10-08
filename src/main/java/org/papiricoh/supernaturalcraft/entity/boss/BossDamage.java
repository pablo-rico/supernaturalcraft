package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.balance.Balance;
import net.neoforged.neoforge.entity.PartEntity;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The rules every boss shares for how much of a blow it takes.
 *
 * <p>Damage tagged {@code #supernaturalcraft:exact_boss_damage} (the Colt's rounds) is
 * <em>exact</em>: it skips a boss's multipliers and its soft cap, up to its own cap ({@link #coltCap}).
 * Phase floors, invulnerable phases, shells and deflections still apply, so no single shot ever skips a
 * phase. A new boss implements {@link CappedBoss}, runs its incoming damage through {@link #softCap} and
 * its floor through {@link #clampToFloor}, and tags itself into {@code #supernaturalcraft:bosses}.
 * ({@link #scaleAndCap} is the pre-v0.15 flat cap, kept for its rule test.)
 *
 * <p>v0.15: the per-hit cap is a <em>soft cap relative to the boss's true max health</em>
 * ({@link #softCap}, from {@code ProgressionScale}); only {@link #passesThrough} damage (/kill and the void)
 * skips the pipeline, and {@link #onFinalDamage} re-applies the hard cap and the phase floor to the final
 * number, after every other mod's and every bonus's multipliers. A boss implements {@link CappedBoss} for that.
 */
public final class BossDamage {

    private BossDamage() {
    }

    public static boolean isExact(DamageSource source) {
        return source.is(AllTags.DamageTypes.EXACT_BOSS_DAMAGE);
    }

    /** A boss's multiplier and per-hit cap, unless the damage is exact. */
    public static float scaleAndCap(DamageSource source, float amount, float multiplier, float cap) {
        return scaleAndCap(isExact(source), amount, multiplier, cap);
    }

    public static float scaleAndCap(boolean exact, float amount, float multiplier, float cap) {
        return exact ? amount : Math.min(amount * multiplier, cap);
    }

    /**
     * Damage that skips a boss's whole pipeline: only {@code /kill} and the void. Everything else flagged
     * {@code bypasses_invulnerability} by another mod is capped like any other blow.
     */
    public static boolean passesThrough(DamageSource source) {
        return source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    /** Default share of a great enemy's true max health one Colt round takes ({@code colt.bossHealthShare}). */
    public static final float DEFAULT_COLT_SHARE = 0.05f;

    /**
     * The most one exact blow (a Colt round) may take from a great enemy with {@code trueMax} true max health (pure):
     * {@code share} of it, but only the hard cap ({@code hardFraction}) from the Author.
     */
    public static float coltCap(float trueMax, boolean author, float share, float hardFraction) {
        return author ? trueMax * hardFraction : trueMax * share;
    }

    /** {@link #coltCap(float, boolean, float, float)} as the server's config tunes it. */
    public static float coltCap(float trueMax, boolean author) {
        return author ? Balance.hardCap(trueMax) : trueMax * SNConfig.COLT_BOSS_HEALTH_SHARE.get().floatValue();
    }

    /**
     * A blow of {@code amount} after the boss's multiplier and its soft cap, in true health. Exact damage
     * (the Colt) skips the multiplier and the soft cap, up to its own cap ({@link #coltCap}, not the Author).
     */
    public static float softCap(DamageSource source, float amount, float multiplier, float trueMaxHealth) {
        if (isExact(source)) return Math.min(amount, coltCap(trueMaxHealth, false));
        return Balance.softCap(amount * multiplier, trueMaxHealth);
    }

    /**
     * Last word on a blow to a {@link CappedBoss} ({@code LivingDamageEvent.Pre} at the lowest priority):
     * whatever multiplied it after the boss's own pipeline, it takes no more than the hard cap (a Colt round, its own
     * {@link CappedBoss#exactCap}), and never passes the current phase floor.
     */
    public static void onFinalDamage(LivingDamageEvent.Pre event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof CappedBoss boss) || e.level().isClientSide || passesThrough(event.getSource())) return;
        float cap = isExact(event.getSource()) ? boss.exactCap() : Balance.hardCap(boss.trueMaxHealth());
        float max = cap / boss.healthScale();
        float floor = boss.vanillaFloor();
        float amount = Math.min(event.getNewDamage(), max);
        amount = clampToFloor(e.getHealth(), amount, floor);
        if (amount != event.getNewDamage()) event.setNewDamage(amount);
    }

    /** What is left of a blow once it may not take health below {@code floor}. */
    public static float clampToFloor(float health, float amount, float floor) {
        return Math.max(0, Math.min(amount, health - floor));
    }

    /** The boss behind a part, or the entity itself. */
    public static Entity root(Entity e) {
        return e instanceof PartEntity<?> part ? part.getParent() : e;
    }

    public static boolean isBoss(Entity e) {
        return root(e).getType().is(AllTags.Entities.BOSSES);
    }
}
