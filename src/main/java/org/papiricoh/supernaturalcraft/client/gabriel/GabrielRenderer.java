package org.papiricoh.supernaturalcraft.client.gabriel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielDoubleEntity;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

import java.util.Map;

/**
 * Gabriel and his doubles (v0.14): one rig ({@link GabrielAssets#GEO}), one texture per costume. What shows follows the
 * contract: only the worn costume's bone group; a prop ({@code pie}, {@code microphone}, {@code defibrillator}) only while
 * its clip plays; the {@code lollipop} always; the {@code nurse_cap} only on a nurse; the six golden shadow {@code wings}
 * only on the real Gabriel in the commercial (or while he reveals them). Drawn translucent (the wings fade at their tips),
 * with the one glowmask for every costume. A double that free will sees through flickers, half there, for this hunter.
 * Draws nothing until the model is baked.
 */
public class GabrielRenderer<T extends Monster & GeoAnimatable> extends GeoEntityRenderer<T> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(GabrielAssets.GEO),
            ANIMATION = SupernaturalCraft.asResource(GabrielAssets.ANIM), GLOW = SupernaturalCraft.asResource(GabrielAssets.GLOW);
    private static final Map<Channel.Costume, ResourceLocation> TEXTURES = new java.util.EnumMap<>(Channel.Costume.class);

    static {
        for (Channel.Costume c : Channel.Costume.values()) {
            TEXTURES.put(c, SupernaturalCraft.asResource(GabrielAssets.TEXTURE.formatted(c.id())));
        }
    }

    public GabrielRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model<>());
        addRenderLayer(new Glow<>(this));
        shadowRadius = 0.45f;
    }

    /** Whether the rig and its clips are loaded (the art may still be on its way). */
    public static boolean ready() {
        return GeoGuard.ready(MODEL, ANIMATION);
    }

    static Channel.Costume costume(Monster e) {
        if (e instanceof GabrielEntity g) return g.costume();
        if (e instanceof GabrielDoubleEntity d) return d.costume();
        return Channel.Costume.JACKET;
    }

    static ResourceLocation texture(Channel.Costume costume) {
        ResourceLocation t = TEXTURES.get(costume);
        return GeoGuard.exists(t) ? t : TEXTURES.get(Channel.Costume.JACKET);
    }

    /** Whether the real Gabriel shows his six wings: the commercial (once he has arrived), or the reveal clip. */
    static boolean wings(Monster e, String clip) {
        if (!(e instanceof GabrielEntity g)) return false;
        return "wings_reveal".equals(clip) || g.phase() == 4 && g.state() != LuciferEntity.EMERGING;
    }

    @Override
    public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ready()) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    /** His wings reach well past his little hitbox in the commercial. */
    @Override
    public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        double r = e instanceof GabrielEntity g && g.phase() == 4 ? 3 : 0.5;
        return frustum.isVisible(e.getBoundingBox().inflate(r, 1, r));
    }

    @Override
    public RenderType getRenderType(T e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void preRender(PoseStack pose, T e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            GeoModel<T> geo = getGeoModel();
            String clip = AllegianceGeo.clip(e, getInstanceId(e), "action");
            Channel.Costume worn = costume(e);
            for (Map.Entry<Channel.Costume, String> c : GabrielAssets.COSTUME_BONES.entrySet()) {
                AllegianceGeo.show(geo, c.getValue(), c.getKey() == worn);
            }
            for (Map.Entry<String, String> prop : GabrielAssets.PROP_CLIPS.entrySet()) {
                AllegianceGeo.show(geo, prop.getKey(), prop.getValue().equals(clip));
            }
            AllegianceGeo.show(geo, "lollipop", true);
            AllegianceGeo.show(geo, "nurse_cap", e instanceof GabrielDoubleEntity d && d.role() == GabrielDoubleEntity.Role.NURSE);
            boolean wings = wings(e, clip);
            AllegianceGeo.show(geo, "wings", wings);
            // The archangel showing through his eyes: only with the wings out.
            AllegianceGeo.show(geo, "eyes_glow", wings);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** A double that free will sees through: flickering, half there (only for the hunter the server told). */
    @Override
    public Color getRenderColor(T e, float partialTick, int light) {
        Color base = super.getRenderColor(e, partialTick, light);
        if (!ClientGabriel.seenThrough(e.getId())) return base;
        float t = e.tickCount + partialTick;
        float a = 0.28f + 0.14f * Mth.sin(t * 0.9f) + (Math.floorMod((int) t * 7, 13) == 0 ? -0.15f : 0);
        return Color.ofARGB(Mth.clamp(a, 0.08f, 1f), 0.75f, 0.85f, 1f);
    }

    @Override
    protected float getDeathMaxRotation(T e) {
        return 0f;
    }

    /** The rig, the clips, and the costume's texture. */
    static final class Model<T extends Monster & GeoAnimatable> extends GeoModel<T> {
        @Override
        public ResourceLocation getModelResource(T e) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(T e) {
            return texture(costume(e));
        }

        @Override
        public ResourceLocation getAnimationResource(T e) {
            return ANIMATION;
        }
    }

    /** The one glowmask for every costume ({@link GabrielAssets#GLOW}): the wings' light, his eyes in the reveal. */
    static final class Glow<T extends Monster & GeoAnimatable> extends GeoRenderLayer<T> {
        Glow(GeoRenderer<T> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, T e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            if (!GeoGuard.exists(GLOW)) return;
            RenderType eyes = RenderType.eyes(GLOW);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, LightTexture.FULL_BRIGHT, overlay,
                    getRenderer().getRenderColor(e, partialTick, light).argbInt());
        }
    }
}
