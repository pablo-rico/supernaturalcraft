package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.network.AllegianceChoicePayload;
import org.papiricoh.supernaturalcraft.network.AllegianceDialoguePayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared dialogue ({@link AllegianceDialoguePayload} out, {@link AllegianceChoicePayload} back): one question per player
 * at a time, remembered here so an answer is only taken for the question really asked, and silence past the timeout counts
 * as the last option. Each dialogue id has a {@link Handler} ({@code "messenger"}, {@code "lucifer_offer"}).
 */
public final class AllegianceDialogue {

    /** Extra ticks the server waits past the client's timeout before it answers for the player. */
    public static final int GRACE_TICKS = 40;

    public interface Handler {
        /** {@code speaker} may be null (gone); {@code option} is one of the options asked. */
        void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option);
    }

    /** A question in the air. */
    public record Pending(int entity, String dialogue, String node, List<String> options, long askedAt, int timeout) {
        String fallback() {
            return options.getLast();
        }
    }

    private static final Map<String, Handler> HANDLERS = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private AllegianceDialogue() {
    }

    public static void register(String dialogue, Handler handler) {
        HANDLERS.put(dialogue, handler);
    }

    /** {@code speaker} asks {@code player}: the line {@code dialogue.supernaturalcraft.<dialogue>.<node>} and its answers. */
    public static void ask(ServerPlayer player, Entity speaker, String dialogue, String node, List<String> options, int timeout) {
        PENDING.put(player.getUUID(), new Pending(speaker.getId(), dialogue, node, List.copyOf(options), player.serverLevel().getGameTime(), timeout));
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new AllegianceDialoguePayload(speaker.getId(), dialogue, node, options, timeout));
        }
    }

    public static @Nullable Pending pending(ServerPlayer player) {
        return PENDING.get(player.getUUID());
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
    }

    /** Takes back the question {@code speaker} asked {@code player}, if any, and closes their panel (an empty node). */
    public static void close(ServerPlayer player, Entity speaker) {
        Pending q = PENDING.get(player.getUUID());
        if (q == null || q.entity() != speaker.getId()) return;
        PENDING.remove(player.getUUID());
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new AllegianceDialoguePayload(speaker.getId(), q.dialogue(), "", List.of(), 0));
        }
    }

    /** From the network. */
    public static void answer(ServerPlayer player, AllegianceChoicePayload payload) {
        answer(player, payload.entity(), payload.dialogue(), payload.node(), payload.option());
    }

    /** An answer: taken only for the question asked, with one of its options. @return whether it counted */
    public static boolean answer(ServerPlayer player, int entity, String dialogue, String node, String option) {
        Pending q = PENDING.get(player.getUUID());
        if (q == null || q.entity() != entity || !q.dialogue().equals(dialogue) || !q.node().equals(node) || !q.options().contains(option)) {
            return false;
        }
        PENDING.remove(player.getUUID());
        route(player, q, option);
        return true;
    }

    private static void route(ServerPlayer player, Pending q, String option) {
        Handler h = HANDLERS.get(q.dialogue());
        if (h != null) h.answer(player, player.serverLevel().getEntity(q.entity()), q.node(), option);
    }

    /** Silence past the timeout is the last option. */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        for (var e : List.copyOf(PENDING.entrySet())) {
            Pending q = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                // FakePlayers (tests) are not in the list: they answer by hand.
                continue;
            }
            if (q.timeout() > 0 && p.serverLevel().getGameTime() - q.askedAt() > q.timeout() + GRACE_TICKS) {
                PENDING.remove(e.getKey());
                route(p, q, q.fallback());
            }
        }
    }
}
