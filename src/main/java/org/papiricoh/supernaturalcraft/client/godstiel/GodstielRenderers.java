package org.papiricoh.supernaturalcraft.client.godstiel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyAssets;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielBalance;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielEntity;
import org.papiricoh.supernaturalcraft.pact.CrowleyKingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Season 6's rigs on the client (v0.39), each of the contract ({@link GodstielAssets}).
 * <ul>
 *   <li>Castiel (the story NPC): the {@code castiel} rig in {@link GodstielAssets#TEXTURE_NPC}, every toggled bone hidden.</li>
 *   <li>Godstiel: the same rig in {@link GodstielAssets#TEXTURE} ({@link GodstielAssets#TEXTURE_SOAKED} in the last phase), the
 *   toggled bones shown by phase ({@link GodstielBalance}), his glow (eyes, cracks, halo) drawn full bright; not drawn at all
 *   while {@code gone()}.</li>
 *   <li>Crowley, King of Hell: Crowley's rig in {@link GodstielAssets#CROWLEY_KING_TEXTURE}, his red eyes' shell hidden.</li>
 * </ul>
 * Nothing is drawn until the rig's files have loaded ({@link GeoGuard}).
 */
public final class GodstielRenderers {

    static final ResourceLocation GEO = SupernaturalCraft.asResource(GodstielAssets.GEO),
            ANIM = SupernaturalCraft.asResource(GodstielAssets.ANIM),
            NPC = SupernaturalCraft.asResource(GodstielAssets.TEXTURE_NPC),
            GOD = SupernaturalCraft.asResource(GodstielAssets.TEXTURE),
            SOAKED = SupernaturalCraft.asResource(GodstielAssets.TEXTURE_SOAKED),
            GLOW = SupernaturalCraft.asResource(GodstielAssets.GLOW),
            GLOW_SOAKED = SupernaturalCraft.asResource(GodstielAssets.GLOW_SOAKED),
            KING = SupernaturalCraft.asResource(GodstielAssets.CROWLEY_KING_TEXTURE);

    private GodstielRenderers() {
    }

    /** Shows or hides {@code name} and everything under it. */
    static void show(BakedGeoModel baked, String name, boolean on) {
        baked.getBone(name).ifPresent(b -> {
            b.setHidden(!on);
            b.setChildrenHidden(!on);
        });
        // The cracks and veins on the face and hands live under those bones: they go with their key.
        for (String part : GodstielAssets.TOGGLED_PARTS.getOrDefault(name, java.util.List.of())) {
            baked.getBone(part).ifPresent(b -> {
                b.setHidden(!on);
                b.setChildrenHidden(!on);
            });
        }
    }

    /** The toggled bones for a look of {@code phase} (0: the angel as he was, nothing showing). */
    static void dress(BakedGeoModel baked, int phase) {
        boolean god = phase >= 1;
        show(baked, "halo", god && GodstielBalance.halo(phase));
        show(baked, "wings", god && GodstielBalance.halo(phase));
        int cracks = god ? GodstielBalance.cracks(phase) : 0;
        show(baked, "cracks_1", cracks >= 1);
        show(baked, "cracks_2", cracks >= 2);
        show(baked, "cracks_3", cracks >= 3);
        show(baked, "veins", god && GodstielBalance.veins(phase));
        show(baked, "chest_mouth", god && GodstielBalance.chestMouth(phase));
        show(baked, "face_mouth", god && GodstielBalance.faceMouth(phase));
        show(baked, "tentacle_left", god && GodstielBalance.tentacles(phase));
        show(baked, "tentacle_right", god && GodstielBalance.tentacles(phase));
    }

    /** Castiel, the angel who comes at dawn. */
    public static class Castiel<T extends net.minecraft.world.entity.LivingEntity & software.bernie.geckolib.animatable.GeoEntity> extends GeoEntityRenderer<T> {
        /** His coat tails are opaque, but the rig shares the boss's translucent wings (hidden here). */
        public Castiel(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(GodstielAssets.RIG)) {
                @Override
                public ResourceLocation getTextureResource(T e) {
                    return NPC;
                }
            });
            shadowRadius = 0.5f;
        }

        @Override
        public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(GEO, ANIM)) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) dress(baked, 0);
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    /** Godstiel, the new God, coming apart phase by phase. */
    public static class Godstiel extends GeoEntityRenderer<GodstielEntity> {
        /** Previews: dress every Godstiel as this phase whatever he is in (0: his own). */
        private static int forcedLook;

        public Godstiel(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(GodstielAssets.RIG)) {
                @Override
                public ResourceLocation getTextureResource(GodstielEntity e) {
                    return look(e) >= GodstielBalance.RESERVOIR_PHASE || e.condemned() ? SOAKED : GOD;
                }
            });
            shadowRadius = 0.5f;
            addRenderLayer(new Glow(this));
        }

        public static void forceLook(int phase) {
            forcedLook = phase;
        }

        static int look(GodstielEntity e) {
            return forcedLook > 0 ? forcedLook : Math.max(1, e.lookPhase());
        }

        @Override
        public void render(GodstielEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (e.gone() || !GeoGuard.ready(GEO, ANIM)) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, GodstielEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                // Forgiven, the Leviathans pour out of him and he is the angel again.
                dress(baked, e.forgiven() ? 0 : look(e));
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        protected float getDeathMaxRotation(GodstielEntity e) {
            return 0f;
        }

        /** The halo and the shadow wings are half see-through. */
        @Override
        public RenderType getRenderType(GodstielEntity e, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    /** His glow: the eyes, the cracks' light and the halo, full bright (but not once forgiven). */
    static final class Glow extends GeoRenderLayer<GodstielEntity> {
        Glow(GeoRenderer<GodstielEntity> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, GodstielEntity e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            if (e.forgiven()) return;
            ResourceLocation glow = Godstiel.look(e) >= GodstielBalance.RESERVOIR_PHASE || e.condemned() ? GLOW_SOAKED : GLOW;
            if (!MichaelGui.has(glow)) return;
            RenderType lit = RenderType.eyes(glow);
            float pulse = e.struggling() ? 0.55f + 0.45f * (float) Math.abs(Math.sin((e.tickCount + partialTick) * 0.7f)) : 1f;
            int a = Math.round(255 * pulse);
            getRenderer().reRender(model, pose, buffers, e, lit, buffers.getBuffer(lit), partialTick, 0xF00000, overlay, a << 24 | 0xFFFFFF);
        }
    }

    /** Crowley as King of Hell: his rig, his clips, the King's skin. */
    public static class CrowleyKing extends GeoEntityRenderer<CrowleyKingEntity> {
        static final ResourceLocation CR_GEO = SupernaturalCraft.asResource(CrowleyAssets.GEO), CR_ANIM = SupernaturalCraft.asResource(CrowleyAssets.ANIM);

        public CrowleyKing(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(CrowleyAssets.RIG)) {
                @Override
                public ResourceLocation getTextureResource(CrowleyKingEntity e) {
                    return MichaelGui.has(KING) ? KING : SupernaturalCraft.asResource(CrowleyAssets.TEXTURE);
                }
            });
            shadowRadius = 0.5f;
        }

        @Override
        public void render(CrowleyKingEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(CR_GEO, CR_ANIM)) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, CrowleyKingEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) baked.getBone("eyes").ifPresent(b -> b.setHidden(true));
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }
}
