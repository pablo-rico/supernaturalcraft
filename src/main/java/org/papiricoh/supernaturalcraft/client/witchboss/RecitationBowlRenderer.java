package org.papiricoh.supernaturalcraft.client.witchboss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.client.BowlContentsRenderer;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RecitationBowlEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaBalance;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.List;
import java.util.Optional;

/**
 * One of Rowena's recitation bowls (v0.21, P6): the spell bowl's block model brimming with her violet brew, boiling (harder with
 * each blow it has taken), a glow over it and, with each blow ({@link RecitationBowlEntity#hits()}), cracks of violet light
 * spreading over its side and a harder shake. Its letters and its thread to her are {@link RowenaWorldFx}'s.
 */
public class RecitationBowlRenderer extends EntityRenderer<RecitationBowlEntity> {

    private static final int BREW = 0x9B4FD8, BREW_HOT = 0xE6CCFF;
    private static final BowlContents CONTENTS = BowlContents.of(List.of(), List.of(
            new Dose(BowlLiquid.POTION, BREW, Optional.empty(), Optional.empty()),
            new Dose(BowlLiquid.DRAGON_BREATH, 0x7A2FC0, Optional.empty(), Optional.empty()),
            new Dose(BowlLiquid.POTION, BREW, Optional.empty(), Optional.empty())));

    private final BlockRenderDispatcher blocks;

    public RecitationBowlRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        blocks = ctx.getBlockRenderDispatcher();
        shadowRadius = 0.4f;
    }

    @Override
    public void render(RecitationBowlEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int hits = Mth.clamp(e.hits(), 0, RowenaBalance.BOWL_HITS);
        float t = e.tickCount + partialTick, crack = hits / (float) RowenaBalance.BOWL_HITS;
        float shake = 0.006f + hits * 0.018f;
        pose.pushPose();
        pose.translate(-0.5 + Math.sin(t * 2.1f) * shake, 0, -0.5 + Math.cos(t * 1.7f) * shake);
        blocks.renderSingleBlock(AllBlocks.SPELL_BOWL.get().defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        // Her brew, boiling harder as the bowl gives.
        BowlContentsRenderer.render(CONTENTS, pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY, t, 0, 0, e.level(), e.getId(),
                0.45f + 0.18f * hits);
        // Cracks of light on its side (one more web per blow), and a glow over the brim.
        if (hits > 0) cracks(pose, buffers, e.getId(), hits, t);
        pose.popPose();
        glow(pose, buffers, t, crack);
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    /** Jagged violet lines over the bowl's outer wall (block space: the wall is the box 2..14 × 0..5 / 16). */
    private static void cracks(PoseStack pose, MultiBufferSource buffers, int seed, int hits, float t) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float pulse = 0.7f + 0.3f * Mth.sin(t * 0.5f);
        int a = (int) (255 * pulse);
        for (int web = 0; web < hits * 2; web++) {
            float h = hash(seed, web);
            int face = (int) (hash(seed, web + 9) * 4);
            float x = 3 / 16f + h * 10 / 16f, y = 4.6f / 16f;
            for (int seg = 0; seg < 4; seg++) {
                float nx = Mth.clamp(x + (hash(seed, web * 7 + seg) - 0.5f) * 3.2f / 16f, 2.3f / 16f, 13.7f / 16f);
                float ny = Math.max(0.3f / 16f, y - (0.8f + hash(seed, web * 5 + seg + 3) * 1.1f) / 16f);
                line(vc, m, face, x, y, nx, ny, 0.35f / 16f, a);
                x = nx;
                y = ny;
            }
        }
    }

    /** One crack stroke on face {@code face} (0 north, 1 south, 2 west, 3 east), a hair outside the wall. */
    private static void line(VertexConsumer vc, Matrix4f m, int face, float u0, float y0, float u1, float y1, float w, int a) {
        float out = 0.12f / 16f, lo = 2 / 16f - out, hi = 14 / 16f + out;
        float[] p0 = place(face, u0, lo, hi), p1 = place(face, u1, lo, hi);
        int r = BREW_HOT >> 16 & 0xFF, g = BREW_HOT >> 8 & 0xFF, b = BREW_HOT & 0xFF;
        vc.addVertex(m, p0[0], y0 - w, p0[1]).setColor(r, g, b, a);
        vc.addVertex(m, p0[0], y0 + w, p0[1]).setColor(r, g, b, a);
        vc.addVertex(m, p1[0], y1 + w, p1[1]).setColor(r, g, b, a);
        vc.addVertex(m, p1[0], y1 - w, p1[1]).setColor(r, g, b, a);
        vc.addVertex(m, p1[0], y1 - w, p1[1]).setColor(r, g, b, a);
        vc.addVertex(m, p1[0], y1 + w, p1[1]).setColor(r, g, b, a);
        vc.addVertex(m, p0[0], y0 + w, p0[1]).setColor(r, g, b, a);
        vc.addVertex(m, p0[0], y0 - w, p0[1]).setColor(r, g, b, a);
    }

    private static float[] place(int face, float u, float lo, float hi) {
        return switch (face) {
            case 0 -> new float[]{u, lo};
            case 1 -> new float[]{u, hi};
            case 2 -> new float[]{lo, u};
            default -> new float[]{hi, u};
        };
    }

    /** A soft violet glow standing over the brim (a camera-facing quad of additive light). */
    private void glow(PoseStack pose, MultiBufferSource buffers, float t, float crack) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.translate(0, 0.55, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Matrix4f m = pose.last().pose();
        float s = 0.55f + 0.08f * Mth.sin(t * 0.3f) + 0.15f * crack;
        int r = BREW >> 16 & 0xFF, g = BREW >> 8 & 0xFF, b = BREW & 0xFF, a = (int) (70 * (0.8f + 0.5f * crack));
        // A soft disc: bright in the middle, nothing at its rim.
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg, a1 = (i + 1) * Mth.TWO_PI / seg;
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, a);
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, a);
            vc.addVertex(m, Mth.cos(a1) * s, Mth.sin(a1) * s, 0).setColor(r, g, b, 0);
            vc.addVertex(m, Mth.cos(a0) * s, Mth.sin(a0) * s, 0).setColor(r, g, b, 0);
        }
        pose.popPose();
    }

    private static float hash(int seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(RecitationBowlEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
