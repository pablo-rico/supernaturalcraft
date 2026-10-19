package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * What the client knows of each sigil bolt in flight (v0.21): its school and impact (from BOLT_TRAIL; until that arrives, the
 * bolt's own synced colour and no school), and the last few points it passed through, for the trail. Ticked by
 * {@link MagicFxManager}; drawn by {@link SigilBoltRenderer}.
 */
public final class BoltTrails {

    /** Points kept for the trail (one per tick). */
    static final int HISTORY = 12;

    static final class Info {
        School school = School.NONE;
        int impact = -1;
        int primary, accent, core;
        final double[] hx = new double[HISTORY], hy = new double[HISTORY], hz = new double[HISTORY];
        int count, head;
        int missing;
        /** The floor under the bolt (world y, NaN if none near), for the light it throws on the ground. */
        double floor = Double.NaN;

        void colours(School school, int fallback) {
            this.school = school;
            this.primary = school.isNone() ? fallback : FxColor.primary(school);
            this.accent = school.isNone() ? FxColor.mix(fallback, 0xFFFFFF, 0.4f) : FxColor.accent(school);
            this.core = FxColor.mix(primary, 0xFFFFFF, 0.7f);
        }

        void push(double x, double y, double z) {
            head = (head + 1) % HISTORY;
            hx[head] = x;
            hy[head] = y;
            hz[head] = z;
            if (count < HISTORY) count++;
        }

        /** The i-th newest point (0 = newest). */
        int index(int i) {
            return ((head - i) % HISTORY + HISTORY) % HISTORY;
        }
    }

    private static final Map<Integer, Info> BOLTS = new HashMap<>();

    private BoltTrails() {
    }

    /** BOLT_TRAIL: a bolt sets off. */
    static void start(MagicFxPayload p) {
        Info info = BOLTS.computeIfAbsent(p.entity(), id -> new Info());
        Entity e = MagicEffect.entity(p.entity());
        info.colours(p.school(), e instanceof SigilBolt b ? b.getColor() : p.school().primary());
        info.impact = p.argIndex();
        if (info.count == 0) info.push(p.pos().x, p.pos().y, p.pos().z);
    }

    /** The bolt's record (made from its synced colour if no BOLT_TRAIL came). */
    static Info info(SigilBolt bolt) {
        Info info = BOLTS.get(bolt.getId());
        if (info == null) {
            info = new Info();
            info.colours(School.NONE, bolt.getColor());
            info.push(bolt.getX(), bolt.getY(), bolt.getZ());
            BOLTS.put(bolt.getId(), info);
        }
        return info;
    }

    static void tick(ClientLevel level) {
        if (BOLTS.isEmpty()) return;
        RandomSource r = level.random;
        for (Iterator<Map.Entry<Integer, Info>> it = BOLTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Info> en = it.next();
            Info info = en.getValue();
            Entity e = level.getEntity(en.getKey());
            if (e == null || e.isRemoved()) {
                // A payload can come a tick before its entity: give it a moment before forgetting it.
                if (++info.missing > 10) it.remove();
                continue;
            }
            info.missing = 0;
            info.push(e.getX(), e.getY(), e.getZ());
            info.floor = FxWorld.floorOrNaN(level, e.position(), 5);
            trail(level, r, info, e);
        }
    }

    /** The bolt sheds its school's matter as it flies, strewn along the stretch it covered this tick. */
    private static void trail(ClientLevel level, RandomSource r, Info info, Entity e) {
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double bx = x - e.xo, by = y - e.yo, bz = z - e.zo;
        int n = FxParticles.rate(r, 3.2f);
        for (int i = 0; i < n; i++) {
            double u = r.nextDouble();
            FxParticles.spark(level, r.nextBoolean() ? info.core : info.primary, 0.8f + r.nextFloat() * 0.5f, 0,
                    x - bx * u + FxParticles.j(r) * 0.14, y - by * u + FxParticles.j(r) * 0.14, z - bz * u + FxParticles.j(r) * 0.14,
                    FxParticles.j(r) * 0.03, FxParticles.j(r) * 0.03, FxParticles.j(r) * 0.03);
        }
        if (r.nextInt(2) == 0 && FxParticles.n(1) > 0) {
            FxParticles.glyph(level, info.accent, 0.75f, 18, x, y, z, FxParticles.j(r) * 0.015, 0.025, FxParticles.j(r) * 0.015);
        }
        for (int k = FxParticles.rate(r, 1.6f); k > 0; k--) {
            double u = r.nextDouble();
            schoolMatter(level, r, info, x - bx * u, y - by * u, z - bz * u);
        }
    }

    private static void schoolMatter(ClientLevel level, RandomSource r, Info info, double x, double y, double z) {
        switch (info.school) {
            case INFERNAL -> FxParticles.ember(level, r.nextBoolean() ? info.accent : info.primary, 1.1f, 0, x, y, z,
                    FxParticles.j(r) * 0.04, 0.05, FxParticles.j(r) * 0.04);
            case WITCHCRAFT -> FxParticles.mote(level, info.accent, 1.1f, 0, x, y, z, FxParticles.j(r) * 0.03, 0.015, FxParticles.j(r) * 0.03);
            case HUNTER -> FxParticles.salt(level, 0xF4F4F0, 1f, 0, x, y, z, FxParticles.j(r) * 0.04, 0.02, FxParticles.j(r) * 0.04);
            case ENOCHIAN -> FxParticles.spark(level, 0xFFFFFF, 1.3f, 10, x, y, z, FxParticles.j(r) * 0.01, 0.015, FxParticles.j(r) * 0.01);
            default -> {
            }
        }
    }

    static void clear() {
        BOLTS.clear();
    }

    /** Whether any bolt is known (to draw). */
    static boolean any() {
        return !BOLTS.isEmpty();
    }

    /** Every known bolt still in the world, with its record. */
    static void forEach(ClientLevel level, java.util.function.BiConsumer<SigilBolt, Info> action) {
        for (Map.Entry<Integer, Info> en : BOLTS.entrySet()) {
            if (level.getEntity(en.getKey()) instanceof SigilBolt bolt && !bolt.isRemoved()) action.accept(bolt, en.getValue());
        }
    }
}
