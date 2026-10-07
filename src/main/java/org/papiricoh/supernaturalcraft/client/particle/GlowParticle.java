package org.papiricoh.supernaturalcraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * One particle class for every mod effect; the providers below differ only in motion and
 * brightness. Frames advance with age, so each sprite set reads as a short animation.
 */
public class GlowParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final boolean emissive;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           SpriteSet sprites, boolean emissive, float gravity, int lifetime, float size) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.emissive = emissive;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = gravity;
        this.friction = 0.92f;
        this.lifetime = lifetime + random.nextInt(Math.max(1, lifetime / 3));
        this.quadSize = size * (0.8f + random.nextFloat() * 0.4f);
        this.hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
            alpha = 1.0f - Math.max(0, (age - lifetime * 0.6f) / (lifetime * 0.4f));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return emissive ? 0xF000F0 : super.getLightColor(partialTick);
    }

    public record Provider(SpriteSet sprites, boolean emissive, float gravity, int lifetime, float size)
            implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprites, emissive, gravity, lifetime, size);
        }
    }
}
