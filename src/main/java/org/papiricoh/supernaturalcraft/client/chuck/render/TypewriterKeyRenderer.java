package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckText;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.TypewriterKeyEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * A giant typewriter key falling out of the sky: {@code typewriter_key.geo.json}, turned by the entity's yaw and
 * tumbling with its pitch, and its letter typed on the keytop in ink. Where the keytop is (its height and width) is
 * read from the baked model, so the letter follows the art.
 */
public class TypewriterKeyRenderer extends GeoEntityRenderer<TypewriterKeyEntity> {

    /** The model is built at the size it is drawn. */
    public static final float SCALE = 1f;

    private BakedGeoModel measured;
    private float top = 0.5f, width = 1f;

    public TypewriterKeyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("typewriter_key")));
        addRenderLayer(new OptionalGlowLayer<>(this));
        scaleWidth = scaleHeight = SCALE;
        shadowRadius = 0.6f;
    }

    @Override
    public void render(TypewriterKeyEntity key, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model("typewriter_key"), null)) return;
        super.render(key, yaw, partialTick, pose, buffers, light);
        BakedGeoModel model = getGeoModel().getBakedModel(GeoGuard.model("typewriter_key"));
        if (model != measured) measure(model);
        pose.pushPose();
        orient(pose, key, partialTick);
        pose.translate(0, top * SCALE + 0.02f, 0);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        Font font = Minecraft.getInstance().font;
        Component letter = Component.literal(String.valueOf(Character.toChars(key.letter()))).withStyle(ChuckText.style());
        float s = width * SCALE * 0.55f / 8f;
        pose.scale(s, s, -s);
        font.drawInBatch(letter, -font.width(letter) / 2f, -4f, 0xFF1A1410, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        pose.popPose();
    }

    @Override
    public void preRender(PoseStack pose, TypewriterKeyEntity key, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) orient(pose, key, partialTick);
        super.preRender(pose, key, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** Not a living thing: GeckoLib leaves its yaw (and tumble) to us. */
    private static void orient(PoseStack pose, TypewriterKeyEntity key, float partialTick) {
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, key.yRotO, key.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, key.xRotO, key.getXRot())));
    }

    /** The keytop: the highest vertex of the model, and how wide the model is there. */
    private void measure(BakedGeoModel model) {
        measured = model;
        float maxY = -1e9f, minX = 1e9f, maxX = -1e9f;
        java.util.ArrayDeque<GeoBone> bones = new java.util.ArrayDeque<>(model.topLevelBones());
        while (!bones.isEmpty()) {
            GeoBone b = bones.pop();
            bones.addAll(b.getChildBones());
            for (GeoCube c : b.getCubes()) {
                for (GeoQuad q : c.quads()) {
                    if (q == null) continue;
                    for (GeoVertex v : q.vertices()) {
                        maxY = Math.max(maxY, v.position().y);
                        minX = Math.min(minX, v.position().x);
                        maxX = Math.max(maxX, v.position().x);
                    }
                }
            }
        }
        if (maxY > -1e8f) {
            top = maxY;
            width = Math.max(0.2f, maxX - minX);
        }
    }
}
