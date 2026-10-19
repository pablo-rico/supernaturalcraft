package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * IMPACT: where a sigil's effect lands, each with a signature of its own ({@link MagicAssets#IMPACTS}), tinted by the school:
 * <ul>
 *   <li><b>smite</b> a column of light falling from the sky; <b>mend</b> rising motes and soft rings round the healed; <b>banishing</b>
 *   a blinding flash; <b>radiance</b> rays bursting outward; <b>halo</b> a ring of light over the head; <b>judgement</b> a sigil
 *   in the sky, then a pillar of light hammering down;</li>
 *   <li><b>hellfire</b> a bloom of fire over a scorched ring; <b>brimstone</b> burning stones falling through sulphur; <b>soul_rend</b>
 *   pale wisps torn out and dragged back to the caster; <b>hellchain</b> red-hot chains out of cracks in the ground;</li>
 *   <li><b>frost</b> ice shards bursting from the ground in mist; <b>hex</b> a thorny pulse; <b>pain</b> crimson veins crawling over
 *   the victim; <b>glamour</b> a shimmering veil of glyphs;</li>
 *   <li><b>repel</b> a shockwave; <b>reveal</b> a scanning ring sweeping out; <b>snare</b> chains of light pinning the target;
 *   <b>exorcise</b> black smoke torn upward; <b>salt_shot</b> a spray of salt; <b>silver_edge</b> silver slashes;</li>
 *   <li>anything else (index -1): a flash, a ring and sparks.</li>
 * </ul>
 * A positive radius (an area) also draws the area's edge on the ground.
 */
final class ImpactFx extends MagicEffect {

    private static final int SOUL = 0xC8F4FF, HOT_IRON = 0xFF6A24, SULPHUR = 0xE8D04A, SILVER = 0xE8F0FF, CRIMSON = 0xFF2048,
            HEAL = 0x9CFFB0, ROCK = 0x2A1A12;

    private final String style;
    private final int hit;
    private final Vec3 from;
    private final float radius;
    /** Visual size: the area's radius, or a default for a single target. */
    private final float r;
    private final long seed;
    private double ground;
    private boolean grounded;

    ImpactFx(MagicFxPayload p) {
        this(p, styleOf(p.argIndex()));
    }

    private ImpactFx(MagicFxPayload p, String style) {
        super(p.pos(), p.school(), Math.max(p.duration(), life(style)));
        this.style = style;
        this.hit = p.entity();
        this.from = p.target();
        this.radius = Math.max(0, p.arg2());
        this.r = Math.max(1.2f, radius);
        this.seed = (long) (p.pos().x * 31 + p.pos().z * 17 + p.pos().y * 7);
    }

    static String styleOf(int index) {
        return index >= 0 && index < MagicAssets.IMPACTS.size() ? MagicAssets.IMPACTS.get(index) : "generic";
    }

    private static int life(String style) {
        return switch (style) {
            case "smite", "hellfire", "radiance" -> 30;
            case "mend", "glamour", "exorcise", "brimstone" -> 42;
            case "frost" -> 56;
            case "banishing" -> 24;
            case "repel" -> 20;
            case "reveal" -> 64;
            case "snare", "hellchain" -> 50;
            case "halo" -> 60;
            case "judgement" -> 54;
            case "soul_rend" -> 38;
            case "hex" -> 34;
            case "pain" -> 30;
            case "salt_shot" -> 18;
            case "silver_edge" -> 14;
            default -> 18;
        };
    }

    @Override
    protected double reach() {
        return switch (style) {
            case "smite", "judgement", "brimstone" -> 16 + r;
            default -> 4 + r * 2;
        };
    }

    @Override
    protected Vec3 anchor() {
        Entity e = entity(hit);
        return e != null ? e.position() : at;
    }

    @Override
    protected double floor(ClientLevel level) {
        return grounded ? ground : super.floor(level);
    }

    private Vec3 target(float partial) {
        return centre(entity(hit), partial, at);
    }

    private Vec3 feet(float partial) {
        Entity e = entity(hit);
        return e != null && !e.isRemoved() ? e.getPosition(partial) : new Vec3(at.x, ground, at.z);
    }

    private float height() {
        Entity e = entity(hit);
        return e != null ? e.getBbHeight() : 1.8f;
    }

    // --- particles and sound ---------------------------------------------------------------------------------------------

    @Override
    protected void tick(ClientLevel level) {
        if (!grounded) {
            ground = FxWorld.floor(level, at, 8);
            grounded = true;
        }
        RandomSource rnd = level.random;
        Vec3 c = target(1f);
        if (age == 0) sound(level);
        switch (style) {
            case "smite" -> {
                if (age == 0) {
                    burst(level, rnd, new Vec3(at.x, ground + 0.1, at.z), 22, 0.12, 0.35, FxColor.core(school));
                    glyphs(level, rnd, new Vec3(at.x, ground + 0.2, at.z), 8, r * 0.8f);
                    MagicShake.kick(at, 0.6f, 10, 18);
                    MagicOverlay.flash(at, 0xFFF6D8, 0.22f, 10);
                }
            }
            case "mend" -> {
                for (int i = FxParticles.rate(rnd, 1.8f); i > 0; i--) {
                    Vec3 f = feet(1f);
                    float a = rnd.nextFloat() * Mth.TWO_PI;
                    FxParticles.mote(level, rnd.nextBoolean() ? HEAL : primary, 0.8f, 30, f.x + Mth.cos(a) * 0.5, f.y + rnd.nextDouble() * 0.4,
                            f.z + Mth.sin(a) * 0.5, 0, 0.04 + rnd.nextDouble() * 0.03, 0);
                }
            }
            case "banishing" -> {
                if (age == 0) {
                    burst(level, rnd, c, 34, 0.25, 0.1, 0xFFFFFF);
                    MagicOverlay.flash(at, 0xFFFFFF, 0.75f, 14);
                    MagicShake.kick(at, 0.5f, 8, 14);
                }
            }
            case "hellfire" -> {
                if (age == 0) {
                    for (int i = FxParticles.n(28); i > 0; i--) {
                        FxParticles.ember(level, rnd.nextBoolean() ? accent : primary, 1.2f, 0, c.x, c.y, c.z, FxParticles.j(rnd) * 0.18,
                                0.05 + rnd.nextDouble() * 0.2, FxParticles.j(rnd) * 0.18);
                    }
                    for (int i = FxParticles.n(10); i > 0; i--) {
                        FxParticles.plain(level, ParticleTypes.FLAME, c.x, c.y, c.z, FxParticles.j(rnd) * 0.12, rnd.nextDouble() * 0.12,
                                FxParticles.j(rnd) * 0.12);
                    }
                    for (int i = FxParticles.n(4); i > 0; i--) FxParticles.plain(level, ParticleTypes.LAVA, c.x, c.y, c.z, 0, 0, 0);
                    MagicShake.kick(at, 0.4f, 8, 12);
                }
                if (age < 20) {
                    for (int i = FxParticles.rate(rnd, 1.5f); i > 0; i--) {
                        FxParticles.ember(level, accent, 0.9f, 0, at.x + FxParticles.j(rnd) * r, ground + 0.1, at.z + FxParticles.j(rnd) * r,
                                0, 0.05, 0);
                    }
                }
            }
            case "frost" -> {
                if (age == 0) {
                    for (int i = FxParticles.n(24); i > 0; i--) {
                        FxParticles.salt(level, FxColor.ICE, 1.0f, 0, at.x + FxParticles.j(rnd) * r, ground + 0.4 + rnd.nextDouble() * 1.4,
                                at.z + FxParticles.j(rnd) * r, FxParticles.j(rnd) * 0.08, 0.1, FxParticles.j(rnd) * 0.08);
                    }
                    for (int i = FxParticles.n(14); i > 0; i--) {
                        FxParticles.plain(level, ParticleTypes.SNOWFLAKE, at.x + FxParticles.j(rnd) * r, ground + 0.5,
                                at.z + FxParticles.j(rnd) * r, FxParticles.j(rnd) * 0.06, 0.08, FxParticles.j(rnd) * 0.06);
                    }
                }
                if (rnd.nextInt(3) == 0 && FxParticles.n(1) > 0) {
                    FxParticles.plain(level, ParticleTypes.SNOWFLAKE, at.x + FxParticles.j(rnd) * r, ground + 0.2 + rnd.nextDouble(),
                            at.z + FxParticles.j(rnd) * r, 0, -0.01, 0);
                }
            }
            case "repel" -> {
                if (age == 0) {
                    for (int i = FxParticles.n(30); i > 0; i--) {
                        float a = rnd.nextFloat() * Mth.TWO_PI;
                        double s = 0.25 + rnd.nextDouble() * 0.25;
                        FxParticles.salt(level, 0xF4F4F0, 1f, 0, at.x, ground + 0.3 + rnd.nextDouble() * 0.8, at.z, Mth.cos(a) * s, 0.12,
                                Mth.sin(a) * s);
                    }
                    for (int i = FxParticles.n(8); i > 0; i--) FxParticles.plain(level, ParticleTypes.POOF, at.x, ground + 0.5, at.z,
                            FxParticles.j(rnd) * 0.2, 0.02, FxParticles.j(rnd) * 0.2);
                    MagicShake.kick(at, 0.35f, 8, 10);
                }
            }
            case "reveal" -> {
                float k = easeOut(age / (life * 0.7f));
                float rad = Math.max(6, radius) * k;
                for (int i = FxParticles.rate(rnd, 2.2f); i > 0 && age < life * 0.7f; i--) {
                    float a = rnd.nextFloat() * Mth.TWO_PI;
                    FxParticles.glyph(level, primary, 0.9f, 22, at.x + Mth.cos(a) * rad, ground + 0.15, at.z + Mth.sin(a) * rad, 0, 0.05, 0);
                }
            }
            case "snare", "hellchain" -> {
                if (age < 8) {
                    for (int i = FxParticles.rate(rnd, 2f); i > 0; i--) {
                        Vec3 f = feet(1f);
                        FxParticles.spark(level, style.equals("snare") ? accent : HOT_IRON, 0.8f, 0, f.x + FxParticles.j(rnd) * 1.4,
                                f.y + 0.1, f.z + FxParticles.j(rnd) * 1.4, 0, 0.06, 0);
                    }
                }
                if (style.equals("hellchain") && rnd.nextInt(2) == 0 && FxParticles.n(1) > 0) {
                    Vec3 f = feet(1f);
                    FxParticles.ember(level, HOT_IRON, 0.8f, 0, f.x + FxParticles.j(rnd) * 1.6, f.y + 0.05, f.z + FxParticles.j(rnd) * 1.6,
                            0, 0.05, 0);
                }
            }
            case "exorcise" -> {
                if (age < 22) {
                    for (int i = FxParticles.rate(rnd, 3.5f); i > 0; i--) {
                        FxParticles.plain(level, AllParticles.DEMON_SMOKE.get(), c.x + FxParticles.j(rnd) * 0.3, c.y + FxParticles.j(rnd) * 0.4,
                                c.z + FxParticles.j(rnd) * 0.3, FxParticles.j(rnd) * 0.03, 0.25 + rnd.nextDouble() * 0.25,
                                FxParticles.j(rnd) * 0.03);
                    }
                    for (int i = FxParticles.rate(rnd, 1.2f); i > 0; i--) {
                        FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, FxParticles.j(rnd) * 0.04, 0.3,
                                FxParticles.j(rnd) * 0.04);
                    }
                }
                if (age == 0) {
                    burst(level, rnd, c, 16, 0.2, 0.05, primary);
                    MagicShake.kick(at, 0.3f, 10, 10);
                }
            }
            case "radiance" -> {
                if (age == 0) burst(level, rnd, c, 26, 0.32, 0.0, FxColor.core(school));
                for (int i = FxParticles.rate(rnd, 0.8f); i > 0; i--) FxParticles.mote(level, primary, 0.7f, 24, c.x, c.y, c.z,
                        FxParticles.j(rnd) * 0.06, FxParticles.j(rnd) * 0.06, FxParticles.j(rnd) * 0.06);
            }
            case "halo" -> {
                if (rnd.nextInt(2) == 0 && FxParticles.n(1) > 0) {
                    Vec3 f = feet(1f);
                    float a = rnd.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, FxColor.core(school), 0.6f, 26, f.x + Mth.cos(a) * 0.36, f.y + height() + 0.3,
                            f.z + Mth.sin(a) * 0.36, 0, -0.02, 0);
                }
            }
            case "judgement" -> {
                if (age == 14) {
                    Vec3 g = new Vec3(at.x, ground + 0.1, at.z);
                    burst(level, rnd, g, 44, 0.18, 0.45, FxColor.core(school));
                    glyphs(level, rnd, g, 22, r * 1.6f);
                    MagicShake.kick(at, 1.1f, 18, 32);
                    MagicOverlay.flash(at, 0xFFF4D0, 0.5f, 22);
                    MagicSounds.school(school, "impact", at, 1.2f, 0.6f);
                }
                if (age < 14) {
                    for (int i = FxParticles.rate(rnd, 2f); i > 0; i--) {
                        float a = rnd.nextFloat() * Mth.TWO_PI;
                        FxParticles.glyph(level, primary, 1.1f, 20, at.x + Mth.cos(a) * r, ground + 0.1, at.z + Mth.sin(a) * r, 0, 0.12, 0);
                    }
                }
            }
            case "brimstone" -> {
                for (int i = 0; i < 5; i++) {
                    int land = 3 + i * 4 + 8;
                    double[] o = rock(i);
                    if (age < land && age > land - 8 && FxParticles.n(1) > 0) {
                        float f = (age - (land - 8)) / 8f;
                        FxParticles.ember(level, accent, 0.9f, 0, at.x + o[0], ground + 8 * (1 - f * f), at.z + o[1], 0, 0.02, 0);
                    }
                    if (age == land) {
                        for (int k = FxParticles.n(8); k > 0; k--) FxParticles.ember(level, primary, 1f, 0, at.x + o[0], ground + 0.2,
                                at.z + o[1], FxParticles.j(rnd) * 0.15, 0.1 + rnd.nextDouble() * 0.15, FxParticles.j(rnd) * 0.15);
                        for (int k = FxParticles.n(4); k > 0; k--) FxParticles.plain(level, AllParticles.YELLOW_SMOKE.get(), at.x + o[0],
                                ground + 0.3, at.z + o[1], FxParticles.j(rnd) * 0.05, 0.05, FxParticles.j(rnd) * 0.05);
                        if (FxParticles.n(1) > 0) FxParticles.plain(level, ParticleTypes.LAVA, at.x + o[0], ground + 0.2, at.z + o[1], 0, 0, 0);
                        MagicSounds.school(school, "impact", new Vec3(at.x + o[0], ground, at.z + o[1]), 0.35f, 1.3f + i * 0.08f);
                        MagicShake.kick(at, 0.25f, 5, 10);
                    }
                }
            }
            case "soul_rend" -> {
                for (int i = FxParticles.rate(rnd, 2f); i > 0 && age < 26; i--) {
                    FxParticles.plain(level, rnd.nextBoolean() ? AllParticles.SOUL_WISP.get() : ParticleTypes.SOUL_FIRE_FLAME,
                            c.x + FxParticles.j(rnd) * 0.4, c.y + FxParticles.j(rnd) * 0.6, c.z + FxParticles.j(rnd) * 0.4,
                            (from.x - c.x) * 0.02, 0.03, (from.z - c.z) * 0.02);
                }
            }
            case "hex" -> {
                if (age == 0) {
                    for (int i = FxParticles.n(16); i > 0; i--) {
                        float a = rnd.nextFloat() * Mth.TWO_PI;
                        FxParticles.mote(level, rnd.nextBoolean() ? primary : accent, 1f, 34, c.x + Mth.cos(a) * 0.6, c.y,
                                c.z + Mth.sin(a) * 0.6, Mth.cos(a) * 0.08, 0.03, Mth.sin(a) * 0.08);
                    }
                }
            }
            case "pain" -> {
                for (int i = FxParticles.rate(rnd, 1.5f); i > 0; i--) {
                    FxParticles.spark(level, CRIMSON, 0.7f, 0, c.x + FxParticles.j(rnd) * 0.4, c.y + FxParticles.j(rnd) * height() * 0.45,
                            c.z + FxParticles.j(rnd) * 0.4, 0, 0.03, 0);
                }
                if (age % 8 == 0 && FxParticles.n(2) > 0) {
                    FxParticles.plain(level, ParticleTypes.DAMAGE_INDICATOR, c.x, c.y + 0.3, c.z, 0, 0.1, 0);
                }
            }
            case "glamour" -> {
                for (int i = FxParticles.rate(rnd, 1.6f); i > 0; i--) {
                    Vec3 f = feet(1f);
                    float a = rnd.nextFloat() * Mth.TWO_PI;
                    FxParticles.mote(level, rnd.nextBoolean() ? primary : 0xFFFFFF, 0.6f, 20, f.x + Mth.cos(a) * 0.75,
                            f.y + rnd.nextDouble() * height(), f.z + Mth.sin(a) * 0.75, 0, 0.01, 0);
                }
            }
            case "salt_shot" -> {
                if (age == 0) {
                    Vec3 back = from.subtract(at);
                    back = back.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : back.normalize();
                    for (int i = FxParticles.n(40); i > 0; i--) {
                        FxParticles.salt(level, 0xF6F6F2, 1f, 0, at.x, at.y, at.z, back.x * 0.15 + FxParticles.j(rnd) * 0.2,
                                back.y * 0.15 + 0.1 + rnd.nextDouble() * 0.15, back.z * 0.15 + FxParticles.j(rnd) * 0.2);
                    }
                    for (int i = FxParticles.n(4); i > 0; i--) FxParticles.plain(level, ParticleTypes.POOF, at.x, at.y, at.z,
                            FxParticles.j(rnd) * 0.05, 0.03, FxParticles.j(rnd) * 0.05);
                }
            }
            case "silver_edge" -> {
                if (age == 0) {
                    burst(level, rnd, c, 16, 0.22, 0.05, SILVER);
                    for (int i = FxParticles.n(6); i > 0; i--) FxParticles.plain(level, ParticleTypes.CRIT, c.x, c.y, c.z,
                            FxParticles.j(rnd) * 0.4, FxParticles.j(rnd) * 0.3, FxParticles.j(rnd) * 0.4);
                }
            }
            default -> {
                if (age == 0) burst(level, rnd, c, 18, 0.2, 0.05, primary);
            }
        }
    }

    private void sound(ClientLevel level) {
        float pitch = 0.95f + level.random.nextFloat() * 0.1f;
        float vol = switch (style) {
            case "judgement" -> 1.3f;
            case "smite", "banishing", "hellfire" -> 1.0f;
            case "brimstone" -> 0.5f;
            case "salt_shot", "silver_edge", "halo", "mend" -> 0.55f;
            default -> 0.8f;
        };
        if (style.equals("mend") || style.equals("halo") || style.equals("glamour")) pitch += 0.3f;
        if (style.equals("pain") || style.equals("soul_rend") || style.equals("exorcise")) pitch -= 0.2f;
        MagicSounds.school(school, "impact", at, vol, pitch);
    }

    private void burst(ClientLevel level, RandomSource rnd, Vec3 c, int count, double speed, double up, int rgb) {
        for (int i = FxParticles.n(count); i > 0; i--) {
            FxParticles.spark(level, rgb, 1f, 0, c.x, c.y, c.z, FxParticles.j(rnd) * speed, FxParticles.j(rnd) * speed * 0.6 + up * rnd.nextDouble(),
                    FxParticles.j(rnd) * speed);
        }
    }

    private void glyphs(ClientLevel level, RandomSource rnd, Vec3 c, int count, float spread) {
        for (int i = FxParticles.n(count); i > 0; i--) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            FxParticles.glyph(level, rnd.nextBoolean() ? primary : accent, 1.1f, 0, c.x + Mth.cos(a) * spread * rnd.nextFloat(), c.y,
                    c.z + Mth.sin(a) * spread * rnd.nextFloat(), Mth.cos(a) * 0.04, 0.08 + rnd.nextDouble() * 0.08, Mth.sin(a) * 0.04);
        }
    }

    /** Where brimstone stone {@code i} lands, off the centre (fixed per impact). */
    private double[] rock(int i) {
        long s = seed * 31 + i * 977L;
        double a = (s % 360) * Mth.DEG_TO_RAD + i * 1.9, d = (i == 0 ? 0 : 0.4 + (Math.abs(s) % 100) / 100.0) * r * 0.8;
        return new double[]{Math.cos(a) * d, Math.sin(a) * d};
    }

    // --- geometry ----------------------------------------------------------------------------------------------------------

    @Override
    protected void render(FxDraw d) {
        if (!grounded) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            ground = FxWorld.floor(level, at, 8);
            grounded = true;
        }
        float t = time(d), k = progress(d);
        if (radius > 0 && !style.equals("reveal") && !style.equals("repel")) {
            float a = fade(t, 2, life * 0.6f);
            d.groundRing(at.x, ground + 0.03, at.z, radius * 0.94f, radius, primary, 0.6f * a, false);
            d.groundRing(at.x, ground + 0.03, at.z, radius * 0.7f, radius * 0.94f, primary, 0.15f * a, true);
        }
        switch (style) {
            case "smite" -> smite(d, t);
            case "mend" -> mend(d, t);
            case "banishing" -> banishing(d, t);
            case "hellfire" -> hellfire(d, t);
            case "frost" -> frost(d, t);
            case "repel" -> repel(d, t);
            case "reveal" -> reveal(d, t);
            case "snare" -> chains(d, t, accent, primary, 1f, false);
            case "hellchain" -> chains(d, t, HOT_IRON, primary, 1.4f, true);
            case "exorcise" -> exorcise(d, t);
            case "radiance" -> radiance(d, t);
            case "halo" -> halo(d, t);
            case "judgement" -> judgement(d, t);
            case "brimstone" -> brimstone(d, t);
            case "soul_rend" -> soulRend(d, t);
            case "hex" -> hex(d, t);
            case "pain" -> pain(d, t);
            case "glamour" -> glamour(d, t);
            case "salt_shot" -> saltShot(d, t);
            case "silver_edge" -> silverEdge(d, t);
            default -> generic(d, t, k);
        }
    }

    private void generic(FxDraw d, float t, float k) {
        Vec3 c = target(d.partial);
        float a = 1 - k;
        d.star(c.x, c.y, c.z, 1.4f * (1 - k * 0.5f), t * 0.1f, primary, a);
        d.glowDisc(c.x, c.y, c.z, 1.6f, FxColor.core(school), 0.5f * a);
        d.ring(c.x, c.y, c.z, (float) (c.x - d.eye().x), (float) (c.y - d.eye().y), (float) (c.z - d.eye().z),
                0.3f + 1.4f * easeOut(k), 0.5f + 1.6f * easeOut(k), primary, 0.6f * a, true);
    }

    private void smite(FxDraw d, float t) {
        float a = fade(t, 1, 14);
        float w = t < 3 ? 1.6f * t / 3 : 1.6f * Mth.clamp(1 - (t - 3) / 20f, 0.15f, 1);
        double top = ground + 16;
        d.beam(at.x, top, at.z, at.x, ground, at.z, w, primary, a);
        d.softBeam(at.x, ground, at.z, at.x, top, at.z, w * 3.5f, primary, 0.45f * a, 0, false);
        d.cylinder(at.x, ground, at.z, w * 0.55f, 16, primary, 0.5f * a, 0);
        d.glowDisc(at.x, ground + 0.3, at.z, 2.4f * r * (1 - 0.3f * a), FxColor.core(school), 0.7f * a);
        float k = easeOut(t / 14f);
        d.groundRing(at.x, ground + 0.04, at.z, 0.2f + r * 1.4f * k, 0.6f + r * 1.6f * k, primary, 0.8f * (1 - k), true);
        if (t < 6) d.star(at.x, ground + 0.5, at.z, 3.5f, 0.3f, 0xFFFFFF, 1 - t / 6f);
    }

    private void mend(FxDraw d, float t) {
        Vec3 f = feet(d.partial);
        float h = height(), a = fade(t, 4, 14);
        int col = FxColor.mix(primary, HEAL, 0.4f);
        for (int i = 0; i < 3; i++) {
            float u = ((t / 26f) + i / 3f) % 1f;
            float rr = 0.75f - 0.3f * u;
            d.groundRing(f.x, f.y + 0.1 + u * h, f.z, rr - 0.1f, rr, col, 0.65f * a * Mth.sin(Mth.PI * u), true);
        }
        d.glowDisc(f.x, f.y + h * 0.5, f.z, 1.1f, col, 0.25f * a);
        d.groundRing(f.x, f.y + 0.04, f.z, 0.5f, 1.0f, col, 0.35f * a, true);
    }

    private void banishing(FxDraw d, float t) {
        Vec3 c = target(d.partial);
        float k = Mth.clamp(t / 10f, 0, 1), a = 1 - k;
        d.star(c.x, c.y, c.z, 6f * r * (1 - 0.6f * k), 0.2f + t * 0.03f, 0xFFFFFF, a);
        d.glowDisc(c.x, c.y, c.z, 3.2f * r, 0xFFFFFF, 0.9f * a);
        d.glowDisc(c.x, c.y, c.z, 4.5f * r, primary, 0.5f * fade(t, 0, 16));
        float rk = easeOut(t / life);
        d.groundRing(c.x, ground + 0.05, c.z, r * 3.6f * rk, r * 3.6f * rk + 0.6f, primary, 0.7f * (1 - rk), true);
        d.ring(c.x, c.y, c.z, 0, 1, 0, r * 2.4f * rk, r * 2.4f * rk + 0.3f, 0xFFFFFF, 0.6f * (1 - rk), true);
    }

    private void hellfire(FxDraw d, float t) {
        Vec3 c = target(d.partial);
        float bloom = easeOut(t / 9f), a = fade(t, 0, 18);
        d.sphere(c.x, c.y, c.z, r * 1.05f * bloom, primary, 0.35f * a);
        d.sphere(c.x, c.y, c.z, r * 0.6f * bloom, FxColor.mix(accent, 0xFFF0A0, 0.4f), 0.5f * a);
        d.glowDisc(c.x, c.y, c.z, r * 2.4f, accent, 0.6f * a);
        if (t < 4) d.star(c.x, c.y, c.z, r * 3f, 0.6f, 0xFFF0C0, 1 - t / 4f);
        float scorch = fade(t, 2, 20);
        d.darkRing(at.x, ground + 0.02, at.z, 0, r * 1.5f, 0x140804, 0.65f * scorch);
        d.cracks(at.x, ground + 0.035, at.z, 7, r * 1.5f, 0.06f, seed, accent, 0.85f * fade(t, 2, 24));
        d.groundRing(at.x, ground + 0.04, at.z, r * 1.2f * bloom, r * 1.5f * bloom, primary, 0.5f * a, true);
    }

    private void frost(FxDraw d, float t) {
        int shards = 7 + (int) (r * 2);
        float grow = easeOutBack(t / 6f), a = fade(t, 1, 14);
        float sink = Mth.clamp((t - (life - 14)) / 14f, 0, 1);
        for (int i = 0; i < shards; i++) {
            long s = seed + i * 7919L;
            float ang = i * Mth.TWO_PI / shards + (s % 100) * 0.006f;
            float dist = (i % 3 == 0 ? 0.25f : 0.55f) * r + (Math.abs(s) % 50) / 100f;
            float len = (0.7f + (Math.abs(s >> 4) % 60) / 100f) * (i % 3 == 0 ? 1.4f : 1f) * grow;
            float tilt = 0.35f + (Math.abs(s >> 8) % 40) / 100f;
            float dx = Mth.cos(ang) * tilt, dz = Mth.sin(ang) * tilt;
            double bx = at.x + Mth.cos(ang) * dist, bz = at.z + Mth.sin(ang) * dist, by = ground - 0.1 - sink * len;
            d.crystal(bx, by + len * 0.5, bz, dx, 1, dz, 0.16f * Math.max(0.3f, grow), len * 0.6f, FxColor.ICE, 0.5f * a, true);
            d.crystal(bx, by + len * 0.5, bz, dx, 1, dz, 0.1f * Math.max(0.3f, grow), len * 0.55f, 0xFFFFFF, 0.35f * a, false);
        }
        // Mist: pale drifting blots low over the shards.
        for (int i = 0; i < 4; i++) {
            float ang = i * 1.7f + t * 0.01f;
            d.glowDisc(at.x + Mth.cos(ang) * r * 0.6, ground + 0.35 + 0.1 * i, at.z + Mth.sin(ang) * r * 0.6, 1.4f + r * 0.4f, FxColor.ICE,
                    0.12f * fade(t, 6, 24));
        }
        d.groundRing(at.x, ground + 0.04, at.z, 0, r * 1.5f, FxColor.ICE, 0.25f * a, true);
        if (t < 4) d.star(at.x, ground + 0.6, at.z, 2.2f, 0.2f, 0xFFFFFF, 1 - t / 4f);
    }

    private void repel(FxDraw d, float t) {
        float far = Math.max(3f, radius > 0 ? radius : r * 2.6f);
        for (int i = 0; i < 2; i++) {
            float k = easeOut((t - i * 3) / (life * 0.7f));
            if (k <= 0) continue;
            float rad = far * k, a = (1 - k) * (i == 0 ? 1f : 0.6f);
            d.groundRing(at.x, ground + 0.05, at.z, Math.max(0, rad - 0.7f), rad, i == 0 ? primary : accent, 0.85f * a, true);
            d.cylinder(at.x, ground, at.z, rad, 1.3f * (1 - k * 0.5f), primary, 0.45f * a, 0);
        }
        if (t < 4) d.glowDisc(at.x, ground + 0.6, at.z, 2f, FxColor.core(school), 1 - t / 4f);
    }

    private void reveal(FxDraw d, float t) {
        float far = Math.max(6, radius);
        float k = easeOut(t / (life * 0.7f)), a = 1 - Mth.clamp((t - life * 0.5f) / (life * 0.5f), 0, 1);
        float rad = far * k;
        d.groundRing(at.x, ground + 0.06, at.z, Math.max(0, rad - 0.3f), rad, FxColor.core(school), 0.9f * a, false);
        d.groundRing(at.x, ground + 0.05, at.z, Math.max(0, rad - 2.2f), rad - 0.3f, primary, 0.3f * a, true);
        d.cylinder(at.x, ground, at.z, rad, 2.2f, primary, 0.35f * a, 0);
        float k2 = easeOut((t - 10) / (life * 0.7f));
        if (k2 > 0) d.groundRing(at.x, ground + 0.05, at.z, Math.max(0, far * k2 - 0.2f), far * k2, accent, 0.5f * a * (1 - k2), false);
        if (MagicFxManager.detail() > 0) {
            d.band(MagicTextures.glyphStrip(), at.x, ground, at.z, Math.max(0.5f, rad * 0.98f), 0.5f, t * 0.01f, primary, 0.5f * a * (1 - k), 0);
        }
    }

    private void chains(FxDraw d, float t, int link, int glow, float size, boolean infernal) {
        Vec3 c = target(d.partial), f = feet(d.partial);
        float a = fade(t, 1, 10);
        float reach = easeOut(t / 7f);
        int count = 4;
        for (int i = 0; i < count; i++) {
            float ang = Mth.PI / 4 + i * Mth.TWO_PI / count + (float) (seed % 7) * 0.1f;
            double ax = f.x + Mth.cos(ang) * 1.7, az = f.z + Mth.sin(ang) * 1.7, ay = infernal ? f.y - 0.05 : f.y + 0.02;
            double bx = ax + (c.x - ax) * reach, by = ay + (c.y - ay) * reach, bz = az + (c.z - az) * reach;
            chain(d, ax, ay, az, bx, by, bz, size, link, glow, a, t);
            if (infernal) d.cracks(ax, ay + 0.06, az, 4, 0.8f, 0.07f, seed + i, HOT_IRON, 0.9f * a);
            else d.decal(MagicTextures.castCircle(), ax, ay + 0.03, az, 0.38f, t * 0.05f, glow, 0.8f * a);
            d.glowDisc(ax, ay + 0.15, az, 0.5f, glow, 0.5f * a);
        }
        d.glowDisc(c.x, c.y, c.z, 0.9f, glow, 0.25f * a);
    }

    /** A chain of links from a to b (each link a small ring, turned alternately) with a glow along it. */
    static void chain(FxDraw d, double ax, double ay, double az, double bx, double by, double bz, float size, int link, int glow, float alpha,
                      float t) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05) return;
        float ux = (float) (dx / len), uy = (float) (dy / len), uz = (float) (dz / len);
        float[] p = new float[3], q = new float[3];
        FxDraw.basis(ux, uy, uz, p, q);
        float step = 0.2f * size;
        int n = (int) (len / step);
        float lr = 0.085f * size, lw = 0.03f * size;
        for (int i = 0; i <= n; i++) {
            double s = i * step;
            double x = ax + ux * s, y = ay + uy * s, z = az + uz * s;
            // A link's ring is pierced by the next: its plane holds the chain's direction.
            float[] nrm = i % 2 == 0 ? p : q;
            float sh = 0.75f + 0.25f * Mth.sin(t * 0.6f - i * 0.5f);
            d.ring(x, y, z, nrm[0], nrm[1], nrm[2], lr - lw, lr, link, alpha * sh, false);
        }
        d.ribbon(ax, ay, az, bx, by, bz, 0.22f * size, glow, 0.35f * alpha, 0.35f * alpha);
    }

    private void exorcise(FxDraw d, float t) {
        Vec3 c = target(d.partial);
        float a = fade(t, 1, 16);
        // A column of black smoke tearing upward: blots rising fast, stretched out.
        for (int i = 0; i < 7; i++) {
            float u = ((t * 0.06f) + i / 7f) % 1f;
            float h = u * 6f;
            float w = 0.5f + u * 1.2f;
            float fade = Mth.sin(Mth.PI * u) * a;
            d.darkDisc(c.x + Mth.sin(i * 2.1f + t * 0.2f) * 0.2 * u, c.y + h, c.z + Mth.cos(i * 1.3f + t * 0.2f) * 0.2 * u, w,
                    FxColor.SMOKE, 0.75f * fade);
        }
        float k = easeOut(t / 12f);
        d.groundRing(c.x, ground + 0.05, c.z, r * 1.6f * k, r * 1.6f * k + 0.5f, primary, 0.7f * (1 - k), true);
        d.glowDisc(c.x, c.y, c.z, 1.6f, primary, 0.5f * fade(t, 0, 10));
    }

    private void radiance(FxDraw d, float t) {
        Vec3 c = target(d.partial);
        float a = fade(t, 0, 16), k = easeOut(t / 10f);
        int rays = 12;
        for (int i = 0; i < rays; i++) {
            long s = seed + i * 104729L;
            float yaw = i * Mth.TWO_PI / rays + t * 0.01f, pitch = ((Math.abs(s) % 100) / 100f - 0.5f) * 1.6f;
            float len = r * (1.8f + (Math.abs(s >> 6) % 80) / 80f) * k;
            double ex = c.x + Mth.cos(yaw) * Mth.cos(pitch) * len, ey = c.y + Mth.sin(pitch) * len, ez = c.z + Mth.sin(yaw) * Mth.cos(pitch) * len;
            d.taper(c.x, c.y, c.z, ex, ey, ez, 0.35f, 0, primary, 0.8f * a, 0);
            d.taper(c.x, c.y, c.z, ex, ey, ez, 0.1f, 0, 0xFFFFFF, 0.9f * a, 0);
        }
        d.glowDisc(c.x, c.y, c.z, r * 1.6f, FxColor.core(school), 0.65f * a);
    }

    private void halo(FxDraw d, float t) {
        Vec3 f = feet(d.partial);
        float a = fade(t, 6, 14), y = height() + 0.32f + 0.04f * Mth.sin(t * 0.15f);
        d.groundRing(f.x, f.y + y, f.z, 0.3f, 0.38f, FxColor.core(school), 0.95f * a, false);
        d.groundRing(f.x, f.y + y, f.z, 0.18f, 0.6f, primary, 0.4f * a, true);
        d.cylinder(f.x, f.y, f.z, 0.34f, y, primary, 0.02f * a, 0.15f * a);
        d.glowDisc(f.x, f.y + y, f.z, 0.7f, primary, 0.25f * a);
    }

    private void judgement(FxDraw d, float t) {
        double sky = ground + 14;
        float pre = Mth.clamp(t / 14f, 0, 1), a = fade(t, 2, 18);
        float spin = t * 0.04f;
        // The sigil gathers in the sky...
        d.decal(MagicTextures.castCircle(), at.x, sky, at.z, 2.6f * r * easeOutBack(pre), spin, primary, 0.95f * a);
        d.decal(MagicTextures.castCircle(), at.x, sky - 0.02, at.z, 1.5f * r * easeOutBack(pre), -spin * 1.6f, accent, 0.75f * a);
        d.glyphRing(MagicTextures.glyphStrip(), at.x, sky + 0.01, at.z, 0, 1, 0, 2.7f * r * pre, 3.3f * r * pre, spin * 0.5f, primary,
                0.85f * a);
        // ...and its shadow of glyphs on the ground beneath.
        if (t < 14) d.band(MagicTextures.glyphStrip(), at.x, ground, at.z, r * 1.2f, 0.5f * pre, -spin, primary, 0.8f * pre, 0);
        d.groundRing(at.x, ground + 0.04, at.z, r * 1.15f, r * 1.25f, primary, 0.8f * a * pre, false);
        if (t < 14) return;
        // ...then it falls.
        float s = t - 14, w = s < 3 ? 3.2f * s / 3 : 3.2f * Mth.clamp(1 - (s - 3) / 26f, 0.1f, 1);
        float fa = fade(t, 0, 16);
        d.beam(at.x, sky, at.z, at.x, ground, at.z, w, primary, fa);
        d.softBeam(at.x, ground, at.z, at.x, sky, at.z, w * 3.2f, primary, 0.5f * fa, 0.1f * fa, false);
        d.cylinder(at.x, ground, at.z, w * 0.6f, (float) (sky - ground), FxColor.core(school), 0.45f * fa, 0.1f * fa);
        d.glowDisc(at.x, ground + 0.5, at.z, 4.5f * r, FxColor.core(school), 0.8f * fa);
        float k = easeOut(s / 16f);
        d.groundRing(at.x, ground + 0.05, at.z, 0.4f + r * 3f * k, 1f + r * 3.4f * k, primary, 0.9f * (1 - k), true);
        if (s < 6) d.star(at.x, ground + 1, at.z, 7f, 0.25f, 0xFFFFFF, 1 - s / 6f);
        d.cracks(at.x, ground + 0.04, at.z, 9, r * 2.4f, 0.09f, seed, FxColor.core(school), 0.8f * fade(t, 14, 20));
    }

    private void brimstone(FxDraw d, float t) {
        float a = fade(t, 4, 14);
        d.glowDisc(at.x, ground + 0.4, at.z, r * 2.2f, SULPHUR, 0.14f * a);
        for (int i = 0; i < 5; i++) {
            int land = 3 + i * 4 + 8;
            double[] o = rock(i);
            double x = at.x + o[0], z = at.z + o[1];
            if (t < land && t > land - 8) {
                float f = (t - (land - 8)) / 8f;
                double y = ground + 8 * (1 - f * f);
                d.crystal(x, y, z, 0.3f, 1, 0.2f, 0.28f, 0.34f, ROCK, 0.95f, false);
                d.glowDisc(x, y, z, 0.9f, accent, 0.7f);
                d.taper(x, y, z, x, y + 1.6, z, 0.4f, 0, primary, 0.6f, 0);
            } else if (t >= land && t < land + 10) {
                float k = (t - land) / 10f;
                d.glowDisc(x, ground + 0.3, z, 1.8f, accent, 0.8f * (1 - k));
                d.groundRing(x, ground + 0.04, z, 0.2f + 1.4f * k, 0.6f + 1.6f * k, primary, 0.7f * (1 - k), true);
                d.crystal(x, ground + 0.12, z, 0.3f, 1, 0.2f, 0.28f, 0.2f, ROCK, 0.9f * a, false);
            } else if (t >= land + 10) {
                d.darkRing(x, ground + 0.02, z, 0, 0.9f, 0x120804, 0.55f * a);
                d.crystal(x, ground + 0.12, z, 0.3f, 1, 0.2f, 0.28f, 0.2f, ROCK, 0.9f * a, false);
            }
        }
    }

    private void soulRend(FxDraw d, float t) {
        Vec3 c = target(d.partial);
        Vec3 to = from.distanceToSqr(c) < 1 ? c.add(0, 3, 0) : from;
        float a = fade(t, 2, 12);
        int wisps = 5;
        for (int w = 0; w < wisps; w++) {
            float lag = w * 0.08f;
            float head = Mth.clamp(t / (life * 0.75f) - lag, 0, 1);
            Vec3 prev = null;
            for (int s = 0; s <= 6; s++) {
                float u = head - s * 0.05f;
                if (u < 0) break;
                Vec3 p = wisp(c, to, u, w);
                if (prev != null) {
                    float fade = (1 - s / 7f) * a;
                    d.taper(prev.x, prev.y, prev.z, p.x, p.y, p.z, 0.26f * (1 - s / 7f), 0.26f * (1 - (s + 1) / 7f), primary, 0.6f * fade,
                            0.4f * fade);
                    d.taper(prev.x, prev.y, prev.z, p.x, p.y, p.z, 0.1f, 0.06f, SOUL, 0.9f * fade, 0.6f * fade);
                }
                prev = p;
            }
            if (prev != null) d.glowDisc(prev.x, prev.y, prev.z, 0.35f, SOUL, 0.8f * a);
        }
        d.glowDisc(c.x, c.y, c.z, 1.2f, primary, 0.35f * fade(t, 0, 20));
    }

    /** A wisp's path: out of the victim in a widening spiral, then dragged to the caster. */
    private static Vec3 wisp(Vec3 c, Vec3 to, float u, int w) {
        float ang = w * Mth.TWO_PI / 5 + u * 9f;
        float spread = Mth.sin(Mth.PI * Math.min(1, u * 1.4f)) * 1.3f;
        Vec3 along = c.add(to.subtract(c).scale(u * u));
        return along.add(Mth.cos(ang) * spread, Mth.sin(ang * 0.7f) * spread * 0.5 + u * 0.8, Mth.sin(ang) * spread);
    }

    private void hex(FxDraw d, float t) {
        Vec3 f = feet(d.partial), c = target(d.partial);
        float k = easeOut(t / 16f), a = fade(t, 0, 16);
        d.spikedRing(f.x, f.y + 0.06, f.z, r * (0.6f + 1.2f * k), 0.45f, 11, t * 0.03f, primary, 0.85f * a);
        float k2 = easeOut((t - 4) / 16f);
        if (k2 > 0) d.spikedRing(f.x, f.y + 0.07, f.z, r * (0.4f + 0.9f * k2), 0.3f, 7, -t * 0.05f, accent, 0.6f * a);
        // Thorns curling up out of the ground.
        for (int i = 0; i < 8; i++) {
            float ang = i * Mth.TWO_PI / 8 + 0.3f;
            float h = 1.1f * easeOutBack((t - i) / 6f);
            if (h <= 0) continue;
            double bx = f.x + Mth.cos(ang) * 0.9, bz = f.z + Mth.sin(ang) * 0.9;
            d.taper(bx, f.y, bz, bx - Mth.cos(ang) * 0.35, f.y + h, bz - Mth.sin(ang) * 0.35, 0.16f, 0, primary, 0.8f * a, 0.2f * a);
        }
        d.glowDisc(c.x, c.y, c.z, 1.3f, primary, 0.35f * a);
    }

    private void pain(FxDraw d, float t) {
        Entity e = entity(hit);
        Vec3 c = target(d.partial);
        float a = fade(t, 1, 8);
        float hw = e != null ? e.getBbWidth() * 0.6f : 0.4f, hh = height() * 0.5f;
        long frame = (long) (t / 2);
        int col = FxColor.mix(CRIMSON, primary, 0.3f);
        for (int i = 0; i < 5; i++) {
            long s = seed + frame * 131 + i * 17;
            double x0 = c.x + jit(s, hw), y0 = c.y + jit(s >> 3, hh), z0 = c.z + jit(s >> 6, hw);
            double x1 = c.x + jit(s >> 9, hw), y1 = c.y + jit(s >> 12, hh), z1 = c.z + jit(s >> 15, hw);
            d.lightning(x0, y0, z0, x1, y1, z1, s, 5, 0.12f, 0.035f, col, 0.85f * a, false);
        }
        float pulse = 0.6f + 0.4f * Mth.sin(t * 0.9f);
        d.glowDisc(c.x, c.y, c.z, 1.3f, CRIMSON, 0.35f * a * pulse);
    }

    private static double jit(long s, float half) {
        return ((Math.abs(s) % 1000) / 1000.0 - 0.5) * 2 * half;
    }

    private void glamour(FxDraw d, float t) {
        Vec3 f = feet(d.partial);
        float h = height() * 1.1f, a = fade(t, 6, 14);
        float shimmer = 0.7f + 0.3f * Mth.sin(t * 0.5f);
        var strip = MagicTextures.glyphStrip();
        d.band(strip, f.x, f.y, f.z, 0.75f, h, t * 0.04f, primary, 0.55f * a * shimmer, 0.1f * a);
        d.band(strip, f.x, f.y + h * 0.1, f.z, 0.62f, h * 0.8f, -t * 0.06f, accent, 0.35f * a, 0);
        for (int i = 0; i < 7; i++) {
            float ang = i * 2.4f + t * 0.03f, y = (i * 0.37f + t * 0.01f) % 1f;
            float blink = Math.max(0, Mth.sin(t * 0.35f + i * 1.7f));
            d.star(f.x + Mth.cos(ang) * 0.78, f.y + y * h, f.z + Mth.sin(ang) * 0.78, 0.3f, t * 0.1f, 0xFFFFFF, blink * a);
        }
    }

    private void saltShot(FxDraw d, float t) {
        Vec3 back = from.subtract(at);
        if (back.lengthSqr() < 1e-4) back = new Vec3(0, 1, 0);
        float k = easeOut(t / 8f), a = 1 - Mth.clamp(t / life, 0, 1);
        d.ring(at.x, at.y, at.z, (float) back.x, (float) back.y, (float) back.z, 0.15f + 1.1f * k, 0.35f + 1.3f * k, 0xF6F6F2, 0.7f * a, true);
        d.glowDisc(at.x, at.y, at.z, 0.9f, 0xFFFFFF, 0.6f * a);
    }

    private void silverEdge(FxDraw d, float t) {
        Vec3 c = target(d.partial);
        float sweep = Mth.clamp(t / 5f, 0, 1), a = 1 - Mth.clamp((t - 4) / (life - 4), 0, 1);
        for (int arc = 0; arc < 3; arc++) {
            float tilt = -0.6f + arc * 0.6f + (seed % 5) * 0.1f, yaw = arc * 1.1f + (seed % 3);
            float[] nrm = {Mth.sin(tilt) * Mth.cos(yaw), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(yaw)};
            float[] p = new float[3], q = new float[3];
            FxDraw.basis(nrm[0], nrm[1], nrm[2], p, q);
            int segs = 10;
            float span = 2.2f * sweep, start = -1.1f + arc * 0.4f;
            double px = 0, py = 0, pz = 0;
            for (int s = 0; s <= segs; s++) {
                float ang = start + span * s / segs;
                float rad = 0.9f;
                double x = c.x + (p[0] * Mth.cos(ang) + q[0] * Mth.sin(ang)) * rad;
                double y = c.y + (p[1] * Mth.cos(ang) + q[1] * Mth.sin(ang)) * rad;
                double z = c.z + (p[2] * Mth.cos(ang) + q[2] * Mth.sin(ang)) * rad;
                if (s > 0) {
                    float w = 0.16f * Mth.sin(Mth.PI * s / segs);
                    d.ribbon(px, py, pz, x, y, z, w * 2.2f, primary, 0.5f * a, 0.5f * a);
                    d.ribbon(px, py, pz, x, y, z, w, SILVER, a, a);
                }
                px = x;
                py = y;
                pz = z;
            }
        }
        d.glowDisc(c.x, c.y, c.z, 0.8f, SILVER, 0.5f * a);
    }
}
