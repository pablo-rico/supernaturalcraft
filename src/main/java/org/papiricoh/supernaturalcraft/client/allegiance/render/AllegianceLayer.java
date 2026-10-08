package org.papiricoh.supernaturalcraft.client.allegiance.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.client.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.michael.render.SeraphWingsDraw;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What a player's side shows on them (v0.13), on both player models:
 * <ul>
 *   <li>an angel's wings — shadow (ranks I–II, translucent) or light (III–IV, glowing), always out from rank II and while
 *   using powers at rank I; folded crouching, spread and beating in flight, held wide in a fall (as the Seraph Wings);</li>
 *   <li>the eyes ({@code RenderType.eyes}) while the server shows them: a demon's black (I), yellow (II) or red (III–IV),
 *   an angel's light;</li>
 *   <li>the King of Hell's crown and cloak (Demon IV), the cloak swinging with the stride;</li>
 *   <li>an angel's true form: a winged silhouette of light around them.</li>
 * </ul>
 * Every model may still be missing: the wings fall back to the Seraph Wings, the true form to a glowing copy of the
 * player, and the rest draw nothing.
 */
public class AllegianceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation WINGS_GEO = AllegianceGeo.asset(AllegianceAssets.WINGS_GEO),
            WINGS_ANIM = AllegianceGeo.asset(AllegianceAssets.WINGS_ANIM),
            WINGS_SHADOW = AllegianceGeo.asset(AllegianceAssets.WINGS_SHADOW_TEXTURE),
            WINGS_LIGHT = AllegianceGeo.asset(AllegianceAssets.WINGS_LIGHT_TEXTURE),
            WINGS_GLOW = AllegianceGeo.asset(AllegianceAssets.WINGS_LIGHT_GLOW),
            REGALIA_GEO = AllegianceGeo.asset(AllegianceAssets.REGALIA_GEO),
            REGALIA_TEX = AllegianceGeo.asset(AllegianceAssets.REGALIA_TEXTURE),
            REGALIA_GLOW = AllegianceGeo.asset(AllegianceAssets.REGALIA_GLOW),
            TRUE_GEO = AllegianceGeo.asset(AllegianceAssets.TRUE_FORM_GEO),
            TRUE_TEX = AllegianceGeo.asset(AllegianceAssets.TRUE_FORM_TEXTURE),
            TRUE_ANIM = AllegianceGeo.asset(AllegianceAssets.TRUE_FORM_ANIM);
    /** Wings are drawn at this scale from the body bone, as the Seraph Wings are (same attachment). */
    private static final float WINGS_SCALE = 1.0f;
    /** The regalia and the true form are modelled in player space: feet at 0, the neck pivot 24 px up. */
    private static final double NECK = 24 / 16.0;

    private static PlayerGeo.Renderer wings, regalia, trueForm;
    /** {@code SN_PREVIEW=allegiance}: every player's wings play this clip (null: as they move). */
    public static String forcedClip;
    private static final Map<AbstractClientPlayer, PlayerGeo.Wearable> WINGS = new WeakHashMap<>(), CROWNS = new WeakHashMap<>(),
            CLOAKS = new WeakHashMap<>(), TRUE_FORMS = new WeakHashMap<>();
    /** Each player's cloak swing, eased. */
    private static final Map<AbstractClientPlayer, float[]> SWING = new WeakHashMap<>();

    public AllegianceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        Allegiance a = Allegiances.get(player);
        int flags = ClientAllegiance.flags(player);
        float form = AllegianceFx.trueFormLight(player, partial);
        if (a.isAngel()) {
            boolean using = (flags & Allegiances.EYES) != 0 || form > 0;
            if (a.rank() >= 2 || using || player.isFallFlying()) drawWings(pose, buffers, light, player, a, partial, ageInTicks);
        }
        if ((flags & Allegiances.EYES) != 0 || a.isAngel() && form > 0) drawEyes(pose, buffers, a);
        if (a.isDemon() && a.rank() >= 4) drawRegalia(pose, buffers, light, player, partial, ageInTicks, limbSwingAmount);
        if (form > 0) drawTrueForm(pose, buffers, player, form, partial, ageInTicks);
    }

    // --- wings -------------------------------------------------------------------------------------------------------

    /** rest / fold (crouching) / flap (flying) / glide (falling, gliding) — the same moments the Seraph Wings show. */
    static String wingClip(AbstractClientPlayer p) {
        if (forcedClip != null) return forcedClip;
        if (p.isCrouching()) return "fold";
        boolean flying = p.getAbilities().flying || !p.onGround() && ClientAllegiance.wingsGranted(p) && p.getY() - p.yo > -0.25 && !p.isInWater();
        if (p.isFallFlying() || !p.onGround() && p.getY() - p.yo < -0.4) return "glide";
        if (flying) return "flap";
        return "rest";
    }

    private void drawWings(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, Allegiance a,
                           float partial, float ageInTicks) {
        // Seraph Wings already worn in the chest slot draw themselves.
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(AllItems.SERAPH_WINGS.get())) return;
        if (!GeoGuard.ready(WINGS_GEO, WINGS_ANIM)) {
            SeraphWingsDraw.draw(player, getParentModel(), pose, buffers, partial, ageInTicks);
            return;
        }
        if (wings == null) wings = new PlayerGeo.Renderer(new PlayerGeo.Model(WINGS_GEO, WINGS_SHADOW, WINGS_ANIM));
        PlayerGeo.Wearable w = WINGS.computeIfAbsent(player, k -> new PlayerGeo.Wearable("allegiance_wings", "rest"));
        w.clip = wingClip(player);
        w.hold = w.clip.equals("fold") || w.clip.equals("open");
        w.time = ageInTicks;
        boolean shining = a.rank() >= 3 || AllegianceFx.trueFormLight(player, partial) > 0;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.scale(-WINGS_SCALE, -WINGS_SCALE, WINGS_SCALE);
        pose.translate(0, -0.2, 0.02);
        pose.translate(-0.5, -0.51, -0.5);
        if (shining) {
            wings.model().texture = GeoGuard.exists(WINGS_LIGHT) ? WINGS_LIGHT : WINGS_SHADOW;
            wings.colour = 0xFFFFFFFF;
            wings.render(pose, w, buffers, RenderType.entityTranslucent(wings.model().texture), null, LightTexture.FULL_BRIGHT, partial);
            if (GeoGuard.exists(WINGS_GLOW)) {
                wings.render(pose, w, buffers, RenderType.eyes(WINGS_GLOW), null, LightTexture.FULL_BRIGHT, partial);
            }
        } else {
            // Shadow wings: smoke more than feathers.
            wings.model().texture = WINGS_SHADOW;
            wings.colour = 0xB8FFFFFF;
            wings.render(pose, w, buffers, RenderType.entityTranslucent(WINGS_SHADOW), null, light, partial);
        }
        pose.popPose();
    }

    // --- eyes --------------------------------------------------------------------------------------------------------

    /** black (Demon I), yellow (II), red (III–IV); an angel's are light. */
    static String eyes(Allegiance a) {
        if (a.isAngel()) return "light";
        return switch (a.rank()) {
            case 0, 1 -> "black";
            case 2 -> "yellow";
            default -> "red";
        };
    }

    private void drawEyes(PoseStack pose, MultiBufferSource buffers, Allegiance a) {
        ResourceLocation tex = AllegianceGeo.asset(AllegianceAssets.EYES_TEXTURE.formatted(eyes(a)));
        if (!GeoGuard.exists(tex)) return;
        // Black eyes are not light: drawn as a plain cutout over the face; the others glow.
        RenderType type = "black".equals(eyes(a)) ? RenderType.entityCutoutNoCull(tex) : RenderType.eyes(tex);
        getParentModel().renderToBuffer(pose, buffers.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    // --- the King's regalia ------------------------------------------------------------------------------------------

    private void drawRegalia(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial,
                             float ageInTicks, float limbSwingAmount) {
        if (!GeoGuard.ready(REGALIA_GEO, null)) return;
        if (regalia == null) regalia = new PlayerGeo.Renderer(new PlayerGeo.Model(REGALIA_GEO, REGALIA_TEX, null));
        float[] swing = SWING.computeIfAbsent(player, k -> new float[1]);
        double speed = Math.sqrt(Mth.square(player.getX() - player.xo) + Mth.square(player.getZ() - player.zo));
        float want = (float) Math.min(1.1, speed * 4.5) + (player.onGround() ? 0 : 0.35f);
        swing[0] += (want - swing[0]) * 0.15f;
        float s = swing[0], t = ageInTicks;
        PlayerGeo.Wearable crown = CROWNS.computeIfAbsent(player, k -> new PlayerGeo.Wearable(null, null));
        crown.time = t;
        crown.posing = m -> {
            for (String b : AllegianceAssets.REGALIA_BONES) AllegianceGeo.show(m, b, b.equals("crown"));
        };
        PlayerGeo.Wearable cloak = CLOAKS.computeIfAbsent(player, k -> new PlayerGeo.Wearable(null, null));
        cloak.time = t;
        cloak.posing = m -> {
            AllegianceGeo.show(m, "crown", false);
            for (int i = 0; i < 4; i++) {
                int seg = i;
                m.getBone("cloak_" + i).ifPresent(b -> {
                    b.setHidden(false);
                    b.setChildrenHidden(false);
                    // Each segment adds its share of the lift (the cloak streams back as you run), plus a slow ripple.
                    b.setRotX(-(s * (seg == 0 ? 0.45f : 0.18f)) - 0.04f * Mth.sin(t * 0.09f + seg * 0.8f));
                });
            }
        };
        // The crown on the head, the cloak on the body: two draws of one model.
        drawFromPart(pose, buffers, regalia, crown, getParentModel().head, light, partial, REGALIA_TEX, REGALIA_GLOW);
        drawFromPart(pose, buffers, regalia, cloak, getParentModel().body, light, partial, REGALIA_TEX, REGALIA_GLOW);
    }

    private static void drawFromPart(PoseStack pose, MultiBufferSource buffers, PlayerGeo.Renderer r, PlayerGeo.Wearable w,
                                     net.minecraft.client.model.geom.ModelPart part, int light, float partial, ResourceLocation tex,
                                     ResourceLocation glow) {
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(-1, -1, 1);
        // Player space: the part's pivot is the neck, 24 px above the feet.
        pose.translate(0, -NECK, 0);
        pose.translate(-0.5, -0.51, -0.5);
        r.model().texture = tex;
        r.colour = 0xFFFFFFFF;
        r.render(pose, w, buffers, RenderType.entityCutoutNoCull(tex), null, light, partial);
        if (GeoGuard.exists(glow)) r.render(pose, w, buffers, RenderType.eyes(glow), null, LightTexture.FULL_BRIGHT, partial);
        pose.popPose();
    }

    // --- true form ---------------------------------------------------------------------------------------------------

    private void drawTrueForm(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, float form, float partial,
                              float ageInTicks) {
        if (GeoGuard.ready(TRUE_GEO, TRUE_ANIM)) {
            if (trueForm == null) trueForm = new PlayerGeo.Renderer(new PlayerGeo.Model(TRUE_GEO, TRUE_TEX, TRUE_ANIM));
            PlayerGeo.Wearable w = TRUE_FORMS.computeIfAbsent(player, k -> new PlayerGeo.Wearable("true_form", "blaze"));
            w.clip = form < 0.999f ? "fade" : "burn";
            w.hold = w.clip.equals("fade");
            w.time = ageInTicks;
            pose.pushPose();
            // Back to the feet, Y up, in the body's facing.
            pose.translate(0, 1.501, 0);
            pose.scale(-1, -1, 1);
            pose.translate(-0.5, -0.51, -0.5);
            trueForm.colour = ((int) (form * 220) << 24) | 0xFFFFFF;
            trueForm.render(pose, w, buffers, RenderType.entityTranslucentEmissive(TRUE_TEX), null, LightTexture.FULL_BRIGHT, partial);
            pose.popPose();
            return;
        }
        // Stand-in: the player's own shape, a little larger, added on in light.
        pose.pushPose();
        pose.scale(1.15f, 1.08f, 1.15f);
        pose.translate(0, -0.08, 0);
        RenderType glow = RenderType.eyes(player.getSkin().texture());
        int alpha = (int) (form * 255 * (0.85f + 0.15f * Mth.sin(ageInTicks * 0.6f)));
        getParentModel().renderToBuffer(pose, buffers.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                (alpha << 24) | 0xFFF6DA);
        pose.popPose();
    }

    /** Forget every player's copies (after a resource reload the renderers are rebuilt). */
    public static void forget() {
        wings = regalia = trueForm = null;
        WINGS.clear();
        CROWNS.clear();
        CLOAKS.clear();
        TRUE_FORMS.clear();
    }
}
