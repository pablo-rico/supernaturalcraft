package org.papiricoh.supernaturalcraft.bowl.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;

/** A bowl spell's smoke: a soft puff in the spell's colour that rises, swells and thins out. */
public class BowlSmokeParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float startSize;

    protected BowlSmokeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                                ColorParticleOption color, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.xd = vx + (random.nextDouble() - 0.5) * 0.01;
        this.yd = vy + 0.01;
        this.zd = vz + (random.nextDouble() - 0.5) * 0.01;
        this.gravity = -0.015f;
        this.friction = 0.94f;
        this.hasPhysics = false;
        this.lifetime = 30 + random.nextInt(20);
        float shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(color.getRed() * shade, color.getGreen() * shade, color.getBlue() * shade);
        this.startSize = 0.18f + random.nextFloat() * 0.08f;
        this.quadSize = startSize;
        this.alpha = 0.85f;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        float t = age / (float) lifetime;
        quadSize = startSize * (1 + t * 1.8f);
        alpha = 0.85f * (1 - t * t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new BowlSmokeParticle(level, x, y, z, vx, vy, vz, type, sprites);
        }
    }
}
