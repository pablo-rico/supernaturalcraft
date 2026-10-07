package org.papiricoh.supernaturalcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.network.SelectSpellPayload;

/** Page turning: the cycle key, or sneak + mouse wheel while holding a grimoire. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class SNKeys {

    public static final KeyMapping CYCLE_SPELL = new KeyMapping("key.supernaturalcraft.cycle_spell",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.supernaturalcraft");

    /** Held to skip a camera cinematic. */
    public static final KeyMapping SKIP_CINEMATIC = new KeyMapping("key.supernaturalcraft.skip_cinematic",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, "key.categories.supernaturalcraft");

    /** Reload the weapon in hand (the Colt). */
    public static final KeyMapping RELOAD_WEAPON = new KeyMapping("key.supernaturalcraft.reload_weapon",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.supernaturalcraft");

    /** Turn the weapon in hand over to look at it (the Colt). */
    public static final KeyMapping INSPECT_WEAPON = new KeyMapping("key.supernaturalcraft.inspect_weapon",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, "key.categories.supernaturalcraft");

    private SNKeys() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (CYCLE_SPELL.consumeClick()) {
            if (mc.player != null && GrimoireItem.heldHand(mc.player) != null) {
                PacketDistributor.sendToServer(new SelectSpellPayload(1));
            }
        }
        while (RELOAD_WEAPON.consumeClick()) {
            org.papiricoh.supernaturalcraft.client.colt.ColtClient.key(org.papiricoh.supernaturalcraft.network.ColtInputPayload.RELOAD);
        }
        while (INSPECT_WEAPON.consumeClick()) {
            org.papiricoh.supernaturalcraft.client.colt.ColtClient.key(org.papiricoh.supernaturalcraft.network.ColtInputPayload.INSPECT);
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown()) return;
        if (GrimoireItem.heldHand(mc.player) == null || event.getScrollDeltaY() == 0) return;
        PacketDistributor.sendToServer(new SelectSpellPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }
}
