package org.papiricoh.supernaturalcraft.client.allegiance;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceLayer;
import org.papiricoh.supernaturalcraft.client.allegiance.render.MessengerRenderer;
import org.papiricoh.supernaturalcraft.client.allegiance.render.RivalHunterRenderer;
import org.papiricoh.supernaturalcraft.client.michael.render.HostAngelRenderer;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Client registration and per-tick work for the allegiance (v0.13): the messenger's, rival hunters' and Host allies'
 * renderers, the layer on both player models, the wheel and cast keys, the HUD; the smoke a demon pours out, hiding a
 * player who is smoke or riding a mob, and everything laid over the whole screen.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AllegianceClientEvents {

    /** Held: the power wheel. */
    public static final KeyMapping WHEEL = new KeyMapping("key.supernaturalcraft.power_wheel", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.supernaturalcraft");
    /** Casts the selected power. */
    public static final KeyMapping CAST = new KeyMapping("key.supernaturalcraft.cast_power", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.supernaturalcraft");

    private AllegianceClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AllEntities.MESSENGER.get(), MessengerRenderer::new);
        event.registerEntityRenderer(AllEntities.RIVAL_HUNTER.get(), RivalHunterRenderer::new);
        // A soldier of the Host who answers a General is drawn exactly as Michael's.
        event.registerEntityRenderer(AllEntities.HOST_ALLY.get(), HostAngelRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            if (event.getSkin(model) instanceof PlayerRenderer renderer) renderer.addLayer(new AllegianceLayer(renderer));
        }
        AllegianceLayer.forget();
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(WHEEL);
        event.register(CAST);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, SupernaturalCraft.asResource("allegiance"), new AllegianceHud());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        AllegianceHud.tick();
        AllegianceFx.tick();
        TitleCard.tick();
        if (mc.player == null || mc.level == null) return;
        Allegiance mine = Allegiances.get(mc.player);
        boolean any = mine.committed() || mine.rank() > 0;
        if (mc.screen == null && any && WHEEL.isDown()) mc.setScreen(new PowerWheelScreen(WHEEL));
        while (CAST.consumeClick()) {
            if (mine.committed()) ClientPowers.castSelected();
        }
        while (WHEEL.consumeClick()) {
            // Opening is by holding (above); the clicks are only drained.
        }
        if (mc.level.getGameTime() % 2 == 0) {
            for (Player p : mc.level.players()) smoke(mc, p);
        }
    }

    /** A demon pouring out as smoke, or waiting in it while riding a mob: thick black smoke where they are. */
    private static void smoke(Minecraft mc, Player p) {
        int flags = ClientAllegiance.flags(p);
        if ((flags & (Allegiances.SMOKE | Allegiances.POSSESSING)) == 0) return;
        RandomSource r = p.getRandom();
        int n = (flags & Allegiances.SMOKE) != 0 ? 6 : 2;
        for (int i = 0; i < n; i++) {
            double x = p.getX() + r.nextGaussian() * 0.3, y = p.getY() + r.nextDouble() * 1.8, z = p.getZ() + r.nextGaussian() * 0.3;
            mc.level.addParticle(i % 2 == 0 ? AllParticles.DEMON_SMOKE.get() : ParticleTypes.LARGE_SMOKE, x, y, z,
                    r.nextGaussian() * 0.02, 0.04, r.nextGaussian() * 0.02);
        }
    }

    /** A player who is smoke, or away inside a mob, has no body to show. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer p)) return;
        if (ClientAllegiance.flag(p, Allegiances.SMOKE) || ClientAllegiance.flag(p, Allegiances.POSSESSING)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        TitleCard.renderOverlay(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPowers.reset();
        ClientAllegiance.clear();
        AllegianceHud.clear();
        AllegianceFx.clear();
        TitleCard.clear();
    }
}
