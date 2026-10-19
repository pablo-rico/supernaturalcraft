package org.papiricoh.supernaturalcraft.client.witch;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxColor;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicTextures;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * A hexed villager freed (v0.21, its {@code freed} clip): the hex lifts off it — its marks rise as turning sigils from the chest
 * and head, magenta cooling to gold as they climb, a ring rising up its body — and as it becomes a villager again, a burst of
 * golden sparkles. Started by {@code WitchAmbience} the first time the client sees {@link HexedVillagerEntity#freeing()}; the
 * server's HEX_PULSE (protect) shell plays alongside.
 */
final class HexLiftFx extends MagicEffect {

    static final int GOLD = 0xFFD86A, WHITE_GOLD = 0xFFF4D0;

    private final int villager;
    private final float height;
    private Vec3 last;

    HexLiftFx(HexedVillagerEntity v) {
        super(v.position(), School.WITCHCRAFT, HexedVillagerEntity.FREE_TICKS + 14);
        this.villager = v.getId();
        this.height = v.getBbHeight();
        this.last = v.position();
    }

    private Vec3 feet(float partial) {
        Entity e = entity(villager);
        if (e != null && !e.isRemoved()) last = e.getPosition(partial);
        return last;
    }

    @Override
    protected Vec3 anchor() {
        return last;
    }

    /** Magenta at the start of the climb, gold at its end. */
    private int climb(float u) {
        return FxColor.mix(primary, GOLD, Mth.clamp(u, 0, 1));
    }

    @Override
    protected void tick(ClientLevel level) {
        RandomSource r = level.random;
        Vec3 f = feet(1);
        int burst = HexedVillagerEntity.FREE_TICKS - 2;
        if (age < burst) {
            for (int i = FxParticles.rate(r, 1.6f); i > 0; i--) {
                double y = f.y + height * (0.45 + r.nextDouble() * 0.5);
                FxParticles.glyph(level, climb(age / (float) burst), 0.7f, 26, f.x + FxParticles.j(r) * 0.3, y, f.z + FxParticles.j(r) * 0.3,
                        FxParticles.j(r) * 0.01, 0.05 + r.nextDouble() * 0.03, FxParticles.j(r) * 0.01);
            }
        } else if (age == burst) {
            for (int i = FxParticles.n(36); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI, up = r.nextFloat() * Mth.HALF_PI, s = 0.08f + r.nextFloat() * 0.16f;
                FxParticles.spark(level, r.nextBoolean() ? GOLD : WHITE_GOLD, 1f, 18 + r.nextInt(12), f.x, f.y + height * 0.6, f.z,
                        Mth.cos(a) * Mth.cos(up) * s, Mth.sin(up) * s + 0.03, Mth.sin(a) * Mth.cos(up) * s);
            }
            for (int i = FxParticles.n(14); i > 0; i--) {
                FxParticles.mote(level, GOLD, 1f, 30, f.x + FxParticles.j(r) * 0.6, f.y + r.nextDouble() * height, f.z + FxParticles.j(r) * 0.6,
                        0, 0.02, 0);
            }
        }
    }

    @Override
    protected void render(FxDraw d) {
        float t = time(d);
        Vec3 f = feet(d.partial);
        int burst = HexedVillagerEntity.FREE_TICKS - 2;
        float lift = Mth.clamp(t / burst, 0, 1);
        float a = fade(t, 3, 10);
        // The marks rising off it: sigils turning as they climb from chest and head, cooling to gold.
        for (int i = 0; i < 5; i++) {
            float start = i * 4f, u = (t - start) / 18f;
            if (u <= 0 || u >= 1) continue;
            float side = (i % 2 == 0 ? 1 : -1) * (0.12f + 0.05f * i);
            double y = f.y + height * (i % 2 == 0 ? 0.6 : 0.85) + u * 1.6;
            float sa = Mth.sin(Mth.PI * u) * a;
            d.billboard(MagicTextures.castCircle(), f.x + side * Mth.cos(t * 0.05f), y, f.z + side * Mth.sin(t * 0.05f), 0.35f + 0.25f * u,
                    t * 0.08f + i, climb(u), 0.85f * sa);
            d.glowDisc(f.x + side * Mth.cos(t * 0.05f), y, f.z + side * Mth.sin(t * 0.05f), 0.4f, climb(u), 0.3f * sa);
        }
        // A ring climbing its body.
        if (t < burst) {
            double ry = f.y + 0.05 + lift * height * 1.05;
            d.groundRing(f.x, ry, f.z, 0.45f, 0.62f, climb(lift), 0.8f * a * Mth.sin(Mth.PI * lift), true);
        }
        // Gold breaking through.
        float b = t - burst;
        if (b >= 0) {
            float k = Mth.clamp(b / 12f, 0, 1);
            d.star(f.x, f.y + height * 0.6, f.z, 1.6f * (0.6f + 0.6f * k), 0.4f, WHITE_GOLD, 0.9f * (1 - k));
            d.glowDisc(f.x, f.y + height * 0.55, f.z, 1.6f, GOLD, 0.45f * (1 - k));
            float out = easeOut(b / 8f);
            d.groundRing(f.x, f.y + 0.04, f.z, 0.3f + 1.4f * out, 0.5f + 1.6f * out, GOLD, 0.7f * (1 - k), true);
        } else {
            d.glowDisc(f.x, f.y + height * 0.55, f.z, 0.9f, climb(lift), 0.18f * a * lift);
        }
    }
}
