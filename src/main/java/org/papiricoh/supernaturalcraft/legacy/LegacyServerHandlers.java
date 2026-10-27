package org.papiricoh.supernaturalcraft.legacy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.network.LegacyChoicePayload;
import org.papiricoh.supernaturalcraft.network.LegacyFxPayload;

import java.util.List;
import org.papiricoh.supernaturalcraft.network.Send;

/**
 * Server side of Henry's dialogue (v0.17): opening it ({@link #open}) and every answer after that ({@link #choice}). An answer
 * counts only from a hunter near him, at a stage that offers it ({@link HenryDialogue#next}); accepting makes them a member
 * ({@link LegacyOrder#join}), declining puts his next call off ({@link LegacySchedule#declined}), asking for a case hands one
 * over ({@link CaseOffice#issue}).
 */
public final class LegacyServerHandlers {

    private LegacyServerHandlers() {
    }

    /** What Henry knows of {@code player}. */
    public static HenryDialogue.Context context(ServerPlayer player) {
        Legacy l = Legacies.get(player);
        List<CaseFile> cases = l.cases();
        boolean open = cases.stream().anyMatch(c -> !c.closed());
        int last = cases.isEmpty() ? -1 : cases.get(cases.size() - 1).state();
        return new HenryDialogue.Context(l.member(), l.henry() == Legacy.HENRY_DECLINED, open, last);
    }

    /** {@code player} talks to Henry (used him, or he walked up to them). */
    public static void open(HenryEntity henry, ServerPlayer player) {
        if (henry.distanceTo(player) > HenryEntity.REACH) return;
        spareKey(player);
        HenryDialogue.Stage stage = HenryDialogue.start(context(player));
        henry.talkTo(player, stage);
        send(player, henry, stage.ordinal());
    }

    /**
     * A member who has lost the Bunker Key (v0.19) before ever opening the door with it gets a spare from Henry, so the door
     * never locks them out for good. Once the door knows them ({@link Legacy#doorKnown}) the key has done its work: no more spares.
     */
    public static boolean spareKey(ServerPlayer player) {
        if (!Legacies.member(player) || Legacies.get(player).doorKnown()) return false;
        var key = org.papiricoh.supernaturalcraft.registry.AllItems.BUNKER_KEY.get();
        if (player.getInventory().contains(new net.minecraft.world.item.ItemStack(key)) || player.getEnderChestInventory().hasAnyOf(java.util.Set.of(key))) {
            return false;
        }
        net.minecraft.world.item.ItemStack spare = new net.minecraft.world.item.ItemStack(key);
        if (!player.getInventory().add(spare)) player.drop(spare, false);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.supernaturalcraft.legacy.spare_key")
                .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        return true;
    }

    /** A hunter answered in Henry's dialogue. */
    public static void choice(LegacyChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.serverLevel().getEntity(payload.entity()) instanceof HenryEntity henry)) {
            send(player, null, HenryDialogue.CLOSED);
            return;
        }
        answer(player, henry, payload.choice());
    }

    /**
     * Answers {@code choice} to the stage {@code player} is at with {@code henry}.
     *
     * @return the stage he answers with, {@code null} if the talk ended, or the stage unchanged if the answer was refused
     */
    public static @Nullable HenryDialogue.Stage answer(ServerPlayer player, HenryEntity henry, byte choice) {
        HenryDialogue.Stage stage = henry.stage(player.getUUID());
        if (stage == null || !henry.isAlive() || henry.distanceTo(player) > HenryEntity.REACH * 1.5) {
            close(player, henry);
            return null;
        }
        HenryDialogue.Stage next = HenryDialogue.next(stage, choice);
        if (next == stage) return stage;
        if (next == null) {
            close(player, henry);
            if (henry.calling()) henry.leave();
            return null;
        }
        switch (next) {
            case WELCOME -> LegacyOrder.join(player);
            case FAREWELL -> Legacies.update(player, l -> LegacySchedule.declined(l, org.papiricoh.supernaturalcraft.util.WorldClock.day(player.server)));
            case CASE_GIVEN -> {
                if (!Legacies.member(player) || CaseOffice.issue(player, henry.blockPosition()) == null) {
                    boolean waiting = Legacies.member(player) && CaseOffice.waiting(player);
                    player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.case." + (waiting ? "cooldown" : "none"))
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                    close(player, henry);
                    return null;
                }
            }
            default -> {
            }
        }
        henry.talkTo(player, next);
        send(player, henry, next.ordinal());
        return next;
    }

    /** Closes Henry's screen for {@code player} and ends their talk. */
    public static void close(ServerPlayer player, @Nullable HenryEntity henry) {
        if (henry != null) henry.forget(player.getUUID());
        send(player, henry, HenryDialogue.CLOSED);
    }

    private static void send(ServerPlayer player, @Nullable HenryEntity henry, int value) {
        if (player.connection == null) return;
        Send.toPlayer(player, new LegacyFxPayload(LegacyFxPayload.HENRY, henry == null ? -1 : henry.getId(), value, ""));
    }
}
