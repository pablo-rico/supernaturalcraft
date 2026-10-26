package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;

import java.util.Locale;

/**
 * An officer speaking aloud ({@link ChainNoticePayload#SAY}), in the chat: a mark of its side, its name in the side's colour
 * (its rank on hover), its rank in grey, then the line in quotes, tinted after its voice: ember for the Pit, gold-white for
 * the Host, ash for the Fallen.
 */
public final class ChainSpeech {
    private ChainSpeech() {}

    public static void say(ChainNoticePayload p) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int name, line;
        String mark;
        switch (p.faction()) {
            case "angel" -> {
                name = 0xF2D257;
                line = 0xFFF4DC;
                mark = "✦ ";
            }
            case "fallen" -> {
                name = 0xB8B0A8;
                line = 0xD8D2CC;
                mark = "✧ ";
            }
            default -> {
                name = 0xE0503A;
                line = 0xF0C8B8;
                mark = "♆ ";
            }
        }
        Component rank = p.rank().isEmpty() ? Component.empty()
                : Component.translatable("chain.supernaturalcraft.rank." + p.rank().toLowerCase(Locale.ROOT));
        Component who = Component.literal(p.name()).withStyle(Style.EMPTY.withColor(name).withBold(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, rank)));
        Component msg = Component.literal(mark).withColor(name).append(who);
        if (!p.rank().isEmpty()) msg = msg.copy().append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY))
                .append(rank.copy().withStyle(ChatFormatting.GRAY));
        msg = msg.copy().append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("“").withColor(line))
                .append(Component.translatable(p.key(), p.args().toArray()).withStyle(Style.EMPTY.withColor(line).withItalic(true)))
                .append(Component.literal("”").withColor(line));
        player.displayClientMessage(msg, false);
    }
}
