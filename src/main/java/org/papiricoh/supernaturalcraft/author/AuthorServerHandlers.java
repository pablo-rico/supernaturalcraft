package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckSummoning;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.network.AuthorChoicePayload;
import org.papiricoh.supernaturalcraft.network.AuthorDialoguePayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;

/**
 * Server side of the cabin's dialogue: opening it when a hunter uses the Author, and every answer after that. Answers
 * are checked against the conversation ({@link AuthorDialogue}): the hunter must be near him and the option must be one
 * the current node offers. "Begin" starts the test ({@link ChuckSummoning#summon}) in the middle of the cabin.
 */
public final class AuthorServerHandlers {

    private AuthorServerHandlers() {
    }

    /** What the Author knows of {@code player}. */
    public static AuthorDialogue.Context context(ServerPlayer player) {
        AuthorSavedData data = AuthorSavedData.get(player.serverLevel());
        CrossroadsDeal deal = Debts.get(player);
        ResourceLocation colt = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(AllItems.THE_COLT.get());
        return new AuthorDialogue.Context(data.met(player.getUUID()), data.rewarded(player.getUUID()),
                deal.state() != CrossroadsDeal.State.NONE, HunterLogs.get(player).has(colt));
    }

    /** {@code player} used the Author: he gets up and opens the conversation. */
    public static void open(AuthorNpcEntity npc, ServerPlayer player) {
        if (npc.distanceTo(player) > AuthorNpcEntity.REACH) return;
        AuthorDialogue.Context ctx = context(player);
        if (AuthorSavedData.get(player.serverLevel()).meet(player.getUUID())) ChorusRewards.award(player, "main/the_author");
        String node = AuthorDialogue.start(ctx);
        npc.talkTo(player, node);
        send(player, npc, node, ctx);
    }

    /** A hunter chose {@code payload.option()} in the dialogue with the Author. */
    public static void choice(AuthorChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.serverLevel().getEntity(payload.npc()) instanceof AuthorNpcEntity npc)) {
            close(player, payload.npc());
            return;
        }
        choose(player, npc, payload.option());
    }

    /**
     * Answers {@code option} to the node {@code player} is at.
     *
     * @return the node he answers with ({@code ""} if the page closed, {@link AuthorDialogue#BEGIN} if the test began),
     * or null if the answer was refused
     */
    public static @Nullable String choose(ServerPlayer player, AuthorNpcEntity npc, String option) {
        String node = npc.node(player.getUUID());
        if (node == null || npc.distanceTo(player) > AuthorNpcEntity.REACH || !npc.isAlive()) {
            close(player, npc.getId());
            return null;
        }
        AuthorDialogue.Context ctx = context(player);
        String next = AuthorDialogue.next(node, option, ctx);
        if (next == null) return null;
        if (next.isEmpty()) {
            npc.forget(player.getUUID());
            close(player, npc.getId());
            return next;
        }
        if (next.equals(AuthorDialogue.BEGIN)) {
            close(player, npc.getId());
            return begin(player, npc, ctx.victor()) ? next : null;
        }
        npc.talkTo(player, next);
        send(player, npc, next, ctx);
        return next;
    }

    /** "Let's begin": the cabin is unwritten around him, and the test starts. */
    static boolean begin(ServerPlayer player, AuthorNpcEntity npc, boolean rematch) {
        ServerLevel level = player.serverLevel();
        AuthorSite.Site site = AuthorSite.of(level);
        BlockPos centre = AuthorWorld.centre(site);
        // Summoned from somewhere else (a moved NPC, a command), he still fights where he stands.
        if (npc.distanceToSqr(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5) > 24 * 24) centre = npc.blockPosition();
        if (!ChuckSummoning.summon(level, centre, player, rematch)) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.author.busy").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        npc.discard();
        return true;
    }

    private static void send(ServerPlayer player, AuthorNpcEntity npc, String node, AuthorDialogue.Context ctx) {
        PacketDistributor.sendToPlayer(player, new AuthorDialoguePayload(npc.getId(), node, AuthorDialogue.options(node, ctx), ctx.victor()));
    }

    private static void close(ServerPlayer player, int npc) {
        PacketDistributor.sendToPlayer(player, new AuthorDialoguePayload(npc, "", List.of(), false));
    }
}
