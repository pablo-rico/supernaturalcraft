package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.angel.HostsSavedData;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.network.SealFxPayload;
import org.papiricoh.supernaturalcraft.network.SealStatePayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * The seals as the rest of the mod sees them (v0.40): the one door into the Seals slice. Rituals ({@code seals_broken}),
 * Lilith, Ruby, the first Lucifer and the migration call only these; the slice owns everything behind them.
 */
public final class SealWorld {

    /** The great seals the game counts (the lore's 66): the last is Lilith's death. */
    public static final int TOTAL = SealRules.TOTAL;
    /** The advancements the slice reads and awards. */
    public static final String AZAZEL = "main/yellow_eyed", LILITH = "main/lucifer_rising", HELD = "main/seal_defended",
            WITNESSES = "main/the_rising_of_the_witnesses", ELEVENTH = "main/eleventh_seal";

    private SealWorld() {
    }

    /** How many seals have broken in this world (0-{@link #TOTAL}). */
    public static int broken(MinecraftServer server) {
        return SealSavedData.get(server).broken();
    }

    /** The omens' stage: 0 before the count starts, one more every three broken seals (0-4). */
    public static int stage(MinecraftServer server) {
        SealSavedData d = SealSavedData.get(server);
        return SealRules.stage(d.broken(), d.risen());
    }

    /** The stage the world's consequences run at (the faint first one in an echo). */
    public static int effectiveStage(MinecraftServer server) {
        SealSavedData d = SealSavedData.get(server);
        return d.started() ? SealRules.effectiveStage(d.broken(), d.risen(), d.echo()) : 0;
    }

    /** Starts the count, if it has not started (the first Azazel beaten, or the migration). */
    public static void begin(MinecraftServer server) {
        SealSavedData d = SealSavedData.get(server);
        if (d.started()) return;
        d.start(WorldClock.day(server));
        syncAll(server);
    }

    /** Lilith fell: the last seal breaks and Lucifer rises (the world at full Apocalypse until the Cage is shut). */
    public static void lilithFell(MinecraftServer server) {
        SealSavedData d = SealSavedData.get(server);
        boolean was = d.risen();
        d.start(WorldClock.day(server));
        d.setBroken(TOTAL, WorldClock.day(server));
        d.setRisen(true);
        openAngels(server, d);
        SealEvents.endAll(server);
        if (!was) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) Send.toPlayer(p, new SealFxPayload(-1, SealFxPayload.BROKEN, TOTAL,
                    SealRules.MAX_STAGE, Vec3.ZERO, 0));
        }
        syncAll(server);
    }

    /** The first Lucifer was thrown back into the Cage (Swan Song): the world calms to an echo (the plagues turn back). */
    public static void cageClosed(MinecraftServer server) {
        lilithFell(server);
        SealSavedData d = SealSavedData.get(server);
        d.setEcho(true);
        d.ledger().hastenAll(WorldClock.day(server));
        d.ledgerChanged();
        syncAll(server);
    }

    public static boolean risen(MinecraftServer server) {
        return SealSavedData.get(server).risen();
    }

    public static boolean echo(MinecraftServer server) {
        return SealSavedData.get(server).echo();
    }

    /** The angel-family seal events {@code hunter} has held (two open the way to Uriel). */
    public static int defendedAngelSeals(ServerPlayer hunter) {
        return hunter.getData(AllAttachments.SEAL_RECORD).angelsDefended();
    }

    public static SealRecord record(ServerPlayer hunter) {
        return hunter.getData(AllAttachments.SEAL_RECORD);
    }

    public static void setRecord(ServerPlayer hunter, SealRecord r) {
        hunter.setData(AllAttachments.SEAL_RECORD, r);
        sync(hunter);
    }

    /** Whether {@code hunter} is in chapter II (Azazel beaten, Lilith not): the seals call them. */
    public static boolean chapterTwo(ServerPlayer hunter) {
        return SealRules.chapterTwo(AuthorWorld.done(hunter, AZAZEL), AuthorWorld.done(hunter, LILITH));
    }

    // --- a seal breaks ---------------------------------------------------------------------------------------------------------

    /**
     * The next seal breaks ({@code cause}: the event that failed, or null for the passive count): every hunter hears it, the sky
     * reddens, the stage may turn. Never past the eleventh (the last is Lilith's). @return whether one broke
     */
    public static boolean breakSeal(MinecraftServer server, @Nullable SealEvent cause) {
        SealSavedData d = SealSavedData.get(server);
        if (!d.started() || d.risen() || d.broken() >= SealRules.PASSIVE_MAX) return false;
        int stageBefore = SealRules.stage(d.broken(), false);
        int n = d.broken() + 1;
        d.setBroken(n, WorldClock.day(server));
        int stage = SealRules.stage(n, false);
        Component name = Component.translatable(sealName(n)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        Component line = cause != null
                ? Component.translatable("message.supernaturalcraft.seal.broken_by", n, name, Component.translatable(cause.kind.nameKey()))
                : Component.translatable("message.supernaturalcraft.seal.broken", n, name);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(line.copy().withStyle(ChatFormatting.RED));
            Send.toPlayer(p, new SealFxPayload(-1, SealFxPayload.BROKEN, n, stage, Vec3.ZERO, 0));
            if (n >= SealRules.PASSIVE_MAX && AuthorWorld.done(p, AZAZEL)) ChorusRewards.award(p, ELEVENTH);
        }
        if (stage != stageBefore) {
            Component turn = Component.translatable("message.supernaturalcraft.seal.stage." + stage).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(turn);
        }
        if (stage >= SealRules.ANGEL_STAGE) openAngels(server, d);
        syncAll(server);
        return true;
    }

    /** The key of seal {@code n}'s name (1-12). */
    public static String sealName(int n) {
        return "seal.supernaturalcraft.seal." + Math.max(1, Math.min(TOTAL, n));
    }

    /** From the second stage the angels come down to fight it out: their natural spawns open (once; the slice never closes them). */
    static void openAngels(MinecraftServer server, SealSavedData d) {
        if (d.angelsOpened()) return;
        d.setAngelsOpened(true);
        HostsSavedData.get(server).setAngels(true);
    }

    // --- sync ------------------------------------------------------------------------------------------------------------------

    public static SealStatePayload state(MinecraftServer server, @Nullable ServerPlayer hunter) {
        SealSavedData d = SealSavedData.get(server);
        long next = d.nextBreak();
        SealRecord r = hunter != null ? record(hunter) : SealRecord.EMPTY;
        return new SealStatePayload(d.started(), d.broken(), SealRules.stage(d.broken(), d.risen()), next,
                SealRules.daysLeft(WorldClock.day(server), next), d.delayDays(), d.risen(), d.echo(), r.defended(), r.letBreak(), r.angelsDefended());
    }

    /** Sends {@code hunter} the world's seals and their share. */
    public static void sync(ServerPlayer hunter) {
        Send.toPlayer(hunter, state(hunter.server, hunter));
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }
}
