package org.papiricoh.supernaturalcraft.client.abaddon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxColor;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicSounds;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicTextures;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonBalance;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Abaddon's fight in the world (v0.21), on the magic's effect kit ({@link MagicEffect}, {@link FxDraw}): each
 * {@code AbaddonFxPayload} moment over its telegraph. Her identity: black smoke with crimson glints in it, hellfire, the Brand of the
 * Knights ({@link KnightsSeal}).
 * <ul>
 *   <li>{@link HeraldMark}: the Brand burning itself in over the marked hunter's head, a thread of red light from the herald's
 *   bell (or her pointing hand) to it, the bell's toll rippling out; when the mark holds, the Brand drops onto the circle and
 *   lies there, charging;</li>
 *   <li>{@link MarkStrike}: a pillar of hellfire slams down into the circle, the Brand flares white-hot, flames and a scorch;</li>
 *   <li>{@link Sweep}: her arm's sweep drawn as a wall of fire running across the cone, then hellfire rolling out through it in
 *   billows and burning on;</li>
 *   <li>{@link Summon}: the floor cracks crimson and a knight (or her herald) rises out of it in a burst of smoke and embers (the
 *   renderer lifts it out of the ground);</li>
 *   <li>{@link SmokeOut}: a lean, translucent serpent of black smoke with crimson glints pours out of her and rushes down the yellow lane into
 *   the knight's helm; {@link Possess}: the smoke spirals into the helm and the visor blazes;</li>
 *   <li>{@link Reform}: she leaves a knight, by her own will or forced out, each way its own: an exorcism tears the smoke out
 *   upward with gold, Banish by Name stamps a seal down on it and drags it away, salt makes it recoil from a burst of grains, a
 *   devil's trap flares a red pentagram and pulls it down, a dying knight bursts with it; then the smoke coils and condenses into
 *   her body, and {@link Stun} hangs a cracked crimson aura on her for the ×2 window; with its caster known ({@code EXPELLED_BY})
 *   the tear-out recoils away from them and a {@link Streak} in the method's colour runs from them to the knight;</li>
 *   <li>{@link Pin}: violet chains of force from the floor round the hunter, rings of distortion, a thread from her fist;</li>
 *   <li>{@link Brand}: the Brand burnt into the floor under her, the rings' bands smouldering (the green gaps lit), then the
 *   burst: a shockwave and fire along every band but the gaps;</li>
 *   <li>{@link Trapped}: the devil's trap's pentagram flaring red under her, a cage of red light, her smoke dragged down into it;
 *   {@link TrapFlare}: the pentagram shattering in a crimson burst;</li>
 *   <li>{@link Cleave}: the Knights' Cleaver's wave: a wall of hellfire running down the lane, the ground burning behind it;</li>
 *   <li>{@link Crumble}: a knight crumbling to ash as she dies; {@link Death}: she burns from inside, goes to smoke and is sucked
 *   down into the earth with a scream; the Book of the Damned is left with a dark glow.</li>
 * </ul>
 */
public final class AbaddonWorldFx {

    static final int SMOKE = FxColor.SMOKE, SMOKE_RED = 0x260708, CRIMSON = 0xB0101A, CRIMSON_HOT = 0xFF3A2A, HELLFIRE = 0xFF5A1E,
            HELL_HOT = 0xFFD08A, EMBER = 0xFF6A2A, GOLD = 0xFFD27A, GOLD_HOT = 0xFFF2C0, SALT = 0xF4F0E6, VIOLET = 0xB36BFF,
            VIOLET_HOT = 0xEAD6FF, SAFE = 0x6CFF8E, TRAP = FxColor.TRAP, ASH_DARK = 0x2A2624, SCORCH = 0x140806;

    /** The traps holding her now (a flare ends them). */
    private static final List<Trapped> TRAPS = new ArrayList<>();
    /** The expulsions showing now (their expeller arrives in a payload of its own). */
    private static final List<Reform> REFORMS = new ArrayList<>();

    private AbaddonWorldFx() {
    }

    // --- starting them (ClientAbaddonFx and the previews) --------------------------------------------------------------------

    public static void heraldMark(int marker, int victim, float radius, Vec3 start, int ticks) {
        MagicFxManager.add(new HeraldMark(marker, victim, Math.max(1, radius), start, ticks));
    }

    public static void markStrike(Vec3 spot, float radius, boolean hers, int ticks) {
        MagicFxManager.add(new MarkStrike(spot, Math.max(1, radius), hers, ticks));
    }

    public static void sweep(Vec3 apex, float yaw, float reach, int ticks) {
        MagicFxManager.add(new Sweep(apex, yaw, Math.max(2, reach), ticks));
    }

    public static void summon(int id, boolean herald, Vec3 at, int ticks) {
        MagicFxManager.add(new Summon(at, herald, ticks));
    }

    public static void smokeOut(int abaddon, int knight, Vec3 chest, int flight) {
        MagicFxManager.add(new SmokeOut(knight, chest, flight));
    }

    public static void possess(int knight, Vec3 at, int ticks) {
        MagicFxManager.add(new Possess(knight, at));
    }

    /** She leaves knight {@code knight} (at {@code knightAt}) of her own will, back into her body at {@code her}. */
    public static void release(int abaddon, int knight, Vec3 knightAt, Vec3 her) {
        ClientAbaddonFx.reformed(abaddon);
        MagicFxManager.add(new Reform(abaddon, knightAt, her, -1, 0));
    }

    /** She is cast out of a knight ({@code how}: {@code AbaddonFxPayload.BY_*}), stunned for {@code stun} ticks. */
    public static void expel(int abaddon, int knight, Vec3 knightAt, int how, Vec3 her, int stun) {
        ClientAbaddonFx.reformed(abaddon);
        Reform r = new Reform(abaddon, knightAt, her, how, stun);
        REFORMS.removeIf(Reform::isDone);
        REFORMS.add(r);
        MagicFxManager.add(r);
        if (stun > 0) MagicFxManager.add(new Stun(abaddon, her, stun));
    }

    /**
     * Who cast her out ({@code EXPELLED_BY}, sent right after the {@code EXPEL} it explains): the tear-out of that expulsion recoils
     * away from {@code by}, and a streak in the method's colour runs from them to the knight. With no expulsion showing (a late
     * packet), a small recoil of its own on the line from them to her.
     */
    public static void expelledBy(int abaddon, int how, Vec3 by) {
        Reform found = null;
        for (int i = REFORMS.size() - 1; i >= 0 && found == null; i--) {
            Reform r = REFORMS.get(i);
            if (r.abaddon == abaddon && !r.isDone() && r.ticks() < 16) found = r;
        }
        Vec3 target;
        if (found != null) {
            found.expeller(by);
            target = found.head();
        } else {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(abaddon);
            target = e != null ? e.position().add(0, 1.2, 0) : by.add(0, 1.2, 0);
        }
        MagicFxManager.add(new Streak(by, target, how, found == null));
    }

    public static void pin(int abaddon, int target, Vec3 hold, int ticks) {
        MagicFxManager.add(new Pin(abaddon, target, hold, ticks));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == target) MagicOverlay.flash(hold, VIOLET, 0.3f, 6);
    }

    public static void brand(int abaddon, int rings, int gaps, Vec3 centre, int show) {
        MagicFxManager.add(new Brand(rings, centre, show));
    }

    public static void trapped(int abaddon, Vec3 centre, int ticks) {
        Trapped t = new Trapped(abaddon, centre, ticks);
        TRAPS.removeIf(Trapped::finished);
        TRAPS.add(t);
        MagicFxManager.add(t);
    }

    public static void trapFlare(int abaddon, Vec3 centre, int ticks) {
        for (Trapped t : TRAPS) t.close();
        TRAPS.clear();
        MagicFxManager.add(new TrapFlare(centre, ticks));
    }

    public static void cleave(int wielder, float yaw, float reach, Vec3 from, int ticks) {
        MagicFxManager.add(new Cleave(from, yaw, Math.max(2, reach), ticks));
    }

    /** A knight (or herald) of hers crumbling to ash where it stood. */
    public static void crumble(Vec3 pos, float yaw, float height, boolean herald) {
        MagicFxManager.add(new Crumble(pos, height));
    }

    public static void death(int abaddon, Vec3 at, int ticks) {
        MagicFxManager.add(new Death(abaddon, at, ticks));
    }

    static void clear() {
        TRAPS.clear();
        REFORMS.clear();
    }

    // --- shared ----------------------------------------------------------------------------------------------------------------

    private static double groundY(Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? at.y : FxWorld.floor(mc.level, at.add(0, 1.5, 0), 6);
    }

    private static BlockState ground(ClientLevel level, double x, double y, double z) {
        BlockState s = level.getBlockState(BlockPos.containing(x, y - 0.5, z));
        return s.isAir() ? Blocks.BLACKSTONE.defaultBlockState() : s;
    }

    /** A small cheap random fixed by a seed. */
    static float hash(long seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    private static long seedOf(Vec3 at) {
        return Double.doubleToLongBits(at.x * 31.7 + at.z * 17.3 + at.y);
    }

    /** The Brand laid flat on the ground at {@code c}, its top toward {@code yaw}, {@code size} blocks across. */
    static void sealGround(FxDraw d, Vec3 c, double y, float yaw, float size, float fraction, float width, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        float fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float rx = -fz, rz = fx;
        KnightsSeal.walk(fraction, (x0, y0, x1, y1) -> d.groundLine(c.x + rx * x0 * size - fx * y0 * size, c.z + rz * x0 * size - fz * y0 * size,
                c.x + rx * x1 * size - fx * y1 * size, c.z + rz * x1 * size - fz * y1 * size, y, width, rgb, alpha));
    }

    /** The Brand standing in the air at {@code c}, facing the camera, {@code size} blocks across. */
    static void sealAir(FxDraw d, Vec3 c, float size, float fraction, float width, int rgb, float alpha, boolean core) {
        if (alpha <= 0.004f) return;
        Vector3f l = d.camera.getLeftVector(), u = d.camera.getUpVector();
        KnightsSeal.walk(fraction, (x0, y0, x1, y1) -> {
            float ax = -x0 * size, ay = -y0 * size, bx = -x1 * size, by = -y1 * size;
            d.softBeam(c.x + l.x * ax + u.x * ay, c.y + l.y * ax + u.y * ay, c.z + l.z * ax + u.z * ay,
                    c.x + l.x * bx + u.x * by, c.y + l.y * bx + u.y * by, c.z + l.z * bx + u.z * by, width, rgb, alpha, alpha, core);
        });
    }

    /** A devil's trap's pentagram on the ground: the ring, the star, and an inner ring. */
    static void pentagram(FxDraw d, double x, double y, double z, float r, float spin, float width, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        d.groundRing(x, y, z, r - width, r, rgb, alpha, false);
        d.groundRing(x, y, z, r * 0.42f - width * 0.6f, r * 0.42f, rgb, alpha * 0.7f, false);
        for (int i = 0; i < 5; i++) {
            float a0 = spin + i * Mth.TWO_PI / 5, a1 = spin + (i + 2) * Mth.TWO_PI / 5;
            float rr = r - width * 0.5f;
            d.groundLine(x + Mth.cos(a0) * rr, z + Mth.sin(a0) * rr, x + Mth.cos(a1) * rr, z + Mth.sin(a1) * rr, y, width, rgb, alpha);
        }
    }

    /** A flame tongue standing on the ground: a soft wedge of fire with a hot core, flickering. */
    static void flame(FxDraw d, double x, double y, double z, float height, float width, float flicker, float alpha) {
        if (alpha <= 0.004f || height <= 0.05f) return;
        float h = height * (0.85f + 0.15f * flicker);
        double sway = 0.08 * height * flicker;
        d.taper(x, y, z, x + sway, y + h, z, width, 0, HELLFIRE, 0.75f * alpha, 0);
        d.taper(x, y, z, x + sway * 0.6, y + h * 0.62, z, width * 0.42f, 0, HELL_HOT, 0.85f * alpha, 0);
    }

    /** A chain of links from a to b (a link's ring lies along the chain, every other one turned a quarter). */
    static void chain(FxDraw d, Vec3 a, Vec3 b, float link, int rgb, float alpha, float shimmer) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 0.05 || alpha <= 0.004f) return;
        dir = dir.scale(1 / len);
        float[] p = new float[3], q = new float[3];
        FxDraw.basis((float) dir.x, (float) dir.y, (float) dir.z, p, q);
        int n = Math.max(1, (int) (len / (link * 1.6)));
        double step = len / n;
        for (int i = 0; i < n; i++) {
            Vec3 c = a.add(dir.scale((i + 0.5) * step));
            float[] nrm = i % 2 == 0 ? p : q;
            float sh = 0.75f + 0.25f * Mth.sin(shimmer - i * 0.6f);
            d.ring(c.x, c.y, c.z, nrm[0], nrm[1], nrm[2], link * 0.62f, link, rgb, alpha * sh, false);
        }
        d.softBeam(a.x, a.y, a.z, b.x, b.y, b.z, link * 1.4f, rgb, 0.3f * alpha, 0.3f * alpha, false);
    }

    /**
     * A wisp of black smoke with a faint crimson heart: translucent (never a solid blot), and thinning to nothing as it nears the eye
     * (within {@link #NEAR} blocks it is gone, so smoke drifting past the camera never darkens the view).
     */
    static void smoke(FxDraw d, double x, double y, double z, float r, float alpha) {
        if (alpha <= 0.004f) return;
        Vec3 eye = d.eye();
        double dx = x - eye.x, dy = y - eye.y, dz = z - eye.z;
        float near = Mth.clamp(((float) Math.sqrt(dx * dx + dy * dy + dz * dz) - NEAR - r * 0.5f) / 2f, 0, 1);
        alpha *= 0.62f * near * near * (3 - 2 * near);
        if (alpha <= 0.004f) return;
        d.darkDisc(x, y, z, r, SMOKE, alpha);
        d.darkDisc(x, y, z, r * 0.5f, SMOKE_RED, alpha * 0.55f);
    }

    /** Smoke within this many blocks of the eye is not drawn (it fades back in over the next two). */
    static final float NEAR = 1.2f;

    /** A crimson glint in the smoke: a small hot point that comes and goes. */
    static void glint(FxDraw d, double x, double y, double z, float r, float t, float phase, float alpha) {
        float g = Mth.sin(t * 0.9f + phase);
        if (g <= 0.2f || alpha <= 0.004f) return;
        float k = (g - 0.2f) / 0.8f;
        d.glowDisc(x, y, z, r, CRIMSON_HOT, 0.75f * alpha * k);
        d.glowDisc(x, y, z, r * 0.4f, HELL_HOT, 0.5f * alpha * k);
    }

    /** The point on a quadratic curve a → c → b at u. */
    static Vec3 bezier(Vec3 a, Vec3 c, Vec3 b, double u) {
        double v = 1 - u;
        return a.scale(v * v).add(c.scale(2 * v * u)).add(b.scale(u * u));
    }

    /**
     * A serpent of smoke along the curve a → c → b: its head at {@code head} (0-1 of the way), its body {@code length} long behind
     * it, writhing; crimson glints along it and two at its head. {@code alpha} fades it all.
     */
    static void serpent(FxDraw d, Vec3 a, Vec3 c, Vec3 b, float head, float length, float thick, float t, float alpha, boolean eyes) {
        if (alpha <= 0.004f) return;
        int n = 22;
        Vec3 up = new Vec3(0, 1, 0);
        for (int i = 0; i <= n; i++) {
            float v = i / (float) n;
            double u = head - length * (1 - v);
            if (u < 0) continue;
            Vec3 p = bezier(a, c, b, u), q = bezier(a, c, b, Math.min(1, u + 0.02));
            Vec3 dir = q.subtract(p);
            Vec3 side = dir.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : dir.cross(up);
            side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
            float w = Mth.sin(v * 9 - t * 0.8f) * 0.28f * (1 - v * 0.6f), h = Mth.cos(v * 7 - t * 0.6f) * 0.16f;
            Vec3 at = p.add(side.scale(w)).add(0, h, 0);
            float r = thick * (0.3f + 0.7f * v);
            smoke(d, at.x, at.y, at.z, r, alpha * (0.45f + 0.5f * v));
            if (i % 3 == 1) {
                float g = 0.5f + 0.5f * Mth.sin(t * 1.3f + i * 2.1f);
                d.glowDisc(at.x, at.y, at.z, r * 0.35f, CRIMSON_HOT, 0.55f * alpha * g);
            }
            if (i == n && eyes) {
                Vec3 e1 = at.add(side.scale(0.13)).add(0, 0.05, 0), e2 = at.subtract(side.scale(0.13)).add(0, 0.05, 0);
                d.glowDisc(e1.x, e1.y, e1.z, 0.09f, CRIMSON_HOT, 0.95f * alpha);
                d.glowDisc(e2.x, e2.y, e2.z, 0.09f, CRIMSON_HOT, 0.95f * alpha);
            }
        }
        // A faint crimson vein down its middle, so it reads in the dark.
        int m = 8;
        Vec3 prev = null;
        for (int i = 0; i <= m; i++) {
            double u = head - length * (1 - i / (double) m);
            if (u < 0) continue;
            Vec3 p = bezier(a, c, b, u);
            if (prev != null) d.softBeam(prev.x, prev.y, prev.z, p.x, p.y, p.z, thick * 0.5f, CRIMSON, 0.18f * alpha, 0.25f * alpha, false);
            prev = p;
        }
    }

    /** Where a knight's helm is (its visor, or above its feet). */
    static Vec3 helm(Entity e, float partial, Vec3 fallback) {
        if (e == null || e.isRemoved()) return fallback.add(0, 1.85, 0);
        Vec3 v = AbaddonRenderers.bone(e, AbaddonRenderers.Bone.VISOR, partial);
        return v != null ? v : e.getPosition(partial).add(0, e.getBbHeight() * 0.83, 0);
    }

    // --- the herald's mark --------------------------------------------------------------------------------------------------------

    /** The Brand over the marked hunter's head, then on the circle, charging. */
    static final class HeraldMark extends MagicEffect {
        private final int marker, victim, windup, follow;
        private final float radius;
        private Vec3 spot;
        private double gy;
        private Vec3 lastHead;

        HeraldMark(int marker, int victim, float radius, Vec3 start, int ticks) {
            super(start, School.INFERNAL, Math.max(10, ticks) + 6);
            this.marker = marker;
            this.victim = victim;
            this.radius = radius;
            this.windup = Math.max(10, ticks);
            this.follow = Math.round(windup * AbaddonBalance.MARK_FOLLOW / (float) (AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD));
            this.spot = start;
            this.gy = groundY(start);
            this.lastHead = start.add(0, 2.6, 0);
        }

        @Override
        protected double reach() {
            return radius + 5;
        }

        @Override
        protected Vec3 anchor() {
            return spot;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private Vec3 head(float partial) {
            Entity e = entity(victim);
            if (e != null && !e.isRemoved()) lastHead = e.getPosition(partial).add(0, e.getBbHeight() + 0.8, 0);
            return lastHead;
        }

        /** Where the thread comes from: the herald's bell, or her right hand. */
        private Vec3 source(float partial) {
            Entity e = entity(marker);
            if (e == null) return null;
            Vec3 b = AbaddonRenderers.bone(e, e instanceof AbaddonEntity ? AbaddonRenderers.Bone.HAND : AbaddonRenderers.Bone.BELL, partial);
            return b != null ? b : e.getPosition(partial).add(0, e.getBbHeight() * 0.7, 0);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Entity v = entity(victim);
            if (age < follow && v != null && !v.isRemoved() && age % 2 == 0) {
                spot = v.position();
                gy = groundY(spot);
            }
            if (age == follow) {
                // The second toll: the mark holds where it is.
                MagicSounds.play(AllSounds.abaddon("herald_mark"), spot, 1.1f, 0.72f);
            }
            for (int i = FxParticles.rate(r, 1.0f + 2.0f * age / windup); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.ember(level, r.nextBoolean() ? HELLFIRE : CRIMSON, 0.55f, 18, spot.x + Mth.cos(a) * radius, gy + 0.05,
                        spot.z + Mth.sin(a) * radius, 0, 0.05 + r.nextDouble() * 0.04, 0);
            }
            if (age < follow && r.nextFloat() < 0.5f) {
                Vec3 h = lastHead;
                FxParticles.ember(level, CRIMSON_HOT, 0.4f, 14, h.x + FxParticles.j(r) * 0.3, h.y - 0.3, h.z + FxParticles.j(r) * 0.3, 0, -0.04, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 3, 6), k = Mth.clamp(t / windup, 0, 1);
            float pulse = 0.75f + 0.25f * Mth.sin(t * (0.35f + 0.5f * k));
            // Over the head: the Brand burning itself in; once the mark holds, it drops onto the circle.
            Vec3 h = head(d.partial);
            float drop = t < follow ? 0 : easeOut((t - follow) / 7f);
            if (drop < 1) {
                Vec3 g = new Vec3(spot.x, gy + 0.3, spot.z);
                Vec3 at = h.lerp(g, drop * drop);
                float size = 0.85f + drop * radius * 0.8f, drawn = Mth.clamp(t / 10f, 0, 1), fa = a * (1 - drop);
                d.glowDisc(at.x, at.y, at.z, size * 1.3f, CRIMSON, 0.35f * fa * pulse);
                sealAir(d, at, size, drawn, size * 0.11f, CRIMSON, 0.85f * fa * pulse, false);
                sealAir(d, at, size, drawn, size * 0.04f, HELL_HOT, 0.95f * fa, true);
            }
            // The thread from the herald's bell (or her hand), as the mark is laid.
            Vec3 s = source(d.partial);
            if (s != null && t < 16) {
                float ta = a * (1 - t / 16f);
                d.softBeam(s.x, s.y, s.z, h.x, h.y, h.z, 0.12f, CRIMSON, 0.55f * ta, 0.25f * ta, true);
                // The toll rippling out of the bell.
                for (int i = 0; i < 2; i++) {
                    float u = (t - i * 5) / 12f;
                    if (u < 0 || u > 1) continue;
                    d.ring(s.x, s.y, s.z, 0, 1, 0, 0.2f + u * 1.6f, 0.3f + u * 1.7f, GOLD, 0.6f * (1 - u) * a, true);
                }
            }
            // On the ground: the circle smouldering, the Brand on it once it holds, burning brighter to the strike.
            d.flatGlow(spot.x, gy + 0.04, spot.z, radius * 1.2f, HELLFIRE, (0.12f + 0.3f * k) * a * pulse);
            d.spikedRing(spot.x, gy + 0.05, spot.z, radius - 0.15f, 0.3f, 10, t * 0.04f, CRIMSON, 0.55f * a);
            if (t >= follow) {
                float drawn = Mth.clamp((t - follow) / 8f, 0, 1);
                sealGround(d, spot, gy + 0.05, 0, radius * 1.6f, drawn, 0.32f, CRIMSON, 0.8f * a * pulse);
                sealGround(d, spot, gy + 0.06, 0, radius * 1.6f, drawn, 0.1f, HELL_HOT, 0.9f * a);
            }
        }
    }

    // --- the mark's strike --------------------------------------------------------------------------------------------------------

    /** A pillar of hellfire into the circle, the Brand white-hot, flames, a scorch. */
    static final class MarkStrike extends MagicEffect {
        private final float radius;
        private final boolean hers;
        private final double gy;
        private final long seed;

        MarkStrike(Vec3 spot, float radius, boolean hers, int ticks) {
            super(spot, School.INFERNAL, Math.max(16, ticks) + 30);
            this.radius = radius;
            this.hers = hers;
            this.gy = groundY(spot);
            this.seed = seedOf(spot);
        }

        @Override
        protected double reach() {
            return radius + 9;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 2) {
                MagicShake.kick(at, hers ? 0.7f : 0.4f, 10, radius + 12);
                MagicOverlay.flash(at, HELLFIRE, hers ? 0.3f : 0.18f, radius + 8);
                for (int i = FxParticles.n(30); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, d = radius * Mth.sqrt(r.nextFloat());
                    FxParticles.plain(level, ParticleTypes.FLAME, at.x + Mth.cos(a) * d, gy + 0.1, at.z + Mth.sin(a) * d, Mth.cos(a) * 0.08,
                            0.1 + r.nextDouble() * 0.15, Mth.sin(a) * 0.08);
                }
                for (int i = FxParticles.n(10); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x + FxParticles.j(r) * radius * 0.6, gy + 0.4,
                            at.z + FxParticles.j(r) * radius * 0.6, 0, 0.12, 0);
                }
                for (int i = FxParticles.n(14); i > 0; i--) {
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at.x, gy, at.z)),
                            at.x + FxParticles.j(r) * radius, gy + 0.1, at.z + FxParticles.j(r) * radius, 0, 0.2, 0);
                }
            }
            if (age > 2 && age < 18) {
                for (int i = FxParticles.rate(r, 2.5f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, d = radius * Mth.sqrt(r.nextFloat());
                    FxParticles.ember(level, r.nextBoolean() ? HELLFIRE : HELL_HOT, 0.7f, 20, at.x + Mth.cos(a) * d, gy + 0.2,
                            at.z + Mth.sin(a) * d, 0, 0.08 + r.nextDouble() * 0.08, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            // The pillar slamming down.
            if (t < 7) {
                float k = Mth.clamp(t / 2.5f, 0, 1), fa = 1 - Mth.clamp((t - 2) / 5f, 0, 1);
                double top = gy + 10, bottom = top - 10 * k;
                d.softBeam(at.x, top, at.z, at.x, bottom, at.z, radius * (hers ? 1.1f : 0.8f), HELLFIRE, 0.25f * fa, 0.9f * fa, true);
                d.softBeam(at.x, top, at.z, at.x, bottom, at.z, radius * 0.3f, HELL_HOT, 0.4f * fa, 1f * fa, true);
            }
            if (t < 2) return;
            float s = t - 2, w = Mth.clamp(s / 8f, 0, 1), fa = 1 - Mth.clamp((s - 3) / 16f, 0, 1);
            if (s < 10) {
                d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, radius * (0.4f + 0.9f * easeOut(w)), 0.8f, 1.6f * (1 - w) + 0.2f,
                        HELLFIRE, 0.9f * (1 - w * 0.7f));
            }
            if (s < 5) d.star(at.x, gy + 1.2, at.z, radius * 2.2f * (1 - s / 5f) + 0.5f, 0.3f, HELL_HOT, 1 - s / 5f);
            d.flatGlow(at.x, gy + 0.04, at.z, radius * 1.3f, HELLFIRE, 0.6f * fa);
            sealGround(d, at, gy + 0.06, 0, radius * 1.6f, 1, 0.22f, 0xFFF0D8, 0.9f * fa);
            // Flame tongues across the circle, dying down.
            float life = 1 - Mth.clamp((s - 2) / 20f, 0, 1);
            for (int i = 0; i < 14; i++) {
                float ang = hash(seed, i) * Mth.TWO_PI, dist = radius * Mth.sqrt(hash(seed, i + 40));
                float flick = Mth.sin(t * 0.9f + i * 1.7f);
                flame(d, at.x + Mth.cos(ang) * dist, gy, at.z + Mth.sin(ang) * dist, (0.8f + 1.2f * hash(seed, i + 80)) * life, 0.32f, flick,
                        life);
            }
            // The scorch left behind and its embers' cracks.
            float sc = fade(t, 4, 18);
            d.darkRing(at.x, gy + 0.03, at.z, 0, radius * 1.05f, SCORCH, 0.6f * sc);
            d.cracks(at.x, gy + 0.04, at.z, 8, radius * 1.2f, 0.08f, seed, CRIMSON_HOT, 0.6f * sc);
        }
    }

    // --- the hellfire sweep -------------------------------------------------------------------------------------------------------

    /** A wall of fire running across the cone with her arm, then hellfire rolling out through it. */
    static final class Sweep extends MagicEffect {
        private static final int RUN = 6, FLAMES = 52;
        private final float yaw, reach;
        private final int linger;
        private final double gy;
        private final long seed;

        Sweep(Vec3 apex, float yaw, float reach, int ticks) {
            super(apex, School.INFERNAL, RUN + Math.max(10, ticks) + 16);
            this.yaw = yaw;
            this.reach = reach;
            this.linger = Math.max(10, ticks);
            this.gy = groundY(apex);
            this.seed = seedOf(apex);
        }

        @Override
        protected double reach() {
            return reach + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        /** The direction at {@code off} degrees from the cone's axis. */
        private double[] dir(float off) {
            float a = (yaw + off) * Mth.DEG_TO_RAD;
            return new double[]{-Mth.sin(a), Mth.cos(a)};
        }

        /** The sweep's front, in degrees off the axis: from her left to her right. */
        private float front(float t) {
            return -60 + 120 * Mth.clamp(t / RUN, 0, 1);
        }

        private float dying(float t) {
            return 1 - Mth.clamp((t - RUN - linger) / 16f, 0, 1);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 1) {
                MagicShake.kick(at, 0.4f, 8, reach + 8);
                MagicOverlay.flash(at, HELLFIRE, 0.2f, reach + 4);
            }
            if (age > RUN + linger) return;
            float lit = front(age);
            for (int i = FxParticles.rate(r, 5f); i > 0; i--) {
                float off = -60 + r.nextFloat() * (lit + 60);
                double[] dv = dir(off);
                double dist = 0.8 + (reach - 0.8) * Math.sqrt(r.nextDouble());
                double x = at.x + dv[0] * dist, z = at.z + dv[1] * dist;
                if (i % 3 == 0) FxParticles.plain(level, ParticleTypes.FLAME, x, gy + 0.15, z, 0, 0.06 + r.nextDouble() * 0.06, 0);
                else FxParticles.ember(level, r.nextBoolean() ? HELLFIRE : EMBER, 0.7f, 22, x, gy + 0.2, z, dv[0] * 0.03, 0.08, dv[1] * 0.03);
            }
            if (r.nextFloat() < 0.5f) {
                double[] dv = dir(-60 + r.nextFloat() * (lit + 60));
                double dist = reach * r.nextDouble();
                FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x + dv[0] * dist, gy + 1.2, at.z + dv[1] * dist, 0, 0.06, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), lit = front(t), die = dying(t);
            float a = Mth.clamp(t / 2f, 0, 1) * die;
            if (a <= 0.01f) return;
            // The swept ground glowing: arcs at several depths, as far round as the sweep has come.
            for (int ring = 1; ring <= 4; ring++) {
                float rr = reach * ring / 4.4f;
                int segs = 10;
                for (int s = 0; s < segs; s++) {
                    float o0 = -60 + 120f * s / segs, o1 = -60 + 120f * (s + 1) / segs;
                    if (o0 > lit) break;
                    o1 = Math.min(o1, lit);
                    double[] d0 = dir(o0), d1 = dir(o1);
                    d.groundLine(at.x + d0[0] * rr, at.z + d0[1] * rr, at.x + d1[0] * rr, at.z + d1[1] * rr, gy + 0.05, 1.3f, HELLFIRE,
                            0.22f * a);
                }
            }
            // The running wall of fire at the front while her arm sweeps.
            if (t < RUN + 3) {
                float wa = 1 - Mth.clamp((t - RUN) / 3f, 0, 1);
                double[] dv = dir(lit);
                for (int j = 1; j <= 9; j++) {
                    double dist = reach * j / 9.0;
                    float flick = Mth.sin(t * 1.4f + j * 1.9f);
                    flame(d, at.x + dv[0] * dist, gy, at.z + dv[1] * dist, 1.6f + 0.8f * (1 - j / 9f), 0.6f, flick, wa);
                }
                d.softBeam(at.x, gy + 0.5, at.z, at.x + dv[0] * reach, gy + 0.5, at.z + dv[1] * reach, 1.2f, HELLFIRE, 0.6f * wa, 0.3f * wa, true);
            }
            // Fire burning on where it has passed, and billows rolling out from her through the cone.
            for (int i = 0; i < FLAMES; i++) {
                float off = -58 + 116 * hash(seed, i);
                if (off > lit) continue;
                float litAt = RUN * (off + 60) / 120f, since = t - litAt;
                double[] dv = dir(off);
                double dist = 0.8 + (reach - 0.8) * Math.sqrt(hash(seed, i + 100));
                float grow = Mth.clamp(since / 3f, 0, 1), flick = Mth.sin(t * 0.8f + i * 2.3f);
                flame(d, at.x + dv[0] * dist, gy, at.z + dv[1] * dist, (0.6f + 1.0f * hash(seed, i + 200)) * grow * die, 0.38f, flick, a);
            }
            for (int k = 0; k < 5; k++) {
                float u = (t * 0.07f + k / 5f) % 1f;
                double dist = 0.8 + u * (reach - 0.8);
                float ba = Mth.sin(Mth.PI * u) * a;
                for (int s = 0; s < 7; s++) {
                    float off = -54 + 108 * s / 6f + 6 * Mth.sin(t * 0.2f + s);
                    if (off > lit) continue;
                    double[] dv = dir(off);
                    double x = at.x + dv[0] * dist, z = at.z + dv[1] * dist;
                    d.glowDisc(x, gy + 0.4 + u * 0.6, z, 0.7f + u * 0.5f, HELLFIRE, 0.28f * ba);
                    smoke(d, x, gy + 1.3 + u * 1.4, z, 0.5f + u * 0.7f, 0.3f * ba);
                }
            }
            d.flatGlow(at.x, gy + 0.04, at.z, 1.6f, HELL_HOT, 0.5f * a * (1 - Mth.clamp(t / 12f, 0, 1)));
        }
    }

    // --- the retinue rising -------------------------------------------------------------------------------------------------------

    /** The floor cracking crimson under a knight rising out of it, smoke and embers bursting up. */
    static final class Summon extends MagicEffect {
        private final boolean herald;
        private final double gy;
        private final long seed;

        Summon(Vec3 at, boolean herald, int ticks) {
            super(at, School.INFERNAL, Math.max(30, ticks) + 20);
            this.herald = herald;
            this.gy = groundY(at);
            this.seed = seedOf(at);
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
            RandomSource r = level.random;
            if (age == 0) {
                MagicSounds.play(AllSounds.abaddon("smoke"), at, 0.8f, 1.35f);
                for (int i = FxParticles.n(18); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), at.x + FxParticles.j(r) * 0.6, gy + 0.2, at.z + FxParticles.j(r) * 0.6,
                            FxParticles.j(r) * 0.04, 0.12 + r.nextDouble() * 0.2, FxParticles.j(r) * 0.04);
                }
                for (int i = FxParticles.n(16); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.ember(level, r.nextBoolean() ? HELLFIRE : CRIMSON_HOT, 0.7f, 24, at.x, gy + 0.2, at.z, Mth.cos(a) * 0.08,
                            0.2 + r.nextDouble() * 0.15, Mth.sin(a) * 0.08);
                }
                for (int i = FxParticles.n(12); i > 0; i--) {
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at.x, gy, at.z)),
                            at.x + FxParticles.j(r) * 0.9, gy + 0.1, at.z + FxParticles.j(r) * 0.9, 0, 0.15, 0);
                }
            }
            if (age < ClientAbaddonFx.RISE_TICKS + 4) {
                for (int i = FxParticles.rate(r, 2.5f); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), at.x + FxParticles.j(r) * 0.7, gy + 0.1, at.z + FxParticles.j(r) * 0.7,
                            0, 0.08 + r.nextDouble() * 0.08, 0);
                }
                if (r.nextFloat() < 0.6f) {
                    FxParticles.ember(level, HELLFIRE, 0.6f, 20, at.x + FxParticles.j(r) * 0.8, gy + 0.1, at.z + FxParticles.j(r) * 0.8, 0, 0.1, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 0, 20), open = easeOut(t / 6f);
            d.cracks(at.x, gy + 0.03, at.z, 9, 1.9f * open, 0.13f, seed, CRIMSON_HOT, 0.85f * a);
            d.darkRing(at.x, gy + 0.025, at.z, 0, 0.95f * open, 0x0A0404, 0.7f * a);
            d.flatGlow(at.x, gy + 0.04, at.z, 1.7f, herald ? HELLFIRE : CRIMSON, 0.5f * a);
            d.glowDisc(at.x, gy + 0.3, at.z, 1.2f, HELLFIRE, 0.35f * a * (1 - Mth.clamp(t / 24f, 0, 1)));
            // The column of smoke it rises in.
            float col = fade(t, 2, 16);
            for (int i = 0; i < 7; i++) {
                float u = (t * 0.05f + i / 7f) % 1f;
                double x = at.x + Mth.sin(i * 2.3f + t * 0.1f) * 0.3 * u, z = at.z + Mth.cos(i * 1.7f + t * 0.1f) * 0.3 * u;
                smoke(d, x, gy + 0.2 + u * 3.2, z, 0.45f + u * 0.9f, 0.7f * Mth.sin(Mth.PI * u) * col);
            }
        }
    }

    // --- the smoke out of her -----------------------------------------------------------------------------------------------------

    /** A serpent of smoke pouring out of her and rushing into the knight's helm. */
    static final class SmokeOut extends MagicEffect {
        private final int knight, flight;
        private Vec3 end;

        SmokeOut(int knight, Vec3 chest, int flight) {
            super(chest, School.INFERNAL, Math.max(6, flight) + 26);
            this.knight = knight;
            this.flight = Math.max(6, flight);
            this.end = chest.add(0, 0.5, 3);
            Entity k = entity(knight);
            if (k != null) end = helm(k, 1f, k.position());
        }

        @Override
        protected double reach() {
            return at.distanceTo(end) + 4;
        }

        private Vec3 control() {
            double dist = Math.sqrt(at.distanceToSqr(end));
            return at.add(end).scale(0.5).add(0, 1.6 + dist * 0.12, 0);
        }

        private float head(float t) {
            float k = Mth.clamp(t / flight, 0, 1);
            return (float) Math.pow(k, 1.35);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Entity k = entity(knight);
            if (k != null && age <= flight) end = helm(k, 1f, k.position());
            if (age == 0) {
                for (int i = FxParticles.n(5); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), at.x, at.y + 0.2, at.z, FxParticles.j(r) * 0.06, 0.12 + r.nextDouble() * 0.08,
                            FxParticles.j(r) * 0.06);
                }
            }
            if (age <= flight) {
                // A thin wake of wisps and crimson sparks behind the head (the body itself is drawn, translucent).
                Vec3 h = bezier(at, control(), end, head(age));
                for (int i = FxParticles.rate(r, 0.6f); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), h.x + FxParticles.j(r) * 0.15, h.y + FxParticles.j(r) * 0.15,
                            h.z + FxParticles.j(r) * 0.15, 0, 0.01, 0);
                }
                if (r.nextFloat() < 0.6f) {
                    FxParticles.ember(level, r.nextBoolean() ? CRIMSON : CRIMSON_HOT, 0.4f, 12, h.x + FxParticles.j(r) * 0.25, h.y + FxParticles.j(r) * 0.25,
                            h.z + FxParticles.j(r) * 0.25, FxParticles.j(r) * 0.02, 0.02, FxParticles.j(r) * 0.02);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 c = control();
            float hd = head(t), after = Mth.clamp((t - flight) / 26f, 0, 1);
            // Pouring out of her: smoke welling up from her chest and mouth for the first ticks.
            if (t < 10) {
                float wa = 1 - t / 10f;
                for (int i = 0; i < 4; i++) {
                    float u = (t / 10f + i / 4f) % 1f;
                    smoke(d, at.x, at.y + u * 0.8, at.z, 0.22f + u * 0.28f, 0.7f * wa * Mth.sin(Mth.PI * u));
                    glint(d, at.x, at.y + u * 0.8, at.z, 0.08f, t, i * 2.1f, wa);
                }
            }
            // The serpent; once it is in, its body thins and drifts apart behind it.
            float length = 0.55f * (1 - after);
            if (length > 0.02f) {
                serpent(d, at, c, end, hd, Math.min(hd, length), 0.42f * (1 - after * 0.5f), t, 1 - after, after <= 0);
            }
        }
    }

    /** The smoke diving into the knight's helm in a spiral; the visor blazes. */
    static final class Possess extends MagicEffect {
        private final int knight;

        Possess(int knight, Vec3 at) {
            super(at, School.INFERNAL, 34);
            this.knight = knight;
        }

        @Override
        protected Vec3 anchor() {
            Entity k = entity(knight);
            return k != null ? k.position() : at;
        }

        @Override
        protected double reach() {
            return 5;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 10) {
                Entity k = entity(knight);
                Vec3 h = helm(k, 1f, at);
                MagicShake.kick(h, 0.4f, 8, 14);
                for (int i = FxParticles.n(20); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, i % 2 == 0 ? CRIMSON_HOT : HELLFIRE, 0.6f, 12, h.x, h.y, h.z, Mth.cos(a) * 0.18,
                            FxParticles.j(r) * 0.1, Mth.sin(a) * 0.18);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Entity k = entity(knight);
            Vec3 h = helm(k, d.partial, at);
            Vec3 feet = k != null ? k.getPosition(d.partial) : at;
            // The spiral in.
            for (int i = 0; i < 16; i++) {
                float u = 1 - Mth.clamp((t - i * 0.55f) / 12f, 0, 1);
                if (u <= 0 || u >= 1) continue;
                float ang = i * 0.9f + t * 0.55f, rr = 1.5f * u;
                smoke(d, h.x + Mth.cos(ang) * rr, h.y + u * 1.3, h.z + Mth.sin(ang) * rr, 0.16f + 0.2f * u, 0.8f * Mth.sin(Mth.PI * u));
                if (i % 4 == 0) glint(d, h.x + Mth.cos(ang) * rr, h.y + u * 1.3, h.z + Mth.sin(ang) * rr, 0.07f, t, i, Mth.sin(Mth.PI * u));
            }
            // The visor blazing as she takes it; a ring of heat out from its feet.
            if (t > 8) {
                float s = t - 8, f = Mth.clamp(1 - s / 14f, 0, 1);
                d.star(h.x, h.y, h.z, 1.6f * f + 0.3f, 0, CRIMSON_HOT, f);
                d.glowDisc(h.x, h.y, h.z, 0.9f, CRIMSON, 0.6f * f);
                float w = easeOut(s / 10f);
                d.groundRing(feet.x, feet.y + 0.05, feet.z, w * 2.6f, w * 2.6f + 0.35f, CRIMSON, 0.7f * (1 - w), true);
            }
        }
    }

    // --- her leaving a knight -----------------------------------------------------------------------------------------------------

    /**
     * The smoke out of a knight and back into her body: the tear-out its own for each way ({@code how}, -1 of her own will), then
     * the smoke coiling and condensing where she reforms (the renderer keeps her hidden until it has).
     */
    static final class Reform extends MagicEffect {
        /** Ticks until her body stands again (the renderer's hiding). */
        static final int CONDENSED = 22;
        private final int abaddon, how;
        private final Vec3 knightAt, her;
        private final double gyK, gyH;
        private final long seed;
        /** Away from whoever cast her out (unit, level), or zero when it was her own will or they are unknown. */
        private double ax, az;

        boolean isDone() {
            return done();
        }

        int ticks() {
            return age;
        }

        /** Whoever cast her out stands at {@code by}: the tear-out recoils away from them. */
        void expeller(Vec3 by) {
            double dx = knightAt.x - by.x, dz = knightAt.z - by.z, len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.2) {
                dx = her.x - by.x;
                dz = her.z - by.z;
                len = Math.sqrt(dx * dx + dz * dz);
            }
            if (len < 0.2) return;
            ax = dx / len;
            az = dz / len;
        }

        private boolean pushed() {
            return ax != 0 || az != 0;
        }

        Reform(int abaddon, Vec3 knightAt, Vec3 her, int how, int stun) {
            super(her, School.INFERNAL, 40);
            this.abaddon = abaddon;
            this.knightAt = knightAt;
            this.her = her;
            this.how = how;
            this.gyK = groundY(knightAt);
            this.gyH = groundY(her);
            this.seed = seedOf(knightAt);
        }

        @Override
        protected double reach() {
            return knightAt.distanceTo(her) + 6;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gyH;
        }

        Vec3 head() {
            return new Vec3(knightAt.x, gyK + 1.85, knightAt.z);
        }

        /** Where the smoke goes to settle: beside her, off to one side so the coil shows even when she stands in the knight. */
        private Vec3 side() {
            Vec3 d = her.subtract(knightAt).multiply(1, 0, 1);
            if (d.lengthSqr() < 0.25) d = new Vec3(hash(seed, 1) - 0.5, 0, hash(seed, 2) - 0.5);
            d = d.normalize();
            return new Vec3(her.x + d.x * 1.4, gyH + 1.2, her.z + d.z * 1.4);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 h = head();
            if (age == 0) {
                switch (how) {
                    case AbaddonFxPayload.BY_EXORCISE -> {
                        MagicShake.kick(h, 0.6f, 10, 16);
                        MagicOverlay.flash(h, GOLD, 0.25f, 12);
                        for (int i = FxParticles.n(24); i > 0; i--) {
                            float a = r.nextFloat() * Mth.TWO_PI;
                            FxParticles.spark(level, i % 2 == 0 ? GOLD : GOLD_HOT, 0.7f, 18, h.x, h.y, h.z, Mth.cos(a) * 0.2,
                                    0.1 + r.nextDouble() * 0.3, Mth.sin(a) * 0.2);
                        }
                    }
                    case AbaddonFxPayload.BY_NAME -> MagicSounds.play(SoundEvents.BELL_BLOCK, h, 0.7f, 0.5f);
                    case AbaddonFxPayload.BY_SALT -> {
                        MagicSounds.play(SoundEvents.FIRE_EXTINGUISH, h, 1f, 0.8f);
                        for (int i = FxParticles.n(40); i > 0; i--) {
                            double vx = FxParticles.j(r) + ax * 0.9, vy = FxParticles.j(r) * 0.6 + 0.2, vz = FxParticles.j(r) + az * 0.9;
                            FxParticles.salt(level, SALT, 0.6f, 24, h.x, h.y - 0.5, h.z, vx * 0.25, vy * 0.25, vz * 0.25);
                        }
                    }
                    case AbaddonFxPayload.BY_TRAP -> MagicSounds.play(AllSounds.abaddon("trapped"), knightAt, 1f, 1.3f);
                    case AbaddonFxPayload.BY_VESSEL -> {
                        MagicShake.kick(h, 0.5f, 8, 14);
                        MagicOverlay.flash(h, CRIMSON, 0.2f, 10);
                        for (int i = FxParticles.n(10); i > 0; i--) {
                            double vx = FxParticles.j(r) + ax * 0.7, vy = FxParticles.j(r) * 0.5, vz = FxParticles.j(r) + az * 0.7;
                            FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), h.x, h.y - 0.6, h.z, vx * 0.2, vy * 0.2, vz * 0.2);
                        }
                    }
                    default -> {
                    }
                }
            }
            if (age < 12) {
                for (int i = FxParticles.rate(r, 0.7f); i > 0; i--) {
                    double vy = how == AbaddonFxPayload.BY_TRAP ? -0.12 : how == AbaddonFxPayload.BY_EXORCISE ? 0.3 : 0.04;
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), h.x + FxParticles.j(r) * 0.2, h.y, h.z + FxParticles.j(r) * 0.2,
                            FxParticles.j(r) * 0.03, vy, FxParticles.j(r) * 0.03);
                }
            }
            if (age == CONDENSED - 2) {
                MagicSounds.play(AllSounds.abaddon("smoke"), her, 0.8f, 0.9f);
                for (int i = FxParticles.n(14); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.ember(level, CRIMSON_HOT, 0.6f, 16, her.x, gyH + 1.0, her.z, Mth.cos(a) * 0.15, FxParticles.j(r) * 0.1,
                            Mth.sin(a) * 0.15);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 h = head();
            tearOut(d, t, h);
            // The smoke flowing off to her side, coiling down, and condensing into her shape.
            Vec3 s = side();
            if (t > 4 && t < CONDENSED + 2) {
                float k = Mth.clamp((t - 4) / 8f, 0, 1), fa = 1 - Mth.clamp((t - CONDENSED + 4) / 6f, 0, 1);
                Vec3 c = h.add(s).scale(0.5).add(ax * 1.4, how == AbaddonFxPayload.BY_EXORCISE ? 2.5 : 0.8, az * 1.4);
                serpent(d, h, c, s, easeOut(k), 0.6f, 0.34f, t, 0.9f * fa, false);
            }
            if (t > 9) {
                float u = Mth.clamp((t - 9) / (CONDENSED - 9f), 0, 1), fa = 1 - Mth.clamp((t - CONDENSED) / 14f, 0, 1);
                for (int i = 0; i < 18; i++) {
                    float v = i / 18f;
                    float ang = v * Mth.TWO_PI * 2 + t * 0.6f, rr = (1.4f - 1.0f * u) * (0.6f + 0.4f * Mth.sin(v * 9 + t * 0.3f));
                    double y = gyH + 0.15 + v * 2.0;
                    Vec3 from = s.lerp(new Vec3(her.x, y, her.z), easeOut(u));
                    double x = from.x + Mth.cos(ang) * rr * (1 - u * 0.6), z = from.z + Mth.sin(ang) * rr * (1 - u * 0.6);
                    smoke(d, x, Mth.lerp(u, from.y, y), z, 0.24f + 0.16f * (1 - u), 0.75f * fa);
                    if (i % 5 == 2) glint(d, x, Mth.lerp(u, from.y, y), z, 0.07f, t, i * 1.3f, fa);
                }
                if (t > CONDENSED - 2) {
                    float f = Mth.clamp(1 - (t - CONDENSED + 2) / 10f, 0, 1);
                    d.star(her.x, gyH + 1.1, her.z, 2.2f * f + 0.4f, 0.4f, CRIMSON_HOT, f);
                    d.cracks(her.x, gyH + 0.04, her.z, 7, 1.8f, 0.1f, seed, CRIMSON_HOT, 0.7f * fa);
                }
            }
        }

        /** How the smoke left the knight. */
        private void tearOut(FxDraw d, float t, Vec3 h) {
            if (t > 16) return;
            float a = 1 - Mth.clamp((t - 8) / 8f, 0, 1);
            switch (how) {
                case AbaddonFxPayload.BY_EXORCISE -> {
                    // Torn out upward: a column of smoke shooting from the helm, gold light breaking round it.
                    for (int i = 0; i < 8; i++) {
                        float u = Mth.clamp(t / 6f - i * 0.08f, 0, 1);
                        // Leaning away from whoever spoke the rite.
                        double lean = 1.8 * u * u;
                        smoke(d, h.x + Mth.sin(i * 2.1f) * 0.15 * u + ax * lean, h.y + u * 5.5, h.z + Mth.cos(i * 1.3f) * 0.15 * u + az * lean,
                                0.28f + u * 0.5f, 0.85f * a * (1 - u * 0.5f));
                    }
                    for (int i = 0; i < 8; i++) {
                        float ang = i * Mth.TWO_PI / 8 + 0.3f, len = 1.2f + 1.4f * hash(seed, i);
                        double ex = h.x + Mth.cos(ang) * len, ez = h.z + Mth.sin(ang) * len, ey = h.y + 0.6 + hash(seed, i + 9) * 1.2;
                        d.taper(h.x, h.y, h.z, ex, ey, ez, 0.25f, 0, GOLD, 0.8f * a, 0);
                    }
                    d.star(h.x, h.y, h.z, 2.6f * a + 0.3f, 0.785f, GOLD_HOT, a);
                    d.glowDisc(h.x, h.y + 1.5, h.z, 1.6f, GOLD, 0.35f * a);
                }
                case AbaddonFxPayload.BY_NAME -> {
                    // A seal stamped down over it: a ring of written glyphs dropping onto the knight, flashing where it lands.
                    float k = Mth.clamp(t / 5f, 0, 1);
                    double y = Mth.lerp(easeOut(k), h.y + 2.5, gyK + 0.08);
                    d.glyphRing(MagicTextures.glyphStrip(), knightAt.x, y, knightAt.z, 0, 1, 0, 1.0f, 1.45f, t * 0.05f, GOLD, 0.85f * a);
                    d.groundRing(knightAt.x, y, knightAt.z, 1.45f, 1.6f, CRIMSON_HOT, 0.8f * a, false);
                    sealGround(d, knightAt, y + 0.01, 0, 1.9f, 1, 0.12f, GOLD_HOT, 0.8f * a);
                    if (t > 5) {
                        float f = Mth.clamp(1 - (t - 5) / 8f, 0, 1);
                        double off = (1 - f) * 1.2;
                        d.shockwave(MagicTextures.ENERGY_RING, knightAt.x + ax * off, gyK + 0.05, knightAt.z + az * off, 1.6f + (1 - f) * 2.5f, 0.5f,
                                0.9f * f, GOLD, 0.8f * f);
                    }
                }
                case AbaddonFxPayload.BY_SALT -> {
                    // A burst of salt; the smoke recoiling from it in puffs thrown out and curling back.
                    d.star(h.x, h.y - 0.4, h.z, 1.8f * a + 0.2f, 0.2f, SALT, a);
                    float awayAng = (float) Math.atan2(az, ax);
                    for (int i = 0; i < 9; i++) {
                        // A full ring of puffs, or (with the salt's thrower known) a fan thrown away from them.
                        float ang = pushed() ? awayAng + (i / 8f - 0.5f) * 2.4f + (hash(seed, i) - 0.5f) * 0.4f
                                : i * Mth.TWO_PI / 9 + hash(seed, i) * 0.6f, u = easeOut(t / 9f);
                        float rr = 0.3f + 1.6f * u - 0.5f * u * u;
                        smoke(d, h.x + Mth.cos(ang) * rr, h.y - 0.3 + Mth.sin(i * 1.7f) * 0.5 + u * 0.4, h.z + Mth.sin(ang) * rr,
                                0.28f + 0.2f * u, 0.8f * a);
                        d.glowDisc(h.x + Mth.cos(ang) * rr * 1.2, h.y - 0.3, h.z + Mth.sin(ang) * rr * 1.2, 0.1f, SALT, 0.6f * a);
                    }
                }
                case AbaddonFxPayload.BY_TRAP -> {
                    // A red pentagram flaring under it, pulling the smoke down into the floor.
                    float f = 0.7f + 0.3f * Mth.sin(t * 1.2f);
                    pentagram(d, knightAt.x, gyK + 0.05, knightAt.z, 1.45f, 0.3f, 0.14f, TRAP, a * f);
                    d.flatGlow(knightAt.x, gyK + 0.04, knightAt.z, 2.2f, TRAP, 0.5f * a);
                    d.cylinder(knightAt.x, gyK, knightAt.z, 1.45f, 2.6f, TRAP, 0.4f * a, 0);
                    for (int i = 0; i < 10; i++) {
                        float u = Mth.clamp(t / 8f - i * 0.05f, 0, 1);
                        float ang = i * 1.9f + t * 0.4f, rr = 0.5f * (1 - u);
                        smoke(d, knightAt.x + Mth.cos(ang) * rr, Mth.lerp(u, h.y, gyK + 0.1), knightAt.z + Mth.sin(ang) * rr, 0.35f * (1 - u * 0.5f),
                                0.8f * a);
                    }
                }
                case AbaddonFxPayload.BY_VESSEL -> {
                    // The knight falls: the smoke bursts out of its body.
                    for (int i = 0; i < 12; i++) {
                        float ang = i * Mth.TWO_PI / 12 + hash(seed, i), el = (hash(seed, i + 20) - 0.3f) * 1.2f, u = easeOut(t / 8f);
                        float rr = 2.2f * u;
                        smoke(d, h.x + Mth.cos(ang) * rr + ax * u * 1.2, h.y - 0.6 + el * rr, h.z + Mth.sin(ang) * rr + az * u * 1.2,
                                0.24f + 0.3f * u, 0.85f * a);
                    }
                    d.star(h.x, h.y - 0.6, h.z, 2.0f * a + 0.3f, 0.5f, CRIMSON_HOT, a);
                }
                default -> {
                    // Of her own will: the smoke pouring calmly out of the helm.
                    for (int i = 0; i < 5; i++) {
                        float u = (t / 10f + i / 5f) % 1f;
                        smoke(d, h.x, h.y + u * 0.6, h.z, 0.25f + u * 0.3f, 0.7f * a * Mth.sin(Mth.PI * u));
                    }
                }
            }
        }
    }

    /**
     * The blow that cast her out, from whoever dealt it: a streak in its colour from their hands to the knight (gold for the rite and
     * the Name, a spray of salt, the trap's red, crimson for the vessel), a flare where it lands. {@code alone} (no expulsion showing
     * to recoil): a small recoil of smoke of its own there, thrown away from them.
     */
    static final class Streak extends MagicEffect {
        private static final int TRAVEL = 6;
        private final Vec3 from, to;
        private final int how, rgb, hot;
        private final boolean alone;
        private final double ax, az;
        private final long seed;

        Streak(Vec3 by, Vec3 to, int how, boolean alone) {
            super(to, School.INFERNAL, 24);
            this.from = by.add(0, 1.3, 0);
            this.to = to;
            this.how = how;
            this.alone = alone;
            this.seed = seedOf(by);
            this.rgb = switch (how) {
                case AbaddonFxPayload.BY_EXORCISE, AbaddonFxPayload.BY_NAME -> GOLD;
                case AbaddonFxPayload.BY_SALT -> SALT;
                case AbaddonFxPayload.BY_TRAP -> TRAP;
                default -> CRIMSON;
            };
            this.hot = switch (how) {
                case AbaddonFxPayload.BY_EXORCISE, AbaddonFxPayload.BY_NAME -> GOLD_HOT;
                case AbaddonFxPayload.BY_SALT -> 0xFFFFFF;
                case AbaddonFxPayload.BY_TRAP -> 0xFF8A6A;
                default -> CRIMSON_HOT;
            };
            double dx = to.x - by.x, dz = to.z - by.z, len = Math.sqrt(dx * dx + dz * dz);
            this.ax = len < 0.1 ? 0 : dx / len;
            this.az = len < 0.1 ? 0 : dz / len;
        }

        @Override
        protected double reach() {
            return from.distanceTo(to) + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Double.NaN;
        }

        private Vec3 at(float k) {
            // A slight arc, so it reads as thrown rather than a laser.
            Vec3 p = from.lerp(to, k);
            return p.add(0, Mth.sin(Mth.PI * k) * 0.35, 0);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (how == AbaddonFxPayload.BY_SALT && age < TRAVEL) {
                // The grains themselves, flung down the line and fanning out.
                for (int i = FxParticles.n(8); i > 0; i--) {
                    Vec3 p = at((age + r.nextFloat()) / TRAVEL);
                    Vec3 v = to.subtract(from).normalize().scale(0.35);
                    FxParticles.salt(level, SALT, 0.5f, 14, p.x, p.y, p.z, v.x + FxParticles.j(r) * 0.08, v.y + 0.04, v.z + FxParticles.j(r) * 0.08);
                }
            }
            if (age == TRAVEL) {
                for (int i = FxParticles.n(12); i > 0; i--) {
                    double vx = ax * 0.25 + FxParticles.j(r) * 0.12, vz = az * 0.25 + FxParticles.j(r) * 0.12;
                    FxParticles.spark(level, i % 2 == 0 ? rgb : hot, 0.5f, 12, to.x, to.y, to.z, vx, 0.05 + r.nextDouble() * 0.12, vz);
                }
                if (alone) {
                    for (int i = FxParticles.n(4); i > 0; i--) {
                        FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), to.x, to.y, to.z, ax * 0.12 + FxParticles.j(r) * 0.03, 0.03,
                                az * 0.12 + FxParticles.j(r) * 0.03);
                    }
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float head = Mth.clamp(t / TRAVEL, 0, 1), tail = Mth.clamp((t - 3) / (TRAVEL + 4f), 0, 1);
            float a = 1 - Mth.clamp((t - TRAVEL - 4) / 12f, 0, 1);
            if (a <= 0.01f) return;
            // The streak: a tapering beam between its tail and head, a bright point at its head.
            int n = 8;
            Vec3 prev = null;
            for (int i = 0; i <= n; i++) {
                float k = Mth.lerp(i / (float) n, tail, head);
                Vec3 p = at(k);
                if (prev != null) {
                    float w0 = 0.05f + 0.16f * (i - 1) / n, w1 = 0.05f + 0.16f * i / n;
                    d.taper(prev.x, prev.y, prev.z, p.x, p.y, p.z, w0, w1, rgb, 0.7f * a, 0.9f * a);
                }
                prev = p;
            }
            if (t < TRAVEL + 1) {
                Vec3 h = at(head);
                d.glowDisc(h.x, h.y, h.z, 0.35f, rgb, 0.6f * a);
                d.glowDisc(h.x, h.y, h.z, 0.14f, hot, 0.9f * a);
            }
            // The flare where it lands, and (alone) the smoke thrown back away from them.
            if (t >= TRAVEL) {
                float s = t - TRAVEL, f = Mth.clamp(1 - s / 10f, 0, 1);
                d.star(to.x, to.y, to.z, 1.3f * f + 0.2f, (float) Math.atan2(az, ax), hot, f);
                d.glowDisc(to.x, to.y, to.z, 0.8f, rgb, 0.4f * f);
                if (alone) {
                    float u = easeOut(s / 12f);
                    for (int i = 0; i < 6; i++) {
                        float spread = (hash(seed, i) - 0.5f) * 1.2f;
                        double px = to.x + (ax * Mth.cos(spread) - az * Mth.sin(spread)) * u * 1.6;
                        double pz = to.z + (az * Mth.cos(spread) + ax * Mth.sin(spread)) * u * 1.6;
                        smoke(d, px, to.y + (hash(seed, i + 7) - 0.4f) * 0.6 * u, pz, 0.18f + 0.18f * u, 0.7f * (1 - u));
                        glint(d, px, to.y, pz, 0.06f, t, i * 1.7f, 1 - u);
                    }
                }
            }
        }
    }

    /** The cracked crimson aura of the ×2 window after she is cast out. */
    static final class Stun extends MagicEffect {
        private final int abaddon;
        private final long seed;
        private Vec3 pos;
        private boolean ended;
        private int endAt;

        Stun(int abaddon, Vec3 at, int ticks) {
            super(at, School.INFERNAL, Math.max(20, ticks) + 10);
            this.abaddon = abaddon;
            this.pos = at;
            this.seed = seedOf(at);
        }

        @Override
        protected boolean done() {
            return super.done() || ended && age - endAt > 8;
        }

        @Override
        protected Vec3 anchor() {
            return pos;
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected void tick(ClientLevel level) {
            Entity e = entity(abaddon);
            if (e instanceof AbaddonEntity a) {
                pos = a.position();
                if (!ended && age > 6 && !a.stunned()) {
                    ended = true;
                    endAt = age;
                }
            }
            RandomSource r = level.random;
            if (!ended && r.nextFloat() < 0.6f) {
                FxParticles.ember(level, r.nextBoolean() ? CRIMSON : CRIMSON_HOT, 0.45f, 14, pos.x + FxParticles.j(r) * 0.5,
                        pos.y + 0.2 + r.nextDouble() * 1.7, pos.z + FxParticles.j(r) * 0.5, 0, 0.02, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = Mth.clamp(t / 4f, 0, 1) * (ended ? 1 - Mth.clamp((age - endAt + d.partial) / 8f, 0, 1) : 1)
                    * Mth.clamp((life - t) / 10f, 0, 1);
            if (a <= 0.01f) return;
            Entity e = entity(abaddon);
            Vec3 p = e != null ? e.getPosition(d.partial) : pos;
            float flick = 0.7f + 0.3f * Mth.sin(t * 2.3f) * Mth.sin(t * 0.7f + 1);
            d.glowDisc(p.x, p.y + 1.0, p.z, 1.5f, CRIMSON, 0.3f * a * flick);
            // The crack lines round her silhouette, broken, flickering.
            Vector3f l = d.camera.getLeftVector();
            for (int i = 0; i < 6; i++) {
                float side = (i % 2 == 0 ? 1 : -1) * (0.42f + 0.12f * hash(seed, i));
                double y0 = p.y + 0.1 + hash(seed, i + 10) * 0.6, y1 = y0 + 0.6 + hash(seed, i + 20) * 0.9;
                double x0 = p.x + l.x * side, z0 = p.z + l.z * side;
                float on = Mth.sin(t * 0.9f + i * 2.2f) > -0.3f ? 1 : 0.25f;
                d.lightning(x0, y0, z0, x0 + l.x * side * 0.2, y1, z0 + l.z * side * 0.2, seed + i * 31L + (long) (t / 3), 4, 0.15f, 0.05f,
                        CRIMSON_HOT, 0.85f * a * on, i % 3 == 0);
            }
            // Shards of her broken hold orbiting slowly.
            for (int i = 0; i < 5; i++) {
                float ang = t * 0.06f + i * Mth.TWO_PI / 5, rr = 0.75f;
                double sx = p.x + Mth.cos(ang) * rr, sz = p.z + Mth.sin(ang) * rr, sy = p.y + 0.5 + 0.9 * hash(seed, i + 40) + 0.08 * Mth.sin(t * 0.2f + i);
                d.crystal(sx, sy, sz, Mth.cos(ang + 1), 0.6f, Mth.sin(ang + 1), 0.05f, 0.14f, CRIMSON_HOT, 0.8f * a, true);
            }
            d.cracks(p.x, p.y + 0.04, p.z, 6, 1.3f, 0.07f, seed, CRIMSON_HOT, 0.5f * a * flick);
            d.spikedRing(p.x, p.y + 0.05, p.z, 0.9f, 0.25f, 7, t * 0.05f, CRIMSON, 0.45f * a);
        }
    }

    // --- the telekinetic pin ------------------------------------------------------------------------------------------------------

    /** Violet chains of force from the floor round the hunter, rings of distortion, a thread from her fist. */
    static final class Pin extends MagicEffect {
        private static final int CHAINS = 4;
        private final int abaddon, target;
        private final double gy;
        private final float base;

        Pin(int abaddon, int target, Vec3 hold, int ticks) {
            super(hold, School.INFERNAL, Math.max(10, ticks) + 10);
            this.abaddon = abaddon;
            this.target = target;
            this.gy = groundY(hold);
            this.base = hash(seedOf(hold), 3) * Mth.TWO_PI;
        }

        @Override
        protected double reach() {
            return 5;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private Vec3 torso(float partial) {
            Entity e = entity(target);
            if (e == null || e.isRemoved()) return at.add(0, 1.0, 0);
            return e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 c = torso(1f);
            if (age == 0) {
                MagicShake.kick(at, 0.3f, 6, 10);
                for (int i = FxParticles.n(18); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, i % 2 == 0 ? VIOLET : VIOLET_HOT, 0.6f, 14, c.x, c.y, c.z, Mth.cos(a) * 0.15, FxParticles.j(r) * 0.1,
                            Mth.sin(a) * 0.15);
                }
            }
            if (age < life - 10 && r.nextFloat() < 0.6f) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.mote(level, VIOLET, 0.5f, 20, c.x + Mth.cos(a) * 0.7, c.y + FxParticles.j(r) * 0.7, c.z + Mth.sin(a) * 0.7, 0, 0.01, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 2, 10);
            Vec3 c = torso(d.partial);
            float k = easeOut(t / 4f);
            for (int i = 0; i < CHAINS; i++) {
                float ang = base + i * Mth.TWO_PI / CHAINS;
                Vec3 anchor = new Vec3(at.x + Mth.cos(ang) * AbaddonBalance.PIN_RADIUS, gy + 0.05, at.z + Mth.sin(ang) * AbaddonBalance.PIN_RADIUS);
                Vec3 end = anchor.lerp(c, k);
                // Taut, trembling a little.
                Vec3 jit = new Vec3(0.02 * Mth.sin(t * 3.1f + i), 0.02 * Mth.sin(t * 2.7f + i * 2), 0.02 * Mth.cos(t * 3.3f + i));
                chain(d, anchor, end.add(jit), 0.11f, VIOLET, 0.85f * a, t * 0.5f);
                d.groundRing(anchor.x, gy + 0.05, anchor.z, 0.25f, 0.34f, VIOLET_HOT, 0.8f * a, false);
                d.spikedRing(anchor.x, gy + 0.05, anchor.z, 0.4f, 0.15f, 6, -t * 0.08f, VIOLET, 0.5f * a);
            }
            // Distortion rings rippling round them, and one about their feet.
            Vec3 look = new Vec3(d.camera.getLookVector());
            for (int i = 0; i < 3; i++) {
                float u = ((t + i * 6) % 18) / 18f;
                d.ring(c.x, c.y, c.z, (float) look.x, (float) look.y, (float) look.z, 0.3f + u * 1.6f, 0.4f + u * 1.7f, VIOLET, 0.5f * (1 - u) * a, true);
            }
            float u = (t % 12) / 12f;
            d.groundRing(at.x, gy + 0.06, at.z, 0.4f + u * 1.8f, 0.55f + u * 1.8f, VIOLET, 0.5f * (1 - u) * a, true);
            d.glowDisc(c.x, c.y, c.z, 1.0f, VIOLET, 0.25f * a);
            // The thread from her clenched fist.
            Entity ab = entity(abaddon);
            if (ab != null) {
                Vec3 hand = AbaddonRenderers.bone(ab, AbaddonRenderers.Bone.HAND, d.partial);
                if (hand == null) hand = ab.getPosition(d.partial).add(0, 1.2, 0);
                float pulse = 0.6f + 0.4f * Mth.sin(t * 0.5f);
                d.softBeam(hand.x, hand.y, hand.z, c.x, c.y, c.z, 0.1f, VIOLET, 0.5f * a * pulse, 0.3f * a, true);
                d.glowDisc(hand.x, hand.y, hand.z, 0.35f, VIOLET_HOT, 0.6f * a * pulse);
            }
        }
    }

    // --- the Brand of the Knights -------------------------------------------------------------------------------------------------

    /** The Brand burnt into the floor, the rings' bands smouldering round their green gaps, then fire along every band. */
    static final class Brand extends MagicEffect {
        private static final float SEAL = 3.4f;
        private final int rings, windup;
        private final double gy;
        private final long seed;
        /** The gaps (from the green telegraphs), by ring. */
        private final List<List<Vec3>> gaps = new ArrayList<>();

        Brand(int rings, Vec3 centre, int show) {
            super(centre, School.INFERNAL, Math.max(10, show) + 32);
            this.rings = Mth.clamp(rings, 1, AbaddonBalance.BRAND_RINGS.length);
            this.windup = Math.max(6, show - 4);
            this.gy = groundY(centre);
            this.seed = seedOf(centre);
            for (int i = 0; i < this.rings; i++) gaps.add(new ArrayList<>());
        }

        @Override
        protected double reach() {
            return AbaddonBalance.BRAND_RINGS[rings - 1] + AbaddonBalance.BRAND_BAND + 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        /** The gaps, from the green circles the server laid on the rings (they arrive with or just after this). */
        private void find(ClientLevel level) {
            float outer = (float) reach();
            for (List<Vec3> g : gaps) g.clear();
            for (TelegraphMarker m : level.getEntitiesOfClass(TelegraphMarker.class, new AABB(at, at).inflate(outer, 4, outer))) {
                if (m.shape() != TelegraphMarker.Shape.CIRCLE || m.color() != TelegraphMarker.SAFE) continue;
                double dist = Math.sqrt(Math.pow(m.getX() - at.x, 2) + Math.pow(m.getZ() - at.z, 2));
                int best = -1;
                double bd = 2.5;
                for (int i = 0; i < rings; i++) {
                    double dd = Math.abs(dist - AbaddonBalance.BRAND_RINGS[i]);
                    if (dd < bd) {
                        bd = dd;
                        best = i;
                    }
                }
                if (best >= 0) gaps.get(best).add(new Vec3(m.getX(), gy, m.getZ()));
            }
        }

        private boolean inGap(int ring, double x, double z) {
            for (Vec3 g : gaps.get(ring)) {
                double dx = x - g.x, dz = z - g.z;
                if (dx * dx + dz * dz < AbaddonBalance.BRAND_GAP_RADIUS * AbaddonBalance.BRAND_GAP_RADIUS) return true;
            }
            return false;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age < windup && age % 4 == 1) find(level);
            if (age < windup) {
                float k = age / (float) windup;
                for (int i = FxParticles.rate(r, 1.5f + 4 * k); i > 0; i--) {
                    int ring = r.nextInt(rings);
                    float a = r.nextFloat() * Mth.TWO_PI, rr = AbaddonBalance.BRAND_RINGS[ring] + (float) FxParticles.j(r) * AbaddonBalance.BRAND_BAND;
                    double x = at.x + Mth.cos(a) * rr, z = at.z + Mth.sin(a) * rr;
                    if (inGap(ring, x, z)) continue;
                    FxParticles.ember(level, r.nextBoolean() ? HELLFIRE : CRIMSON, 0.6f, 20, x, gy + 0.08, z, 0, 0.05 + k * 0.05, 0);
                }
            }
            if (age == windup) {
                MagicShake.kick(at, 1.0f, 16, reach() + 10);
                MagicOverlay.flash(at, HELLFIRE, 0.35f, reach() + 6);
            }
            if (age >= windup && age < windup + 10) {
                for (int i = FxParticles.n(30); i > 0; i--) {
                    int ring = r.nextInt(rings);
                    float a = r.nextFloat() * Mth.TWO_PI, rr = AbaddonBalance.BRAND_RINGS[ring] + (float) FxParticles.j(r) * AbaddonBalance.BRAND_BAND;
                    double x = at.x + Mth.cos(a) * rr, z = at.z + Mth.sin(a) * rr;
                    if (inGap(ring, x, z)) continue;
                    if (i % 3 == 0) FxParticles.plain(level, ParticleTypes.FLAME, x, gy + 0.2, z, 0, 0.1 + r.nextDouble() * 0.1, 0);
                    else if (i % 3 == 1) FxParticles.ember(level, HELL_HOT, 0.8f, 22, x, gy + 0.2, z, 0, 0.15, 0);
                    else FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, x, gy + 0.5, z, 0, 0.08, 0);
                }
            }
        }

        /** One ring's band as arcs, broken where its gaps are. */
        private void band(FxDraw d, int ring, float width, int rgb, float alpha, double y) {
            float r = AbaddonBalance.BRAND_RINGS[ring];
            int segs = Math.max(24, (int) (r * 7));
            for (int s = 0; s < segs; s++) {
                float a0 = Mth.TWO_PI * s / segs, a1 = Mth.TWO_PI * (s + 1) / segs, am = (a0 + a1) / 2;
                if (inGap(ring, at.x + Mth.cos(am) * r, at.z + Mth.sin(am) * r)) continue;
                d.groundLine(at.x + Mth.cos(a0) * r, at.z + Mth.sin(a0) * r, at.x + Mth.cos(a1) * r, at.z + Mth.sin(a1) * r, y, width, rgb, alpha);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            if (t < windup) {
                float k = Mth.clamp(t / windup, 0, 1), a = fade(t, 4, 0);
                float pulse = 0.75f + 0.25f * Mth.sin(t * (0.3f + 0.6f * k));
                float drawn = Mth.clamp(t / (windup * 0.5f), 0, 1);
                sealGround(d, at, gy + 0.05, 0, SEAL, drawn, 0.5f, CRIMSON, 0.75f * a * pulse);
                sealGround(d, at, gy + 0.06, 0, SEAL, drawn, 0.16f, HELL_HOT, 0.9f * a);
                d.flatGlow(at.x, gy + 0.04, at.z, SEAL * 1.1f, CRIMSON, 0.35f * a * pulse);
                for (int i = 0; i < rings; i++) {
                    band(d, i, AbaddonBalance.BRAND_BAND * 2 * (0.6f + 0.4f * k), HELLFIRE, (0.08f + 0.22f * k) * a * pulse, gy + 0.04);
                    band(d, i, 0.12f, HELL_HOT, (0.25f + 0.5f * k) * a, gy + 0.05);
                    for (Vec3 g : gaps.get(i)) {
                        float sp = 0.8f + 0.2f * Mth.sin(t * 0.3f + (float) g.x);
                        float gr = AbaddonBalance.BRAND_GAP_RADIUS;
                        d.cylinder(g.x, gy + 0.02, g.z, gr, 2.2f, SAFE, 0.3f * sp * a, 0);
                        d.flatGlow(g.x, gy + 0.05, g.z, gr * 1.2f, SAFE, 0.3f * sp * a);
                    }
                }
            } else {
                float s = t - windup, w = Mth.clamp(s / 10f, 0, 1), fa = 1 - Mth.clamp((s - 4) / 24f, 0, 1);
                float outer = AbaddonBalance.BRAND_RINGS[rings - 1] + AbaddonBalance.BRAND_BAND;
                d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, Math.max(0.3f, outer * easeOut(w)), 1.6f, 2.4f * (1 - w) + 0.3f, HELLFIRE,
                        0.9f * (1 - w * 0.6f));
                if (s < 5) d.star(at.x, gy + 1.5, at.z, 7f * (1 - s / 5f) + 1, 0.3f, HELL_HOT, 1 - s / 5f);
                sealGround(d, at, gy + 0.06, 0, SEAL, 1, 0.3f, 0xFFF0D8, 0.95f * fa);
                d.flatGlow(at.x, gy + 0.04, at.z, outer, HELLFIRE, 0.35f * fa);
                float flames = 1 - Mth.clamp((s - 2) / 22f, 0, 1);
                for (int i = 0; i < rings; i++) {
                    band(d, i, AbaddonBalance.BRAND_BAND * 2, HELLFIRE, 0.4f * fa, gy + 0.04);
                    float r = AbaddonBalance.BRAND_RINGS[i];
                    int n = Math.max(12, (int) (r * 3.2f));
                    for (int j = 0; j < n; j++) {
                        float ang = Mth.TWO_PI * (j + hash(seed, i * 100 + j) * 0.6f) / n;
                        float rr = r + (hash(seed, i * 100 + j + 50) - 0.5f) * AbaddonBalance.BRAND_BAND * 1.6f;
                        double x = at.x + Mth.cos(ang) * rr, z = at.z + Mth.sin(ang) * rr;
                        if (inGap(i, x, z)) continue;
                        flame(d, x, gy, z, (1.2f + 1.4f * hash(seed, i * 100 + j + 90)) * flames, 0.45f, Mth.sin(t * 0.9f + j * 1.3f), flames);
                    }
                    d.darkRing(at.x, gy + 0.03, at.z, r - AbaddonBalance.BRAND_BAND, r + AbaddonBalance.BRAND_BAND, SCORCH, 0.45f * fade(t, 0, 20));
                }
            }
        }
    }

    // --- the devil's trap ---------------------------------------------------------------------------------------------------------

    /** The pentagram flaring red under her, a cage of red light, her smoke dragged down into it. */
    static final class Trapped extends MagicEffect {
        private static final float R = 1.45f;
        private final int abaddon;
        private final double gy;
        private boolean closing;
        private int closeAt;

        Trapped(int abaddon, Vec3 centre, int ticks) {
            super(centre, School.INFERNAL, Math.max(20, ticks) + 8);
            this.abaddon = abaddon;
            this.gy = groundY(centre.add(0, 0.4, 0));
        }

        void close() {
            if (!closing) {
                closing = true;
                closeAt = age;
            }
        }

        boolean finished() {
            return done();
        }

        @Override
        protected boolean done() {
            return super.done() || closing && age - closeAt > 4;
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
            if (age > 10 && entity(abaddon) instanceof AbaddonEntity a && !a.trapped()) close();
            RandomSource r = level.random;
            if (age == 0) {
                MagicOverlay.flash(at, TRAP, 0.2f, 10);
                for (int i = FxParticles.n(16); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, TRAP, 0.6f, 14, at.x + Mth.cos(a) * R, gy + 0.1, at.z + Mth.sin(a) * R, 0, 0.12, 0);
                }
            }
            if (closing) return;
            for (int i = FxParticles.rate(r, 1.2f); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI, d = R * (0.4f + 0.6f * r.nextFloat());
                FxParticles.ember(level, TRAP, 0.5f, 18, at.x + Mth.cos(a) * d, gy + 0.06, at.z + Mth.sin(a) * d, 0, 0.05, 0);
            }
            if (r.nextFloat() < 0.5f) {
                FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), at.x + FxParticles.j(r) * 0.4, gy + 1.2 + r.nextDouble() * 0.7,
                        at.z + FxParticles.j(r) * 0.4, 0, -0.08, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = Mth.clamp(t / 3f, 0, 1) * (closing ? 1 - Mth.clamp((age - closeAt + d.partial) / 4f, 0, 1) : 1) * Mth.clamp((life - t) / 8f, 0, 1);
            if (a <= 0.01f) return;
            float flare = Mth.clamp(1 - t / 8f, 0, 1), pulse = 0.75f + 0.25f * Mth.sin(t * 0.4f);
            int col = FxColor.mix(TRAP, 0xFFE0D0, flare * 0.6f);
            pentagram(d, at.x, gy + 0.05, at.z, R, -Mth.HALF_PI, 0.12f + 0.1f * flare, col, a * (pulse + flare));
            d.flatGlow(at.x, gy + 0.04, at.z, R * 1.5f, TRAP, (0.3f + 0.4f * flare) * a);
            d.cylinder(at.x, gy, at.z, R, 2.8f, TRAP, 0.35f * a * pulse, 0);
            for (int i = 0; i < 5; i++) {
                float ang = -Mth.HALF_PI + i * Mth.TWO_PI / 5;
                d.glowDisc(at.x + Mth.cos(ang) * R, gy + 0.15, at.z + Mth.sin(ang) * R, 0.25f, 0xFFB0A0, 0.7f * a * pulse);
            }
            // Her smoke dragged down into the trap.
            for (int i = 0; i < 6; i++) {
                float u = ((t * 0.05f) + i / 6f) % 1f;
                float ang = i * 2.1f + t * 0.15f, rr = 0.55f * (1 - u);
                smoke(d, at.x + Mth.cos(ang) * rr, gy + 2.0 - u * 1.9, at.z + Mth.sin(ang) * rr, 0.3f * (1 - u * 0.5f), 0.55f * a * Mth.sin(Mth.PI * u));
            }
        }
    }

    /** The pentagram shattering in a crimson burst. */
    static final class TrapFlare extends MagicEffect {
        private static final int SHARDS = 22;
        private final double gy;
        private final long seed;

        TrapFlare(Vec3 centre, int ticks) {
            super(centre, School.INFERNAL, Math.max(20, ticks) + 16);
            this.gy = groundY(centre.add(0, 0.4, 0));
            this.seed = seedOf(centre);
        }

        @Override
        protected double reach() {
            return 7;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age != 0) return;
            RandomSource r = level.random;
            MagicShake.kick(at, 0.8f, 12, 16);
            MagicOverlay.flash(at, CRIMSON_HOT, 0.4f, 12);
            for (int i = FxParticles.n(30); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.ember(level, i % 2 == 0 ? CRIMSON_HOT : HELLFIRE, 0.8f, 22, at.x, gy + 0.3, at.z, Mth.cos(a) * 0.3, 0.1 + r.nextDouble() * 0.2,
                        Mth.sin(a) * 0.3);
            }
            for (int i = FxParticles.n(14); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x, gy + 0.4, at.z, Mth.cos(a) * 0.2, 0.05, Mth.sin(a) * 0.2);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), w = Mth.clamp(t / 8f, 0, 1), fa = fade(t, 0, 14);
            if (t < 6) d.star(at.x, gy + 1.0, at.z, 3.5f * (1 - t / 6f) + 0.5f, 0.2f, CRIMSON_HOT, 1 - t / 6f);
            d.shockwave(MagicTextures.ENERGY_RING, at.x, gy + 0.05, at.z, 0.5f + 5f * easeOut(w), 0.7f, 1.4f * (1 - w) + 0.2f, CRIMSON_HOT, 0.85f * (1 - w));
            d.flatGlow(at.x, gy + 0.04, at.z, 3f, CRIMSON, 0.5f * fa);
            // The trap's lines broken into shards flung out and falling.
            for (int i = 0; i < SHARDS; i++) {
                float ang = hash(seed, i) * Mth.TWO_PI, sp = 0.12f + 0.18f * hash(seed, i + 30), up = 0.15f + 0.2f * hash(seed, i + 60);
                double x = at.x + Mth.cos(ang) * (0.6 + sp * t), z = at.z + Mth.sin(ang) * (0.6 + sp * t);
                double y = gy + 0.1 + Math.max(0, up * t - 0.02 * t * t);
                d.crystal(x, y, z, Mth.cos(ang + t * 0.4f), 0.4f, Mth.sin(ang + t * 0.4f), 0.06f, 0.2f, TRAP, 0.9f * fa, true);
            }
            // Smoke rolling out along the floor.
            for (int i = 0; i < 10; i++) {
                float ang = i * Mth.TWO_PI / 10, rr = 0.5f + 3.5f * easeOut(t / 14f);
                smoke(d, at.x + Mth.cos(ang) * rr, gy + 0.4, at.z + Mth.sin(ang) * rr, 0.5f + rr * 0.15f, 0.6f * fa);
            }
        }
    }

    // --- the Knights' Cleaver -----------------------------------------------------------------------------------------------------

    /** A wall of hellfire running down the lane, the ground burning behind it. */
    static final class Cleave extends MagicEffect {
        private final float yaw, reach;
        private final int run;
        private final double gy;
        private final long seed;

        Cleave(Vec3 from, float yaw, float reach, int ticks) {
            super(from, School.INFERNAL, Math.max(6, ticks) + 30);
            this.yaw = yaw;
            this.reach = reach;
            this.run = Math.max(6, ticks);
            this.gy = groundY(from);
            this.seed = seedOf(from);
        }

        @Override
        protected double reach() {
            return reach + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private double[] dir() {
            float a = yaw * Mth.DEG_TO_RAD;
            return new double[]{-Mth.sin(a), Mth.cos(a)};
        }

        private float front(float t) {
            return reach * Mth.clamp(t / run, 0, 1);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age > run + 2) return;
            double[] dv = dir();
            double f = 0.6 + front(age);
            double sx = -dv[1], sz = dv[0];
            for (int i = FxParticles.rate(r, 4f); i > 0; i--) {
                double across = FxParticles.j(r) * AbaddonBalance.CLEAVE_WIDTH / 2;
                double x = at.x + dv[0] * f + sx * across, z = at.z + dv[1] * f + sz * across;
                if (i % 2 == 0) FxParticles.plain(level, ParticleTypes.FLAME, x, gy + 0.15, z, dv[0] * 0.05, 0.08, dv[1] * 0.05);
                else FxParticles.ember(level, r.nextBoolean() ? HELLFIRE : CRIMSON_HOT, 0.7f, 18, x, gy + 0.2, z, dv[0] * 0.06, 0.1, dv[1] * 0.06);
            }
            if (r.nextFloat() < 0.4f) FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x + dv[0] * f, gy + 0.8, at.z + dv[1] * f, 0, 0.05, 0);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), f = front(t);
            double[] dv = dir();
            double sx = -dv[1], sz = dv[0];
            float half = AbaddonBalance.CLEAVE_WIDTH / 2;
            float trail = fade(t, 1, 22);
            // The burning lane behind the wave.
            double x0 = at.x + dv[0] * 0.6, z0 = at.z + dv[1] * 0.6, x1 = at.x + dv[0] * (0.6 + f), z1 = at.z + dv[1] * (0.6 + f);
            d.groundLine(x0, z0, x1, z1, gy + 0.05, AbaddonBalance.CLEAVE_WIDTH * 1.1f, HELLFIRE, 0.3f * trail);
            d.groundLine(x0, z0, x1, z1, gy + 0.06, 0.3f, HELL_HOT, 0.45f * trail);
            for (int i = 0; i < 18; i++) {
                float along = 0.6f + reach * hash(seed, i), across = (hash(seed, i + 30) - 0.5f) * 2 * half;
                if (along - 0.6f > f) continue;
                float since = t - run * (along - 0.6f) / reach, life = 1 - Mth.clamp((since - 4) / 18f, 0, 1);
                double x = at.x + dv[0] * along + sx * across, z = at.z + dv[1] * along + sz * across;
                flame(d, x, gy, z, (0.5f + 0.8f * hash(seed, i + 60)) * life, 0.3f, Mth.sin(t * 0.9f + i * 1.6f), life);
                d.darkRing(x, gy + 0.03, z, 0, 0.45f, SCORCH, 0.4f * trail);
            }
            // The wave's wall of fire at its front.
            if (t < run + 4) {
                float wa = 1 - Mth.clamp((t - run) / 4f, 0, 1);
                double fx = at.x + dv[0] * (0.6 + f), fz = at.z + dv[1] * (0.6 + f);
                for (int j = 0; j < 7; j++) {
                    float across = -half + 2 * half * j / 6f;
                    flame(d, fx + sx * across, gy, fz + sz * across, 1.5f + 0.5f * Mth.sin(j * 1.3f), 0.55f, Mth.sin(t * 1.5f + j * 2f), wa);
                }
                d.groundLine(fx - sx * half, fz - sz * half, fx + sx * half, fz + sz * half, gy + 0.06, 0.6f, HELL_HOT, 0.8f * wa);
                d.glowDisc(fx, gy + 0.7, fz, 1.4f, HELLFIRE, 0.4f * wa);
            }
        }
    }

    // --- her death ----------------------------------------------------------------------------------------------------------------

    /** A knight crumbling to ash where it stood. */
    static final class Crumble extends MagicEffect {
        private final float height;
        private final double gy;
        private final long seed;

        Crumble(Vec3 pos, float height) {
            super(pos, School.INFERNAL, 54);
            this.height = Math.max(1, height);
            this.gy = groundY(pos);
            this.seed = seedOf(pos);
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private float top(float t) {
            return height * (1 - easeOut(t / 28f));
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                MagicSounds.play(AllSounds.abaddon("knight_death"), at, 0.7f, 0.75f);
                for (int i = FxParticles.n(12); i > 0; i--) {
                    FxParticles.ember(level, CRIMSON_HOT, 0.5f, 20, at.x + FxParticles.j(r) * 0.4, gy + r.nextDouble() * height, at.z + FxParticles.j(r) * 0.4,
                            FxParticles.j(r) * 0.04, 0.05, FxParticles.j(r) * 0.04);
                }
            }
            float top = top(age);
            if (age < 32) {
                for (int i = FxParticles.rate(r, 4f); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.ASH.get(), at.x + FxParticles.j(r) * 0.4, gy + r.nextDouble() * Math.max(0.2, top),
                            at.z + FxParticles.j(r) * 0.4, FxParticles.j(r) * 0.02, -0.04, FxParticles.j(r) * 0.02);
                }
                if (r.nextFloat() < 0.5f) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), at.x + FxParticles.j(r) * 0.3, gy + top, at.z + FxParticles.j(r) * 0.3, 0, 0.05, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), top = top(t), a = fade(t, 0, 20);
            // The silhouette of ash, slumping down into a heap.
            for (float y = 0.15f; y < top; y += 0.28f) {
                float w = y > height * 0.6f && y < height * 0.85f ? 0.45f : 0.32f;
                d.darkDisc(at.x + 0.03 * Mth.sin(y * 7 + t * 0.3f), gy + y, at.z, w, ASH_DARK, 0.75f * a);
                if (hash(seed, (int) (y * 10)) < 0.35f) d.glowDisc(at.x, gy + y, at.z, 0.12f, CRIMSON_HOT, 0.6f * a * (1 - t / 30f));
            }
            d.darkRing(at.x, gy + 0.03, at.z, 0, 0.35f + 0.5f * Mth.clamp(t / 24f, 0, 1), ASH_DARK, 0.7f * a);
        }
    }

    /**
     * Her death: she burns from inside (light breaking out of her), black smoke wraps her, the renderer stops drawing her at
     * {@link #HIDE}, and the smoke is sucked down into a crack with a scream; after, the Book of the Damned glows darkly where it
     * lies until it is taken -- or, as it goes straight to the hunters who fought her, it shows itself where she fell in a dark glow
     * and flies into the hunter.
     */
    static final class Death extends MagicEffect {
        /** Her ticks of dying: burning, the smoke wrapping her, her body gone, the smoke sucked down, the Book. */
        static final int BURN = 150, WRAP = 140, HIDE = 172, SUCKED = 198;
        private final int abaddon, dying;
        private final long seed;
        private Vec3 pos;
        private double gy;
        private final List<Entity> books = new ArrayList<>();
        /** When the Book went straight to the hunters (no Book on the ground): it shows itself, then flies to them. */
        private int phantomAt = -1;
        private final ItemStack book = new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get());

        Death(int abaddon, Vec3 at, int ticks) {
            super(at, School.INFERNAL, Math.max(SUCKED + 2, ticks) + 240);
            this.abaddon = abaddon;
            this.dying = Math.max(SUCKED + 2, ticks);
            this.pos = at;
            this.gy = groundY(at);
            this.seed = seedOf(at);
        }

        @Override
        protected Vec3 anchor() {
            return pos;
        }

        @Override
        protected double reach() {
            return 8;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected boolean done() {
            return super.done() || age > dying + 20 && books.isEmpty() && (phantomAt < 0 || age - phantomAt > PHANTOM);
        }

        /** Ticks the Book shows itself (hovering where she fell) before it flies to the hunter, and the flight's end. */
        private static final int HOVER = 34, PHANTOM = 48;

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Entity e = entity(abaddon);
            if (e != null && !e.isRemoved() && age < SUCKED) {
                pos = e.position();
                if (age % 10 == 0) gy = groundY(pos);
            }
            if (age < BURN) {
                float k = age / (float) BURN;
                for (int i = FxParticles.rate(r, 0.8f + 4 * k); i > 0; i--) {
                    FxParticles.ember(level, r.nextBoolean() ? CRIMSON_HOT : HELLFIRE, 0.5f + 0.3f * k, 22, pos.x + FxParticles.j(r) * 0.35,
                            pos.y + 0.2 + r.nextDouble() * 1.6, pos.z + FxParticles.j(r) * 0.35, 0, 0.05 + 0.05 * k, 0);
                }
            }
            if (age >= WRAP && age < SUCKED) {
                for (int i = FxParticles.rate(r, 3f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double vy = age < HIDE ? 0.02 : -0.15;
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), pos.x + Mth.cos(a) * 0.7, pos.y + r.nextDouble() * 2.2,
                            pos.z + Mth.sin(a) * 0.7, -Mth.cos(a) * 0.05, vy, -Mth.sin(a) * 0.05);
                }
            }
            if (age == HIDE) {
                // The scream as she is dragged down.
                MagicSounds.play(AllSounds.abaddon("death"), pos, 2.2f, 1.35f);
                MagicShake.kick(pos, 0.5f, 26, 24);
            }
            if (age == SUCKED - 4) {
                MagicOverlay.flash(pos, 0x6A0A0A, 0.35f, 20);
                MagicShake.kick(pos, 0.9f, 10, 24);
            }
            if (age >= dying - 4 && age % 5 == 0) {
                books.clear();
                for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(10, 6, 10))) {
                    if (it.getItem().is(AllItems.BOOK_OF_THE_DAMNED.get())) books.add(it);
                }
            }
            for (Entity b : books) {
                if (b.isRemoved()) continue;
                if (r.nextFloat() < 0.5f) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), b.getX() + FxParticles.j(r) * 0.2, b.getY() + 0.3, b.getZ() + FxParticles.j(r) * 0.2,
                            0, 0.04, 0);
                }
                if (r.nextFloat() < 0.2f) FxParticles.ember(level, CRIMSON, 0.4f, 20, b.getX(), b.getY() + 0.3, b.getZ(), FxParticles.j(r) * 0.02, 0.03, FxParticles.j(r) * 0.02);
            }
            books.removeIf(Entity::isRemoved);
            if (age == dying + 8 && books.isEmpty()) phantomAt = age;
            if (phantomAt >= 0) {
                int k = age - phantomAt;
                Vec3 bp = phantomAt(k, 1f);
                if (k < HOVER && r.nextFloat() < 0.6f) {
                    FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), bp.x + FxParticles.j(r) * 0.2, bp.y, bp.z + FxParticles.j(r) * 0.2, 0, 0.03, 0);
                }
                Minecraft mc = Minecraft.getInstance();
                if (k == PHANTOM - 4 && mc.player != null && mc.player.distanceToSqr(pos) < 48 * 48) {
                    MagicOverlay.flash(mc.player.position(), CRIMSON, 0.25f, 8);
                    MagicSounds.play(SoundEvents.BOOK_PAGE_TURN, mc.player.position(), 1f, 0.6f);
                }
            }
        }

        /** Where the shown Book is {@code k} ticks after it appeared: sinking where she fell, then flying into the hunter. */
        private Vec3 phantomAt(float k, float partial) {
            Vec3 hover = new Vec3(pos.x, gy + 1.5 - 0.5 * easeOut(Math.min(k, HOVER) / HOVER) + 0.06 * Mth.sin(k * 0.2f), pos.z);
            if (k < HOVER) return hover;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.distanceToSqr(pos) > 48 * 48) return hover;
            Vec3 to = mc.player.getPosition(partial).add(0, mc.player.getBbHeight() * 0.55, 0);
            float u = Mth.clamp((k - HOVER) / (PHANTOM - 4f - HOVER), 0, 1);
            return hover.lerp(to, u * u).add(0, Mth.sin(Mth.PI * u) * 0.8, 0);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Entity e = entity(abaddon);
            Vec3 p = e != null && !e.isRemoved() && t < SUCKED ? e.getPosition(d.partial) : pos;
            if (t < HIDE + 4) {
                // Burning from inside: light breaking out of her through cracks, more and longer as it goes.
                float k = Mth.clamp(t / BURN, 0, 1), out = 1 - Mth.clamp((t - HIDE + 6) / 10f, 0, 1);
                int rays = 3 + (int) (k * 13);
                for (int i = 0; i < rays; i++) {
                    float yaw = hash(seed, i) * Mth.TWO_PI, pitch = (hash(seed, i + 50) - 0.5f) * 1.6f;
                    float len = (0.5f + 1.8f * k) * (0.6f + 0.4f * hash(seed, i + 90));
                    float flick = 0.6f + 0.4f * Mth.sin(t * 1.1f + i * 2.7f);
                    double cx = p.x, cy = p.y + 0.5 + 1.1 * hash(seed, i + 130), cz = p.z;
                    double ex = cx + Mth.cos(yaw) * Mth.cos(pitch) * len, ey = cy + Mth.sin(pitch) * len, ez = cz + Mth.sin(yaw) * Mth.cos(pitch) * len;
                    d.taper(cx, cy, cz, ex, ey, ez, 0.22f, 0, CRIMSON_HOT, 0.75f * flick * out * Math.min(1, k * 3), 0);
                    d.taper(cx, cy, cz, ex, ey, ez, 0.07f, 0, HELL_HOT, 0.9f * flick * out * Math.min(1, k * 3), 0);
                }
                d.glowDisc(p.x, p.y + 1.1, p.z, 0.8f + 1.4f * k, CRIMSON, (0.2f + 0.45f * k) * out);
                d.cracks(p.x, gy + 0.04, p.z, 8, 0.8f + 2.2f * k, 0.1f, seed, CRIMSON_HOT, (0.3f + 0.6f * k) * out);
            }
            if (t >= WRAP && t < SUCKED + 6) {
                // The smoke wrapping her, then sucked down into the crack under her.
                float wrap = Mth.clamp((t - WRAP) / 24f, 0, 1), suck = Mth.clamp((t - HIDE) / (SUCKED - HIDE), 0, 1);
                float top = 2.4f * (1 - suck * suck), fa = 1 - Mth.clamp((t - SUCKED) / 6f, 0, 1);
                for (int i = 0; i < 20; i++) {
                    float v = i / 20f;
                    float ang = v * Mth.TWO_PI * 2.5f + t * (0.3f + suck * 0.9f), rr = (0.75f - 0.25f * wrap) * (1 - suck * 0.7f);
                    double y = gy + 0.1 + v * top;
                    smoke(d, p.x + Mth.cos(ang) * rr, y, p.z + Mth.sin(ang) * rr, 0.45f + 0.15f * Mth.sin(v * 6 + t * 0.2f), 0.85f * wrap * fa);
                }
                float crack = suck > 0 ? fa : 0;
                d.cracks(p.x, gy + 0.05, p.z, 10, 1.2f + 1.8f * suck, 0.16f, seed + 7, CRIMSON_HOT, 0.9f * crack);
                d.darkRing(p.x, gy + 0.04, p.z, 0, 0.9f * crack, 0x050202, 0.8f * crack);
                d.flatGlow(p.x, gy + 0.05, p.z, 2.2f, CRIMSON, 0.5f * crack);
            }
            // The Book shown where she fell (it went straight to the hunters), then flying to them.
            if (phantomAt >= 0 && age - phantomAt < PHANTOM - 2) {
                float k = age - phantomAt + d.partial;
                Vec3 bp = phantomAt(k, d.partial);
                float in = Mth.clamp(k / 6f, 0, 1), pulse = 0.7f + 0.3f * Mth.sin(t * 0.15f), size = k < HOVER ? 0.9f : 0.9f - 0.6f * (k - HOVER) / (PHANTOM - HOVER);
                d.darkDisc(bp.x, bp.y, bp.z, 1.0f * size, SMOKE, 0.5f * in);
                d.glowDisc(bp.x, bp.y, bp.z, 0.7f * size, CRIMSON, 0.5f * in * pulse);
                item(d, bp, k * 0.06f, size * in);
            }
            // The Book of the Damned, lying in a dark glow.
            for (Entity b : books) {
                if (b.isRemoved()) continue;
                Vec3 bp = b.getPosition(d.partial).add(0, 0.35, 0);
                float pulse = 0.7f + 0.3f * Mth.sin(t * 0.15f);
                d.darkDisc(bp.x, bp.y, bp.z, 1.0f, SMOKE, 0.5f);
                d.glowDisc(bp.x, bp.y, bp.z, 0.7f, CRIMSON, 0.45f * pulse);
                for (int i = 0; i < 3; i++) {
                    float ang = t * 0.05f + i * Mth.TWO_PI / 3;
                    d.glowDisc(bp.x + Mth.cos(ang) * 0.5, bp.y + 0.15 * Mth.sin(t * 0.1f + i), bp.z + Mth.sin(ang) * 0.5, 0.08f, CRIMSON_HOT, 0.8f);
                }
                d.flatGlow(bp.x, b.getY() + 0.03, bp.z, 1.2f, CRIMSON, 0.25f * pulse);
            }
        }

        /** The Book itself, turning slowly. */
        private void item(FxDraw d, Vec3 p, float spin, float scale) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || scale <= 0.02f) return;
            Vec3 cam = d.eye();
            PoseStack pose = d.pose;
            pose.pushPose();
            pose.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            pose.mulPose(Axis.YP.rotation(spin));
            pose.scale(scale, scale, scale);
            mc.getItemRenderer().renderStatic(book, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, d.buffers, mc.level, 0);
            pose.popPose();
        }
    }
}
