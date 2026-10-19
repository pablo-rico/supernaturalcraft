package org.papiricoh.supernaturalcraft.client.cain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainAssets;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Cain (v0.21): his rig ({@link CainAssets#GEO}). What shows follows the contract and his synced phase: the Mark
 * ({@link CainAssets#MARK}) from phase II, the First Blade ({@link CainAssets#FIRST_BLADE}) and his red eyes
 * ({@link CainAssets#EYES}) from phase III.
 *
 * <p>The glowmask is drawn in two passes ({@link Glow}) so its parts can breathe apart: the Mark (and the blade's vein) beats like a
 * heart -- slow in phase II, racing in phase III, blazing white-hot while it wakes (his transitions) -- fading in when it first
 * shows; his eyes kindle over a second and a half when the First Blade is taken up, then smoulder with a faint flicker.
 */
public class CainRenderer extends GeoEntityRenderer<CainEntity> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(CainAssets.GEO), ANIMATION = SupernaturalCraft.asResource(CainAssets.ANIM);

    /** Per Cain: the Mark's and the eyes' fade (0-1) and the last time they were stepped. */
    private static final Map<CainEntity, float[]> FADES = new WeakHashMap<>();

    public CainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("cain")));
        addRenderLayer(new Glow(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(CainEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(MODEL, ANIMATION)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
        markHalo(e, partialTick, pose, buffers);
    }

    /**
     * A halo round the Mark, beating with it: the rune itself is a few texels on the forearm's front and is lost at a distance or
     * whenever the arm turns, so its light is also thrown round the forearm (between the tracked elbow and wrist), seen from
     * every side.
     */
    private void markHalo(CainEntity e, float partial, PoseStack pose, MultiBufferSource buffers) {
        if (e.lookPhase() < 2 || e.isInvisible()) return;
        BakedGeoModel baked = getGeoModel().getBakedModel(getGeoModel().getModelResource(e, this));
        var elbow = baked.getBone("right_forearm").orElse(null);
        var wrist = baked.getBone("right_hand").orElse(null);
        if (elbow == null || wrist == null || !elbow.isTrackingMatrices()) return;
        float[] f = fades(e, partial);
        float k = Mth.clamp(markStrength(e, f[0], partial), 0, 1);
        if (k < 0.02f) return;
        var a = elbow.getWorldPosition();
        var b = wrist.getWorldPosition();
        // The tracked positions are the tick's (not interpolated): moved to this frame.
        Vec3 lag = e.getPosition(partial).subtract(e.position());
        Vec3 at = new Vec3(a.x + (b.x - a.x) * 0.55, a.y + (b.y - a.y) * 0.55, a.z + (b.z - a.z) * 0.55).add(lag);
        if (at.distanceToSqr(e.getPosition(partial)) > 9) return;
        Minecraft mc = Minecraft.getInstance();
        FxDraw d = new FxDraw(pose, buffers, mc.gameRenderer.getMainCamera(), e.getPosition(partial), partial, e.tickCount + partial);
        d.glowDisc(at.x, at.y, at.z, 0.32f + 0.12f * k, 0xD0141E, 0.5f * k);
        d.glowDisc(at.x, at.y, at.z, 0.12f, 0xFFB070, 0.65f * k);
    }

    @Override
    public void preRender(PoseStack pose, CainEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            GeoModel<CainEntity> geo = getGeoModel();
            int phase = e.lookPhase();
            AllegianceGeo.show(geo, CainAssets.MARK, phase >= 2);
            AllegianceGeo.show(geo, CainAssets.FIRST_BLADE, phase >= 3);
            AllegianceGeo.show(geo, CainAssets.EYES, phase >= 3);
            // The forearm's ends are tracked for the Mark's halo.
            model.getBone("right_forearm").ifPresent(b -> b.setTrackingMatrices(true));
            model.getBone("right_hand").ifPresent(b -> b.setTrackingMatrices(true));
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(CainEntity e) {
        return 0f;
    }

    /** The Mark's and the eyes' fades this frame: {mark, eyes}, stepped toward their targets (in over {@code in} ticks). */
    static float[] fades(CainEntity e, float partial) {
        float[] f = FADES.computeIfAbsent(e, k -> new float[]{0, 0, Float.NaN});
        float now = e.tickCount + partial;
        float dt = Float.isNaN(f[2]) ? 100 : Mth.clamp(now - f[2], 0, 10);
        f[2] = now;
        int phase = e.lookPhase();
        f[0] = step(f[0], phase >= 2 ? 1 : 0, dt, 20);
        f[1] = step(f[1], phase >= 3 ? 1 : 0, dt, 30);
        return f;
    }

    private static float step(float v, float target, float dt, float in) {
        return target > v ? Math.min(target, v + dt / in) : Math.max(target, v - dt / 10);
    }

    /** How strongly the Mark glows this frame (0-1+): its heartbeat, and a blaze while it wakes. */
    static float markStrength(CainEntity e, float fade, float partial) {
        if (fade <= 0.01f) return 0;
        float t = e.tickCount + partial;
        if (e.state() == LuciferEntity.TRANSITION || e.state() == LuciferEntity.EMERGING && e.lookPhase() >= 2) {
            return fade * (0.85f + 0.15f * Mth.sin(t * 1.7f));
        }
        float period = e.lookPhase() >= 3 ? 14 : 24, ph = (t % period) / period;
        float thump = Math.max(bump(ph, 0.05f), 0.6f * bump(ph, 0.25f));
        float rest = e.state() == LuciferEntity.DYING ? 0.25f : 0.5f;
        return fade * (rest + (1 - rest) * thump);
    }

    private static float bump(float x, float at) {
        float d = (x - at) / 0.07f;
        return (float) Math.exp(-d * d);
    }

    /** The glowmask in two passes: the Mark and the blade at the heartbeat's strength, then the eyes at theirs. */
    static final class Glow extends AutoGlowingGeoLayer<CainEntity> {

        Glow(CainRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, CainEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            if (!GeoGuard.exists(GeoGuard.glowmask(getTextureResource(e)))) return;
            RenderType glow = getRenderType(e, buffers);
            if (glow == null) return;
            GeoModel<CainEntity> geo = getGeoModel();
            int phase = e.lookPhase();
            float[] f = fades(e, partialTick);
            float mark = Mth.clamp(markStrength(e, f[0], partialTick), 0, 1);
            float t = e.tickCount + partialTick;
            float eyes = f[1] * (0.88f + 0.12f * Mth.sin(t * 0.7f + e.getId()));
            if (phase >= 2 && mark > 0.02f) {
                AllegianceGeo.show(geo, CainAssets.EYES, false);
                pass(pose, e, model, buffers, glow, partialTick, overlay, light, mark);
            }
            if (phase >= 3 && eyes > 0.02f) {
                AllegianceGeo.show(geo, CainAssets.EYES, true);
                AllegianceGeo.show(geo, CainAssets.MARK, false);
                AllegianceGeo.show(geo, CainAssets.FIRST_BLADE, false);
                pass(pose, e, model, buffers, glow, partialTick, overlay, light, eyes);
            }
            AllegianceGeo.show(geo, CainAssets.MARK, phase >= 2);
            AllegianceGeo.show(geo, CainAssets.FIRST_BLADE, phase >= 3);
            AllegianceGeo.show(geo, CainAssets.EYES, phase >= 3);
        }

        private void pass(PoseStack pose, CainEntity e, BakedGeoModel model, MultiBufferSource buffers, RenderType glow, float partial,
                          int overlay, int light, float k) {
            int c = getRenderer().getRenderColor(e, partial, light).argbInt();
            int r = Math.round((c >> 16 & 0xFF) * k), g = Math.round((c >> 8 & 0xFF) * k), b = Math.round((c & 0xFF) * k);
            int a = Math.round((c >>> 24) * k);
            getRenderer().reRender(model, pose, buffers, e, glow, buffers.getBuffer(glow), partial, 0xF00000, overlay,
                    a << 24 | r << 16 | g << 8 | b);
        }
    }
}
