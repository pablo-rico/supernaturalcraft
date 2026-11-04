package org.papiricoh.supernaturalcraft.client.crowley;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyAssets;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyCompanionEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySuitEntity;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.PossessedPatientEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Crowley and his retinue (v0.35), each rig of the contract ({@link CrowleyAssets}).
 * <ul>
 *   <li>Crowley (the boss and the companion share the rig): not drawn at all while {@code vanished()}; his eyes' shell (bone
 *   {@code eyes}) and its glow only while {@code eyesLit()}. Dying (the boss), the demon's light flickers through him from inside
 *   at the {@code death} clip's flare (tick {@link #FLARE}), orange-red, strobing, then gone.</li>
 *   <li>The suited demon and the possessed patient: plain rigs (their black eyes are painted).</li>
 * </ul>
 */
public final class CrowleyRenderers {

    /** The death clip's flare, in ticks from its start ({@code CrowleyAssets.HIT_TICKS.death}). */
    static final int FLARE = CrowleyAssets.HIT_TICKS.getOrDefault("death", 50);
    private static final int FLARE_RED = 0xFF7A3A, FLARE_HOT = 0xFFE0A0;
    /** Per boss: the client tick it was first seen dying. */
    private static final Map<LivingEntity, float[]> DYING = new WeakHashMap<>();
    /** Previews: light (or darken) every Crowley's eyes whatever his flag says (null: his own). */
    private static @Nullable Boolean forcedEyes;

    private CrowleyRenderers() {
    }

    public static void forceEyes(@Nullable Boolean lit) {
        forcedEyes = lit;
    }

    static boolean eyes(LivingEntity e) {
        if (forcedEyes != null) return forcedEyes;
        if (e instanceof CrowleyEntity c) return c.eyesLit();
        return e instanceof CrowleyCompanionEntity c && c.eyesLit();
    }

    /** Ticks since boss {@code e} began to die, or -1. */
    static float dying(LivingEntity e, float partial) {
        if (!(e instanceof LuciferEntity l) || l.state() != LuciferEntity.DYING) {
            DYING.remove(e);
            return -1;
        }
        float now = e.tickCount + partial;
        float[] at = DYING.computeIfAbsent(e, k -> new float[]{now});
        return now - at[0];
    }

    /** How bright the death flare is now (0-1): it builds through the clip's arch, peaks at {@link #FLARE}, strobes and dies. */
    static float flare(float dt) {
        if (dt < 0) return 0;
        float up = Mth.clamp((dt - (FLARE - 22)) / 22f, 0, 1);
        float down = 1 - Mth.clamp((dt - FLARE - 4) / 14f, 0, 1);
        float strobe = 0.65f + 0.35f * Mth.sin(dt * 2.7f) * Mth.sin(dt * 1.3f + 1);
        return up * up * down * strobe;
    }

    static int argb(float k, int rgb) {
        k = Mth.clamp(k, 0, 1);
        int r = Math.round((rgb >> 16 & 0xFF) * k), g = Math.round((rgb >> 8 & 0xFF) * k), b = Math.round((rgb & 0xFF) * k);
        return Math.round(255 * k) << 24 | r << 16 | g << 8 | b;
    }

    /** A rig of the contract, drawn once its files have loaded. */
    public static class Rig<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {
        private final ResourceLocation model, animation;

        public Rig(EntityRendererProvider.Context ctx, String rig, float shadow) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(rig)));
            shadowRadius = shadow;
            model = SupernaturalCraft.asResource(CrowleyAssets.geo(rig));
            animation = SupernaturalCraft.asResource(CrowleyAssets.anim(rig));
        }

        @Override
        public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(model, animation)) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        protected float getDeathMaxRotation(T e) {
            return 0f;
        }
    }

    /** Crowley: the boss and his companion form. */
    public static class Crowley<T extends LivingEntity & GeoEntity> extends Rig<T> {
        public Crowley(EntityRendererProvider.Context ctx) {
            super(ctx, CrowleyAssets.RIG, 0.5f);
            addRenderLayer(new Eyes<>(this));
        }

        @Override
        public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (e instanceof CrowleyEntity c && c.vanished()) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
            float k = flare(dying(e, partialTick));
            if (k > 0.02f) {
                // The demon's light bursting out of him: a glow round his chest and head, rays at the peak.
                Minecraft mc = Minecraft.getInstance();
                Vec3 at = e.getPosition(partialTick);
                FxDraw d = new FxDraw(pose, buffers, mc.gameRenderer.getMainCamera(), at, partialTick, e.tickCount + partialTick);
                d.glowDisc(at.x, at.y + 1.2, at.z, 1.1f + 0.6f * k, FLARE_RED, 0.55f * k);
                d.glowDisc(at.x, at.y + 1.45, at.z, 0.45f + 0.3f * k, FLARE_HOT, 0.8f * k);
                if (k > 0.6f) d.star(at.x, at.y + 1.45, at.z, 2.2f * k, e.tickCount * 0.05f, FLARE_HOT, k - 0.4f);
            }
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel baked, MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int light, int overlay, int colour) {
            if (!isReRender) baked.getBone("eyes").ifPresent(b -> b.setHidden(!eyes(e)));
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    /** His glowmask (the red eyes) only while they are lit; the whole body lit from inside at his death's flare. */
    static final class Eyes<T extends LivingEntity & GeoEntity> extends AutoGlowingGeoLayer<T> {
        Eyes(Crowley<T> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, T e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            if (eyes(e)) super.render(pose, e, model, type, buffers, buffer, partialTick, light, overlay);
            float k = flare(dying(e, partialTick));
            if (k > 0.02f) {
                RenderType lit = RenderType.eyes(getTextureResource(e));
                getRenderer().reRender(model, pose, buffers, e, lit, buffers.getBuffer(lit), partialTick, 0xF00000, overlay,
                        argb(0.85f * k, FLARE_RED));
            }
        }
    }

    public static class Suit extends Rig<CrowleySuitEntity> {
        public Suit(EntityRendererProvider.Context ctx) {
            super(ctx, CrowleyAssets.SUIT, 0.5f);
        }
    }

    public static class Patient extends Rig<PossessedPatientEntity> {
        public Patient(EntityRendererProvider.Context ctx) {
            super(ctx, CrowleyAssets.PATIENT, 0.45f);
        }
    }
}
