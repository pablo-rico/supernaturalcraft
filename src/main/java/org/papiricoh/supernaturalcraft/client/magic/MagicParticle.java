package org.papiricoh.supernaturalcraft.client.magic;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.magic.fx.MagicParticleOptions;

/**
 * The magic's coloured particles (v0.21, {@code MagicAssets.PARTICLES}), tinted by their options' colour, sized by its scale and
 * living its lifetime (0 = the kind's own). Light is added to what is behind it (an additive sheet), except the salt.
 * <ul>
 *   <li>{@link Kind#SPARK}: a hot point that starts white and cools to its colour, slowing and shrinking as it goes;</li>
 *   <li>{@link Kind#GLYPH}: one glyph of the sheet, kept, fading in and out as it turns slowly and drifts;</li>
 *   <li>{@link Kind#EMBER}: rises, sways and flickers, dimming toward a darker red-shifted end;</li>
 *   <li>{@link Kind#MOTE}: spirals lazily round where it set off, breathing;</li>
 *   <li>{@link Kind#SALT}: a grain that falls, bounces on what it lands on, and lies there a moment.</li>
 * </ul>
 */
public class MagicParticle extends TextureSheetParticle {

    public enum Kind {
        SPARK(12, 0.09f), GLYPH(28, 0.14f), EMBER(22, 0.1f), MOTE(40, 0.09f), SALT(30, 0.04f);

        final int lifetime;
        final float size;

        Kind(int lifetime, float size) {
            this.lifetime = lifetime;
            this.size = size;
        }
    }

    /** Light added to the scene behind it (source alpha, one); depth not written. */
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "SUPERNATURALCRAFT_MAGIC_ADDITIVE";
        }
    };

    private final Kind kind;
    private final SpriteSet sprites;
    private final float r0, g0, b0;
    private final float baseSize;
    private final float phase;
    private final double ox, oz;
    private float spin;

    protected MagicParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Kind kind,
                            MagicParticleOptions o) {
        super(level, x, y, z, vx, vy, vz);
        this.kind = kind;
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.ox = x;
        this.oz = z;
        this.r0 = o.red();
        this.g0 = o.green();
        this.b0 = o.blue();
        int life = o.lifetime() > 0 ? o.lifetime() : kind.lifetime;
        this.lifetime = Math.max(2, life + random.nextInt(Math.max(1, life / 4)) - life / 8);
        this.baseSize = kind.size * o.scale() * (0.8f + random.nextFloat() * 0.4f);
        this.quadSize = baseSize;
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.hasPhysics = kind == Kind.SALT;
        switch (kind) {
            case SPARK -> {
                friction = 0.86f;
                gravity = 0.01f;
            }
            case GLYPH -> {
                friction = 0.93f;
                gravity = -0.002f;
                spin = (random.nextFloat() - 0.5f) * 0.12f;
                roll = random.nextFloat() * Mth.TWO_PI;
            }
            case EMBER -> {
                friction = 0.95f;
                gravity = -0.012f;
            }
            case MOTE -> {
                friction = 0.96f;
                gravity = -0.003f;
            }
            case SALT -> {
                friction = 0.98f;
                gravity = 0.9f;
                roll = random.nextFloat() * Mth.TWO_PI;
            }
        }
        oRoll = roll;
        // Glyphs and salt keep one sprite (eight glyphs, two grains); the rest play their frames by age.
        if (kind == Kind.GLYPH || kind == Kind.SALT) pickSprite(sprites);
        else setSpriteFromAge(sprites);
        tint(0);
    }

    /** Colour at {@code k} (0-1) of its life. */
    private void tint(float k) {
        switch (kind) {
            case SPARK -> {
                // Starts white-hot, cools to its colour by a third of its life.
                float w = Math.max(0, 1 - k * 3);
                setColor(Mth.lerp(w, r0, 1), Mth.lerp(w, g0, 1), Mth.lerp(w, b0, 1));
            }
            case EMBER -> {
                // Dims and reddens toward the end.
                float d = 1 - 0.55f * k;
                setColor(r0 * d + 0.1f * k, g0 * d * (1 - 0.4f * k), b0 * d * (1 - 0.6f * k));
            }
            default -> setColor(r0, g0, b0);
        }
    }

    /**
     * Drawn thinned near the eye (v0.21): a spark or glyph that drifts past the camera (a curse on the local hunter, a spell at
     * their feet) fades out within about a block instead of becoming a wall of light; it never draws wider than half a block there.
     */
    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTicks) {
        double dx = Mth.lerp(partialTicks, xo, x) - camera.getPosition().x, dy = Mth.lerp(partialTicks, yo, y) - camera.getPosition().y,
                dz = Mth.lerp(partialTicks, zo, z) - camera.getPosition().z;
        float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d >= 2.4f) {
            super.render(buffer, camera, partialTicks);
            return;
        }
        float k = Mth.clamp((d - 0.6f) / 1.8f, 0, 1);
        if (k <= 0.01f) return;
        float a = alpha, q = quadSize;
        alpha = a * k * k * (3 - 2 * k);
        quadSize = Math.min(q, 0.25f + 0.5f * k);
        super.render(buffer, camera, partialTicks);
        alpha = a;
        quadSize = q;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float k = age / (float) lifetime;
        if (kind != Kind.GLYPH && kind != Kind.SALT) setSpriteFromAge(sprites);
        tint(k);
        oRoll = roll;
        switch (kind) {
            case SPARK -> {
                quadSize = baseSize * (1 - 0.7f * k);
                alpha = 1 - k * k;
            }
            case GLYPH -> {
                roll += spin;
                alpha = Math.min(1, age / 4f) * (1 - Math.max(0, (k - 0.55f) / 0.45f));
                quadSize = baseSize * (0.9f + 0.1f * Mth.sin(age * 0.3f + phase));
            }
            case EMBER -> {
                xd += Mth.sin(age * 0.25f + phase) * 0.003;
                zd += Mth.cos(age * 0.21f + phase) * 0.003;
                float flicker = 0.75f + 0.25f * Mth.sin(age * 1.7f + phase) * Mth.sin(age * 0.6f);
                alpha = flicker * (1 - k * k);
                quadSize = baseSize * (1 - 0.5f * k);
            }
            case MOTE -> {
                // Spiral round the vertical through where it began.
                double dx = x - ox, dz = z - oz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double tx = dist < 1e-3 ? Math.cos(phase) : -dz / dist, tz = dist < 1e-3 ? Math.sin(phase) : dx / dist;
                xd += tx * 0.004 - dx * 0.002;
                zd += tz * 0.004 - dz * 0.002;
                alpha = (0.65f + 0.35f * Mth.sin(age * 0.25f + phase)) * Math.min(1, age / 5f) * (1 - Math.max(0, (k - 0.6f) / 0.4f));
            }
            case SALT -> {
                if (onGround) {
                    xd *= 0.6;
                    zd *= 0.6;
                }
                alpha = 1 - Math.max(0, (k - 0.75f) / 0.25f);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return kind == Kind.SALT ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : ADDITIVE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return kind == Kind.SALT ? super.getLightColor(partialTick) : 0xF000F0;
    }

    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<MagicParticleOptions> {
        @Override
        public Particle createParticle(MagicParticleOptions o, ClientLevel level, double x, double y, double z, double vx, double vy,
                                       double vz) {
            return new MagicParticle(level, x, y, z, vx, vy, vz, sprites, kind, o);
        }
    }
}
