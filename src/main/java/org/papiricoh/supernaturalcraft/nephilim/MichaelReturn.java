package org.papiricoh.supernaturalcraft.nephilim;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.Collection;

/**
 * v0.41: season 14's opening. AU Michael's fight calls {@link #fled} when he breaks off and escapes through the rift into this
 * world: the hunters move on to {@link NephilimRules.Step#MICHAEL_FLED} (Jack has spent his grace driving him back), and
 * Michael's offer comes at the dawn after next ({@code NephilimEvents}).
 */
public final class MichaelReturn {

    private MichaelReturn() {
    }

    /** AU Michael fled from {@code hunters} (everyone in his fight, present or fallen). */
    public static void fled(Collection<ServerPlayer> hunters) {
        for (ServerPlayer p : hunters) {
            Nephilims.advance(p, NephilimRules.Step.MICHAEL_FLED);
            long day = p.getServer() == null ? 0 : WorldClock.day(p.getServer()) / 24000L;
            Nephilims.update(p, n -> n.withVisit(day));
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.grace_spent").withStyle(ChatFormatting.YELLOW));
        }
    }
}
