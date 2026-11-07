package org.papiricoh.supernaturalcraft.apocalypse;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyData;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyRules;
import org.papiricoh.supernaturalcraft.apocalypse.ruby.Rubys;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Worlds from before v0.40, when the road ran Lilith → Lucifer → the Horsemen (once per hunter, at login):
 * <ul>
 *   <li>the world's seals: past Lilith they are all broken (risen), past Lucifer the Cage is shut again (echo); a hunter who has
 *   only beaten Azazel starts the count today;</li>
 *   <li>a hunter who beat the old Lucifer and has not fought Lucifer Uncaged gets the Cage Shackle the new rite wants (they
 *   would otherwise be stuck), and Ruby is behind them (they have the Colt already);</li>
 *   <li>a hunter past Lilith but not past Lucifer still owes Ruby her fight: she will come for them at night.</li>
 * </ul>
 * A hunter on the new road (nothing beaten yet, or migrated) is left alone ({@link RubyData#MIGRATED}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ApocalypseMigration {

    private ApocalypseMigration() {
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) migrate(p);
    }

    /** Looks at {@code p} once (public for tests and the command). */
    public static void migrate(ServerPlayer p) {
        MinecraftServer server = p.server;
        boolean azazel = AuthorWorld.done(p, "main/yellow_eyed"), lilith = AuthorWorld.done(p, "main/lucifer_rising"),
                lucifer = AuthorWorld.done(p, "main/devil_went_down"), uncaged = AuthorWorld.done(p, "main/back_in_the_box");
        // The world's state follows the furthest hunter, every login (it only ever moves forward).
        if (lucifer) SealWorld.cageClosed(server);
        else if (lilith) SealWorld.lilithFell(server);
        else if (azazel) SealWorld.begin(server);
        RubyData d = Rubys.get(p);
        if (d.has(RubyData.MIGRATED)) return;
        Rubys.update(p, x -> x.withFlag(RubyData.MIGRATED));
        if (lucifer) {
            Rubys.advance(p, RubyRules.Step.DONE);
            if (!uncaged && !Rubys.get(p).has(RubyData.SHACKLE_GIVEN) && !p.getInventory().contains(new ItemStack(AllItems.CAGE_SHACKLE.get()))) {
                Stakes.give(server, p.getUUID(), List.of(new ItemStack(AllItems.CAGE_SHACKLE.get())), false);
                Rubys.update(p, x -> x.withFlag(RubyData.SHACKLE_GIVEN));
                p.sendSystemMessage(Component.translatable("message.supernaturalcraft.apocalypse.shackle_given").withStyle(ChatFormatting.GOLD));
            }
        } else if (lilith) {
            Rubys.advance(p, RubyRules.Step.BETRAYAL_PENDING);
        }
    }
}
