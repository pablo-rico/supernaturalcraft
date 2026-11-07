package org.papiricoh.supernaturalcraft.client.godstiel;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Season 6's particles (v0.39): one white-alpha sprite ({@code GodstielAssets.PARTICLES}) tinted by its provider, fading out over
 * its life; the light ones full bright, the goo not.
 */
public final class TintedParticle extends TextureSheetParticle {

    private final boolean emissive;

    TintedParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, int rgb,
                   boolean emissive, float gravity, int lifetime, float size) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.emissive = emissive;
        this.gravity = gravity;
        this.lifetime = lifetime + random.nextInt(Math.max(1, lifetime / 3));
        this.quadSize = size * (0.8f + random.nextFloat() * 0.4f);
        setColor((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 1f - (float) age / lifetime;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return emissive ? 0xF000F0 : super.getLightColor(partialTick);
    }

    public record Provider(SpriteSet sprites, int rgb, boolean emissive, float gravity, int lifetime, float size)
            implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new TintedParticle(level, x, y, z, vx, vy, vz, sprites, rgb, emissive, gravity, lifetime, size);
        }
    }
}
