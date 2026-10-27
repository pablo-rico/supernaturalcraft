package org.papiricoh.supernaturalcraft.compat.nationsandwar.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.compat.nationsandwar.PowerNations;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.nationsandwar.api.client.ManagedNationScreens;
import org.papiricoh.nationsandwar.client.ClientClaims;
import org.papiricoh.nationsandwar.view.Briefs;

/**
 * The cards of the Pit, the Host and SucroCorp in Nations &amp; War's GUI: who they are, and a button that opens the Hunter's
 * Book at their chain of command when the player carries the book. Also the one place the book asks Nations &amp; War's client
 * data ({@link #myNationName}); call it only when {@code NWPresence.LOADED}.
 */
public final class NWClientCompat {
    private NWClientCompat() {
    }

    /** The name of the nation the local player is a citizen of, or null. */
    public static @org.jetbrains.annotations.Nullable String myNationName() {
        java.util.UUID id = ClientClaims.myNation();
        if (id == null) return null;
        Briefs.NationBrief n = ClientClaims.nation(id);
        return n == null ? null : n.name();
    }

    public static void register() {
        for (ChainFaction f : ChainFaction.FIXED) {
            String key = PowerNations.key(f);
            ManagedNationScreens.register(key, new ManagedNationScreens.Handler() {
                @Override
                public Component description(Briefs.NationBrief nation) {
                    return Component.translatable("supernaturalcraft.nw.card." + f.id());
                }

                @Override
                public Component button(Briefs.NationBrief nation) {
                    return Component.translatable("supernaturalcraft.nw.open_book");
                }

                @Override
                public boolean canOpen(Player player, Briefs.NationBrief nation) {
                    return player.getInventory().contains(AllItems.GRIMOIRE.get().getDefaultInstance());
                }

                @Override
                public Component cannotOpen(Briefs.NationBrief nation) {
                    return Component.translatable("supernaturalcraft.nw.need_book");
                }

                @Override
                public void open(Player player, Briefs.NationBrief nation) {
                    Minecraft.getInstance().setScreen(HunterBookScreen.atChain(f));
                }
            });
        }
    }
}
