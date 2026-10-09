package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.legacy.render.HenryRenderer;
import org.papiricoh.supernaturalcraft.client.legacy.render.ShapeshifterRenderer;
import org.papiricoh.supernaturalcraft.client.legacy.render.VampireRenderer;
import org.papiricoh.supernaturalcraft.client.legacy.render.WerewolfRenderer;
import org.papiricoh.supernaturalcraft.datagen.legacy.LegacyAssetData;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMenus;

/**
 * Client registration for the Men of Letters (v0.17): renderers for Henry and the three monsters, the research desk's screen,
 * the field notes' and cursed artifacts' icons (item properties and the rarity tint), the rank's title card.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class LegacyClientEvents {

    /** Rarity tints of a cursed artifact's halo: common, uncommon, rare, legendary. */
    static final int[] RARITY = {0xFFB8C4B0, 0xFF6FA8FF, 0xFFB070FF, 0xFFFFB040};

    private LegacyClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.HENRY.get(), HenryRenderer::new);
        event.registerEntityRenderer(AllEntities.VAMPIRE.get(), VampireRenderer::new);
        event.registerEntityRenderer(AllEntities.WEREWOLF.get(), WerewolfRenderer::new);
        event.registerEntityRenderer(AllEntities.SHAPESHIFTER.get(), ShapeshifterRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AllMenus.RESEARCH.get(), ResearchScreen::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(AllItems.FIELD_NOTES.get(), LegacyAssetData.NOTE_TOPIC, (stack, level, entity, seed) -> noteTopic(stack));
            ItemProperties.register(AllItems.CURSED_ARTIFACT.get(), LegacyAssetData.ARTIFACT_FORM, (stack, level, entity, seed) -> {
                ArtifactData a = stack.get(AllDataComponents.ARTIFACT.get());
                int i = a == null ? 0 : Math.max(0, LegacyAssets.ARTIFACT_FORMS.indexOf(a.form()));
                return i / 8f;
            });
        });
    }

    /** 0 creature (and bosses), .25 arcane, .5 relic, .75 place. */
    static float noteTopic(ItemStack stack) {
        String family = FieldNotesItem.family(FieldNotesItem.topic(stack));
        int i = LegacyAssets.NOTE_TOPICS.indexOf(family);
        return Math.max(0, i) / 4f;
    }

    @SubscribeEvent
    public static void itemColours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            if (tint != 0) return -1;
            ArtifactData a = stack.get(AllDataComponents.ARTIFACT.get());
            return RARITY[a == null ? 0 : Math.max(0, Math.min(RARITY.length - 1, a.rarity()))];
        }, AllItems.CURSED_ARTIFACT.get());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        LegacyOverlay.tick();
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        LegacyOverlay.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLegacy.reset();
        LegacyOverlay.clear();
    }
}
