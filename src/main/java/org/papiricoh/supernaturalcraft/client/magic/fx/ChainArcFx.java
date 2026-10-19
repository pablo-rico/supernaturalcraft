package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/**
 * CHAIN_ARC: an arc of the school's lightning leaping from one target to the next, re-forking every other tick, a flash where it
 * lands (on the struck entity, followed). Later hops are thinner.
 */
final class ChainArcFx extends MagicEffect {

    private final Vec3 to;
    private final int struck;
    private final int hop;

    ChainArcFx(MagicFxPayload p) {
        super(p.pos(), p.school(), Math.max(10, p.duration()));
        this.to = p.target();
        this.struck = p.entity();
        this.hop = Math.max(0, p.argIndex());
    }

    @Override
    protected double reach() {
        return 4 + at.distanceTo(to);
    }

    @Override
    protected void tick(ClientLevel level) {
        if (age != 0) return;
        RandomSource r = level.random;
        Vec3 end = centre(entity(struck), 1f, to);
        MagicSounds.school(school, "impact", end, 0.45f, 1.4f + hop * 0.08f + r.nextFloat() * 0.1f);
        for (int i = FxParticles.n(10); i > 0; i--) {
            FxParticles.spark(level, r.nextBoolean() ? accent : FxColor.core(school), 0.8f, 0, end.x, end.y, end.z, FxParticles.j(r) * 0.2,
                    FxParticles.j(r) * 0.2, FxParticles.j(r) * 0.2);
        }
    }

    @Override
    protected void render(FxDraw d) {
        float t = time(d);
        float a = fade(t, 0, life * 0.6f);
        Vec3 end = centre(entity(struck), d.partial, to);
        float w = 0.07f * Math.max(0.4f, 1 - hop * 0.15f);
        long seed = (long) (t / 2) * 7919L + hop * 31L + (long) (at.x * 13);
        float len = (float) at.distanceTo(end);
        int segs = Mth.clamp((int) (len * 2.5f), 4, 24);
        d.lightning(at.x, at.y, at.z, end.x, end.y, end.z, seed, segs, Math.min(0.6f, 0.12f + len * 0.06f), w, primary, a, true);
        if (t < 6) d.lightning(at.x, at.y, at.z, end.x, end.y, end.z, seed + 3, segs, 0.3f, w * 0.6f, accent, 0.6f * a, false);
        d.glowDisc(end.x, end.y, end.z, 0.9f, primary, 0.7f * a);
        d.glowDisc(at.x, at.y, at.z, 0.6f, primary, 0.5f * a);
        if (t < 3) d.star(end.x, end.y, end.z, 1.4f, t, 0xFFFFFF, 1 - t / 3f);
    }
}
