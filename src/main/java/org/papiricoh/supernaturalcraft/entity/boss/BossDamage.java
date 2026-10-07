package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.entity.PartEntity;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The rules every boss shares for how much of a blow it takes.
 *
 * <p>Damage tagged {@code #supernaturalcraft:exact_boss_damage} (the Colt's rounds) is
 * <em>exact</em>: it skips a boss's multipliers and its per-hit cap. Phase floors, invulnerable
 * phases, shells and deflections still apply, so no single shot ever skips a phase. A new boss
 * runs its incoming damage through {@link #scaleAndCap} and its floor through
 * {@link #clampToFloor}, and tags itself into {@code #supernaturalcraft:bosses}.
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
