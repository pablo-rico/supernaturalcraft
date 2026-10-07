package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * One boss attack, run as WINDUP (telegraphed, nothing hurts yet) → ACTIVE (it lands) →
 * RECOVER (the boss is open: the punish window). A fresh instance is created per use, so
 * attacks may keep state (target spots, timers) in fields.
 */
public abstract class BossAttack<E extends Mob> {

    public final String id;
    public final String animation;
    public final int windup, active, recover;

    protected BossAttack(String id, String animation, int windup, int active, int recover) {
        this.id = id;
        this.animation = animation;
        this.windup = windup;
        this.active = active;
        this.recover = recover;
    }

    /** Relative chance of being picked against this target. Zero rules it out. */
    public float weight(E boss, LivingEntity target) {
        return 1f;
    }

    /** Whether the boss walks or flies on its own during this attack. Most attacks root it. */
    public boolean movesBoss() {
        return false;
    }

    public void onWindup(E boss, LivingEntity target) {
    }

    public void tickWindup(E boss, LivingEntity target, int t) {
    }

    public void onActive(E boss, LivingEntity target) {
    }

    public void tickActive(E boss, LivingEntity target, int t) {
    }

    /** Lets an attack finish its active stage early (a broken tether, a dead target). */
    public boolean endEarly(E boss, LivingEntity target) {
        return false;
    }

    /** Always called once, however the attack ends: cleanup goes here. */
    public void onEnd(E boss) {
    }
}
