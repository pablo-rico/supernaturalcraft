package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainTalkPayload;

@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChainClient {
    private ChainClient() {}
    public static void sync(ChainSyncPayload p) { ClientChain.sync(p); }
    public static void notice(ChainNoticePayload p) {
        if (p.kind() == ChainNoticePayload.TITLE) { ClientChain.lastTitle = p; OfficerCard.show(p); }
        else if (p.kind() == ChainNoticePayload.SAY) ChainSpeech.say(p);
        else if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(p.key(), args(p)), true);
    }
    /** Notice arguments that are translation keys of the mod (a rank, a side) are translated here. */
    private static Object[] args(ChainNoticePayload p) {
        return p.args().stream().map(a -> a.startsWith("chain.supernaturalcraft.") || a.startsWith("screen.supernaturalcraft.")
                ? (Object) Component.translatable(a) : a).toArray();
    }
    /** An officer speaks to the local hunter face to face, or answers a message (its own screen). */
    public static void talk(ChainTalkPayload p) { ClientChain.talk = p; ChainTalkScreen.open(p); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        OfficerCard.tick();
        OfficerBar.tick(mc);
        DungeonTitle.tick(mc);
        OfficerAura.tick(mc);
        OfficerPuppets.tick();
        DuelHud.tick(mc);
    }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        // The officers' bars sit right after the boss bars (and under them on screen); the title cards over everything.
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, OfficerBar.LAYER, OfficerBar.layer());
        // A duel's banner under the officers' bars (it hides itself in camera sequences).
        event.registerAbove(OfficerBar.LAYER, DuelHud.LAYER, DuelHud.layer());
        event.registerAboveAll(DungeonTitle.LAYER, DungeonTitle.layer());
        event.registerAboveAll(OfficerCard.LAYER, OfficerCard.layer());
        CameraDirector.showDuringSequences(OfficerCard.LAYER);
        CameraDirector.showDuringSequences(DungeonTitle.LAYER);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientChain.clear(); OfficerCard.clear(); OfficerBar.clear(); DungeonTitle.clear(); OfficerPuppets.clear(); DuelHud.clear(); }
}
