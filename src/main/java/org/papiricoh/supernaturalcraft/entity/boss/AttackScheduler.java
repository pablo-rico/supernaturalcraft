package org.papiricoh.supernaturalcraft.entity.boss;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Runs one attack at a time and picks the next by weight, never repeating the last one. The
 * host supplies the pool (which changes with the phase), the gap between attacks and how
 * much to shorten wind-ups when enraged.
 */
public class AttackScheduler<E extends Mob> {

    public enum Stage { IDLE, WINDUP, ACTIVE, RECOVER }

    public interface Host<E extends Mob> {
        List<Option<E>> attackPool();

        @Nullable LivingEntity attackTarget();

        int attackGap();

        float windupScale();

        void onStage(Stage stage, @Nullable BossAttack<E> attack);

        /** An attack that must be used now regardless of the pool (gap closers), or null. */
        default @Nullable Supplier<BossAttack<E>> forcedAttack(LivingEntity target) {
            return null;
        }
    }

    public record Option<E extends Mob>(Supplier<BossAttack<E>> factory, float baseWeight) {
    }

    private final E boss;
    private final Host<E> host;
    private @Nullable BossAttack<E> current;
    private @Nullable LivingEntity target;
    private Stage stage = Stage.IDLE;
    private int t, stageLength, gap = 40;
    private String lastId = "";

    public AttackScheduler(E boss, Host<E> host) {
        this.boss = boss;
        this.host = host;
    }

    public Stage stage() {
        return stage;
    }

    public @Nullable BossAttack<E> current() {
        return current;
    }

    public void delay(int ticks) {
        gap = Math.max(gap, ticks);
    }

    public void tick() {
        if (stage == Stage.IDLE) {
            if (--gap > 0) return;
            LivingEntity tgt = host.attackTarget();
            if (tgt == null) return;
            Supplier<BossAttack<E>> forced = host.forcedAttack(tgt);
            BossAttack<E> next = forced != null ? forced.get() : choose(tgt, boss.getRandom());
            if (next == null) {
                gap = 10;
                return;
            }
            start(next, tgt);
            return;
        }
        if (target == null || !target.isAlive()) {
            LivingEntity replacement = host.attackTarget();
            if (replacement == null) {
                cancel();
                return;
            }
            target = replacement;
        }
        t++;
        switch (stage) {
            case WINDUP -> {
                current.tickWindup(boss, target, t);
                if (t >= stageLength) enter(Stage.ACTIVE, current.active);
            }
            case ACTIVE -> {
                current.tickActive(boss, target, t);
                if (t >= stageLength || current.endEarly(boss, target)) enter(Stage.RECOVER, current.recover);
            }
            case RECOVER -> {
                if (t >= stageLength) finish();
            }
            default -> {
            }
        }
    }

    private @Nullable BossAttack<E> choose(LivingEntity tgt, RandomSource random) {
        List<BossAttack<E>> candidates = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        float total = 0;
        for (Option<E> o : host.attackPool()) {
            BossAttack<E> a = o.factory().get();
            if (a.id.equals(lastId)) continue;
            float w = o.baseWeight() * a.weight(boss, tgt);
            if (w <= 0) continue;
            candidates.add(a);
            weights.add(w);
            total += w;
        }
        if (candidates.isEmpty()) return null;
        float roll = random.nextFloat() * total;
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll <= 0) return candidates.get(i);
        }
        return candidates.getLast();
    }

    private void start(BossAttack<E> attack, LivingEntity tgt) {
        current = attack;
        target = tgt;
        lastId = attack.id;
        enter(Stage.WINDUP, Math.max(1, Math.round(attack.windup * host.windupScale())));
        attack.onWindup(boss, tgt);
    }

    private void enter(Stage next, int length) {
        stage = next;
        t = 0;
        stageLength = length;
        host.onStage(next, current);
        if (next == Stage.ACTIVE) current.onActive(boss, target);
    }

    private void finish() {
        if (current != null) current.onEnd(boss);
        current = null;
        target = null;
        stage = Stage.IDLE;
        gap = host.attackGap();
        host.onStage(Stage.IDLE, null);
    }

    /** Abandons the current attack (phase change, death). */
    public void cancel() {
        if (current != null) current.onEnd(boss);
        current = null;
        target = null;
        stage = Stage.IDLE;
        gap = host.attackGap();
        host.onStage(Stage.IDLE, null);
    }

    /** Lengthens the punish window (Bind). */
    public void extendRecover(int ticks) {
        if (stage == Stage.RECOVER) stageLength += ticks;
    }
}
