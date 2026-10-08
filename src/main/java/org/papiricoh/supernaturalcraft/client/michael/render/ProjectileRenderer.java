package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;

/**
 * A steel feather or a spear of the halo in flight: its little GeckoLib model ({@code geo/entity/<id>.geo.json}) pointed
 * along the flight; failing that, its texture as two crossed quads along the flight (the feather's flat sprite pair).
 * Both full bright.
 */
public class ProjectileRenderer<T extends AbstractArrow> extends EntityRenderer<T> {

    private final ResourceLocation texture;
    private final GeoProp prop;
    private final float length, width;

    public ProjectileRenderer(EntityRendererProvider.Context ctx, String id, float length, float width) {
        super(ctx);
        texture = SupernaturalCraft.asResource("textures/entity/" + id + ".png");
        prop = new GeoProp(SupernaturalCraft.asResource("geo/entity/" + id + ".geo.json"), texture, null, null).fullBright();
        this.length = length;
        this.width = width;
    }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, e.xRotO, e.getXRot())));
        if (prop.ready()) {
            prop.draw(pose, buffers, texture, e.tickCount + partial, partial, light);
        } else if (GeoGuard.exists(texture)) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
            Matrix4f m = pose.last().pose();
            // Two quads crossed along +Z (the flight), each both ways round.
            quad(vc, pose, m, true);
            quad(vc, pose, m, false);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private void quad(VertexConsumer vc, PoseStack pose, Matrix4f m, boolean flat) {
        float hw = width / 2, l = length;
        float[][] c = flat ? new float[][]{{-hw, 0, 0}, {hw, 0, 0}, {hw, 0, l}, {-hw, 0, l}}
                : new float[][]{{0, -hw, 0}, {0, hw, 0}, {0, hw, l}, {0, -hw, l}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                int i = pass == 0 ? k : 3 - k;
                vc.addVertex(m, c[i][0], c[i][1], c[i][2] - l / 2).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose.last(), 0, 1, 0);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return texture;
    }
}
