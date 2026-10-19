package org.papiricoh.supernaturalcraft.client.witchboss;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicSounds;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicTextures;
import org.papiricoh.supernaturalcraft.client.witch.WitchBones;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchBalance;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.papiricoh.supernaturalcraft.client.witchboss.WitchFxKit.*;

/**
 * The Grand Witch's fight in the world (v0.21, P6): each {@code GrandWitchFxPayload} moment over its telegraph, and her coven's
 * channel and shield read off the synced flags ({@link ClientGrandWitchFx}).
 * <ul>
 *   <li>{@link Coven}: a beam from each channelling Coven Witch's raised hands to the staff's crystal (plum round a silver core,
 *   two strands twisting round it, the power flowing in) and, while she is shielded, a layered dome round her (a plum glow, a
 *   lattice of silver hexagons shimmering, a ring of glyphs at its foot); a beam snaps back when its witch is interrupted, the
 *   dome shatters into hexagonal shards when the last one stops;</li>
 *   <li>{@link FrostHex}: her hex frozen into the floor under a hunter, a crown of icicles round it and rime spreading;</li>
 *   <li>{@link Inferno}: a wall of fire rising round the red circle, the floor inside it burning;</li>
 *   <li>{@link Crush}: violet rings pressing down and in on the one she crushes, the floor cracking under them;</li>
 *   <li>{@link Taglock}: a red thread from her crystal to every hunter, a lock of their hair burning red over each head, tightening
 *   until the curse lands — as {@link MassCurse} magenta thorns, or, inside one of Rowena's circles, a green flare;</li>
 *   <li>{@link SafeCircle}: Rowena's green circles written into the floor, a low curtain of green light round each;</li>
 *   <li>{@link AllyArrives}: Rowena out of her yellow smoke, her sigil in gold under her feet;</li>
 *   <li>{@link StaffHex}: the Coven Staff's hex, a plum lash from the crystal to its mark.</li>
 * </ul>
 */
public final class GrandWitchWorldFx {

    static final int PLUM = 0x8A3AA8, PLUM_DEEP = 0x4A1660, SILVER = 0xE8E6FF, SILVER_DIM = 0xA8A4C8, CRYSTAL = 0xC8A8FF;

    private static final Map<Integer, Coven> COVENS = new HashMap<>();

    private GrandWitchWorldFx() {
    }

    static void clear() {
        COVENS.clear();
    }

    /** The coven's channel and shield for the Grand Witch {@code w} (started once; it ends with her). */
    static Coven coven(GrandWitchEntity w) {
        Coven c = COVENS.get(w.getId());
        if (c != null && !c.isDone()) return c;
        c = new Coven(w.getId(), w.position());
        COVENS.put(w.getId(), c);
        MagicFxManager.add(c);
        return c;
    }

    // --- starting them --------------------------------------------------------------------------------------------------

    /** A channel begins ({@code on}) or is broken. */
    public static void channel(int witch, int grandWitch, boolean on, Vec3 at) {
        Entity g = entity(grandWitch);
        if (g instanceof GrandWitchEntity w) {
            Coven c = coven(w);
            if (on) c.kindle(witch);
            else c.snap(witch, at);
        }
    }

    public static void shield(int grandWitch, boolean raised, Vec3 at) {
        Entity g = entity(grandWitch);
        if (g instanceof GrandWitchEntity w) {
            Coven c = coven(w);
            if (raised) c.raise();
            else c.shatter();
        }
    }

    public static void frost(Vec3 centre, float radius, int ticks) {
        MagicFxManager.add(new FrostHex(centre, Math.max(1, radius), ticks));
    }

    public static void inferno(int witch, Vec3 centre, float radius, int ticks) {
        MagicFxManager.add(new Inferno(witch, centre, Math.max(1, radius), ticks));
    }

    public static void crush(int witch, int target, Vec3 centre, int ticks) {
        MagicFxManager.add(new Crush(witch, target, centre, ticks));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == target) {
            MagicOverlay.flash(centre, VIOLET, 0.35f, 6);
            MagicShake.kick(centre, 0.7f, 10, 6);
        }
    }

    public static void taglock(int witch, int hunter, float radius, int ticks) {
        MagicFxManager.add(new Taglock(witch, hunter, radius, ticks));
    }

    public static void safe(int rowena, Vec3 at, float radius, int ticks) {
        MagicFxManager.add(new SafeCircle(rowena, at, Math.max(1, radius), ticks));
    }

    public static void massCurse(int hunter, boolean cursed, Vec3 at) {
        MagicFxManager.add(new MassCurse(hunter, cursed, at));
        Minecraft mc = Minecraft.getInstance();
        if (cursed && mc.player != null && mc.player.getId() == hunter) {
            MagicOverlay.flash(at, MAGENTA, 0.5f, 6);
            MagicShake.kick(at, 0.5f, 8, 6);
        }
    }

    public static void veil(Vec3 at, int ticks) {
        gather(at, ticks, GrandWitchBalance.VEIL_RADIUS);
    }

    public static void ally(int rowena, boolean arrives, Vec3 at, int ticks) {
        MagicFxManager.add(new AllyArrives(rowena, arrives, at));
    }

    public static void staff(int wielder, int foe, Vec3 at, int ticks) {
        MagicFxManager.add(new StaffHex(wielder, foe, at, ticks));
    }

    // --- the coven's channel and her shield -----------------------------------------------------------------------------

    /** Where a channeller's raised hands are. */
    static Vec3 hands(Entity w, float partial) {
        Vec3 r = WitchBones.bone(w, WitchBones.Bone.RIGHT_HAND, partial), l = WitchBones.bone(w, WitchBones.Bone.LEFT_HAND, partial);
        if (r != null && l != null) return r.add(l).scale(0.5).add(0, 0.12, 0);
        return w.getPosition(partial).add(0, w.getBbHeight() + 0.1, 0);
    }

    /** Where the Grand Witch's crystal is. */
    static Vec3 crystal(Entity w, float partial) {
        return WitchBones.boneOr(w, WitchBones.Bone.CRYSTAL, partial, w.getBbHeight() * 1.05, w.position());
    }

    /**
     * Her coven's channel and her shield, for as long as she lives: drawn from the flags (who channels, whether she is shielded);
     * the payloads add the moments (a beam kindled, a beam snapped, the dome raised, the dome shattered).
     */
    static final class Coven extends MagicEffect {
        private final int witch;
        private Vec3 pos;
        /** Channelers seen, with the client tick their beam was kindled (or -1 if not lately). */
        private final Map<Integer, Integer> kindled = new HashMap<>();
        private final Map<Integer, Integer> snapped = new HashMap<>();
        private final Map<Integer, Vec3> snappedAt = new HashMap<>();
        private final Set<Integer> channeling = new HashSet<>();
        private float shield, shieldO;
        private int raisedAt = -100, shatteredAt = -100, gone = -1;
        private Vec3 shatterPos;

        Coven(int witch, Vec3 at) {
            super(at, School.WITCHCRAFT, 20 * 60 * 30);
            this.witch = witch;
            this.pos = at;
        }

        boolean isDone() {
            return done();
        }

        @Override
        protected boolean done() {
            return super.done() || gone >= 0 && age - gone > 30;
        }

        @Override
        protected Vec3 anchor() {
            return pos;
        }

        @Override
        protected double reach() {
            return 14;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        void kindle(int id) {
            kindled.put(id, age);
            snapped.remove(id);
        }

        void snap(int id, Vec3 at) {
            if (snapped.containsKey(id) || !channeling.contains(id) && !kindled.containsKey(id)) return;
            snapped.put(id, age);
            snappedAt.put(id, at);
            channeling.remove(id);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                RandomSource r = mc.level.random;
                Entity w = entity(id);
                Vec3 h = w != null ? hands(w, 1f) : at.add(0, 2, 0);
                MagicSounds.play(SoundEvents.AMETHYST_BLOCK_BREAK, h, 1f, 1.4f);
                for (int i = FxParticles.n(16); i > 0; i--) {
                    FxParticles.spark(mc.level, i % 2 == 0 ? SILVER : PLUM, 0.5f, 14, h.x, h.y, h.z, FxParticles.j(r) * 0.2, 0.1 + r.nextDouble() * 0.15,
                            FxParticles.j(r) * 0.2);
                }
            }
        }

        void raise() {
            raisedAt = age;
        }

        void shatter() {
            if (shield < 0.2f && age - raisedAt > 30) return;
            shatteredAt = age;
            Entity e = entity(witch);
            shatterPos = e != null ? e.position() : pos;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                RandomSource r = mc.level.random;
                float rad = domeRadius(e);
                Vec3 c = shatterPos.add(0, rad * 0.5, 0);
                MagicShake.kick(c, 0.5f, 8, 16);
                for (int i = FxParticles.n(40); i > 0; i--) {
                    double th = r.nextDouble() * Math.PI * 2, ph = r.nextDouble() * Math.PI / 2;
                    double nx = Math.cos(th) * Math.cos(ph), ny = Math.sin(ph), nz = Math.sin(th) * Math.cos(ph);
                    FxParticles.spark(mc.level, i % 3 == 0 ? CRYSTAL : SILVER, 0.6f, 20, c.x + nx * rad, c.y + ny * rad, c.z + nz * rad,
                            nx * 0.2, ny * 0.15 + 0.05, nz * 0.2);
                }
            }
        }

        private static float domeRadius(Entity e) {
            return Math.max(1.7f, (e != null ? e.getBbHeight() : 2.3f) * 0.82f);
        }

        @Override
        protected void tick(ClientLevel level) {
            Entity e = entity(witch);
            if (!(e instanceof GrandWitchEntity w) || w.isRemoved() || !w.isAlive()) {
                if (gone < 0) gone = age;
                shieldO = shield;
                shield = Math.max(0, shield - 0.1f);
                return;
            }
            pos = w.position();
            shieldO = shield;
            boolean up = w.shielded();
            if (up && shield == 0 && age - raisedAt > 20) raisedAt = age;
            shield = Mth.clamp(shield + (up ? 0.06f : -0.25f), 0, 1);
            // Who channels now, from their flags.
            Set<Integer> now = new HashSet<>();
            for (CovenWitchEntity c : level.getEntitiesOfClass(CovenWitchEntity.class, new AABB(pos, pos).inflate(24, 8, 24))) {
                if (c.isAlive() && c.channeling()) now.add(c.getId());
            }
            for (int id : now) if (!channeling.contains(id) && !kindled.containsKey(id)) kindled.put(id, age);
            for (int id : List.copyOf(channeling)) {
                if (!now.contains(id) && !snapped.containsKey(id)) snap(id, entity(id) != null ? entity(id).position() : pos);
            }
            channeling.clear();
            channeling.addAll(now);
            kindled.values().removeIf(t -> age - t > 40);
            snapped.values().removeIf(t -> age - t > 16);
            RandomSource r = level.random;
            // The power flowing in along each beam.
            for (int id : channeling) {
                Entity c = entity(id);
                if (c == null || r.nextFloat() > 0.5f) continue;
                Vec3 a = hands(c, 1f), b = crystal(w, 1f);
                Vec3 p = a.lerp(b, r.nextDouble());
                FxParticles.mote(level, r.nextBoolean() ? CRYSTAL : SILVER, 0.4f, 12, p.x, p.y, p.z, (b.x - a.x) * 0.02, (b.y - a.y) * 0.02,
                        (b.z - a.z) * 0.02);
            }
            if (shield > 0.5f && r.nextFloat() < 0.25f) {
                float rad = domeRadius(w);
                double th = r.nextDouble() * Math.PI * 2;
                FxParticles.mote(level, SILVER, 0.35f, 20, pos.x + Math.cos(th) * rad, pos.y + 0.1, pos.z + Math.sin(th) * rad, 0, 0.04, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Entity e = entity(witch);
            Vec3 p = e != null ? e.getPosition(d.partial) : pos;
            Vec3 crys = e != null ? crystal(e, d.partial) : p.add(0, 2.4, 0);
            float rad = domeRadius(e);
            // The beams.
            for (int id : channeling) {
                Entity c = entity(id);
                if (c == null) continue;
                Integer k0 = kindled.get(id);
                float grow = k0 == null ? 1 : easeOut((age - k0 + d.partial) / 8f);
                beam(d, hands(c, d.partial), crys, grow, t, id, 1f, rad, p);
            }
            for (Map.Entry<Integer, Integer> s : snapped.entrySet()) {
                Entity c = entity(s.getKey());
                Vec3 from = c != null ? hands(c, d.partial) : snappedAt.getOrDefault(s.getKey(), p).add(0, 2, 0);
                float k = (age - s.getValue() + d.partial) / 16f;
                if (k >= 1) continue;
                // Snapped: the beam whips back toward its witch and frays.
                Vec3 end = crys.lerp(from, easeOut(k));
                d.lightning(from.x, from.y, from.z, end.x, end.y, end.z, s.getKey() * 31L + (long) (t / 2), 6, 0.35f, 0.05f, SILVER, 0.8f * (1 - k), false);
                d.star(from.x, from.y, from.z, 1.2f * (1 - k) + 0.2f, 0.3f, CRYSTAL, 1 - k);
            }
            // The dome.
            float s = Mth.lerp(d.partial, shieldO, shield);
            if (s > 0.01f) dome(d, p, rad, s, t, Mth.clamp((age - raisedAt + d.partial) / 18f, 0, 1));
            if (shatterPos != null) {
                float k = (age - shatteredAt + d.partial) / 24f;
                if (k < 1) shards(d, shatterPos, rad, k);
            }
            if (!channeling.isEmpty() && s > 0.01f) d.glowDisc(crys.x, crys.y, crys.z, 0.6f + 0.15f * channeling.size(), CRYSTAL, 0.5f * s);
        }

        /** One channel: plum round a silver core, two strands twisting round it, the power pulsing toward her. */
        private void beam(FxDraw d, Vec3 a, Vec3 b, float grow, float t, int id, float alpha, float rad, Vec3 her) {
            Vec3 end = a.lerp(b, grow);
            d.softBeam(a.x, a.y, a.z, end.x, end.y, end.z, 0.42f, PLUM, 0.45f * alpha, 0.55f * alpha, false);
            d.softBeam(a.x, a.y, a.z, end.x, end.y, end.z, 0.12f, SILVER, 0.75f * alpha, 0.85f * alpha, true);
            Vec3 dir = end.subtract(a);
            double len = dir.length();
            if (len < 0.1) return;
            Vec3 n = dir.scale(1 / len), side = n.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
            side = side.normalize();
            Vec3 up = side.cross(n).normalize();
            int seg = Math.max(6, (int) (len * 3));
            for (int strand = 0; strand < 2; strand++) {
                Vec3 prev = null;
                for (int i = 0; i <= seg; i++) {
                    double u = i / (double) seg;
                    double ang = u * len * 2.2 - t * 0.35 + strand * Math.PI + id;
                    double rr = 0.16 * Math.sin(Math.PI * u) + 0.04;
                    Vec3 q = a.add(dir.scale(u)).add(side.scale(Math.cos(ang) * rr)).add(up.scale(Math.sin(ang) * rr));
                    if (prev != null) d.ribbon(prev.x, prev.y, prev.z, q.x, q.y, q.z, 0.035f, CRYSTAL, 0.7f * alpha, 0.7f * alpha);
                    prev = q;
                }
            }
            // Pulses running toward her, and a splash where the beam passes into the dome.
            for (int k = 0; k < 3; k++) {
                float u = ((t * 0.045f + k / 3f + id * 0.13f) % 1f) * grow;
                Vec3 q = a.add(dir.scale(u));
                d.glowDisc(q.x, q.y, q.z, 0.28f, SILVER, 0.6f * alpha);
            }
            d.glowDisc(a.x, a.y, a.z, 0.45f, CRYSTAL, 0.55f * alpha);
            // Where the beam passes into her dome (a sphere round her feet): a silver splash on its skin.
            if (grow > 0.9f && shield > 0.05f) {
                Vec3 f = a.subtract(her);
                double bq = 2 * f.dot(dir), cq = f.lengthSqr() - rad * rad, disc = bq * bq - 4 * dir.lengthSqr() * cq;
                if (cq > 0 && disc > 0) {
                    double s0 = (-bq - Math.sqrt(disc)) / (2 * dir.lengthSqr());
                    if (s0 > 0 && s0 < 1) {
                        Vec3 hit = a.add(dir.scale(s0));
                        d.glowDisc(hit.x, hit.y, hit.z, 0.4f, SILVER, 0.55f * alpha * shield * (0.7f + 0.3f * Mth.sin(t * 0.8f + id)));
                        d.star(hit.x, hit.y, hit.z, 0.5f, t * 0.1f + id, CRYSTAL, 0.4f * alpha * shield);
                    }
                }
            }
        }

        /**
         * The shield: a plum glow inside, a lattice of silver hexagons over a hemisphere (built up from the ground as it is raised),
         * shimmering in a wave that climbs it, a ring of glyphs at its foot.
         */
        private void dome(FxDraw d, Vec3 p, float rad, float s, float t, float built) {
            double gy = groundY(p);
            Vec3 c = new Vec3(p.x, gy, p.z);
            d.glowDisc(c.x, c.y + rad * 0.55, c.z, rad * 1.15f, PLUM, 0.22f * s);
            d.sphere(c.x, c.y + rad * 0.2, c.z, rad * 0.98f, PLUM_DEEP, 0.08f * s);
            d.flatGlow(c.x, gy + 0.03, c.z, rad * 1.1f, PLUM, 0.3f * s);
            d.glyphRing(MagicTextures.glyphStrip(), c.x, gy + 0.05, c.z, 0, 1, 0, rad - 0.1f, rad + 0.35f, t * 0.01f, SILVER, 0.75f * s);
            d.groundRing(c.x, gy + 0.04, c.z, rad - 0.08f, rad + 0.04f, SILVER, 0.8f * s, false);
            // The lattice.
            int rows = 5;
            for (int row = 0; row < rows; row++) {
                float el = (row + 0.5f) / rows * (Mth.PI / 2 - 0.12f);
                if (el / (Mth.PI / 2) > built + 0.05f) continue;
                int n = Math.max(3, Math.round(12 * Mth.cos(el)));
                float hexR = 0.5f * (Mth.PI / 2) / rows;
                for (int i = 0; i < n; i++) {
                    float az = (i + (row % 2) * 0.5f) / n * Mth.TWO_PI + t * 0.004f * (row % 2 == 0 ? 1 : -1);
                    float wave = 0.5f + 0.5f * Mth.sin(el * 5 - t * 0.12f + i * 0.3f);
                    float a = s * (0.35f + 0.45f * wave);
                    hexagon(d, c, rad, el, az, hexR * 0.92f, a, wave > 0.85f);
                }
            }
            // A cap at the top.
            d.ring(c.x, c.y + rad * 0.99, c.z, 0, 1, 0, 0.05f, 0.35f, SILVER, 0.5f * s * built, true);
        }

        /** One hexagon of the lattice, centred at elevation {@code el}, azimuth {@code az} on the dome. */
        private static void hexagon(FxDraw d, Vec3 c, float rad, float el, float az, float size, float a, boolean bright) {
            Vec3 n = new Vec3(Mth.cos(el) * Mth.cos(az), Mth.sin(el), Mth.cos(el) * Mth.sin(az));
            Vec3 e1 = new Vec3(-Mth.sin(az), 0, Mth.cos(az));
            Vec3 e2 = n.cross(e1).normalize().scale(-1);
            Vec3 prev = null, first = null;
            for (int k = 0; k <= 6; k++) {
                float ang = k * Mth.PI / 3;
                Vec3 dirv = n.add(e1.scale(Mth.cos(ang) * size)).add(e2.scale(Mth.sin(ang) * size)).normalize();
                Vec3 q = c.add(dirv.scale(rad));
                if (prev != null) d.ribbon(prev.x, prev.y, prev.z, q.x, q.y, q.z, bright ? 0.045f : 0.03f, bright ? 0xFFFFFF : SILVER, a, a);
                else first = q;
                prev = q;
            }
            if (bright) {
                Vec3 q = c.add(n.scale(rad));
                d.glowDisc(q.x, q.y, q.z, size * rad * 0.9f, CRYSTAL, a * 0.35f);
            }
        }

        /** The dome shattering: hexagonal shards thrown out and falling, glinting. */
        private void shards(FxDraw d, Vec3 at, float rad, float k) {
            double gy = groundY(at);
            Vec3 c = new Vec3(at.x, gy, at.z);
            long seed = seedOf(at);
            float f = 1 - k;
            for (int i = 0; i < 26; i++) {
                float az = hash(seed, i) * Mth.TWO_PI, el = hash(seed, i + 30) * 1.3f;
                Vec3 n = new Vec3(Mth.cos(el) * Mth.cos(az), Mth.sin(el), Mth.cos(el) * Mth.sin(az));
                float out = rad + k * (1.5f + 2.5f * hash(seed, i + 60));
                Vec3 q = c.add(n.scale(out)).add(0, -k * k * 2.5, 0);
                if (q.y < gy + 0.05) q = new Vec3(q.x, gy + 0.05, q.z);
                d.crystal(q.x, q.y, q.z, (float) n.x + hash(seed, i + 5) - 0.5f, (float) n.y, (float) n.z, 0.14f, 0.1f, SILVER, 0.9f * f, true);
            }
            d.glowDisc(c.x, c.y + rad * 0.5, c.z, rad * (1 + k), CRYSTAL, 0.4f * f);
            d.ring(c.x, c.y + 0.1, c.z, 0, 1, 0, rad + k * 3, rad + k * 3 + 0.3f, SILVER, 0.7f * f, true);
        }
    }

    // --- P2 · frost, fire, the fist -------------------------------------------------------------------------------------------

    /** Her hex frozen into the floor under a hunter: a crown of icicles round it, rime spreading, the hex's mark glowing in the ice. */
    static final class FrostHex extends MagicEffect {
        private static final int MELT = 14;
        private final float radius;
        private final int stay;
        private final double gy;
        private final long seed;

        FrostHex(Vec3 centre, float radius, int ticks) {
            super(centre, School.WITCHCRAFT, Math.max(16, ticks) + MELT);
            this.radius = radius;
            this.stay = Math.max(16, ticks);
            this.gy = groundY(centre);
            this.seed = seedOf(centre);
        }

        @Override
        protected double reach() {
            return radius + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                for (int i = FxParticles.n(24); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.plain(level, AllParticles.FROST.get(), at.x + Mth.cos(a) * radius, gy + 0.2, at.z + Mth.sin(a) * radius,
                            -Mth.cos(a) * 0.08, 0.1, -Mth.sin(a) * 0.08);
                }
            }
            if (age < stay && r.nextFloat() < 0.6f) {
                float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * radius;
                FxParticles.plain(level, ParticleTypes.SNOWFLAKE, at.x + Mth.cos(a) * k, gy + 0.2 + r.nextDouble() * 1.6, at.z + Mth.sin(a) * k, 0, -0.02, 0);
            }
            if (age == stay) MagicSounds.play(SoundEvents.GLASS_BREAK, at, 0.5f, 1.6f);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, MELT), melt = Mth.clamp((t - stay) / MELT, 0, 1), spread = easeOut(t / 7f);
            // The hex stamped into the ice: a seal glowing blue, rime spreading round it.
            d.decal(MagicTextures.seal((int) (seed & 7)), at.x, gy + 0.04, at.z, radius * 0.8f * spread, t * 0.005f, ICE, 0.8f * a);
            d.flatGlow(at.x, gy + 0.035, at.z, radius * 1.2f * spread, ICE, 0.45f * a);
            d.cracks(at.x, gy + 0.03, at.z, 10, radius * spread, 0.05f, seed, 0xFFFFFF, 0.6f * a);
            d.groundRing(at.x, gy + 0.05, at.z, radius - 0.1f, radius + 0.08f, ICE_DEEP, 0.85f * a, false);
            // A crown of icicles round the circle, leaning in over whoever stands in it.
            int n = 10 + Math.round(radius * 3);
            for (int i = 0; i < n; i++) {
                float ang = (i + hash(seed, i) * 0.4f) / n * Mth.TWO_PI;
                float grow = easeOutBack(Mth.clamp((t - hash(seed, i + 20) * 3) / 4f, 0, 1)) * (1 - melt * 0.8f);
                if (grow < 0.02f) continue;
                float len = (0.6f + 0.7f * hash(seed, i + 40)) * grow, rr = radius * (0.92f + 0.12f * hash(seed, i + 60));
                double x = at.x + Mth.cos(ang) * rr, z = at.z + Mth.sin(ang) * rr;
                float lx = -Mth.cos(ang) * 0.45f, lz = -Mth.sin(ang) * 0.45f;
                double cy = gy - 0.05 + len * 0.8;
                d.crystal(x + lx * len * 0.4, cy, z + lz * len * 0.4, lx, 1, lz, 0.1f + 0.05f * len, len, 0xE0F4FF, 0.85f * a, false);
                d.crystal(x + lx * len * 0.4, cy, z + lz * len * 0.4, lx, 1, lz, 0.06f, len * 0.9f, ICE, 0.45f * a, true);
            }
            if (t < 8) {
                float k = t / 8f;
                d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, radius * (1.3f - 0.5f * easeOut(k)), 0.4f, 0.9f * (1 - k), ICE, 0.8f * (1 - k));
            }
            d.glowDisc(at.x, gy + 0.7, at.z, radius * 0.8f, ICE, 0.15f * a * (1 - melt));
        }
    }

    /** A wall of fire rising round the red circle, the floor inside burning. */
    static final class Inferno extends MagicEffect {
        private final int witch;
        private final float radius;
        private final int burn;
        private final double gy;
        private final long seed;

        Inferno(int witch, Vec3 centre, float radius, int ticks) {
            super(centre, School.WITCHCRAFT, Math.max(16, ticks) + 22);
            this.witch = witch;
            this.radius = radius;
            this.burn = Math.max(16, ticks);
            this.gy = groundY(centre);
            this.seed = seedOf(centre);
        }

        @Override
        protected double reach() {
            return radius + 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) MagicShake.kick(at, 0.6f, 10, radius + 8);
            if (age < burn) {
                for (int i = FxParticles.rate(r, radius * 2.2f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.plain(level, ParticleTypes.FLAME, at.x + Mth.cos(a) * radius, gy + 0.2 + r.nextDouble() * 1.5, at.z + Mth.sin(a) * radius,
                            0, 0.06 + r.nextDouble() * 0.06, 0);
                }
                for (int i = FxParticles.rate(r, radius * 0.8f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * radius;
                    FxParticles.plain(level, ParticleTypes.SMALL_FLAME, at.x + Mth.cos(a) * k, gy + 0.1, at.z + Mth.sin(a) * k, 0, 0.03, 0);
                }
                if (r.nextFloat() < 0.8f) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.ember(level, FIRE_HOT, 0.6f, 24, at.x + Mth.cos(a) * radius, gy + 1 + r.nextDouble() * 2, at.z + Mth.sin(a) * radius,
                            0, 0.08, 0);
                }
            } else if (r.nextFloat() < 0.7f) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x + Mth.cos(a) * radius, gy + 0.4, at.z + Mth.sin(a) * radius, 0, 0.05, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 20), burning = 1 - Mth.clamp((t - burn) / 14f, 0, 1), rise = easeOutBack(Mth.clamp(t / 6f, 0, 1));
            // The staff's blast: a ring rushing out from her to the wall.
            if (t < 7) {
                float k = easeOut(t / 7f);
                d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, radius * k, 0.6f, 1.0f * (1 - k), FIRE_HOT, 0.9f * (1 - k));
            }
            d.flatGlow(at.x, gy + 0.04, at.z, radius * 1.1f, FIRE, 0.35f * a * burning);
            d.darkRing(at.x, gy + 0.025, at.z, radius * 0.3f, radius * 1.05f, 0x1A0804, 0.5f * Mth.clamp(t / 12f, 0, 1) * Mth.clamp((life - t) / 18f, 0, 1));
            // The wall: a glowing curtain at the rim with tongues of flame licking up it.
            float h = 3.0f * rise * burning;
            d.cylinder(at.x, gy, at.z, radius, h, FIRE, 0.45f * a, 0);
            d.cylinder(at.x, gy, at.z, radius - 0.15f, h * 0.6f, FIRE_HOT, 0.35f * a, 0);
            int n = Math.round(radius * 7);
            for (int i = 0; i < n; i++) {
                float ang = (i + hash(seed, i) * 0.6f) / n * Mth.TWO_PI;
                float lick = 0.55f + 0.45f * Mth.sin(t * (0.5f + hash(seed, i + 7)) + i * 1.9f);
                float hh = h * (0.6f + 0.6f * lick);
                if (hh < 0.05f) continue;
                double x = at.x + Mth.cos(ang) * radius, z = at.z + Mth.sin(ang) * radius;
                d.softBeam(x, gy, z, x + Mth.sin(t * 0.2f + i) * 0.1, gy + hh, z, 0.7f, FIRE, 0.6f * a, 0, false);
                if (i % 2 == 0) d.softBeam(x, gy, z, x, gy + hh * 0.6f, z, 0.25f, FIRE_HOT, 0.75f * a, 0, true);
            }
            d.glyphRing(MagicTextures.glyphStrip(), at.x, gy + 0.06, at.z, 0, 1, 0, radius - 0.6f, radius - 0.1f, -t * 0.03f, FIRE_HOT, 0.7f * a * burning);
            // Fire leaving the staff's crystal.
            Entity e = entity(witch);
            if (e != null && t < 12) {
                Vec3 c = crystal(e, d.partial);
                d.star(c.x, c.y, c.z, 1.6f * (1 - t / 12f) + 0.2f, 0.2f, FIRE_HOT, 1 - t / 12f);
            }
        }
    }

    /** Violet rings pressing down and in on the one she crushes; the floor cracks under them. */
    static final class Crush extends MagicEffect {
        private final int witch, target, held;
        private final double gy;
        private final long seed;

        Crush(int witch, int target, Vec3 centre, int ticks) {
            super(centre, School.WITCHCRAFT, Math.max(10, ticks) + 14);
            this.witch = witch;
            this.target = target;
            this.held = Math.max(10, ticks);
            this.gy = groundY(centre);
            this.seed = seedOf(centre);
        }

        @Override
        protected double reach() {
            return 5;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private Vec3 spot(float partial) {
            Entity e = entity(target);
            return e != null ? e.getPosition(partial) : at;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 p = spot(1f);
            if (age == 0) {
                MagicSounds.play(SoundEvents.ANVIL_LAND, p, 0.6f, 0.5f);
                for (int i = FxParticles.n(24); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double x = p.x + Mth.cos(a) * 0.6, z = p.z + Mth.sin(a) * 0.6;
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, x, gy, z)), x, gy + 0.05, z, Mth.cos(a) * 0.25,
                            0.08, Mth.sin(a) * 0.25);
                }
            }
            if (age < held && r.nextFloat() < 0.7f) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.mote(level, VIOLET, 0.45f, 12, p.x + Mth.cos(a) * 0.9, p.y + 2.4, p.z + Mth.sin(a) * 0.9, -Mth.cos(a) * 0.03, -0.12,
                        -Mth.sin(a) * 0.03);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 12), hold = t < held ? 1 : 1 - Mth.clamp((t - held) / 12f, 0, 1);
            Vec3 p = target >= 0 ? spot(d.partial) : at;
            float slam = easeOut(t / 4f);
            // The press: rings coming down from above and closing in, over and over.
            for (int i = 0; i < 4; i++) {
                float u = ((t * 0.07f + i / 4f) % 1f);
                double y = p.y + 2.8 - 2.6 * u;
                float r = 1.6f - 1.0f * u;
                d.ring(p.x, y, p.z, 0, 1, 0, r, r + 0.12f, VIOLET_HOT, 0.75f * a * hold * Mth.sin(Mth.PI * u), true);
            }
            d.cylinder(p.x, gy, p.z, 0.75f, 2.4f, VIOLET, 0.3f * a * hold, 0.05f * a * hold);
            // Downward chevrons over their head.
            org.joml.Vector3f l = d.camera.getLeftVector();
            for (int i = 0; i < 3; i++) {
                double y = p.y + 2.5 + i * 0.32 - ((t * 0.08f) % 0.32f);
                float ca = 0.65f * a * hold * (1 - i / 3f);
                d.ribbon(p.x + l.x * 0.35, y + 0.22, p.z + l.z * 0.35, p.x, y, p.z, 0.07f, VIOLET_HOT, ca, ca);
                d.ribbon(p.x - l.x * 0.35, y + 0.22, p.z - l.z * 0.35, p.x, y, p.z, 0.07f, VIOLET_HOT, ca, ca);
            }
            d.cracks(p.x, gy + 0.03, p.z, 9, 1.5f * slam, 0.07f, seed, VIOLET, 0.75f * a);
            d.flatGlow(p.x, gy + 0.04, p.z, 1.6f, VIOLET, 0.4f * a * hold);
            if (t < 6) d.shockwave(MagicTextures.ENERGY_RING, p.x, gy + 0.05, p.z, 0.5f + 1.8f * slam, 0.4f, 0.6f * (1 - t / 6f), VIOLET, 0.8f * (1 - t / 6f));
            // Her fist's grip: a violet thread from her clenched hand.
            Entity e = entity(witch);
            if (e != null && hold > 0) {
                Vec3 h = WitchBones.boneOr(e, WitchBones.Bone.LEFT_HAND, d.partial, 1.3, e.position().add(0, 1.3, 0));
                Vec3 to = p.add(0, 1.2, 0), mid = h.add(to).scale(0.5).add(0, 0.7, 0);
                thread(d, h, mid, to, 0, 1, 0.025f, VIOLET, VIOLET_HOT, 0.55f * a * hold, t, -0.05f);
            }
        }
    }

    // --- P3 · the mass taglock ------------------------------------------------------------------------------------------------

    /** A red thread from her crystal to a hunter and a lock of their hair burning over their head, tightening until it lands. */
    static final class Taglock extends MagicEffect {
        private final int witch, hunter, windup;
        private final float radius;

        Taglock(int witch, int hunter, float radius, int ticks) {
            super(Vec3.ZERO, School.WITCHCRAFT, Math.max(10, ticks) + 6);
            this.witch = witch;
            this.hunter = hunter;
            this.radius = radius;
            this.windup = Math.max(10, ticks);
        }

        @Override
        protected Vec3 anchor() {
            Entity h = entity(hunter);
            return h != null ? h.position() : at;
        }

        @Override
        protected double reach() {
            return 40;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        @Override
        protected void tick(ClientLevel level) {
            Entity h = entity(hunter);
            if (h == null) return;
            RandomSource r = level.random;
            float k = age / (float) windup;
            if (r.nextFloat() < 0.3f + 0.5f * k) {
                Vec3 top = h.position().add(0, h.getBbHeight() + 0.5, 0);
                FxParticles.ember(level, r.nextBoolean() ? RED : CRIMSON_HOT, 0.4f, 12, top.x + FxParticles.j(r) * 0.1, top.y, top.z + FxParticles.j(r) * 0.1,
                        0, 0.03, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), k = Mth.clamp(t / windup, 0, 1), a = fade(t, 4, 5);
            Entity h = entity(hunter), w = entity(witch);
            if (h == null) return;
            Vec3 feet = h.getPosition(d.partial), top = feet.add(0, h.getBbHeight() + 0.5, 0);
            float beat = 0.5f + 0.5f * Mth.sin(t * (0.25f + 0.9f * k));
            // The lock of hair: a little twisted strand over their head, burning red.
            for (int s = 0; s < 3; s++) {
                Vec3 prev = null;
                for (int i = 0; i <= 6; i++) {
                    float u = i / 6f, ang = u * 5 + s * 2.1f + t * 0.1f;
                    Vec3 q = top.add(Mth.cos(ang) * 0.06, 0.35 * u - 0.15, Mth.sin(ang) * 0.06);
                    if (prev != null) d.ribbon(prev.x, prev.y, prev.z, q.x, q.y, q.z, 0.03f, 0x5A2A1A, a, a);
                    prev = q;
                }
            }
            d.glowDisc(top.x, top.y + 0.05, top.z, 0.35f + 0.15f * beat, RED, (0.4f + 0.4f * k) * a * (0.6f + 0.4f * beat));
            // The thread from her crystal, drawn tighter (straighter, brighter) as the curse nears.
            if (w != null) {
                Vec3 c = crystal(w, d.partial);
                Vec3 mid = c.add(top).scale(0.5).add(0, 1.2 * (1 - k) + 0.1, 0);
                thread(d, c, mid, top, 0, Mth.clamp(t / 6f, 0, 1), 0.02f + 0.015f * k, RED, CRIMSON_HOT, (0.35f + 0.45f * k) * a, t, -0.04f - 0.06f * k);
            }
            // The ring at their feet, closing as it nears.
            float r = radius * (1.1f - 0.1f * k);
            d.groundRing(feet.x, feet.y + 0.06, feet.z, r - 0.06f, r + 0.06f, CRIMSON_HOT, 0.7f * a * beat, false);
        }
    }

    /** Rowena's green circle: written into the floor, a low curtain of green light round it, motes rising. */
    static final class SafeCircle extends MagicEffect {
        private final int rowena, hold;
        private final float radius;
        private final double gy;

        SafeCircle(int rowena, Vec3 at, float radius, int ticks) {
            super(at, School.WITCHCRAFT, Math.max(20, ticks) + 12);
            this.rowena = rowena;
            this.radius = radius;
            this.hold = Math.max(20, ticks);
            this.gy = groundY(at);
        }

        @Override
        protected double reach() {
            return radius + 12;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) MagicSounds.play(SoundEvents.AMETHYST_BLOCK_CHIME, at, 1.2f, 1.2f);
            if (age < hold && r.nextFloat() < 0.8f) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.mote(level, r.nextBoolean() ? SAFE : SAFE_HOT, 0.45f, 24, at.x + Mth.cos(a) * radius, gy + 0.1, at.z + Mth.sin(a) * radius,
                        0, 0.05, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 2, 12), write = Mth.clamp(t / 10f, 0, 1);
            float breathe = 0.8f + 0.2f * Mth.sin(t * 0.15f);
            d.wipe(MagicTextures.castCircle(), at.x, gy + 0.04, at.z, 0, 1, 0, radius * 0.95f, t * 0.01f, write, SAFE, 0.85f * a);
            d.glyphRing(MagicTextures.glyphStrip(), at.x, gy + 0.05, at.z, 0, 1, 0, radius - 0.05f, radius + 0.4f, -t * 0.015f, SAFE_HOT, 0.7f * a * write);
            d.flatGlow(at.x, gy + 0.03, at.z, radius * 1.05f, SAFE, 0.35f * a * breathe);
            d.cylinder(at.x, gy, at.z, radius + 0.15f, 0.9f, SAFE, 0.35f * a * write * breathe, 0);
            // Rowena's hands drawing it.
            Entity r = entity(rowena);
            if (r != null && t < 14) {
                Vec3 h = WitchBones.boneOr(r, WitchBones.Bone.RIGHT_HAND, d.partial, 1.2, r.position().add(0, 1.2, 0));
                float ang = write * Mth.TWO_PI;
                Vec3 pen = new Vec3(at.x + Mth.cos(ang) * radius, gy + 0.1, at.z + Mth.sin(ang) * radius);
                Vec3 mid = h.add(pen).scale(0.5).add(0, 0.6, 0);
                thread(d, h, mid, pen, 0, 1, 0.025f, SAFE, SAFE_HOT, 0.7f * (1 - t / 14f), t, 0.08f);
                d.glowDisc(pen.x, pen.y, pen.z, 0.4f, SAFE_HOT, 0.8f * (1 - t / 14f));
            }
        }
    }

    /** The curse landing (magenta thorns round them) or turned away by Rowena's circle (a green flare, the red thread snapping). */
    static final class MassCurse extends MagicEffect {
        private final int hunter;
        private final boolean cursed;
        private final long seed;

        MassCurse(int hunter, boolean cursed, Vec3 at) {
            super(at, School.WITCHCRAFT, 28);
            this.hunter = hunter;
            this.cursed = cursed;
            this.seed = seedOf(at);
        }

        @Override
        protected Vec3 anchor() {
            Entity h = entity(hunter);
            return h != null ? h.position() : at;
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age != 0) return;
            RandomSource r = level.random;
            Vec3 p = anchor();
            int a = cursed ? MAGENTA : SAFE, b = cursed ? MAGENTA_HOT : SAFE_HOT;
            for (int i = FxParticles.n(20); i > 0; i--) {
                float ang = r.nextFloat() * Mth.TWO_PI;
                FxParticles.spark(level, i % 2 == 0 ? a : b, 0.55f, 16, p.x + Mth.cos(ang) * 0.5, p.y + 0.2 + r.nextDouble() * 1.6, p.z + Mth.sin(ang) * 0.5,
                        Mth.cos(ang) * (cursed ? -0.05 : 0.15), 0.1, Mth.sin(ang) * (cursed ? -0.05 : 0.15));
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), f = Mth.clamp(1 - t / life, 0, 1), k = easeOutBack(Mth.clamp(t / 4f, 0, 1));
            Entity h = entity(hunter);
            Vec3 p = h != null ? h.getPosition(d.partial) : at;
            double gy = groundY(p);
            Minecraft mc = Minecraft.getInstance();
            boolean self = h != null && h == mc.player && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == h;
            if (cursed && self) {
                // On the local hunter, seen from their own eyes: only the thorny ring at their feet (the screen's flash says the rest).
                d.spikedRing(p.x, gy + 0.05, p.z, 0.9f, 0.4f, 10, t * 0.03f, MAGENTA, 0.6f * f);
            } else if (cursed) {
                // Thorns of magenta bursting up round them, leaning in.
                for (int i = 0; i < 10; i++) {
                    float ang = (i + hash(seed, i) * 0.5f) / 10 * Mth.TWO_PI, rr = 0.7f + 0.2f * hash(seed, i + 10);
                    float len = (0.7f + 0.6f * hash(seed, i + 20)) * k * (0.4f + 0.6f * f);
                    double x = p.x + Mth.cos(ang) * rr, z = p.z + Mth.sin(ang) * rr;
                    float lx = -Mth.cos(ang) * 0.6f, lz = -Mth.sin(ang) * 0.6f;
                    d.crystal(x + lx * len * 0.35, gy + len * 0.7, z + lz * len * 0.35, lx, 1, lz, 0.07f, len, 0x5A0A3A, 0.9f * f, false);
                    d.crystal(x + lx * len * 0.35, gy + len * 0.7, z + lz * len * 0.35, lx, 1, lz, 0.04f, len * 0.9f, MAGENTA, 0.6f * f, true);
                }
                d.spikedRing(p.x, gy + 0.05, p.z, 0.8f, 0.35f, 10, t * 0.03f, MAGENTA, 0.8f * f);
                d.star(p.x, p.y + 1.0, p.z, 1.6f * f, 0.2f, MAGENTA_HOT, f);
            } else {
                d.cylinder(p.x, gy, p.z, 1.0f, 2.4f * k, SAFE, (self ? 0.15f : 0.45f) * f, 0);
                d.ring(p.x, p.y + 1.0, p.z, 0, 1, 0, 0.6f + t * 0.06f, 0.75f + t * 0.06f, SAFE_HOT, 0.8f * f, true);
                d.star(p.x, p.y + 2.3, p.z, 1.0f * f, 0.5f, CRIMSON_HOT, f * 0.8f);
            }
        }
    }

    // --- Rowena at her side; the staff ----------------------------------------------------------------------------------------

    /** Rowena out of her yellow smoke (or back into it): her sigil in gold under her, a ring of sparks. */
    static final class AllyArrives extends MagicEffect {
        private final int rowena;
        private final boolean arrives;
        private final double gy;

        AllyArrives(int rowena, boolean arrives, Vec3 at) {
            super(at, School.WITCHCRAFT, arrives ? 40 : 24);
            this.rowena = rowena;
            this.arrives = arrives;
            this.gy = groundY(at);
        }

        @Override
        protected double reach() {
            return 5;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age != 0) return;
            RandomSource r = level.random;
            for (int i = FxParticles.n(arrives ? 30 : 16); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.spark(level, i % 3 == 0 ? 0x6FE08A : i % 3 == 1 ? GOLD : YELLOW_HOT, 0.6f, 20, at.x + Mth.cos(a) * 0.5, gy + 0.2 + r.nextDouble() * 1.8,
                        at.z + Mth.sin(a) * 0.5, Mth.cos(a) * 0.12, 0.05, Mth.sin(a) * 0.12);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 2, arrives ? 20 : 12), k = easeOut(t / 12f);
            d.decal(MagicTextures.seal(5), at.x, gy + 0.04, at.z, 1.2f + 0.3f * k, t * 0.01f, GOLD, 0.85f * a);
            d.flatGlow(at.x, gy + 0.03, at.z, 1.8f, YELLOW, 0.4f * a);
            d.groundRing(at.x, gy + 0.05, at.z, 0.5f + 2.2f * k, 0.7f + 2.2f * k, GOLD_HOT, 0.7f * (1 - k), true);
            if (arrives && t < 16) d.star(at.x, gy + 1.1, at.z, 2.2f * (1 - t / 16f) + 0.3f, 0.5f, YELLOW_HOT, 1 - t / 16f);
        }
    }

    /** The Coven Staff's hex: a plum lash from its crystal to its mark, a hex blooming on it. */
    static final class StaffHex extends MagicEffect {
        private final int wielder, foe;
        private final long seed;

        StaffHex(int wielder, int foe, Vec3 at, int ticks) {
            super(at, School.WITCHCRAFT, Math.max(10, ticks) + 6);
            this.wielder = wielder;
            this.foe = foe;
            this.seed = seedOf(at);
        }

        @Override
        protected double reach() {
            Entity w = entity(wielder);
            return (w != null ? w.position().distanceTo(at) : 0) + 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), f = Mth.clamp(1 - t / life, 0, 1);
            Entity w = entity(wielder), e = entity(foe);
            Vec3 to = e != null ? e.getPosition(d.partial).add(0, e.getBbHeight() * 0.55, 0) : at;
            if (w != null && t < 8) {
                Vec3 from = w.getEyePosition(d.partial).add(w.getViewVector(d.partial).scale(0.9)).subtract(0, 0.25, 0);
                d.lightning(from.x, from.y, from.z, to.x, to.y, to.z, seed + (long) (t / 2), 8, 0.3f, 0.06f, PLUM, (1 - t / 8f), true);
                d.softBeam(from.x, from.y, from.z, to.x, to.y, to.z, 0.2f, CRYSTAL, 0.5f * (1 - t / 8f), 0.5f * (1 - t / 8f), true);
            }
            d.disc(MagicTextures.seal((int) (seed & 7)), to.x, to.y, to.z, (float) (d.eye().x - to.x), 0, (float) (d.eye().z - to.z),
                    0.5f + 0.3f * (1 - f), t * 0.1f, CRYSTAL, 0.85f * f);
            d.glowDisc(to.x, to.y, to.z, 0.9f, PLUM, 0.5f * f);
        }
    }
}
