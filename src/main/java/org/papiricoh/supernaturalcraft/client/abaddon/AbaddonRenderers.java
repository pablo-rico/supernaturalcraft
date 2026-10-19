package org.papiricoh.supernaturalcraft.client.abaddon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonHeraldEntity;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonKnightEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Abaddon and her retinue (v0.21): each rig of the contract ({@link AbaddonAssets}) with its glowmask lit.
 * <ul>
 *   <li>Her: not drawn while she is smoke riding a knight, nor while she reforms out of the smoke after leaving one (until the
 *   smoke has condensed, {@link AbaddonWorldFx.Reform#CONDENSED}); then, and through the stun's ×2 window, a crimson shimmer
 *   (her body again, additive, jittering) over her. Dying she burns from inside: her glow and a crimson heat over her whole body
 *   rise until the smoke takes her ({@link AbaddonWorldFx.Death#HIDE}), and she is gone.</li>
 *   <li>A knight rises out of the floor when called ({@link ClientAbaddonFx#risen}). Possessed, its glowmask burns crimson and
 *   beats (the visor's points, the blade's veins), its visor blazes as she goes in, and a heat runs up the blade's veins.</li>
 *   <li>The herald's bell and clapper are its clips' own.</li>
 * </ul>
 * A few bones are tracked so the world effects can start from them ({@link #bone}): her right hand and head, the knight's visor
 * and blade, the herald's bell.
 */
public final class AbaddonRenderers {

    /** The bones the effects use. */
    public enum Bone {
        HAND("right_hand"), HEAD("head"), VISOR("visor"), BLADE("blade"), VEINS("blade_veins"), BELL("bell");

        final String name;

        Bone(String name) {
            this.name = name;
        }
    }

    /** Per entity: each tracked bone's offset from its position, as last drawn. */
    private static final Map<Entity, Vec3[]> BONES = new WeakHashMap<>();

    private AbaddonRenderers() {
    }

    /** Where bone {@code b} of {@code e} is this frame, if it has been drawn lately (null otherwise). */
    public static @Nullable Vec3 bone(Entity e, Bone b, float partial) {
        Vec3[] v = BONES.get(e);
        if (v == null || v[b.ordinal()] == null) return null;
        return e.getPosition(partial).add(v[b.ordinal()]);
    }

    static float easeOut(float k) {
        k = Mth.clamp(k, 0, 1);
        return 1 - (1 - k) * (1 - k) * (1 - k);
    }

    /** A rig of the contract, drawn once its files have loaded. */
    public static class Rig<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {
        private final ResourceLocation model, animation;
        private final Bone[] tracked;

        public Rig(EntityRendererProvider.Context ctx, String rig, float shadow, Bone... tracked) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(rig)));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = shadow;
            model = SupernaturalCraft.asResource(AbaddonAssets.geo(rig));
            animation = SupernaturalCraft.asResource(AbaddonAssets.anim(rig));
            this.tracked = tracked;
        }

        @Override
        public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(model, animation)) return;
            float rise = ClientAbaddonFx.risen(e.getId(), partialTick);
            if (rise < 1) {
                // Called up: out of the floor.
                pose.pushPose();
                pose.translate(0, -(1 - easeOut(rise)) * e.getBbHeight() * 1.05, 0);
                super.render(e, yaw, partialTick, pose, buffers, light);
                pose.popPose();
            } else {
                super.render(e, yaw, partialTick, pose, buffers, light);
            }
            record(e, partialTick);
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel baked, MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                for (Bone b : tracked) baked.getBone(b.name).ifPresent(g -> g.setTrackingMatrices(true));
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        /** The tracked bones' places (they are the tick's, not the frame's: kept as offsets from the entity). */
        private void record(T e, float partial) {
            if (tracked.length == 0) return;
            BakedGeoModel baked = getGeoModel().getBakedModel(getGeoModel().getModelResource(e, this));
            Vec3[] v = BONES.computeIfAbsent(e, k -> new Vec3[Bone.values().length]);
            Vec3 base = e.getPosition(partial);
            for (Bone b : tracked) {
                GeoBone g = baked.getBone(b.name).orElse(null);
                if (g == null || !g.isTrackingMatrices()) continue;
                var w = g.getWorldPosition();
                Vec3 off = new Vec3(w.x - base.x, w.y - base.y, w.z - base.z);
                // A stale or unset matrix (far off, or not drawn yet) is dropped.
                v[b.ordinal()] = off.lengthSqr() < 16 ? off : null;
            }
        }

        @Override
        protected float getDeathMaxRotation(T e) {
            return 0f;
        }
    }

    /** One more pass of a rig's model in {@code type}, tinted {@code argb} (additive types brighten). */
    static <T extends GeoAnimatable> void pass(GeoRenderer<T> r, PoseStack pose, T e, BakedGeoModel model, MultiBufferSource buffers,
                                               RenderType type, float partial, int overlay, int argb) {
        r.reRender(model, pose, buffers, e, type, buffers.getBuffer(type), partial, 0xF00000, overlay, argb);
    }

    static int argb(float k, int rgb) {
        k = Mth.clamp(k, 0, 1);
        int r = Math.round((rgb >> 16 & 0xFF) * k), g = Math.round((rgb >> 8 & 0xFF) * k), b = Math.round((rgb & 0xFF) * k);
        return Math.round(255 * k) << 24 | r << 16 | g << 8 | b;
    }

    // --- her -----------------------------------------------------------------------------------------------------------------------

    /** Abaddon: hidden while she is smoke (and while she reforms); her shimmer, her burning. */
    public static class Abaddon extends Rig<AbaddonEntity> {
        public Abaddon(EntityRendererProvider.Context ctx) {
            super(ctx, "abaddon", 0.5f, Bone.HAND, Bone.HEAD);
            addRenderLayer(new Heat(this));
        }

        @Override
        public void render(AbaddonEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (e.smoke()) return;
            float reform = ClientAbaddonFx.reforming(e.getId(), partialTick);
            if (reform >= 0 && reform < AbaddonWorldFx.Reform.CONDENSED) return;
            if (ClientAbaddonFx.dying(e.getId(), partialTick) >= AbaddonWorldFx.Death.HIDE) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        /** Her burning from inside as she dies (0-1), or 0. */
        static float burn(AbaddonEntity e, float partial) {
            float d = ClientAbaddonFx.dying(e.getId(), partial);
            if (d < 0) return e.state() == LuciferEntity.DYING ? 0.4f : 0;
            return Mth.clamp(d / AbaddonWorldFx.Death.BURN, 0, 1);
        }

        /** Her crimson shimmer: strong as she reforms, then flickering through the stun. */
        static float shimmer(AbaddonEntity e, float partial) {
            float reform = ClientAbaddonFx.reforming(e.getId(), partial);
            float k = 0;
            if (reform >= 0) k = Math.max(k, 1 - Mth.clamp((reform - AbaddonWorldFx.Reform.CONDENSED) / 18f, 0, 1));
            if (e.stunned()) {
                float t = e.tickCount + partial;
                k = Math.max(k, 0.28f + 0.18f * Mth.sin(t * 1.9f) * Mth.sin(t * 0.37f + 1));
            }
            return k;
        }
    }

    /** Her heat: the glowmask hotter and a crimson light over her whole body as she burns; her shimmer as she reforms. */
    static final class Heat extends AutoGlowingGeoLayer<AbaddonEntity> {
        Heat(Abaddon renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, AbaddonEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            float t = e.tickCount + partialTick;
            float burn = Abaddon.burn(e, partialTick);
            ResourceLocation tex = getTextureResource(e);
            if (burn > 0.01f) {
                float flick = 0.8f + 0.2f * Mth.sin(t * 1.3f) * Mth.sin(t * 0.43f);
                RenderType glow = getRenderType(e, buffers);
                if (glow != null && GeoGuard.exists(GeoGuard.glowmask(tex))) {
                    pass(getRenderer(), pose, e, model, buffers, glow, partialTick, overlay,
                            argb(burn * flick, 0xFF5A3A));
                }
                pass(getRenderer(), pose, e, model, buffers, RenderType.eyes(tex), partialTick, overlay,
                        argb(burn * burn * 0.55f * flick, 0xFF2A14));
            }
            float sh = Abaddon.shimmer(e, partialTick);
            if (sh > 0.01f) {
                pose.pushPose();
                float j = 0.035f * sh;
                pose.translate(j * Mth.sin(t * 7.1f), 0.01f * Mth.sin(t * 5.3f), j * Mth.cos(t * 6.3f));
                pass(getRenderer(), pose, e, model, buffers, RenderType.eyes(tex), partialTick, overlay,
                        argb(0.5f * sh, 0xC0141E));
                pose.popPose();
            }
        }
    }

    // --- the knight ------------------------------------------------------------------------------------------------------------------

    /** A knight of hers: possessed, it burns crimson. */
    public static class Knight extends Rig<AbaddonKnightEntity> {
        private static final Map<AbaddonKnightEntity, float[]> FADES = new WeakHashMap<>();

        public Knight(EntityRendererProvider.Context ctx) {
            super(ctx, AbaddonAssets.KNIGHT, 0.7f, Bone.VISOR, Bone.BLADE, Bone.VEINS);
            addRenderLayer(new Possessed(this));
        }

        /** How possessed it looks this frame (0-1): fading in over 8 ticks, out over 16. */
        static float possession(AbaddonKnightEntity e, float partial) {
            float[] f = FADES.computeIfAbsent(e, k -> new float[]{0, Float.NaN});
            float now = e.tickCount + partial, dt = Float.isNaN(f[1]) ? 100 : Mth.clamp(now - f[1], 0, 10);
            f[1] = now;
            f[0] = e.possessed() ? Math.min(1, f[0] + dt / 8) : Math.max(0, f[0] - dt / 16);
            return f[0];
        }

        /** Its glow's beat (0-1): a slow, heavy throb. */
        static float beat(AbaddonKnightEntity e, float partial) {
            float t = e.tickCount + partial, ph = (t % 22) / 22f;
            float d = (ph - 0.08f) / 0.09f, d2 = (ph - 0.3f) / 0.09f;
            return Math.max((float) Math.exp(-d * d), 0.6f * (float) Math.exp(-d2 * d2));
        }

        @Override
        public void render(AbaddonKnightEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            super.render(e, yaw, partialTick, pose, buffers, light);
            float k = possession(e, partialTick);
            if (k < 0.02f || e.isInvisible() || !GeoGuard.ready(GeoGuard.model(AbaddonAssets.KNIGHT), null)) return;
            // The visor's points blazing, the blade's veins running with heat.
            Minecraft mc = Minecraft.getInstance();
            FxDraw d = new FxDraw(pose, buffers, mc.gameRenderer.getMainCamera(), e.getPosition(partialTick), partialTick, e.tickCount + partialTick);
            float beat = beat(e, partialTick), taken = ClientAbaddonFx.taken(e.getId(), partialTick);
            float flare = taken >= 0 ? Mth.clamp(1 - Math.abs(taken - 12) / 14f, 0, 1) : 0;
            Vec3 visor = bone(e, Bone.VISOR, partialTick);
            if (visor != null) {
                d.glowDisc(visor.x, visor.y, visor.z, 0.28f + 0.1f * beat + 0.6f * flare, AbaddonWorldFx.CRIMSON, (0.45f + 0.25f * beat) * k);
                d.glowDisc(visor.x, visor.y, visor.z, 0.1f + 0.2f * flare, AbaddonWorldFx.CRIMSON_HOT, (0.6f + 0.4f * flare) * k);
                if (flare > 0.05f) d.star(visor.x, visor.y, visor.z, 1.4f * flare, 0, AbaddonWorldFx.CRIMSON_HOT, flare * k);
            }
            Vec3 hilt = bone(e, Bone.BLADE, partialTick), veins = bone(e, Bone.VEINS, partialTick);
            if (hilt != null && veins != null) {
                Vec3 tip = hilt.add(veins.subtract(hilt).scale(1.6));
                d.softBeam(hilt.x, hilt.y, hilt.z, tip.x, tip.y, tip.z, 0.16f, AbaddonWorldFx.CRIMSON, (0.25f + 0.35f * beat) * k, 0.05f * k, false);
            }
        }
    }

    /** A possessed knight's glowmask again, crimson and beating. */
    static final class Possessed extends AutoGlowingGeoLayer<AbaddonKnightEntity> {
        Possessed(Knight renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, AbaddonKnightEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            float k = Knight.possession(e, partialTick);
            if (k < 0.02f || !GeoGuard.exists(GeoGuard.glowmask(getTextureResource(e)))) return;
            RenderType glow = getRenderType(e, buffers);
            if (glow == null) return;
            float beat = Knight.beat(e, partialTick), taken = ClientAbaddonFx.taken(e.getId(), partialTick);
            float flare = taken >= 0 ? Mth.clamp(1 - Math.abs(taken - 12) / 14f, 0, 1) : 0;
            pass(getRenderer(), pose, e, model, buffers, glow, partialTick, overlay,
                    argb(k * (0.55f + 0.45f * beat + flare), 0xFF2A1E));
        }
    }

    // --- the herald ------------------------------------------------------------------------------------------------------------------

    /** The herald: its bell tracked (the mark's toll and thread start there). */
    public static class Herald extends Rig<AbaddonHeraldEntity> {
        public Herald(EntityRendererProvider.Context ctx) {
            super(ctx, AbaddonAssets.HERALD, 0.5f, Bone.BELL);
        }
    }
}
