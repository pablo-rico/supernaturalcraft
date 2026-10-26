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
 * <p>v0.24: there is no per-hit cap. A boss multiplies a blow by its own factors ({@link #scale}: mundane damage,
 * vulnerability windows) and takes the rest; its health is sized for that ({@code ProgressionScale.trueHealth}). What
 * stops a one-shot is the <em>phase floor</em>: {@link #onFinalDamage} (lowest priority, after every other mod's and
 * every bonus's multipliers) never lets a blow take a boss past its current phase threshold ({@link #clampToFloor}).
 *
 * <p>Damage tagged {@code #supernaturalcraft:exact_boss_damage} (the Colt's rounds) is <em>exact</em>: it skips the
 * boss's multipliers and takes its own share ({@link #coltCap}: 10% by config, 3% of the Author). The config's
 * {@code balance.perHitCapFraction} (off by default) caps every blow for modpacks with outsized weapons. Only
 * {@link #passesThrough} damage (/kill and the void) skips all of this. A new boss implements {@link CappedBoss} and
 * tags itself into {@code #supernaturalcraft:bosses}.
 */
public final class BossDamage {

    private BossDamage() {
    }

    public static boolean isExact(DamageSource source) {
        return source.is(AllTags.DamageTypes.EXACT_BOSS_DAMAGE);
    }

    public static float scale(DamageSource source, float amount, float multiplier) {
        return scale(isExact(source), amount, multiplier);
    }

    public static float scale(boolean exact, float amount, float multiplier) {
        return exact ? amount : amount * multiplier;
    }

    /**
     * Damage that skips a boss's whole pipeline: only {@code /kill} and the void. Everything else flagged
     * {@code bypasses_invulnerability} by another mod is capped like any other blow.
     */
    public static boolean passesThrough(DamageSource source) {
        return source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    /** Default share of a great enemy's true max health one Colt round takes ({@code colt.bossShare}). */
    public static final float DEFAULT_COLT_SHARE = 0.10f;

    public static final float AUTHOR_COLT_SHARE = 0.03f;

    /** The Colt's own budget, independent of any optional modpack limit. */
    public static float coltCap(float trueMax, boolean author, float share, float authorShare) {
        return trueMax * (author ? authorShare : share);
    }

    public static float coltCap(float trueMax, boolean author) {
        return coltCap(trueMax, author, SNConfig.COLT_BOSS_HEALTH_SHARE.get().floatValue(), AUTHOR_COLT_SHARE);
    }

    /** Final bonuses may never skip a phase, including bonuses added by other mods. */
    public static void onFinalDamage(LivingDamageEvent.Pre event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof CappedBoss boss) || e.level().isClientSide || passesThrough(event.getSource())) return;
        float amount = event.getNewDamage();
        if (isExact(event.getSource())) amount = Math.min(amount, boss.exactCap() / boss.healthScale());
        amount = Balance.perHitLimit(amount * boss.healthScale(), boss.trueMaxHealth()) / boss.healthScale();
        float floor = boss.vanillaFloor();
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
