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
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicSounds;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicTextures;
import org.papiricoh.supernaturalcraft.client.witch.WitchBones;
import org.papiricoh.supernaturalcraft.client.witch.WitchGeo;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RecitationBowlEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaBalance;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaEntity;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.papiricoh.supernaturalcraft.client.witchboss.WitchFxKit.*;

/**
 * Rowena's fight in the world (v0.21, P6), on the magic's effect kit: each {@code RowenaFxPayload} moment over its telegraph, and
 * the states that last (her levitation, her recitation, her stun) read off her synced flags by {@link ClientRowenaFx}.
 * <ul>
 *   <li>{@link Fling}: a violet gust tearing down the lane — chevrons of force racing out, streamers of wind, the floor's dust and
 *   splinters thrown along it;</li>
 *   <li>{@link Ignis}: fire erupting out of the red circle in pillars, a ring of flame glyphs turning round it, embers and a scorch
 *   left behind;</li>
 *   <li>{@link Glacies}: ice spikes bursting out of the blue circle and a frost bloom spreading under them; they melt away;</li>
 *   <li>{@link Familiar}: the raven out of a burst of black feathers and witch-fire;</li>
 *   <li>{@link Homing}: the stolen taglock's thread drawn taut from her familiar to its owner, the curse running down it and
 *   biting in as a ring of magenta thorns;</li>
 *   <li>{@link Rising}: magenta cracks splitting the floor and the hexed clawing up out of them ({@link WitchGeo#rise});</li>
 *   <li>{@link Fate}: a victim killed bleeds a crimson thread of life into her (she heals); one freed throws a golden sigil
 *   that bursts on her (it hurts);</li>
 *   <li>{@link Recitation}: letters rising off each bowl, violet threads from the bowls to her hands, a spell circle writing itself
 *   under her with her progress, the whole garden turning red in its last seconds;</li>
 *   <li>{@link Shatter}, {@link HexWave}, {@link Fall}: a bowl breaking, the hex sweeping the arena, her fall out of the air;</li>
 *   <li>{@link Levitation}: the magenta updraft she floats on and her grimoire's loose pages circling her.</li>
 * </ul>
 */
public final class RowenaWorldFx {

    static final int RECITE = 0x9B4FD8, RECITE_HOT = 0xE6CCFF;
    /** The recitation showing for each Rowena (by entity id). */
    private static final Map<Integer, Recitation> RECITING = new HashMap<>();
    private static final Map<Integer, Levitation> FLOATING = new HashMap<>();
    private static final Map<Integer, Fall> FALLEN = new HashMap<>();

    private RowenaWorldFx() {
    }

    static void clear() {
        RECITING.clear();
        FLOATING.clear();
        FALLEN.clear();
    }

    // --- starting them --------------------------------------------------------------------------------------------------

    public static void fling(int rowena, Vec3 from, float yaw, float length, int ticks) {
        MagicFxManager.add(new Fling(rowena, from, yaw, Math.max(2, length), ticks));
    }

    public static void ignis(Vec3 centre, float radius, int ticks) {
        MagicFxManager.add(new Ignis(centre, Math.max(1, radius), ticks));
    }

    public static void glacies(Vec3 centre, float radius, int ticks) {
        MagicFxManager.add(new Glacies(centre, Math.max(1, radius), ticks));
    }

    public static void familiar(int familiar, Vec3 at, int ticks) {
        MagicFxManager.add(new Familiar(familiar, at, ticks));
    }

    public static void homing(int rowena, int victim, int curse, Vec3 hands, int travel) {
        MagicFxManager.add(new Homing(rowena, victim, curse, hands, travel));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == victim) MagicOverlay.watched(true);
    }

    public static void victim(int victim, Vec3 at, int ticks) {
        WitchGeo.rise(victim);
        MagicFxManager.add(new Rising(at, ticks));
    }

    public static void fate(int rowena, int victim, boolean freed, Vec3 at, int ticks) {
        MagicFxManager.add(new Fate(rowena, victim, freed, at, ticks));
    }

    /** A progress update of her recitation ({@code progress} 0-1, {@code whole} bowls standing). */
    public static void recite(int rowena, float progress, int whole, int every) {
        Recitation r = recitation(rowena);
        if (r != null) r.update(progress, whole, every);
    }

    /** The recitation for Rowena {@code id}, started if it is not showing. */
    static Recitation recitation(int id) {
        Recitation r = RECITING.get(id);
        if (r != null && !r.isDone()) return r;
        Entity e = entity(id);
        if (!(e instanceof RowenaEntity)) return null;
        r = new Recitation(id, e.position());
        RECITING.put(id, r);
        MagicFxManager.add(r);
        return r;
    }

    public static void bowlBroken(int rowena, int bowl, int left, Vec3 at, int ticks) {
        Recitation r = RECITING.get(rowena);
        Vec3 where = at;
        if (r != null) {
            Vec3 known = r.bowls.get(bowl);
            if (known != null) where = known;
            r.bowls.remove(bowl);
            r.whole = left;
        }
        MagicFxManager.add(new Shatter(rowena, where, left));
    }

    /** The recitation's end: broken off (she falls) or spoken (the hex sweeps the arena from {@code centre}). */
    public static void reciteEnd(int rowena, boolean spoken, float damage, Vec3 centre, int ticks) {
        Recitation r = RECITING.remove(rowena);
        if (r != null) r.end(spoken);
        if (spoken) {
            MagicFxManager.add(new HexWave(rowena, centre, RowenaHouseLayout.ARENA_RADIUS, damage));
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.distanceToSqr(centre) < (RowenaHouseLayout.ARENA_RADIUS + 4) * (RowenaHouseLayout.ARENA_RADIUS + 4)) {
                MagicOverlay.flash(mc.player.position(), 0xC0102A, 0.55f, 40);
                MagicShake.kick(mc.player.position(), 0.8f, 14, 40);
            }
        } else {
            fall(rowena);
        }
    }

    static void fall(int rowena) {
        Fall f = FALLEN.get(rowena);
        if (f != null && !f.isDone()) return;
        Entity e = entity(rowena);
        if (e == null) return;
        f = new Fall(rowena, e.position());
        FALLEN.put(rowena, f);
        MagicFxManager.add(f);
    }

    public static void smoke(Vec3 at, int ticks) {
        gather(at, ticks, RowenaBalance.SMOKE_RADIUS);
    }

    /** Her levitation (P3) shows while she floats (started from her flags; it ends by itself when she lands). */
    static void levitate(RowenaEntity r) {
        Levitation l = FLOATING.get(r.getId());
        if (l != null && !l.isDone()) return;
        l = new Levitation(r.getId(), r.position());
        FLOATING.put(r.getId(), l);
        MagicFxManager.add(l);
    }

    static boolean floating(RowenaEntity r) {
        return r.phase() >= RowenaEntity.MAX_PHASE && !r.stunned() && r.state() != RowenaEntity.DYING && r.state() != RowenaEntity.EMERGING;
    }

    // --- P1 · fling ---------------------------------------------------------------------------------------------------------

    /** The violet gust down the lane. */
    static final class Fling extends MagicEffect {
        private static final int RUSH = 7;
        private final int rowena;
        private final float length;
        private final double gy, dx, dz;
        private final long seed;

        Fling(int rowena, Vec3 from, float yaw, float length, int ticks) {
            super(from, School.WITCHCRAFT, Math.max(10, ticks) + 12);
            this.rowena = rowena;
            this.length = length;
            this.gy = groundY(from);
            float r = yaw * Mth.DEG_TO_RAD;
            this.dx = -Mth.sin(r);
            this.dz = Mth.cos(r);
            this.seed = seedOf(from);
        }

        @Override
        protected double reach() {
            return length + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private float front(float t) {
            return length * easeOut(t / RUSH);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age <= RUSH + 1) {
                float f = front(age), f0 = front(Math.max(0, age - 1));
                for (float s = f0; s <= f; s += 0.7f) {
                    double x = at.x + dx * s, z = at.z + dz * s;
                    for (int i = FxParticles.rate(r, 1.4f); i > 0; i--) {
                        double side = FxParticles.j(r) * 1.1;
                        double px = x - dz * side, pz = z + dx * side;
                        FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, px, gy, pz)), px, gy + 0.1, pz,
                                dx * (0.35 + r.nextDouble() * 0.3), 0.15 + r.nextDouble() * 0.25, dz * (0.35 + r.nextDouble() * 0.3));
                    }
                    if (r.nextFloat() < 0.5f) {
                        FxParticles.mote(level, VIOLET, 0.5f, 14, x + FxParticles.j(r) * 0.6, gy + 0.3 + r.nextDouble() * 1.4,
                                z + FxParticles.j(r) * 0.6, dx * 0.25, 0.02, dz * 0.25);
                    }
                }
                if (age == RUSH) {
                    Vec3 end = new Vec3(at.x + dx * length, gy + 0.6, at.z + dz * length);
                    for (int i = FxParticles.n(10); i > 0; i--) {
                        FxParticles.plain(level, ParticleTypes.POOF, end.x + FxParticles.j(r) * 0.6, end.y, end.z + FxParticles.j(r) * 0.6,
                                dx * 0.15, 0.04, dz * 0.15);
                    }
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), f = front(t), a = fade(t, 1, 12);
            // Her hands: a ripple of force where it leaves her.
            Entity e = entity(rowena);
            Vec3 hand = e != null ? WitchBones.boneOr(e, WitchBones.Bone.RIGHT_HAND, d.partial, 1.3, at.add(0, 1.3, 0)) : at.add(0, 1.3, 0);
            float rip = easeOut(t / 6f);
            d.ring(hand.x, hand.y, hand.z, (float) dx, 0, (float) dz, rip * 1.2f, rip * 1.2f + 0.18f, VIOLET_HOT, 0.8f * (1 - rip), true);
            // The torn floor: a streak down the lane, brightest at the front.
            double ex = at.x + dx * f, ez = at.z + dz * f;
            d.groundLine(at.x, at.z, ex, ez, gy + 0.03, 2.2f, VIOLET, 0.35f * a);
            d.groundLine(at.x, at.z, ex, ez, gy + 0.035, 0.5f, VIOLET_HOT, 0.45f * a);
            // Chevrons of force racing out along it.
            for (int i = 0; i < 5; i++) {
                float s = f - i * 1.6f;
                if (s < 0.5f) continue;
                float ca = a * (1 - i / 5f) * (t < RUSH + 2 ? 1 : 0.6f);
                double cx = at.x + dx * s, cz = at.z + dz * s, bx = at.x + dx * (s - 0.9f), bz = at.z + dz * (s - 0.9f);
                d.groundLine(bx - dz * 1.1, bz + dx * 1.1, cx, cz, gy + 0.05, 0.25f, VIOLET_HOT, 0.7f * ca);
                d.groundLine(bx + dz * 1.1, bz - dx * 1.1, cx, cz, gy + 0.05, 0.25f, VIOLET_HOT, 0.7f * ca);
            }
            // Streamers of wind above it.
            for (int i = 0; i < 6; i++) {
                float h = 0.4f + hash(seed, i) * 1.4f, side = (hash(seed, i + 10) - 0.5f) * 1.8f;
                float lag = hash(seed, i + 20) * 2.5f, s1 = Math.max(0, f - lag), s0 = Math.max(0, s1 - 2.5f - hash(seed, i + 30) * 2f);
                if (s1 - s0 < 0.2f) continue;
                double wob = 0.12 * Mth.sin(t * 0.8f + i);
                d.taper(at.x + dx * s0 - dz * side, gy + h + wob, at.z + dz * s0 + dx * side, at.x + dx * s1 - dz * side, gy + h, at.z + dz * s1 + dx * side,
                        0.02f, 0.14f, VIOLET, 0, 0.55f * a);
            }
            if (t < RUSH + 3) d.glowDisc(ex, gy + 0.9, ez, 1.2f, VIOLET, 0.35f * a);
        }
    }

    // --- P1 · Ignis -----------------------------------------------------------------------------------------------------------

    /** Fire out of the red circle. */
    static final class Ignis extends MagicEffect {
        private final float radius;
        private final int burn;
        private final double gy;
        private final long seed;

        Ignis(Vec3 centre, float radius, int ticks) {
            super(centre, School.WITCHCRAFT, Math.max(16, ticks) + 24);
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
            if (age == 0) {
                MagicShake.kick(at, 0.35f, 6, radius + 6);
                for (int i = FxParticles.n(16); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * radius;
                    FxParticles.plain(level, ParticleTypes.LAVA, at.x + Mth.cos(a) * k, gy + 0.2, at.z + Mth.sin(a) * k, 0, 0, 0);
                }
            }
            if (age < burn) {
                float rate = radius * 1.4f * (age < 6 ? 2 : 1);
                for (int i = FxParticles.rate(r, rate); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * radius;
                    FxParticles.plain(level, r.nextInt(4) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, at.x + Mth.cos(a) * k,
                            gy + 0.1, at.z + Mth.sin(a) * k, FxParticles.j(r) * 0.02, 0.04 + r.nextDouble() * 0.08, FxParticles.j(r) * 0.02);
                }
                // Flame glyphs climbing out of the ring.
                if (r.nextFloat() < 0.6f) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.glyph(level, r.nextBoolean() ? FIRE : FIRE_HOT, 0.7f, 22, at.x + Mth.cos(a) * radius, gy + 0.2,
                            at.z + Mth.sin(a) * radius, 0, 0.06, 0);
                }
                if (r.nextFloat() < 0.5f) {
                    FxParticles.ember(level, FIRE_HOT, 0.5f, 20, at.x + FxParticles.j(r) * radius * 0.7, gy + 0.5 + r.nextDouble(),
                            at.z + FxParticles.j(r) * radius * 0.7, FxParticles.j(r) * 0.02, 0.06, FxParticles.j(r) * 0.02);
                }
            } else if (r.nextFloat() < 0.5f) {
                FxParticles.plain(level, ParticleTypes.SMOKE, at.x + FxParticles.j(r) * radius * 0.6, gy + 0.1, at.z + FxParticles.j(r) * radius * 0.6,
                        0, 0.03, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 18), burning = 1 - Mth.clamp((t - burn) / 14f, 0, 1);
            float flick = 0.8f + 0.2f * Mth.sin(t * 1.7f) * Mth.sin(t * 0.6f + 1);
            // The eruption: a flash and a ring of fire blown outward.
            if (t < 8) {
                float k = t / 8f;
                d.flatGlow(at.x, gy + 0.04, at.z, radius * 1.4f, FIRE_HOT, 0.8f * (1 - k));
                d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, radius * (0.6f + 0.7f * easeOut(k)), 0.6f, 1.2f * (1 - k), FIRE, 0.9f * (1 - k));
            }
            // The scorch it leaves, darkening as the fire dies.
            d.darkRing(at.x, gy + 0.025, at.z, 0, radius * 0.95f, 0x1A0A04, 0.55f * Mth.clamp(t / 10f, 0, 1) * Mth.clamp((life - t) / 18f, 0, 1));
            // The bed of fire.
            d.flatGlow(at.x, gy + 0.05, at.z, radius * 1.05f, FIRE, 0.55f * a * burning * flick);
            // The ring of flame glyphs turning round it.
            d.glyphRing(MagicTextures.glyphStrip(), at.x, gy + 0.06, at.z, 0, 1, 0, radius - 0.15f, radius + 0.35f, t * 0.04f, FIRE_HOT,
                    0.85f * a * burning);
            d.groundRing(at.x, gy + 0.05, at.z, radius - 0.05f, radius + 0.1f, FIRE, 0.8f * a * burning, false);
            // Pillars of flame: tall at the burst, then licking up unevenly while it burns.
            int n = 6 + Math.round(radius * 2);
            for (int i = 0; i < n; i++) {
                float ang = hash(seed, i) * Mth.TWO_PI, k = Mth.sqrt(hash(seed, i + 40)) * radius * 0.85f;
                double x = at.x + Mth.cos(ang) * k, z = at.z + Mth.sin(ang) * k;
                float burst = easeOutBack(Mth.clamp((t - hash(seed, i + 80) * 4) / 5f, 0, 1));
                float lick = 0.6f + 0.4f * Mth.sin(t * (0.6f + hash(seed, i + 7) * 0.8f) + i * 2.1f);
                float h = (t < 10 ? 2.4f * burst : Mth.lerp(Mth.clamp((t - 10) / 8f, 0, 1), 2.4f, 1.1f * lick)) * burning;
                if (h < 0.05f) continue;
                d.softBeam(x, gy, z, x + Mth.sin(t * 0.3f + i) * 0.08, gy + h, z, 0.55f, FIRE, 0.75f * a, 0, false);
                d.softBeam(x, gy, z, x, gy + h * 0.7f, z, 0.22f, FIRE_HOT, 0.8f * a, 0, true);
            }
            d.glowDisc(at.x, gy + 0.8, at.z, radius * 0.9f, FIRE, 0.25f * a * burning * flick);
        }
    }

    // --- P1 · Glacies ---------------------------------------------------------------------------------------------------------

    /** Ice spikes out of the blue circle and a bloom of frost. */
    static final class Glacies extends MagicEffect {
        private static final int MELT = 16;
        private final float radius;
        private final int stay;
        private final double gy;
        private final long seed;
        private final int spikes;

        Glacies(Vec3 centre, float radius, int ticks) {
            super(centre, School.WITCHCRAFT, Math.max(16, ticks) + MELT);
            this.radius = radius;
            this.stay = Math.max(16, ticks);
            this.gy = groundY(centre);
            this.seed = seedOf(centre);
            this.spikes = 8 + Math.round(radius * 3);
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
                MagicShake.kick(at, 0.3f, 6, radius + 6);
                for (int i = FxParticles.n(30); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * radius;
                    FxParticles.plain(level, AllParticles.FROST.get(), at.x + Mth.cos(a) * k, gy + 0.2 + r.nextDouble(), at.z + Mth.sin(a) * k,
                            Mth.cos(a) * 0.05, 0.08, Mth.sin(a) * 0.05);
                }
            }
            if (age < stay && r.nextFloat() < 0.8f) {
                float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * radius;
                FxParticles.plain(level, ParticleTypes.SNOWFLAKE, at.x + Mth.cos(a) * k, gy + 0.3 + r.nextDouble() * 1.4, at.z + Mth.sin(a) * k,
                        0, -0.01, 0);
            }
            if (age == stay) MagicSounds.play(SoundEvents.GLASS_BREAK, at, 0.6f, 1.4f);
            if (age >= stay && age < stay + 8) {
                for (int i = FxParticles.rate(r, 2f); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.DRIPPING_WATER, at.x + FxParticles.j(r) * radius * 0.7, gy + 0.4 + r.nextDouble(),
                            at.z + FxParticles.j(r) * radius * 0.7, 0, 0, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, MELT), melt = Mth.clamp((t - stay) / MELT, 0, 1);
            // The frost bloom: rime spreading out from the middle, then a pale wash under the spikes.
            float bloom = easeOut(t / 8f);
            d.flatGlow(at.x, gy + 0.04, at.z, radius * 1.15f * bloom, ICE, 0.45f * a);
            d.cracks(at.x, gy + 0.03, at.z, 12, radius * bloom, 0.06f, seed, ICE, 0.7f * a);
            d.groundRing(at.x, gy + 0.05, at.z, radius * bloom - 0.15f, radius * bloom + 0.05f, ICE_DEEP, 0.7f * a * (1 - melt), true);
            if (t < 8) {
                float k = t / 8f;
                d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, radius * (0.5f + 0.8f * easeOut(k)), 0.4f, 0.8f * (1 - k), ICE,
                        0.8f * (1 - k));
            }
            // The spikes: out of the floor with a snap, leaning outward; they sink and fade as they melt.
            for (int i = 0; i < spikes; i++) {
                float ang = hash(seed, i) * Mth.TWO_PI, k = (0.15f + 0.85f * Mth.sqrt(hash(seed, i + 50))) * radius * 0.92f;
                float delay = k / radius * 3f, grow = easeOutBack(Mth.clamp((t - delay) / 4f, 0, 1)) * (1 - melt * 0.7f);
                if (grow <= 0.02f) continue;
                float len = (0.45f + 0.9f * hash(seed, i + 9)) * (1.1f - 0.4f * k / radius), lean = 0.25f + 0.35f * k / radius;
                float ddx = Mth.cos(ang) * lean, ddz = Mth.sin(ang) * lean;
                double x = at.x + Mth.cos(ang) * k, z = at.z + Mth.sin(ang) * k;
                float ry = len * grow, rx = 0.09f + 0.07f * len;
                double cy = gy - 0.1 + ry * 0.75 - melt * ry * 0.5;
                d.crystal(x + ddx * ry * 0.4, cy, z + ddz * ry * 0.4, ddx, 1, ddz, rx, ry, 0xD8F0FF, 0.82f * a, false);
                d.crystal(x + ddx * ry * 0.4, cy, z + ddz * ry * 0.4, ddx, 1, ddz, rx * 0.55f, ry * 0.9f, ICE, 0.5f * a, true);
            }
            d.glowDisc(at.x, gy + 0.6, at.z, radius * 0.9f, ICE, 0.18f * a * (1 - melt));
        }
    }

    // --- P2 · the familiar ----------------------------------------------------------------------------------------------------

    /** The raven out of black feathers and witch-fire. */
    static final class Familiar extends MagicEffect {
        private final int familiar;

        Familiar(int familiar, Vec3 at, int ticks) {
            super(at, School.WITCHCRAFT, 26);
            this.familiar = familiar;
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(familiar);
            return e != null ? e.position() : at;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                MagicSounds.play(AllSounds.coven("witch.raven_caw"), at, 1f, 0.9f);
                for (int i = FxParticles.n(22); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, e = (r.nextFloat() - 0.4f) * 1.2f;
                    FxParticles.plain(level, ParticleTypes.SQUID_INK, at.x, at.y, at.z, Mth.cos(a) * 0.18, e * 0.15, Mth.sin(a) * 0.18);
                }
                for (int i = FxParticles.n(12); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.mote(level, i % 2 == 0 ? MAGENTA : 0x6FE08A, 0.6f, 18, at.x, at.y, at.z, Mth.cos(a) * 0.12, 0.05, Mth.sin(a) * 0.12);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 14);
            Vec3 p = anchor().add(0, 0.3, 0);
            float k = easeOut(t / 10f);
            d.ring(p.x, p.y, p.z, 0, 1, 0, k * 1.4f, k * 1.4f + 0.12f, MAGENTA_HOT, 0.8f * (1 - k), true);
            d.glowDisc(p.x, p.y, p.z, 0.9f, MAGENTA, 0.5f * a * (1 - k * 0.6f));
            // Its eyes, lit for a moment.
            Entity e = entity(familiar);
            if (e != null && t < 20) {
                Vec3 eye = e.getPosition(d.partial).add(0, e.getBbHeight() * 0.8, 0);
                d.glowDisc(eye.x, eye.y, eye.z, 0.12f, MAGENTA_HOT, 0.9f * a);
            }
        }
    }

    // --- P2 · the curse down the stolen taglock -------------------------------------------------------------------------------

    /**
     * The taglock's thread drawn taut between the familiar (the lock in its beak) and its owner, pulsing while the curse runs down
     * it; the curse bites in as a ring of thorns and its mark over their head. Her own seal flies on its own ({@code BOWL_SEND}).
     */
    static final class Homing extends MagicEffect {
        private final int rowena, victim, curse, travel;
        private final long seed;

        Homing(int rowena, int victim, int curse, Vec3 hands, int travel) {
            super(hands, School.WITCHCRAFT, Math.max(8, travel) + 26);
            this.rowena = rowena;
            this.victim = victim;
            this.curse = curse;
            this.travel = Math.max(8, travel);
            this.seed = seedOf(hands);
        }

        @Override
        protected double reach() {
            return 40;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        private int familiar = -1;

        private Vec3 lock(float partial) {
            Entity f = entity(familiar);
            return f != null && !f.isRemoved() ? f.getPosition(partial).add(0, f.getBbHeight() * 0.6, 0) : at;
        }

        /** Her familiar (the lock in its beak): the nearest to her, looked up once a tick. */
        private void findFamiliar(ClientLevel level) {
            Entity r = entity(rowena);
            if (r == null) return;
            FamiliarEntity best = null;
            for (FamiliarEntity f : level.getEntitiesOfClass(FamiliarEntity.class, r.getBoundingBox().inflate(40))) {
                if (best == null || f.distanceToSqr(r) < best.distanceToSqr(r)) best = f;
            }
            familiar = best != null ? best.getId() : -1;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age % 5 == 0) findFamiliar(level);
            if (age == travel) {
                Entity v = entity(victim);
                if (v == null) return;
                RandomSource r = level.random;
                Vec3 p = v.position();
                for (int i = FxParticles.n(16); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, i % 2 == 0 ? MAGENTA_HOT : MAGENTA, 0.6f, 14, p.x + Mth.cos(a) * 0.7, p.y + 0.1, p.z + Mth.sin(a) * 0.7,
                            -Mth.cos(a) * 0.05, 0.18 + r.nextDouble() * 0.1, -Mth.sin(a) * 0.05);
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == v) {
                    MagicOverlay.flash(p, MAGENTA, 0.35f, 8);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 3, 14);
            Entity v = entity(victim);
            if (v == null) return;
            Vec3 lock = lock(d.partial), heart = v.getPosition(d.partial).add(0, v.getBbHeight() * 0.55, 0);
            Vec3 mid = lock.add(heart).scale(0.5).add(0, 0.8 + lock.distanceTo(heart) * 0.06, 0);
            // The thread, taut and pulsing; the curse a bright knot running down it.
            float k = Mth.clamp(t / travel, 0, 1);
            float throb = 0.6f + 0.4f * Mth.sin(t * 0.9f);
            thread(d, lock, mid, heart, 0, 1, 0.03f, MAGENTA, MAGENTA_HOT, 0.6f * a * throb, t, 0.06f);
            if (k < 1) {
                Vec3 p = bezier(lock, mid, heart, easeOut(k));
                d.star(p.x, p.y, p.z, 0.55f, t * 0.2f, MAGENTA_HOT, 0.9f * a);
                d.glowDisc(p.x, p.y, p.z, 0.6f, MAGENTA, 0.5f * a);
            }
            // Bitten in: a ring of thorns closing on their feet and the curse's mark over their head.
            if (t >= travel) {
                float s = t - travel, f = Mth.clamp(1 - s / 22f, 0, 1), close = easeOut(s / 6f);
                Vec3 feet = v.getPosition(d.partial);
                d.spikedRing(feet.x, feet.y + 0.05, feet.z, 1.1f - 0.5f * close, 0.35f, 9, t * 0.05f, MAGENTA, 0.85f * f);
                d.groundRing(feet.x, feet.y + 0.04, feet.z, 0.4f, 0.7f, MAGENTA_HOT, 0.6f * f, true);
                Minecraft mc = Minecraft.getInstance();
                if (v == mc.player && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == v) return;
                Vec3 top = feet.add(0, v.getBbHeight() + 0.45, 0);
                int col = curse == 1 ? CRIMSON_HOT : 0xC8B0E8;
                d.disc(MagicTextures.seal(Math.floorMod((int) (seed & 0xFF) + curse, 8)), top.x, top.y, top.z,
                        (float) (d.eye().x - top.x), 0, (float) (d.eye().z - top.z), 0.32f, 0, col, 0.9f * f);
                d.glowDisc(top.x, top.y, top.z, 0.45f, col, 0.4f * f);
            }
        }
    }

    // --- P2 · the hexed rising ----------------------------------------------------------------------------------------------

    /** Magenta cracks splitting the floor where a hexed victim claws its way up. */
    static final class Rising extends MagicEffect {
        private final double gy;
        private final long seed;

        Rising(Vec3 at, int ticks) {
            super(at, School.WITCHCRAFT, WitchGeo.RISE_TICKS + 26);
            this.gy = groundY(at);
            this.seed = seedOf(at);
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age < WitchGeo.RISE_TICKS) {
                for (int i = FxParticles.rate(r, 1.5f); i > 0; i--) {
                    double x = at.x + FxParticles.j(r) * 0.6, z = at.z + FxParticles.j(r) * 0.6;
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, x, gy, z)), x, gy + 0.05, z,
                            FxParticles.j(r) * 0.08, 0.12 + r.nextDouble() * 0.1, FxParticles.j(r) * 0.08);
                }
                if (r.nextFloat() < 0.5f) {
                    FxParticles.mote(level, MAGENTA, 0.5f, 18, at.x + FxParticles.j(r) * 0.8, gy + 0.1, at.z + FxParticles.j(r) * 0.8, 0, 0.05, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 2, 16), spread = easeOut(t / 8f);
            float flick = 0.75f + 0.25f * Mth.sin(t * 1.3f);
            // Thin glowing cracks flat on the floor, a feathered pool of light and a dark split where it climbs out.
            d.cracks(at.x, gy + 0.02, at.z, 7, 1.4f * spread, 0.045f, seed, MAGENTA_HOT, 0.55f * a * flick);
            d.flatGlow(at.x, gy + 0.03, at.z, 1.4f * spread, MAGENTA, 0.3f * a * flick);
            d.darkRing(at.x, gy + 0.025, at.z, 0, 0.55f * spread, 0x14040E, 0.5f * a);
            float rise = 1 - Mth.clamp(t / WitchGeo.RISE_TICKS, 0, 1);
            d.glowDisc(at.x, gy + 0.5, at.z, 0.7f, MAGENTA, 0.3f * a * rise);
        }
    }

    // --- P2 · a victim's fate -------------------------------------------------------------------------------------------------

    /** Killed: a crimson thread of its life into her. Freed: a golden sigil thrown into her. */
    static final class Fate extends MagicEffect {
        private final int rowena, victim;
        private final boolean freed;
        private final int flight;

        Fate(int rowena, int victim, boolean freed, Vec3 at, int ticks) {
            super(at, School.WITCHCRAFT, Math.max(12, ticks) + 22);
            this.rowena = rowena;
            this.victim = victim;
            this.freed = freed;
            this.flight = Math.max(12, ticks);
        }

        @Override
        protected double reach() {
            Entity r = entity(rowena);
            return (r != null ? r.position().distanceTo(at) : 0) + 5;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        private Vec3 her(float partial) {
            Entity r = entity(rowena);
            return r != null ? r.getPosition(partial).add(0, r.getBbHeight() * 0.6, 0) : at.add(0, 1, 0);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 from = at.add(0, 0.8, 0), to = her(1f);
            if (!freed && age < flight) {
                // Drops of it running toward her.
                Vec3 mid = from.add(to).scale(0.5).add(0, 1.2, 0);
                Vec3 p = bezier(from, mid, to, r.nextFloat() * Mth.clamp(age / 6f, 0, 1));
                FxParticles.spark(level, CRIMSON_HOT, 0.45f, 10, p.x, p.y, p.z, 0, 0.01, 0);
            }
            if (age == flight) {
                if (freed) {
                    MagicSounds.play(SoundEvents.AMETHYST_BLOCK_RESONATE, to, 1f, 1.3f);
                    for (int i = FxParticles.n(24); i > 0; i--) {
                        float a = r.nextFloat() * Mth.TWO_PI;
                        FxParticles.spark(level, i % 2 == 0 ? GOLD : GOLD_HOT, 0.6f, 16, to.x, to.y, to.z, Mth.cos(a) * 0.22,
                                FxParticles.j(r) * 0.15, Mth.sin(a) * 0.22);
                    }
                } else {
                    for (int i = FxParticles.n(14); i > 0; i--) {
                        FxParticles.ember(level, CRIMSON_HOT, 0.55f, 22, to.x + FxParticles.j(r) * 0.4, to.y - 0.8 + r.nextDouble() * 1.4,
                                to.z + FxParticles.j(r) * 0.4, 0, 0.06, 0);
                    }
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 2, 14);
            Vec3 from = at.add(0, 0.8, 0), to = her(d.partial);
            if (freed) {
                // The hex's sigil rising off it in gold, then hurled at her like a comet.
                float lift = Mth.clamp(t / 6f, 0, 1), fly = Mth.clamp((t - 6) / (flight - 6f), 0, 1);
                Vec3 up = from.add(0, 0.8 * easeOut(lift), 0);
                Vec3 p = fly <= 0 ? up : bezier(up, up.add(to).scale(0.5).add(0, 1.0, 0), to, fly * fly);
                if (t < flight) {
                    d.disc(MagicTextures.seal(3), p.x, p.y, p.z, (float) (d.eye().x - p.x), 0, (float) (d.eye().z - p.z), 0.45f, t * 0.15f,
                            GOLD, 0.95f * a);
                    d.glowDisc(p.x, p.y, p.z, 0.7f, GOLD, 0.5f * a);
                    if (fly > 0) {
                        Vec3 tail = bezier(up, up.add(to).scale(0.5).add(0, 1.0, 0), to, Math.max(0, fly - 0.25f) * Math.max(0, fly - 0.25f));
                        d.taper(tail.x, tail.y, tail.z, p.x, p.y, p.z, 0, 0.25f, GOLD, 0, 0.8f * a);
                    }
                } else {
                    float s = t - flight, f = Mth.clamp(1 - s / 14f, 0, 1);
                    d.star(to.x, to.y, to.z, 1.8f * f + 0.3f, 0.4f, GOLD_HOT, f);
                    d.ring(to.x, to.y, to.z, 0, 1, 0, easeOut(s / 8f) * 2f, easeOut(s / 8f) * 2f + 0.2f, GOLD, 0.8f * f, true);
                }
            } else {
                // Its life bled into her along a crimson thread, which then thins away from it.
                Vec3 mid = from.add(to).scale(0.5).add(0, 1.2, 0);
                float head = Mth.clamp(t / 6f, 0, 1), tailU = Mth.clamp((t - flight) / 10f, 0, 1);
                thread(d, from, mid, to, tailU, head, 0.04f, CRIMSON, CRIMSON_HOT, 0.8f * a, t, 0.08f);
                d.glowDisc(from.x, from.y, from.z, 0.6f, CRIMSON, 0.4f * a * (1 - tailU));
                if (t > flight - 4) {
                    float s = t - flight + 4, f = Mth.clamp(1 - s / 18f, 0, 1);
                    d.glowDisc(to.x, to.y, to.z, 1.4f, CRIMSON, 0.5f * f);
                    d.ring(to.x, to.y - 0.6, to.z, 0, 1, 0, 0.6f + easeOut(s / 10f) * 0.8f, 0.8f + easeOut(s / 10f) * 0.8f, CRIMSON_HOT, 0.7f * f, true);
                }
            }
        }
    }

    // --- P3 · the levitation ----------------------------------------------------------------------------------------------------

    /** The magenta updraft she floats on and her grimoire's loose pages circling her (while she floats). */
    static final class Levitation extends MagicEffect {
        private static final int PAGES = 6;
        private final int rowena;
        private Vec3 pos;
        private boolean ending;
        private int endAt;

        Levitation(int rowena, Vec3 at) {
            super(at, School.WITCHCRAFT, 20 * 60 * 10);
            this.rowena = rowena;
            this.pos = at;
        }

        boolean isDone() {
            return done();
        }

        @Override
        protected boolean done() {
            return super.done() || ending && age - endAt > 14;
        }

        @Override
        protected Vec3 anchor() {
            return pos;
        }

        @Override
        protected double reach() {
            return 5;
        }

        @Override
        protected void tick(ClientLevel level) {
            Entity e = entity(rowena);
            if (!(e instanceof RowenaEntity r) || r.isRemoved()) {
                if (!ending) {
                    ending = true;
                    endAt = age;
                }
                return;
            }
            pos = r.position();
            if (!ending && !floating(r)) {
                ending = true;
                endAt = age;
            }
            if (ending) return;
            RandomSource rand = level.random;
            double gy = groundY(pos);
            if (rand.nextFloat() < 0.7f) {
                float a = rand.nextFloat() * Mth.TWO_PI, k = rand.nextFloat() * 0.7f;
                FxParticles.mote(level, rand.nextBoolean() ? MAGENTA : MAGENTA_HOT, 0.45f, 26, pos.x + Mth.cos(a) * k, gy + 0.1,
                        pos.z + Mth.sin(a) * k, 0, 0.07 + rand.nextDouble() * 0.04, 0);
            }
            if (rand.nextFloat() < 0.08f) {
                FxParticles.plain(level, AllParticles.PAGE.get(), pos.x + FxParticles.j(rand) * 0.8, pos.y + 1 + rand.nextDouble(),
                        pos.z + FxParticles.j(rand) * 0.8, FxParticles.j(rand) * 0.02, 0.02, FxParticles.j(rand) * 0.02);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = Mth.clamp(t / 12f, 0, 1) * (ending ? 1 - Mth.clamp((age - endAt + d.partial) / 14f, 0, 1) : 1);
            if (a <= 0.01f) return;
            Entity e = entity(rowena);
            Vec3 p = e != null ? e.getPosition(d.partial) : pos;
            double gy = d.floor();
            if (Double.isNaN(gy)) gy = p.y - 1.2;
            double lift = Math.max(0.3, p.y - gy);
            // The updraft: a soft column from the floor to her skirts, a circle turning under her.
            d.cylinder(p.x, gy, p.z, 0.75f, (float) lift + 0.4f, MAGENTA, 0.28f * a, 0);
            d.softBeam(p.x, gy, p.z, p.x, p.y + 0.4, p.z, 1.1f, MAGENTA, 0.25f * a, 0.05f * a, false);
            d.disc(MagicTextures.castCircle(), p.x, gy + 0.04, p.z, 0, 1, 0, 1.2f, t * 0.02f, MAGENTA, 0.55f * a);
            d.flatGlow(p.x, gy + 0.03, p.z, 1.6f, MAGENTA, 0.3f * a);
            for (int i = 0; i < 3; i++) {
                float u = ((t * 0.03f + i / 3f) % 1f);
                double y = gy + u * (lift + 0.3);
                d.ring(p.x, y, p.z, 0, 1, 0, 0.5f + 0.3f * u, 0.6f + 0.3f * u, MAGENTA_HOT, 0.4f * a * Mth.sin(Mth.PI * u), true);
            }
            // Her grimoire's loose pages circling her, flapping, lit faintly from beneath.
            int light = light(p.add(0, 1, 0));
            for (int i = 0; i < PAGES; i++) {
                float ang = t * 0.035f + i * Mth.TWO_PI / PAGES, rr = 1.05f + 0.25f * Mth.sin(t * 0.05f + i * 1.3f);
                double y = p.y + 0.6 + 0.9 * ((i * 0.37f) % 1f) + 0.12 * Mth.sin(t * 0.09f + i);
                Vec3 c = new Vec3(p.x + Mth.cos(ang) * rr, y, p.z + Mth.sin(ang) * rr);
                float flap = 0.35f * Mth.sin(t * 0.35f + i * 2.1f);
                // The page lies tangent to its circle, tipped by its flap.
                float tx = -Mth.sin(ang), tz = Mth.cos(ang);
                float w = 0.17f, h = 0.22f;
                float ux = tx * w, uz = tz * w, uy = 0;
                float vx = Mth.cos(ang) * Mth.sin(flap) * h, vy = Mth.cos(flap) * h, vz = Mth.sin(ang) * Mth.sin(flap) * h;
                paper(d, PAGE, c, ux, uy, uz, vx, vy, vz, PAPER, 0.95f * a, light);
                paper(d, PAGE, c, -ux, -uy, -uz, vx, vy, vz, 0xD8CCB0, 0.95f * a, light);
                d.glowDisc(c.x, c.y, c.z, 0.28f, MAGENTA, 0.18f * a);
            }
        }
    }

    // --- P3 · the recitation ------------------------------------------------------------------------------------------------------

    /**
     * Her recitation over the bowls: letters rising off each, threads of violet from the bowls to her hands, a spell circle writing
     * itself under her with her progress, and in its last {@link RowenaBalance#WIPE_WARNING} ticks the whole arena (her front
     * garden, round the bowls' middle) turning red.
     */
    static final class Recitation extends MagicEffect {
        final Map<Integer, Vec3> bowls = new HashMap<>();
        private final Map<Integer, Integer> hits = new HashMap<>();
        private final int rowena;
        private float progress, rate = 1f / RowenaBalance.RECITE_TICKS;
        int whole = RowenaBalance.BOWLS;
        private int updated;
        private boolean ended, spoken;
        private int endAt;
        private Vec3 pos;

        Recitation(int rowena, Vec3 at) {
            super(at, School.WITCHCRAFT, RowenaBalance.RECITE_TICKS + 200);
            this.rowena = rowena;
            this.pos = at;
        }

        boolean isDone() {
            return done();
        }

        void update(float progress, int whole, int every) {
            this.progress = Math.max(this.progress, progress);
            this.whole = whole;
            this.updated = age;
        }

        void end(boolean spoken) {
            if (ended) return;
            ended = true;
            this.spoken = spoken;
            endAt = age;
        }

        private float progress(float partial) {
            return Mth.clamp(progress + rate * Math.min(20, age - updated + partial), 0, 1);
        }

        @Override
        protected boolean done() {
            return super.done() || ended && age - endAt > 20;
        }

        @Override
        protected Vec3 anchor() {
            return pos;
        }

        @Override
        protected double reach() {
            return 16;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        @Override
        protected void tick(ClientLevel level) {
            Entity e = entity(rowena);
            if (e instanceof RowenaEntity r) {
                pos = r.position();
                if (!ended && age > 30 && !r.reciting()) end(false);
            } else if (!ended) {
                end(false);
            }
            if (ended) return;
            // Her bowls, as the client sees them (their last places are kept for when they break).
            for (RecitationBowlEntity b : level.getEntitiesOfClass(RecitationBowlEntity.class, new AABB(pos, pos).inflate(20, 10, 20))) {
                bowls.put(b.getId(), b.position());
                Integer before = hits.put(b.getId(), b.hits());
                if (before != null && b.hits() > before) crack(level, b);
            }
            RandomSource r = level.random;
            float k = progress(0);
            boolean red = k >= 1 - RowenaBalance.WIPE_WARNING / (float) RowenaBalance.RECITE_TICKS;
            for (Vec3 b : bowls.values()) {
                // Letters off the liquid, faster as she nears the end.
                if (r.nextFloat() < 0.18f + 0.3f * k) {
                    String abc = "ABCDEFGHIKLMNOPQRSTVXYZ";
                    MagicFxManager.add(new Letter(b.add(0, 0.55, 0), String.valueOf(abc.charAt(r.nextInt(abc.length()))), k, red));
                }
                if (r.nextFloat() < 0.3f) {
                    FxParticles.plain(level, ParticleTypes.BUBBLE_POP, b.x + FxParticles.j(r) * 0.2, b.y + 0.55, b.z + FxParticles.j(r) * 0.2, 0, 0.02, 0);
                }
            }
            if (red) {
                // The whole garden going red: embers rising everywhere in it.
                Vec3 mid = middle();
                for (int i = FxParticles.rate(r, 3f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * RowenaHouseLayout.ARENA_RADIUS;
                    double x = mid.x + Mth.cos(a) * d, z = mid.z + Mth.sin(a) * d;
                    double gy = groundY(new Vec3(x, mid.y + 2, z));
                    FxParticles.ember(level, r.nextBoolean() ? RED : CRIMSON_HOT, 0.5f, 26, x, gy + 0.1, z, 0, 0.05 + r.nextDouble() * 0.04, 0);
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && age % 12 == 0 && mc.player.distanceToSqr(pos) < 20 * 20) MagicOverlay.flash(mc.player.position(), RED, 0.12f, 30);
            }
        }

        /** The arena's middle: the bowls stand round it (her own place while there are fewer than two). */
        private Vec3 middle() {
            if (bowls.size() < 2) return pos;
            Vec3 sum = Vec3.ZERO;
            for (Vec3 b : bowls.values()) sum = sum.add(b);
            return sum.scale(1.0 / bowls.size());
        }

        /** A blow on a bowl: shards and a spurt of its brew. */
        private void crack(ClientLevel level, RecitationBowlEntity b) {
            RandomSource r = level.random;
            Vec3 p = b.position().add(0, 0.5, 0);
            for (int i = FxParticles.n(10); i > 0; i--) {
                FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DECORATED_POT.defaultBlockState()),
                        p.x, p.y, p.z, FxParticles.j(r) * 0.15, 0.15 + r.nextDouble() * 0.1, FxParticles.j(r) * 0.15);
            }
            for (int i = FxParticles.n(8); i > 0; i--) {
                FxParticles.spark(level, RECITE_HOT, 0.5f, 12, p.x, p.y, p.z, FxParticles.j(r) * 0.15, 0.12, FxParticles.j(r) * 0.15);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = Mth.clamp(t / 10f, 0, 1) * (ended ? 1 - Mth.clamp((age - endAt + d.partial) / 20f, 0, 1) : 1);
            if (a <= 0.01f) return;
            Entity e = entity(rowena);
            Vec3 p = e != null ? e.getPosition(d.partial) : pos;
            float k = progress(d.partial);
            boolean red = k >= 1 - RowenaBalance.WIPE_WARNING / (float) RowenaBalance.RECITE_TICKS;
            float urgency = red ? 0.6f + 0.4f * Mth.sin(t * 0.6f) : 0;
            int col = red ? RED : RECITE, hot = red ? CRIMSON_HOT : RECITE_HOT;
            Vec3 hands = e != null ? WitchBones.boneOr(e, WitchBones.Bone.LEFT_HAND, d.partial, 1.3, p.add(0, 1.3, 0)) : p.add(0, 1.3, 0);
            // Threads from each bowl to her hands, the spell flowing up them.
            for (Vec3 b : bowls.values()) {
                Vec3 from = b.add(0, 0.55, 0), mid = from.add(hands).scale(0.5).add(0, 0.9, 0);
                thread(d, from, mid, hands, 0, 1, 0.025f, col, hot, (0.45f + 0.35f * k) * a, t, 0.05f + 0.06f * k);
                double by = b.y;
                // A ring round each bowl, filling with her progress.
                d.wipe(MagicTextures.castCircle(), b.x, by + 0.03, b.z, 0, 1, 0, 0.9f, -t * 0.02f, Math.max(0.05f, k), col, 0.7f * a);
                d.flatGlow(b.x, by + 0.03, b.z, 1.1f, col, (0.25f + 0.25f * urgency) * a);
                d.glowDisc(b.x, by + 0.6, b.z, 0.5f, hot, (0.3f + 0.3f * k) * a);
            }
            // The spell circle writing itself under her with her progress.
            double gy = groundY(p);
            d.wipe(MagicTextures.castCircle(), p.x, gy + 0.05, p.z, 0, 1, 0, 2.2f, t * 0.015f, Math.max(0.02f, k), col, 0.8f * a);
            d.glyphRing(MagicTextures.glyphStrip(), p.x, gy + 0.06, p.z, 0, 1, 0, 2.3f, 2.75f, -t * 0.02f, hot, 0.5f * a * k);
            d.glowDisc(hands.x, hands.y, hands.z, 0.5f + 0.3f * k, hot, (0.35f + 0.3f * k) * a);
            if (red) {
                // The red over everything: the whole garden's floor glowing, pulsing faster.
                Vec3 mid = middle();
                double my = groundY(mid.add(0, 2, 0));
                d.flatGlow(mid.x, my + 0.04, mid.z, RowenaHouseLayout.ARENA_RADIUS, RED, 0.22f * a * urgency);
                d.groundRing(mid.x, my + 0.05, mid.z, RowenaHouseLayout.ARENA_RADIUS - 0.6f, RowenaHouseLayout.ARENA_RADIUS, RED, 0.6f * a * urgency, true);
            }
        }
    }

    /** A letter of her spell rising off a bowl (violet; red at the end). */
    static final class Letter extends MagicEffect {
        private final String ch;
        private final float sway, rise, turn;
        private final boolean red;

        Letter(Vec3 at, String ch, float progress, boolean red) {
            super(at, School.WITCHCRAFT, 34);
            this.ch = ch;
            this.red = red;
            RandomSource r = RandomSource.create(seedOf(at) ^ ch.hashCode() ^ System.nanoTime());
            this.sway = r.nextFloat() * Mth.TWO_PI;
            this.rise = 1.2f + 0.8f * progress + r.nextFloat() * 0.3f;
            this.turn = r.nextBoolean() ? 1 : -1;
        }

        @Override
        protected double reach() {
            return 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        private Vec3 path(float t) {
            float k = Mth.clamp(t / life, 0, 1), r = 0.08f + 0.25f * k, ang = sway + turn * t * 0.15f;
            return new Vec3(at.x + Mth.cos(ang) * r, at.y + rise * easeOut(k), at.z + Mth.sin(ang) * r);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), k = progress(d), a = fade(t, 3, 14);
            Vec3 p = path(t);
            int glow = red ? RED : RECITE, ink = red ? 0xFFB0A0 : RECITE_HOT;
            d.glowDisc(p.x, p.y, p.z, 0.45f, glow, 0.25f * a);
            d.text(ch, p.x, p.y, p.z, 0.28f * (1 - 0.3f * k), Math.max(4, (int) (a * 255)) << 24 | ink);
        }
    }

    // --- P3 · a bowl breaking -------------------------------------------------------------------------------------------------------

    /** A bowl shattering: shards, its brew splashing out violet, its thread snapping back. */
    static final class Shatter extends MagicEffect {
        private final int rowena, left;
        private final double gy;
        private final long seed;

        Shatter(int rowena, Vec3 at, int left) {
            super(at, School.WITCHCRAFT, 26);
            this.rowena = rowena;
            this.left = left;
            this.gy = groundY(at);
            this.seed = seedOf(at);
        }

        @Override
        protected double reach() {
            return 14;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age != 0) return;
            RandomSource r = level.random;
            Vec3 p = at.add(0, 0.5, 0);
            MagicShake.kick(p, 0.35f, 6, 12);
            MagicSounds.play(SoundEvents.DECORATED_POT_SHATTER, p, 1.2f, 0.7f);
            MagicSounds.play(SoundEvents.AMETHYST_BLOCK_BREAK, p, 1f, 0.6f);
            for (int i = FxParticles.n(26); i > 0; i--) {
                FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DECORATED_POT.defaultBlockState()),
                        p.x, p.y, p.z, FxParticles.j(r) * 0.3, 0.2 + r.nextDouble() * 0.25, FxParticles.j(r) * 0.3);
            }
            for (int i = FxParticles.n(20); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.spark(level, i % 2 == 0 ? RECITE : RECITE_HOT, 0.6f, 16, p.x, p.y, p.z, Mth.cos(a) * 0.25, 0.1 + r.nextDouble() * 0.2,
                        Mth.sin(a) * 0.25);
            }
            for (int i = FxParticles.n(12); i > 0; i--) {
                FxParticles.plain(level, ParticleTypes.SPLASH, p.x + FxParticles.j(r) * 0.3, p.y, p.z + FxParticles.j(r) * 0.3,
                        FxParticles.j(r) * 0.2, 0.3, FxParticles.j(r) * 0.2);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), f = Mth.clamp(1 - t / 16f, 0, 1), a = fade(t, 0.5f, 14);
            d.star(at.x, gy + 0.5, at.z, 1.6f * f + 0.2f, 0.3f, RECITE_HOT, f);
            float k = easeOut(t / 10f);
            d.groundRing(at.x, gy + 0.04, at.z, 0.4f + k * 2.4f, 0.6f + k * 2.4f, RECITE, 0.8f * (1 - k), true);
            // The spilt brew, a stain that cools.
            d.flatGlow(at.x, gy + 0.03, at.z, 1.2f, RECITE, 0.5f * a);
            d.cracks(at.x, gy + 0.025, at.z, 6, 1.2f, 0.06f, seed, RECITE_HOT, 0.6f * a);
            // Its thread snapping back to her.
            Entity e = entity(rowena);
            if (e != null && t < 10) {
                Vec3 h = e.getPosition(d.partial).add(0, 1.3, 0), from = at.add(0, 0.55, 0);
                Vec3 mid = from.add(h).scale(0.5).add(0, 0.9, 0);
                float u = easeOut(t / 10f);
                thread(d, from, mid, h, u, 1, 0.03f, RECITE, 0xFFFFFF, 0.9f * (1 - u), t, 0);
            }
        }
    }

    // --- P3 · the end of the recitation -----------------------------------------------------------------------------------------

    /** The hex spoken: a red-and-magenta wave sweeping out through the whole arena from its centre. */
    static final class HexWave extends MagicEffect {
        private final int rowena;
        private final float radius, damage;
        private final double gy;

        HexWave(int rowena, Vec3 centre, float radius, float damage) {
            super(centre, School.WITCHCRAFT, 34);
            this.rowena = rowena;
            this.radius = radius;
            this.damage = damage;
            this.gy = groundY(centre);
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
            if (age == 0) MagicSounds.play(SoundEvents.WARDEN_SONIC_BOOM, at, 0.8f, 0.7f);
            if (age < 16) {
                float k = easeOut(age / 14f) * radius;
                for (int i = FxParticles.rate(r, 8f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double x = at.x + Mth.cos(a) * k, z = at.z + Mth.sin(a) * k;
                    FxParticles.ember(level, r.nextBoolean() ? RED : MAGENTA, 0.6f, 18, x, groundY(new Vec3(x, at.y, z)) + 0.2, z,
                            Mth.cos(a) * 0.05, 0.08, Mth.sin(a) * 0.05);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), k = easeOut(t / 14f), f = Mth.clamp(1 - (t - 10) / 24f, 0, 1);
            float rr = radius * k, power = Mth.clamp(damage / (RowenaBalance.WIPE_PER_BOWL * RowenaBalance.BOWLS), 0.3f, 1);
            d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, Math.max(0.2f, rr), 1.2f, 1.6f * power * f, RED, 0.85f * f);
            d.groundRing(at.x, gy + 0.06, at.z, Math.max(0, rr - 1.4f), rr, MAGENTA, 0.7f * f, true);
            d.flatGlow(at.x, gy + 0.04, at.z, Math.max(0.5f, rr), RED, 0.35f * f * (1 - k * 0.5f));
            d.glyphRing(MagicTextures.glyphStrip(), at.x, gy + 0.07, at.z, 0, 1, 0, Math.max(0, rr - 0.9f), Math.max(0.1f, rr - 0.2f), t * 0.05f,
                    CRIMSON_HOT, 0.8f * f);
            Entity e = entity(rowena);
            if (e != null && t < 12) {
                Vec3 p = e.getPosition(d.partial).add(0, 1.2, 0);
                d.star(p.x, p.y, p.z, 2.6f * (1 - t / 12f) + 0.3f, 0.3f, CRIMSON_HOT, 1 - t / 12f);
            }
        }
    }

    /**
     * Her fall out of the air, the bowls all broken: a violet burst as the spell fails, a crash of dust where she lands, then, while
     * she lies stunned, her spell's broken letters circling her head and a crack of violet light under her.
     */
    static final class Fall extends MagicEffect {
        private final int rowena;
        private Vec3 pos;
        private int landed = -1, endAt = -1;
        private final long seed;

        Fall(int rowena, Vec3 at) {
            super(at, School.WITCHCRAFT, RowenaBalance.STUN_TICKS + 80);
            this.rowena = rowena;
            this.pos = at;
            this.seed = seedOf(at);
        }

        boolean isDone() {
            return done();
        }

        @Override
        protected boolean done() {
            return super.done() || endAt >= 0 && age - endAt > 10;
        }

        @Override
        protected Vec3 anchor() {
            return pos;
        }

        @Override
        protected double reach() {
            return 5;
        }

        @Override
        protected void tick(ClientLevel level) {
            Entity e = entity(rowena);
            RandomSource r = level.random;
            if (age == 0) {
                MagicSounds.play(SoundEvents.AMETHYST_CLUSTER_BREAK, pos.add(0, 1, 0), 1.5f, 0.6f);
                for (int i = FxParticles.n(30); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, i % 2 == 0 ? RECITE : RECITE_HOT, 0.6f, 18, pos.x, pos.y + 1.2, pos.z, Mth.cos(a) * 0.25,
                            FxParticles.j(r) * 0.2, Mth.sin(a) * 0.25);
                }
            }
            if (e instanceof RowenaEntity rw) {
                pos = rw.position();
                if (landed < 0 && (rw.onGround() || age > 30)) {
                    landed = age;
                    double gy = groundY(pos);
                    MagicShake.kick(pos, 0.7f, 10, 16);
                    MagicSounds.play(SoundEvents.GENERIC_BIG_FALL, pos, 1.5f, 0.8f);
                    for (int i = FxParticles.n(30); i > 0; i--) {
                        float a = r.nextFloat() * Mth.TWO_PI;
                        double x = pos.x + Mth.cos(a) * 0.6, z = pos.z + Mth.sin(a) * 0.6;
                        FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, x, gy, z)), x, gy + 0.1, z,
                                Mth.cos(a) * 0.25, 0.15 + r.nextDouble() * 0.15, Mth.sin(a) * 0.25);
                    }
                    for (int i = FxParticles.n(10); i > 0; i--) {
                        float a = r.nextFloat() * Mth.TWO_PI;
                        FxParticles.plain(level, ParticleTypes.POOF, pos.x + Mth.cos(a) * 0.8, gy + 0.2, pos.z + Mth.sin(a) * 0.8, Mth.cos(a) * 0.08,
                                0.02, Mth.sin(a) * 0.08);
                    }
                    for (int i = FxParticles.n(8); i > 0; i--) {
                        FxParticles.plain(level, AllParticles.PAGE.get(), pos.x + FxParticles.j(r) * 0.8, pos.y + 1.4 + r.nextDouble() * 0.8,
                                pos.z + FxParticles.j(r) * 0.8, FxParticles.j(r) * 0.04, 0.0, FxParticles.j(r) * 0.04);
                    }
                }
                if (endAt < 0 && landed >= 0 && age - landed > 10 && !rw.stunned()) endAt = age;
            } else if (endAt < 0) {
                endAt = age;
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Entity e = entity(rowena);
            Vec3 p = e != null ? e.getPosition(d.partial) : pos;
            float a = Mth.clamp(t / 4f, 0, 1) * (endAt >= 0 ? 1 - Mth.clamp((age - endAt + d.partial) / 10f, 0, 1) : 1);
            if (t < 12) {
                float f = 1 - t / 12f;
                d.star(p.x, p.y + 1.2, p.z, 2.4f * f + 0.3f, 0.6f, RECITE_HOT, f);
            }
            if (landed < 0) return;
            float s = age - landed + d.partial;
            double gy = groundY(p);
            if (s < 12) {
                float k = easeOut(s / 10f), f = 1 - s / 12f;
                d.shockwave(MagicTextures.ENERGY_RING, p.x, gy + 0.05, p.z, 0.6f + 2.6f * k, 0.5f, 0.6f * f, 0xD8C8B0, 0.6f * f);
            }
            d.cracks(p.x, gy + 0.03, p.z, 8, 1.6f, 0.07f, seed, RECITE, 0.55f * a * (0.7f + 0.3f * Mth.sin(t * 0.4f)));
            d.darkRing(p.x, gy + 0.025, p.z, 0, 1.2f, 0x0C0610, 0.4f * a);
            // Stunned: her spell's letters, broken, circling her head.
            Vec3 head = p.add(0, (e != null ? e.getBbHeight() : 1.9) + 0.25, 0);
            String abc = "RWMXQ";
            for (int i = 0; i < 5; i++) {
                float ang = t * 0.12f + i * Mth.TWO_PI / 5;
                double x = head.x + Mth.cos(ang) * 0.45, z = head.z + Mth.sin(ang) * 0.45, y = head.y + 0.06 * Mth.sin(t * 0.3f + i);
                d.glowDisc(x, y, z, 0.16f, RECITE, 0.4f * a);
                d.text(String.valueOf(abc.charAt(i)), x, y, z, 0.16f, Math.max(4, (int) (a * 230)) << 24 | RECITE_HOT);
            }
        }
    }
}
