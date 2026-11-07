package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

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
import org.papiricoh.supernaturalcraft.apocalypse.seal.RebelAngelEntity;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealAssets;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealReaperEntity;
import org.papiricoh.supernaturalcraft.apocalypse.seal.WitnessEntity;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers.Glow;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseRenderers.Rig;
import org.papiricoh.supernaturalcraft.client.godstiel.GodstielRenderers;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;

/**
 * The Seals slice's rigs on the client (v0.40), each of its contract ({@link SealAssets}, {@link UrielAssets}).
 * <ul>
 *   <li>Uriel: his wings of light (and all under {@code wings}) hidden in phase I, shown from phase II on (they open with
 *   {@code unfurl}); his eyes' shell lit from phase II and while he dies; the glowmask (eyes, wings' light, the blade) full
 *   bright; dying, the angel's light bursts out of him.</li>
 *   <li>A rebel angel: its vessel's texture ({@code rebel_angel_<n>}), its eyes lit, the shadow of its wings only as it dies;
 *   see-through (the shadow wings).</li>
 *   <li>A Witness: its look ({@code witness_<n>}), drawn see-through and flickering, its hollow eyes glowing with it.</li>
 *   <li>A reaper at a seal: its rig, its faint glow.</li>
 *   <li>Castiel at the seals: the Castiel NPC's renderer ({@link GodstielRenderers.Castiel}).</li>
 * </ul>
 * A variant is fixed by the entity's UUID ({@code floorMod(uuid.hashCode(), variants)}, the same as {@code SealCreature.variant()}),
 * so every client agrees. The renderers never move a bone; they only show or hide whole bones.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class SealRenderers {

    private static final int GRACE = 0xFFF4C8, BLUE_GRACE = 0xCFE6FF;

    private SealRenderers() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.URIEL.get(), Uriel::new);
        event.registerEntityRenderer(AllEntities.REBEL_ANGEL.get(), Rebel::new);
        event.registerEntityRenderer(AllEntities.WITNESS.get(), Witness::new);
        event.registerEntityRenderer(AllEntities.SEAL_REAPER.get(), Reaper::new);
        event.registerEntityRenderer(AllEntities.CASTIEL_ENVOY.get(), GodstielRenderers.Castiel::new);
    }

    /** The texture variant of {@code e} among {@code variants}. */
    public static int variant(LivingEntity e, int variants) {
        return Math.floorMod(e.getUUID().hashCode(), Math.max(1, variants));
    }

    static ResourceLocation variantTexture(LivingEntity e, String rig, int variants) {
        return SupernaturalCraft.asResource(SealAssets.texture(rig, variant(e, variants)));
    }

    // --- Uriel -----------------------------------------------------------------------------------------------------------------

    /** Previews: show (or fold away) every Uriel's wings whatever his phase (null: his own). */
    private static @Nullable Boolean forcedWings;

    public static void forceUrielWings(@Nullable Boolean shown) {
        forcedWings = shown;
    }

    /** His grace shows (the wings, the eyes) from phase II on and as he dies. */
    public static boolean urielGrace(UrielEntity e) {
        if (forcedWings != null) return forcedWings;
        return e.phase() >= 2 || e.state() == LuciferEntity.DYING;
    }

    public static class Uriel extends Rig<UrielEntity> {
        public Uriel(EntityRendererProvider.Context ctx) {
            super(ctx, UrielAssets.RIG, 0.6f, false);
            addRenderLayer(new Glow<>(this, e -> true, UrielAssets.HIT_TICKS.getOrDefault("death", 60), GRACE));
        }

        @Override
        public void preRender(PoseStack pose, UrielEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                boolean grace = urielGrace(e);
                ApocalypseRenderers.show(baked, "wings", grace);
                ApocalypseRenderers.show(baked, "eyes", grace);
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    // --- The rebels --------------------------------------------------------------------------------------------------------

    public static class Rebel extends Rig<RebelAngelEntity> {
        public Rebel(EntityRendererProvider.Context ctx) {
            super(ctx, SealAssets.REBEL, 0.5f, true, e -> variantTexture(e, SealAssets.REBEL, SealAssets.REBEL_VARIANTS));
            // Its light bursts out early in its death (a mob's death is short unless the slice holds it).
            addRenderLayer(new Glow<>(this, e -> true, 12, BLUE_GRACE));
        }

        @Override
        public void preRender(PoseStack pose, RebelAngelEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) ApocalypseRenderers.show(baked, "wings", e.isDeadOrDying());
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        /** The shadow of the wings is see-through. */
        @Override
        public RenderType getRenderType(RebelAngelEntity e, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    // --- The Witnesses -----------------------------------------------------------------------------------------------------

    /** How solid a Witness is now (0-1): a slow waver, and now and then it all but goes out for a few ticks. */
    public static float flicker(LivingEntity e, float partialTick) {
        float t = e.tickCount + partialTick + (e.getId() % 97) * 7.3f;
        float waver = 0.78f + 0.12f * Mth.sin(t * 0.17f) + 0.06f * Mth.sin(t * 0.61f + 1.3f);
        int cycle = (int) (t / 5) % 37;
        if (cycle == 0 || cycle == 2) waver *= 0.25f;
        else if (cycle == 1) waver *= 0.55f;
        if (e.isDeadOrDying()) waver *= Math.max(0, 1 - e.deathTime / 20f);
        return Mth.clamp(waver, 0, 1);
    }

    public static class Witness extends Rig<WitnessEntity> {
        public Witness(EntityRendererProvider.Context ctx) {
            super(ctx, SealAssets.WITNESS, 0.35f, false, e -> variantTexture(e, SealAssets.WITNESS, SealAssets.WITNESS_VARIANTS));
            addRenderLayer(new Glow<>(this, e -> true, -1) {
                @Override
                protected int colour(WitnessEntity e, float partialTick) {
                    return ApocalypseRenderers.argb(flicker(e, partialTick), 0xFFFFFF);
                }
            });
        }

        @Override
        public Color getRenderColor(WitnessEntity e, float partialTick, int packedLight) {
            Color c = super.getRenderColor(e, partialTick, packedLight);
            return Color.ofARGB(Math.round(c.getAlpha() * flicker(e, partialTick)), c.getRed(), c.getGreen(), c.getBlue());
        }

        @Override
        public RenderType getRenderType(WitnessEntity e, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    // --- The reapers -------------------------------------------------------------------------------------------------------

    public static class Reaper extends Rig<SealReaperEntity> {
        public Reaper(EntityRendererProvider.Context ctx) {
            super(ctx, SealAssets.REAPER, 0.45f, true);
            addRenderLayer(new Glow<>(this, e -> true, -1));
        }
    }
}
