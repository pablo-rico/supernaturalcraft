package org.papiricoh.supernaturalcraft.client.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;
import org.papiricoh.supernaturalcraft.entity.witch.WitchEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Set;

/**
 * A Hedge or Coven Witch (v0.21): her rig ({@link WitchAssets}) with the head turned toward what she looks at; her glowmask (the
 * eyes, a Coven Witch's hand runes) only while she casts — a {@code cast}, {@code hex}, {@code shield} or {@code teleport} clip on
 * her {@code action} controller, or a Coven Witch's {@code channel} — and nothing drawn for a moment after her smoke teleport
 * lands ({@link WitchGeo#vanish}). A Coven Witch's shield bubble is drawn with the magic's light
 * ({@code client/magic/fx/ShieldBubbles}).
 */
public class WitchRenderer<T extends WitchEntity> extends GeoEntityRenderer<T> {

    /** The clips during which a witch's glowmask shows. */
    public static final Set<String> CASTING = Set.of("cast", "hex", "shield", "teleport");

    private final String rig;

    public WitchRenderer(EntityRendererProvider.Context ctx, String rig) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(rig), true));
        this.rig = rig;
        addRenderLayer(FadingGlowLayer.whileCasting(this, (e, partial) -> casting(e)));
        shadowRadius = 0.45f;
    }

    /** Whether {@code witch} is casting now, as far as the client can tell. */
    public static boolean casting(WitchEntity witch) {
        if (witch instanceof CovenWitchEntity coven && coven.channeling()) return true;
        return CASTING.contains(WitchGeo.clip(witch, "action"));
    }

    @Override
    public void render(T witch, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model(rig), GeoGuard.animation(rig)) || WitchGeo.vanished(witch)) return;
        super.render(witch, yaw, partialTick, pose, buffers, light);
        if (witch instanceof CovenWitchEntity) {
            WitchBones.record(witch, getGeoModel().getBakedModel(getGeoModel().getModelResource(witch, this)), partialTick, HANDS);
        }
    }

    /** A Coven Witch's hands (her channel's beam leaves them: {@code client/witchboss}). */
    private static final WitchBones.Bone[] HANDS = {WitchBones.Bone.RIGHT_HAND, WitchBones.Bone.LEFT_HAND};

    @Override
    public void preRender(PoseStack pose, T witch, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource buffers,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender && witch instanceof CovenWitchEntity) WitchBones.track(model, HANDS);
        super.preRender(pose, witch, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
