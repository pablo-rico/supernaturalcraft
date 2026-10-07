package org.papiricoh.supernaturalcraft.client.chuck.screen;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.author.Manuscript;
import org.papiricoh.supernaturalcraft.network.AuthorDialoguePayload;

/**
 * Opens, turns or closes the typewritten page of the dialogue with the Author; also the page in his typewriter
 * ({@code npc} -1, node {@code page}: the options are the bosses the reader has beaten, {@code rematch} whether he
 * expects them) and "The End".
 */
public final class AuthorScreens {

    private AuthorScreens() {
    }

    public static void open(AuthorDialoguePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.npc() == -1 && payload.node().equals("page")) {
            mc.setScreen(new TypewriterPageScreen(payload.options(), payload.rematch()));
            return;
        }
        if (payload.node().isEmpty()) {
            if (mc.screen instanceof AuthorDialogueScreen s && s.npc() == payload.npc()) s.closedByServer();
            return;
        }
        if (mc.screen instanceof AuthorDialogueScreen s && s.npc() == payload.npc()) s.turn(payload);
        else mc.setScreen(new AuthorDialogueScreen(payload));
    }

    public static void openManuscript(@Nullable Manuscript text) {
        Minecraft.getInstance().setScreen(new ManuscriptScreen(text));
    }
}
