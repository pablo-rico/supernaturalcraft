package org.papiricoh.supernaturalcraft.client.apocalypse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyAssets;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyEntity;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyNpcEntity;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairAssets;
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The core's rigs on the client (v0.40), each of its contract.
 * <ul>
 *   <li>Alastair ({@link AlastairAssets}): his scalpel hidden while he is bound (phase I); his white eyes' shell and its glow
 *   only once he has broken free (phase II on) and while he dies; dying, the demon's light flares through him at the
 *   {@code death} clip's moment.</li>
 *   <li>Ruby ({@link RubyAssets}): one rig for the fight and the NPC. The fight shows the knife in her hand and her black eyes;
 *   the NPC the knife sheathed at her hip, her eyes her own. Dying, the flare.</li>
 * </ul>
 * Nothing is drawn until the rig's files have loaded ({@link GeoGuard}). The rigs' clips drive every bone the renderer does not
 * toggle; the renderer never moves a bone.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ApocalypseRenderers {

    private static final int FLARE_RED = 0xFF7A3A;
    /** Per dying entity: the client tick it was first seen dying. */
    private static final Map<LivingEntity, float[]> DYING = new WeakHashMap<>();

    private ApocalypseRenderers() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.ALASTAIR.get(), Alastair::new);
        event.registerEntityRenderer(AllEntities.RUBY.get(), Ruby::new);
        event.registerEntityRenderer(AllEntities.RUBY_NPC.get(), Ruby::new);
    }

    /** Shows or hides {@code name} and everything under it. */
    public static void show(BakedGeoModel baked, String name, boolean on) {
        baked.getBone(name).ifPresent(b -> {
            b.setHidden(!on);
            b.setChildrenHidden(!on);
        });
    }

    /** Whether a boss (or a mob) is dying now. */
    public static boolean dying(LivingEntity e) {
        if (e instanceof LuciferEntity l) return l.state() == LuciferEntity.DYING;
        return e.isDeadOrDying();
    }

    /** Ticks since {@code e} began to die, or -1. */
    public static float dyingFor(LivingEntity e, float partial) {
        if (!dying(e)) {
            DYING.remove(e);
            return -1;
        }
        float now = e.tickCount + partial;
        float[] at = DYING.computeIfAbsent(e, k -> new float[]{now});
        return now - at[0];
    }

    /** How bright a death flare is (0-1) {@code dt} ticks into dying, peaking at {@code peak}: it builds, strobes and dies. */
    public static float flare(float dt, int peak) {
        if (dt < 0) return 0;
        float up = Mth.clamp((dt - (peak - 22)) / 22f, 0, 1);
        float down = 1 - Mth.clamp((dt - peak - 4) / 14f, 0, 1);
        float strobe = 0.65f + 0.35f * Mth.sin(dt * 2.7f) * Mth.sin(dt * 1.3f + 1);
        return up * up * down * strobe;
    }

    public static int argb(float k, int rgb) {
        k = Mth.clamp(k, 0, 1);
        int r = Math.round((rgb >> 16 & 0xFF) * k), g = Math.round((rgb >> 8 & 0xFF) * k), b = Math.round((rgb & 0xFF) * k);
        return Math.round(255 * k) << 24 | r << 16 | g << 8 | b;
    }

    /** A contract rig, drawn once its files have loaded; it never tips over on death (the clip does the fall). */
    public static class Rig<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {
        private final ResourceLocation model, animation;

        public Rig(EntityRendererProvider.Context ctx, String rig, float shadow, boolean turnsHead) {
            this(ctx, rig, shadow, turnsHead, null);
        }

        /** {@code texture}: the texture of each one (a variant's), or null for the rig's own. */
        public Rig(EntityRendererProvider.Context ctx, String rig, float shadow, boolean turnsHead,
                   java.util.function.Function<T, ResourceLocation> texture) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(rig), turnsHead) {
                @Override
                public ResourceLocation getTextureResource(T e) {
                    return texture == null ? super.getTextureResource(e) : texture.apply(e);
                }
            });
            shadowRadius = shadow;
            model = GeoGuard.model(rig);
            animation = GeoGuard.animation(rig);
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

    /**
     * A glowmask drawn full bright only while {@code lit} says so (the eyes' shell is hidden otherwise, so it is the eyes that
     * glow), and the whole body lit from inside at a death flare peaking at {@code flarePeak} ticks into dying (negative: none).
     */
    public static class Glow<T extends LivingEntity & GeoEntity> extends GeoRenderLayer<T> {
        private final java.util.function.Predicate<T> lit;
        private final int flarePeak, flareRgb;

        public Glow(GeoRenderer<T> renderer, java.util.function.Predicate<T> lit, int flarePeak) {
            this(renderer, lit, flarePeak, FLARE_RED);
        }

        public Glow(GeoRenderer<T> renderer, java.util.function.Predicate<T> lit, int flarePeak, int flareRgb) {
            super(renderer);
            this.lit = lit;
            this.flarePeak = flarePeak;
            this.flareRgb = flareRgb;
        }

        /** The glow's tint and alpha (ARGB). */
        protected int colour(T e, float partialTick) {
            return 0xFFFFFFFF;
        }

        @Override
        public void render(PoseStack pose, T e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            ResourceLocation tex = getTextureResource(e);
            ResourceLocation glow = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), tex.getPath().replace(".png", "_glowmask.png"));
            if (lit.test(e) && GeoGuard.exists(glow)) {
                RenderType eyes = RenderType.eyes(glow);
                getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, 0xF000F0, overlay, colour(e, partialTick));
            }
            if (flarePeak < 0) return;
            float k = flare(dyingFor(e, partialTick), flarePeak);
            if (k > 0.02f) {
                RenderType body = RenderType.eyes(tex);
                getRenderer().reRender(model, pose, buffers, e, body, buffers.getBuffer(body), partialTick, 0xF00000, overlay,
                        argb(0.85f * k, flareRgb));
            }
        }
    }

    // --- Alastair ------------------------------------------------------------------------------------------------------------

    /** Previews: light (or darken) every Alastair's eyes whatever his phase (null: his own). */
    private static @Nullable Boolean forcedAlastairEyes;

    public static void forceAlastairEyes(@Nullable Boolean lit) {
        forcedAlastairEyes = lit;
    }

    /** His eyes go white once he has broken free of the trap, and as he dies. */
    public static boolean alastairEyes(AlastairEntity e) {
        if (forcedAlastairEyes != null) return forcedAlastairEyes;
        return e.phase() >= 2 || e.state() == LuciferEntity.DYING;
    }

    public static class Alastair extends Rig<AlastairEntity> {
        public Alastair(EntityRendererProvider.Context ctx) {
            super(ctx, AlastairAssets.RIG, 0.5f, false);
            addRenderLayer(new Glow<>(this, ApocalypseRenderers::alastairEyes, AlastairAssets.HIT_TICKS.getOrDefault("death", 50)));
        }

        @Override
        public void preRender(PoseStack pose, AlastairEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                show(baked, "scalpel", e.phase() >= 2);
                show(baked, "eyes", alastairEyes(e));
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    // --- Ruby ------------------------------------------------------------------------------------------------------------------

    /** Her black eyes show in the fight (the NPC keeps her own until the reveal). */
    public static boolean rubyEyes(LivingEntity e) {
        return e instanceof RubyEntity;
    }

    public static class Ruby<T extends LivingEntity & GeoEntity> extends Rig<T> {
        public Ruby(EntityRendererProvider.Context ctx) {
            super(ctx, RubyAssets.RIG, 0.45f, false);
            addRenderLayer(new Glow<>(this, ApocalypseRenderers::rubyEyes, RubyAssets.HIT_TICKS.getOrDefault("death", 50)));
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                boolean fight = !(e instanceof RubyNpcEntity);
                show(baked, "knife", fight);
                show(baked, "sheath_knife", !fight);
                show(baked, "eyes", rubyEyes(e));
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }
}
