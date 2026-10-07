package org.papiricoh.supernaturalcraft.client.chuck.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A loose letter of ink, or a scrap of paper: one sprite picked at random and kept (each sprite is a different letter
 * or tear), drifting with a slow sway and a slight spin, fading at the end. Velocity comes from the spawner.
 */
public class InkLetterParticle extends TextureSheetParticle {

    private final float sway, spin;

    protected InkLetterParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites,
                                float gravity, int lifetime, float size, float spin) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = gravity;
        this.friction = 0.94f;
        this.lifetime = lifetime + random.nextInt(Math.max(1, lifetime / 2));
        this.quadSize = size * (0.75f + random.nextFloat() * 0.5f);
        this.hasPhysics = false;
        this.sway = random.nextFloat() * Mth.TWO_PI;
        this.spin = spin * (random.nextBoolean() ? 1 : -1) * (0.5f + random.nextFloat());
        this.roll = random.nextFloat() * spin * 10;
        this.oRoll = roll;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        xd += Mth.sin(age * 0.15f + sway) * 0.0025;
        zd += Mth.cos(age * 0.13f + sway) * 0.0025;
        oRoll = roll;
        roll += spin;
        alpha = 1.0f - Math.max(0, (age - lifetime * 0.65f) / (lifetime * 0.35f));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Letters stay legible in any light: half lit by the world, half their own. */
    @Override
    protected int getLightColor(float partialTick) {
        int world = super.getLightColor(partialTick);
        int block = Math.max(world & 0xFFFF, 0xA0);
        return (world & 0xFFFF0000) | block;
    }

    public record Provider(SpriteSet sprites, float gravity, int lifetime, float size, float spin)
            implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new InkLetterParticle(level, x, y, z, vx, vy, vz, sprites, gravity, lifetime, size, spin);
        }
    }
}
