package org.papiricoh.supernaturalcraft.hex;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Extension points of the witchcraft (v0.21). {@link #breakHex} is what the {@code break_hex} spell calls on whoever it frees:
 * the curses it knows are lifted by the spell itself; later systems (P3's hexed victims, the coven witches' shields) add their
 * own with {@link #onBreakHex}. {@link #saltStruck} is what the {@code salt_shot} sigil calls on whatever it hits (P3: salt frees
 * a hexed villager and cracks a coven witch's shield).
 */
public final class HexHooks {

    /** Something a breaking (by spell or by salt) may undo. */
    @FunctionalInterface
    public interface Breaker {
        /** @param by who broke it, if anyone is known @return whether it freed {@code entity} of anything */
        boolean free(LivingEntity entity, @Nullable LivingEntity by);
    }

    private static final List<Breaker> BREAKERS = new ArrayList<>();
    private static final List<Breaker> SALT = new ArrayList<>();

    private HexHooks() {
    }

    /** Adds something {@link #breakHex} also undoes; the predicate answers whether it freed {@code entity} of anything. */
    public static void onBreakHex(Predicate<LivingEntity> breaker) {
        BREAKERS.add((e, by) -> breaker.test(e));
    }

    /** As {@link #onBreakHex(Predicate)}, told who broke the hex. */
    public static void onBreakHex(Breaker breaker) {
        BREAKERS.add(breaker);
    }

    /** Adds something a salt round undoes ({@link #saltStruck}). */
    public static void onSalt(Breaker breaker) {
        SALT.add(breaker);
    }

    /** Lets every registered breaker free {@code entity}. @return whether any did */
    public static boolean breakHex(LivingEntity entity) {
        return breakHex(entity, null);
    }

    /** As {@link #breakHex(LivingEntity)}, broken by {@code by}. */
    public static boolean breakHex(LivingEntity entity, @Nullable LivingEntity by) {
        boolean any = false;
        for (Breaker b : List.copyOf(BREAKERS)) any |= b.free(entity, by);
        return any;
    }

    /** A salt round from {@code by} struck {@code entity}. @return whether that undid anything (the sigil then counts as a hit) */
    public static boolean saltStruck(LivingEntity entity, @Nullable LivingEntity by) {
        boolean any = false;
        for (Breaker b : List.copyOf(SALT)) any |= b.free(entity, by);
        return any;
    }
}
