package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * The bowl answering the local player's recitation (v0.21; the recitation screen is the client's own, so is all of this). Each
 * letter spoken rises off the liquid as a glowing letter; the liquid boils harder as time runs out (bubbles on its surface, drawn
 * by the bowl's renderer through {@link #boil}, popping and steaming); a typo makes it sputter. Others nearby see only the
 * server's seal or backlash.
 */
public final class RecitationFx {

    private static final int GOLD = 0xE8B030;
    private static @Nullable BlockPos bowl;
    private static float target, boil, boilO;
    private static int idle, sputter, bubbling;

    private RecitationFx() {
    }

    /** The screen is open over the bowl at {@code pos}, {@code urgency} (0-1) of the time gone. */
    public static void reciting(BlockPos pos, float urgency) {
        bowl = pos.immutable();
        target = 0.25f + 0.75f * Mth.clamp(urgency, 0, 1);
        idle = 0;
    }

    /** A letter was spoken: it rises off the liquid. */
    public static void letter(BlockPos pos, String ch, float progress) {
        if (ch.isBlank()) return;
        Vec3 s = surface(pos);
        MagicFxManager.add(new Letter(s, ch, progress));
    }

    /** A typo: the bowl sputters. */
    public static void typo(BlockPos pos) {
        bowl = pos.immutable();
        sputter = 8;
    }

    /** The words are spoken ({@code success}) or failed: the boil settles (the server's seal or backlash takes over). */
    public static void done(BlockPos pos, boolean success) {
        if (!success) sputter = 12;
        target = success ? 0.6f : 0.1f;
        idle = 0;
    }

    /** How hard the bowl at {@code pos} is boiling (0-1), for its renderer. */
    public static float boil(BlockPos pos, float partial) {
        return pos.equals(bowl) ? Mth.lerp(partial, boilO, boil) : 0f;
    }

    static void clear() {
        bowl = null;
        boil = boilO = target = 0;
    }

    private static Vec3 surface(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        int doses = mc.level != null && mc.level.getBlockEntity(pos) instanceof SpellBowlBlockEntity be ? be.contents().liquids().size() : 1;
        return Vec3.atBottomCenterOf(pos).add(0, doses > 0 ? SpellBowlBlock.liquidHeight(doses) : 0.1, 0);
    }

    static void tick(ClientLevel level) {
        if (bowl == null) return;
        boilO = boil;
        if (++idle > 10) target = 0;
        boil += Mth.clamp(target - boil, -0.05f, 0.04f);
        if (target == 0 && boil < 0.01f) {
            bowl = null;
            boil = boilO = 0;
            return;
        }
        RandomSource r = level.random;
        Vec3 s = surface(bowl);
        if (--bubbling <= 0 && boil > 0.2f) {
            MagicSounds.other("bowl.boil", s, 0.3f + 0.7f * boil, 0.9f + 0.25f * boil);
            bubbling = 44;
        }
        for (int i = FxParticles.rate(r, boil * 2.4f); i > 0; i--) {
            level.addParticle(ParticleTypes.BUBBLE_POP, s.x + FxParticles.j(r) * 0.2, s.y + 0.02, s.z + FxParticles.j(r) * 0.2, 0, 0.02, 0);
        }
        for (int i = FxParticles.rate(r, boil * boil * 1.2f); i > 0; i--) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, s.x + FxParticles.j(r) * 0.15, s.y + 0.1, s.z + FxParticles.j(r) * 0.15, 0,
                    0.03 + boil * 0.03, 0);
        }
        if (sputter > 0) {
            sputter--;
            for (int i = FxParticles.n(3); i > 0; i--) {
                level.addParticle(ParticleTypes.SPLASH, s.x + FxParticles.j(r) * 0.15, s.y + 0.05, s.z + FxParticles.j(r) * 0.15,
                        FxParticles.j(r) * 0.1, 0.15, FxParticles.j(r) * 0.1);
            }
            if (FxParticles.n(1) > 0) level.addParticle(ParticleTypes.SMOKE, s.x, s.y + 0.1, s.z, 0, 0.06, 0);
        }
    }

    /**
     * A spoken letter rising off the bowl in gold: it spirals up round the steam, a soft halo of light behind it, two fading echoes of
     * itself trailing below, and it thins to nothing at the top.
     */
    private static final class Letter extends MagicEffect {
        private final String ch;
        private final float sway, rise, turn;

        Letter(Vec3 at, String ch, float progress) {
            super(at, School.NONE, 38);
            this.ch = ch;
            Minecraft mc = Minecraft.getInstance();
            RandomSource r = mc.level != null ? mc.level.random : RandomSource.create();
            this.sway = r.nextFloat() * Mth.TWO_PI;
            this.rise = 1.4f + 0.6f * progress + r.nextFloat() * 0.3f;
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

        /** Where the letter is {@code t} ticks into its life. */
        private Vec3 path(float t) {
            float k = Mth.clamp(t / life, 0, 1);
            float r = 0.08f + 0.22f * k;
            float ang = sway + turn * t * 0.16f;
            return new Vec3(at.x + Mth.cos(ang) * r, at.y + 0.1 + rise * easeOut(k), at.z + Mth.sin(ang) * r);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), k = progress(d);
            float a = fade(t, 3, 16);
            float size = 0.3f * (1 - 0.3f * k);
            Vec3 p = path(t);
            // Echoes first, so the letter itself is drawn over them.
            for (int e = 2; e >= 1; e--) {
                float te = t - e * 2.2f;
                if (te < 0) continue;
                Vec3 q = path(te);
                float ea = a * (e == 1 ? 0.35f : 0.15f);
                d.glowDisc(q.x, q.y, q.z, 0.22f, GOLD, 0.18f * ea);
                d.text(ch, q.x, q.y, q.z, size * (1 - 0.1f * e), Math.max(4, (int) (ea * 255)) << 24 | 0xE8B060);
            }
            d.glowDisc(p.x, p.y, p.z, 0.55f, GOLD, 0.22f * a);
            d.glowDisc(p.x, p.y, p.z, 0.26f, 0xFFE6A0, 0.32f * a);
            d.text(ch, p.x, p.y, p.z, size, Math.max(4, (int) (a * 255)) << 24 | 0xFFE6A0);
        }
    }
}
