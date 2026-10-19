package org.papiricoh.supernaturalcraft.client.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * A hexed villager (v0.21): hollow eyes and hex marks always glowing — until the hex breaks, when they gutter out over its
 * {@code freed} clip as the sigils rise off it ({@code WitchAmbience}). Called up by Rowena, it claws up out of the floor
 * ({@link WitchGeo#rise}).
 */
public class HexedVillagerRenderer extends GeoEntityRenderer<HexedVillagerEntity> {

    public HexedVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(WitchAssets.HEXED_VILLAGER), true));
        addRenderLayer(new FadingGlowLayer<>(this, (v, partial) -> WitchGeo.fade(v, "hex", v.freeing() ? 0 : 1, 1, 18, partial)
                * (v.freeing() ? 1 : 0.9f + 0.1f * net.minecraft.util.Mth.sin((v.tickCount + partial) * 0.13f + v.getId()))));
        shadowRadius = 0.45f;
    }

    @Override
    public void render(HexedVillagerEntity v, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model(WitchAssets.HEXED_VILLAGER), GeoGuard.animation(WitchAssets.HEXED_VILLAGER))) return;
        float rise = WitchGeo.risen(v, partialTick);
        if (rise >= 1) {
            super.render(v, yaw, partialTick, pose, buffers, light);
            return;
        }
        // Called up by Rowena: it claws its way out of the floor, lurching (WitchGeo.rise; the cracks are RowenaWorldFx's).
        float k = 1 - (1 - rise) * (1 - rise);
        float lurch = (1 - rise) * 0.06f * net.minecraft.util.Mth.sin((v.tickCount + partialTick) * 1.7f);
        pose.pushPose();
        pose.translate(lurch, -(1 - k) * v.getBbHeight() * 1.05, 0);
        super.render(v, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }
}
