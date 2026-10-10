package org.papiricoh.supernaturalcraft.client.heaven.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.client.heaven.render.figure.FigureDraw;
import org.papiricoh.supernaturalcraft.client.heaven.render.figure.FigureSpec;

/**
 * Someone standing in a staged memory (v0.18): whoever the scene put there (a creature by its own renderer, an ally, the hunter
 * in their own skin), frozen in its pose at its scale, washed in its tint and lit softly from within. The figure to touch to
 * gather the memory stands in a slow ring of light that breathes. What to draw comes off the entity
 * ({@code figure()}, {@code pose()}, {@code scale()}, {@code focus()}, {@code tint()}); {@link FigureDraw} draws it.
 */
public class MemoryFigureRenderer<E extends Entity> extends EntityRenderer<E> {

    /** The soft light memory figures are lit with at least (0-15). */
    static final int GLOW = 11;

    private final FigureDraw figures;

    public MemoryFigureRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        figures = new FigureDraw(ctx);
        shadowRadius = 0.4f;
    }

    public static FigureSpec spec(Entity e) {
        String figure = Accessors.string(e, "", "figure", "getFigure", "figureId");
        String pose = Accessors.string(e, "stand", "pose", "getPoseName", "figurePose", "posture");
        float scale = Accessors.number(e, 1f, "scale", "figureScale", "getScale");
        int tint = Accessors.integer(e, 0xFFFFFFFF, "tint", "getTint", "colour", "color");
        if ((tint >>> 24) == 0) tint |= 0xFF000000;
        return new FigureSpec(figure.isEmpty() ? "@owner" : figure, pose, scale <= 0 ? 1f : scale, tint, GLOW, false);
    }

    public static boolean focus(Entity e) {
        return Accessors.bool(e, false, "focus", "isFocus");
    }

    @Override
    public void render(E e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        FigureSpec spec = spec(e);
        java.util.UUID owner = e instanceof org.papiricoh.supernaturalcraft.entity.heaven.MemoryFigureEntity f ? f.ownerId() : null;
        figures.draw(e, spec, owner, Mth.rotLerp(partial, e.yRotO, e.getYRot()), partial, pose, buffers, light);
        if (focus(e)) halo(pose, buffers, e.tickCount + partial, spec.scale());
    }

    /** A ring of light on the ground round the figure to touch, and a faint column rising from it. */
    static void halo(PoseStack pose, MultiBufferSource buffers, float time, float scale) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float breathe = 0.5f + 0.5f * Mth.sin(time * 0.08f);
        float r0 = 0.55f * Math.max(1, scale), r1 = r0 + 0.18f + 0.08f * breathe;
        float h = 1.8f * Math.max(1, scale), y = 0.02f;
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(vc, m, c0 * r0, y, s0 * r0, c1 * r0, y, s1 * r0, c1 * r1, y, s1 * r1, c0 * r1, y, s0 * r1, 0.55f + 0.3f * breathe, 0f);
            // The column: a thin wall of light fading upward.
            quad(vc, m, c0 * r0, y, s0 * r0, c1 * r0, y, s1 * r0, c1 * r0, h, s1 * r0, c0 * r0, h, s0 * r0, 0.16f + 0.08f * breathe, 0f);
        }
    }

    /** A quad whose first two corners carry {@code a0} alpha and the last two {@code a1}: warm white light. */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, float a0, float a1) {
        vc.addVertex(m, x0, y0, z0).setColor(1f, 0.93f, 0.72f, a0);
        vc.addVertex(m, x1, y1, z1).setColor(1f, 0.93f, 0.72f, a0);
        vc.addVertex(m, x2, y2, z2).setColor(1f, 0.93f, 0.72f, a1);
        vc.addVertex(m, x3, y3, z3).setColor(1f, 0.93f, 0.72f, a1);
    }

    @Override
    public boolean shouldRender(E e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        float s = Math.max(1, spec(e).scale());
        return frustum.isVisible(e.getBoundingBox().inflate(1.5 * s, 2 * s, 1.5 * s));
    }

    @Override
    public ResourceLocation getTextureLocation(E e) {
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }
}
