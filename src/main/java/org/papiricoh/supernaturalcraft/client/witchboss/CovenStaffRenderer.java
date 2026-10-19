package org.papiricoh.supernaturalcraft.client.witchboss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.CovenStaffItem;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAnimations;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * The Coven Staff (v0.21, P6): its GeckoLib model with the crystal's glowmask breathing (a slow swell, never dark) and a soft plum
 * halo round the crystal, flaring white-violet while the {@code cast} clip plays (its hex is {@code GrandWitchWorldFx.StaffHex}).
 * The halo's place is taken from the {@code crystal} bone as the model is drawn and lit after it (a layer), so no buffer is held
 * across another's fetch.
 */
public class CovenStaffRenderer extends GeoItemRenderer<CovenStaffItem> {

    private static final int PLUM = 0x9A4AC0, HOT = 0xF0DCFF;
    private final Matrix4f crystal = new Matrix4f();
    private boolean crystalSeen;

    public CovenStaffRenderer(CovenStaffItem item) {
        super(new DefaultedItemGeoModel<>(BuiltInRegistries.ITEM.getKey(item)));
        addRenderLayer(new Glow(this));
    }

    /** 0.5-1: the crystal's breath; 1 and more while it casts. */
    float strength(float partial) {
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        float s = 0.5f + 0.5f * Mth.sin(t * Mth.TWO_PI / 80f);
        float k = 0.5f + 0.5f * s * s;
        ItemStack stack = getCurrentItemStack();
        if (stack != null && !stack.isEmpty() && GrandWitchAnimations.STAFF_CAST.equals(
                AllegianceGeo.clip(getAnimatable(), GeoItem.getId(stack), "main"))) {
            k = 1.6f;
        }
        return k;
    }

    @Override
    public void renderRecursively(PoseStack pose, CovenStaffItem item, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender && "crystal".equals(bone.getName())) {
            // Only a matrix is kept here: the halo is drawn later, by the layer, with its own buffer.
            pose.pushPose();
            RenderUtil.prepMatrixForBone(pose, bone);
            pose.translate(bone.getPivotX() / 16f, (bone.getPivotY() + 0.9f) / 16f, bone.getPivotZ() / 16f);
            crystal.set(pose.last().pose());
            crystalSeen = true;
            pose.popPose();
        }
        super.renderRecursively(pose, item, bone, type, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    static final class Glow extends AutoGlowingGeoLayer<CovenStaffItem> {
        private final CovenStaffRenderer staff;

        Glow(CovenStaffRenderer renderer) {
            super(renderer);
            this.staff = renderer;
        }

        @Override
        public void render(PoseStack pose, CovenStaffItem item, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            float k = staff.strength(partialTick);
            if (GeoGuard.exists(GeoGuard.glowmask(getTextureResource(item)))) {
                RenderType glow = getRenderType(item, buffers);
                if (glow != null) {
                    int c = Math.round(255 * Math.min(1, k));
                    getRenderer().reRender(model, pose, buffers, item, glow, buffers.getBuffer(glow), partialTick, 0xF00000, overlay,
                            0xFF << 24 | c << 16 | Math.round(c * 0.95f) << 8 | c);
                }
            }
            if (!staff.crystalSeen || staff.renderPerspective == ItemDisplayContext.GUI) return;
            staff.crystalSeen = false;
            halo(buffers, staff.crystal, k);
        }

        /** Two crossed soft discs of light round the crystal (it reads from any side). */
        private static void halo(MultiBufferSource buffers, Matrix4f m, float k) {
            VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
            int rgb = k > 1 ? HOT : PLUM;
            int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = (int) Mth.clamp(70 * k, 0, 200);
            float s = 0.16f + 0.06f * k;
            int seg = 14;
            for (int plane = 0; plane < 2; plane++) {
                for (int i = 0; i < seg; i++) {
                    float a0 = i * Mth.TWO_PI / seg, a1 = (i + 1) * Mth.TWO_PI / seg;
                    float x0 = Mth.cos(a0) * s, y0 = Mth.sin(a0) * s, x1 = Mth.cos(a1) * s, y1 = Mth.sin(a1) * s;
                    float[] p0 = plane == 0 ? new float[]{x0, y0, 0} : new float[]{0, y0, x0};
                    float[] p1 = plane == 0 ? new float[]{x1, y1, 0} : new float[]{0, y1, x1};
                    // Both windings: the disc is seen from either side.
                    for (int side = 0; side < 2; side++) {
                        float[] u = side == 0 ? p1 : p0, v = side == 0 ? p0 : p1;
                        vc.addVertex(m, 0, 0, 0).setColor(r, g, b, a);
                        vc.addVertex(m, 0, 0, 0).setColor(r, g, b, a);
                        vc.addVertex(m, u[0], u[1], u[2]).setColor(r, g, b, 0);
                        vc.addVertex(m, v[0], v[1], v[2]).setColor(r, g, b, 0);
                    }
                }
            }
        }
    }
}
