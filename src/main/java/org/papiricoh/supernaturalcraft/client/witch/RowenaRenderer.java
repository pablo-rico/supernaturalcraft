package org.papiricoh.supernaturalcraft.client.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaAssets;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Set;

/**
 * Rowena (v0.21), for the boss and for {@code rowena_npc} alike ({@link RowenaAssets}: one rig): the closed grimoire at her hip
 * ({@code grimoire}) hidden while the open one is in her hand ({@code recite}, {@code read}); her eyes, rings and jewellery
 * glowing only while she casts (a spell clip, {@code recite}; at home the {@code give} of a page); and nothing drawn while her
 * yellow smoke covers a blink ({@code teleport} at its {@link RowenaAssets#HIT_TICKS} moment, or a TELEPORT payload).
 * Register it for the boss with {@code ctx -> new RowenaRenderer<>(ctx)}.
 */
public class RowenaRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final String RIG = "rowena";
    /** Clips (either controller) during which her glowmask shows. */
    public static final Set<String> CASTING = Set.of("fling", "ignis", "glacies", "summon_victims", "teleport", "recite", "give");
    /** Clips (either controller) during which the open grimoire is in her hand. */
    public static final Set<String> READING = Set.copyOf(RowenaAssets.PROPS.get("grimoire_open"));

    public RowenaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(RIG), true));
        addRenderLayer(FadingGlowLayer.whileCasting(this, (e, partial) -> playing(e, CASTING)));
        shadowRadius = 0.45f;
    }

    static boolean playing(LivingEntity e, Set<String> clips) {
        return clips.contains(WitchGeo.clip(e, "action")) || clips.contains(WitchGeo.clip(e, "base"));
    }

    @Override
    public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model(RIG), GeoGuard.animation(RIG)) || WitchGeo.vanished(e)
                || WitchGeo.inBlink(e, "teleport", RowenaAssets.HIT_TICKS.get("teleport"), partialTick)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
        WitchBones.record(e, getGeoModel().getBakedModel(getGeoModel().getModelResource(e, this)), partialTick, TRACKED);
    }

    /** Her hands and head, for the world effects ({@link WitchBones}). */
    private static final WitchBones.Bone[] TRACKED = {WitchBones.Bone.RIGHT_HAND, WitchBones.Bone.LEFT_HAND, WitchBones.Bone.HEAD};

    @Override
    public void preRender(PoseStack pose, T e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            WitchBones.track(model, TRACKED);
            boolean open = playing(e, READING);
            for (String bone : RowenaAssets.PROCEDURAL) WitchGeo.show(getGeoModel(), bone, !open);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
