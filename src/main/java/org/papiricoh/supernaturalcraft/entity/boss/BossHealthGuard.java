package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.balance.Balance;

/**
 * Health a boss loses without its own pipeline (another mod's {@code setHealth}, damage "by a share of
 * current health", a weapon that writes health directly) is turned into an ordinary capped blow (v0.15).
 *
 * <p>Each boss owns one guard: it calls {@link #accept} after every change its own code makes (after
 * {@code super.hurt}, heals, the {@code setHealth} of transitions) and {@link #tick} at the very start of its
 * server tick, <em>before</em> {@code super.tick()}, so health written to 0 is restored before vanilla starts
 * the death. Heals from outside are always accepted; only drops are guarded.
 */
public final class BossHealthGuard {

    private float expected = -1;

    /**
     * Sets {@code entity}'s health as something trusted does (a command, a test, a preview): a boss takes it as it is,
     * without the guard turning a drop into a capped blow.
     */
    public static void set(LivingEntity entity, float health) {
        entity.setHealth(health);
        if (entity instanceof CappedBoss boss) boss.acceptHealth();
    }

    /** The boss's own code has just set its health: remember it as legitimate. */
    public void accept(LivingEntity boss) {
        expected = boss.getHealth();
    }

    /**
     * Corrects a drop since the last {@link #accept}: the lost true health goes through the soft cap and the
     * phase floor, as if it had been one blow.
     *
     * @return the true health taken (0 if nothing was guarded), so the boss can react (phase change, death)
     */
    public float tick(LivingEntity boss, CappedBoss capped) {
        float h = boss.getHealth();
        if (expected < 0 || boss.isRemoved()) {
            expected = h;
            return 0;
        }
        if (h >= expected - 1e-4f) {
            expected = h;
            return 0;
        }
        float scale = capped.healthScale();
        float allowed = Balance.softCap((expected - h) * scale, capped.trueMaxHealth()) / scale;
        float target = Math.max(expected - allowed, capped.vanillaFloor());
        target = Math.min(target, expected);
        boss.setHealth(target);
        float taken = (expected - target) * scale;
        expected = boss.getHealth();
        return taken;
    }
}
