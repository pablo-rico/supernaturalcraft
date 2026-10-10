package org.papiricoh.supernaturalcraft.client.heaven;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.heaven.render.AshRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.ChairRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.HeavenAngelRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.MemoRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.MemoryFigureRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.NaomiRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.TrainingCopyRenderer;
import org.papiricoh.supernaturalcraft.client.heaven.render.ZachariahRenderer;
import org.papiricoh.supernaturalcraft.client.render.GeoWeaponRenderer;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import software.bernie.geckolib.animatable.GeoAnimatable;

/**
 * Client registration for v0.18: the renderers of Naomi, Zachariah, their angels, the chair, the training copies, Ash, the
 * memory figures and the memos; Heaven's sky ({@link HeavenEffects}); Naomi's Drill and Zachariah's Blade in hand (GeckoLib
 * weapons, drawn only once their models are there); and Heaven's HUD layer ({@link HeavenOverlay}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class HeavenClientEvents {

    private HeavenClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.NAOMI.get(), NaomiRenderer::new);
        event.registerEntityRenderer(AllEntities.HEAVEN_GUARD.get(), HeavenAngelRenderer::guard);
        event.registerEntityRenderer(AllEntities.TRAINING_COPY.get(), TrainingCopyRenderer::new);
        event.registerEntityRenderer(AllEntities.REPROGRAMMING_CHAIR.get(), ChairRenderer::new);
        event.registerEntityRenderer(AllEntities.ZACHARIAH.get(), ZachariahRenderer::new);
        event.registerEntityRenderer(AllEntities.CLERK_ANGEL.get(), HeavenAngelRenderer::clerk);
        event.registerEntityRenderer(AllEntities.MEMO_PROJECTILE.get(), MemoRenderer::new);
        event.registerEntityRenderer(AllEntities.MEMORY_FIGURE.get(), MemoryFigureRenderer::new);
        event.registerEntityRenderer(AllEntities.ASH.get(), AshRenderer::new);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(HeavenDimension.EFFECTS, new HeavenEffects());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SupernaturalCraft.asResource("heaven"), new HeavenOverlay());
        // Title cards and washes must be seen through a camera shot (the layer hides its HUD itself).
        org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.showDuringSequences(SupernaturalCraft.asResource("heaven"));
    }

    /** The two GeckoLib weapons of v0.18; an item that is not (yet) a GeckoLib item keeps its flat model. */
    @SubscribeEvent
    public static void registerItems(RegisterClientExtensionsEvent event) {
        geoWeapon(event, AllItems.NAOMIS_DRILL.get());
        geoWeapon(event, AllItems.ZACHARIAHS_BLADE.get());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void geoWeapon(RegisterClientExtensionsEvent event, Item item) {
        if (!(item instanceof GeoAnimatable)) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation geo = id.withPath("geo/item/" + id.getPath() + ".geo.json"),
                anim = id.withPath("animations/item/" + id.getPath() + ".animation.json");
        event.registerItem(new IClientItemExtensions() {
            private GuardedWeapon renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new GuardedWeapon((Item & GeoAnimatable) item, geo, anim);
                return renderer;
            }
        }, item);
    }

    /** A GeckoLib weapon that draws nothing until its model and clips are there (the art may land later). */
    private static final class GuardedWeapon<T extends Item & GeoAnimatable> extends GeoWeaponRenderer<T> {
        private final ResourceLocation geo, anim;

        GuardedWeapon(T item, ResourceLocation geo, ResourceLocation anim) {
            super(item);
            this.geo = geo;
            this.anim = anim;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light,
                                 int overlay) {
            // An item without clips needs no animation file.
            if (!GeoGuard.ready(geo, GeoGuard.exists(anim) ? anim : null)) return;
            super.renderByItem(stack, context, pose, buffers, light, overlay);
        }
    }
}
