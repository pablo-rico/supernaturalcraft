package org.papiricoh.supernaturalcraft.client.heaven.render.figure;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Buffers that tint whatever another renderer draws into them (v0.18): every vertex colour multiplied by an ARGB and its light
 * lifted to at least a minimum block light. How a memory's figure is washed in its scene's colour and softly lit from within,
 * whoever's renderer draws it.
 */
public record TintedBuffers(MultiBufferSource inner, int argb, int minBlockLight) implements MultiBufferSource {

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        VertexConsumer vc = inner.getBuffer(type);
        return argb == 0xFFFFFFFF && minBlockLight <= 0 ? vc : new Tinted(vc, argb, minBlockLight);
    }

    /** {@code light} with its block light at least {@code min} (0-15). */
    public static int lift(int light, int min) {
        int block = light & 0xFFFF, sky = (light >> 16) & 0xFFFF;
        return Math.max(block, min << 4) | sky << 16;
    }

    private static final class Tinted implements VertexConsumer {
        private final VertexConsumer vc;
        private final int ta, tr, tg, tb, min;

        Tinted(VertexConsumer vc, int argb, int min) {
            this.vc = vc;
            this.ta = argb >>> 24;
            this.tr = (argb >> 16) & 255;
            this.tg = (argb >> 8) & 255;
            this.tb = argb & 255;
            this.min = min;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            vc.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            vc.setColor(r * tr / 255, g * tg / 255, b * tb / 255, a * ta / 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            vc.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            vc.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            vc.setUv2(Math.max(u, min << 4), v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            vc.setNormal(x, y, z);
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            int a = (color >>> 24) * ta / 255, r = ((color >> 16) & 255) * tr / 255, g = ((color >> 8) & 255) * tg / 255,
                    b = (color & 255) * tb / 255;
            vc.addVertex(x, y, z, a << 24 | r << 16 | g << 8 | b, u, v, overlay, lift(light, min), nx, ny, nz);
        }
    }
}
