package org.papiricoh.supernaturalcraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * One particle class for every mod effect; the providers below differ only in motion and
 * brightness. Frames advance with age, so each sprite set reads as a short animation. A provider may cap the alpha and fade
 * the particle out near the camera ({@code nearFade}: smoke that drifts past the eye thins away instead of filling the view).
 */
public class GlowParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final boolean emissive;
    private final float maxAlpha, nearFade;

    protected GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                           SpriteSet sprites, boolean emissive, float gravity, int lifetime, float size, float maxAlpha, float nearFade) {
        super(level, x, y, z, vx, vy, vz);
        this.maxAlpha = maxAlpha;
        this.nearFade = nearFade;
        this.alpha = maxAlpha;
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
            alpha = maxAlpha * (1.0f - Math.max(0, (age - lifetime * 0.6f) / (lifetime * 0.4f)));
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (nearFade <= 0) {
            super.render(buffer, camera, partialTicks);
            return;
        }
        // Within nearFade blocks of the eye the particle is gone; it comes back fully over the next 2 blocks.
        double dx = Mth.lerp(partialTicks, xo, x) - camera.getPosition().x, dy = Mth.lerp(partialTicks, yo, y) - camera.getPosition().y,
                dz = Mth.lerp(partialTicks, zo, z) - camera.getPosition().z;
        float k = Mth.clamp(((float) Math.sqrt(dx * dx + dy * dy + dz * dz) - nearFade) / 2f, 0, 1);
        if (k <= 0.01f) return;
        float a = alpha;
        alpha = a * k * k * (3 - 2 * k);
        super.render(buffer, camera, partialTicks);
        alpha = a;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return emissive ? 0xF000F0 : super.getLightColor(partialTick);
    }

    /** {@code maxAlpha} caps its opacity; {@code nearFade} (blocks, 0 for none) is how near the eye it vanishes. */
    public record Provider(SpriteSet sprites, boolean emissive, float gravity, int lifetime, float size, float maxAlpha, float nearFade)
            implements ParticleProvider<SimpleParticleType> {

        public Provider(SpriteSet sprites, boolean emissive, float gravity, int lifetime, float size) {
            this(sprites, emissive, gravity, lifetime, size, 1f, 0f);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprites, emissive, gravity, lifetime, size, maxAlpha, nearFade);
        }
    }
}
