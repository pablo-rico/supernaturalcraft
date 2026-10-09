package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.network.ResearchActionPayload;

/** Server side of the research desk (v0.17): validates and applies a {@link ResearchActionPayload}, then resends the board. */
public final class ResearchServerHandlers {

    private ResearchServerHandlers() {
    }

    public static void action(ResearchActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) action(player, payload);
    }

    /** Also called directly by tests. */
    public static void action(ServerPlayer player, ResearchActionPayload payload) {
        if (!(player.containerMenu instanceof ResearchMenu menu) || menu.containerId != payload.container() || !menu.stillValid(player)) {
            return;
        }
        String topic = payload.topic();
        if (topic.length() > 256) return;
        switch (payload.action()) {
            case ResearchActionPayload.START -> {
                String why = ResearchService.start(player, topic);
                if (why != null) player.displayClientMessage(Component.translatable(why).withStyle(ChatFormatting.RED), true);
            }
            case ResearchActionPayload.CANCEL -> ResearchService.cancel(player, topic);
            default -> {
                return;
            }
        }
        ResearchService.sendBoard(player, menu.containerId);
    }
}
