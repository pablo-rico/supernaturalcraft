package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A soldier of the Host: its vessel's texture ({@code host_angel_<0-2>.png}, the captain's own), the shared glowing eyes,
 * and the helm, plume, tabard, cloak and open wings only on a captain, whose folded wings give way to them (michael contract).
 */
public class HostAngelRenderer extends GeoEntityRenderer<HostAngelEntity> {

    private static final ResourceLocation[] VESSELS = {tex("host_angel_0"), tex("host_angel_1"), tex("host_angel_2")};
    private static final ResourceLocation CAPTAIN = tex("host_angel_captain"), EYES = tex("host_angel_glowmask");
    private static final String[] CAPTAIN_ONLY = {"helmet", "plume", "wings_open", "tabard", "cloak"};

    public HostAngelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new Eyes(this));
        shadowRadius = 0.5f;
    }

    private static ResourceLocation tex(String name) {
        return SupernaturalCraft.asResource("textures/entity/" + name + ".png");
    }

    public static ResourceLocation texture(HostAngelEntity e) {
        return e.isCaptain() ? CAPTAIN : VESSELS[Math.floorMod(e.vessel(), VESSELS.length)];
    }

    @Override
    public void render(HostAngelEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model("host_angel"), GeoGuard.animation("host_angel"))) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, HostAngelEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            for (String bone : CAPTAIN_ONLY) MichaelRenderer.show(getGeoModel(), bone, e.isCaptain());
            // The captain's open wings grow from the same shoulders as the folded ones.
            MichaelRenderer.show(getGeoModel(), "wings_folded", !e.isCaptain());
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    static class Model extends DefaultedEntityGeoModel<HostAngelEntity> {
        Model() {
            super(SupernaturalCraft.asResource("host_angel"), true);
        }

        @Override
        public ResourceLocation getTextureResource(HostAngelEntity e) {
            ResourceLocation t = texture(e);
            return GeoGuard.exists(t) ? t : VESSELS[0];
        }
    }

    /** The eyes, one glowmask for every vessel. */
    static class Eyes extends GeoRenderLayer<HostAngelEntity> {
        Eyes(GeoRenderer<HostAngelEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, HostAngelEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                           VertexConsumer buffer, float partialTick, int light, int overlay) {
            if (!GeoGuard.exists(EYES)) return;
            RenderType eyes = RenderType.eyes(EYES);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, 0xF000F0, overlay, 0xFFFFFFFF);
        }
    }
}
