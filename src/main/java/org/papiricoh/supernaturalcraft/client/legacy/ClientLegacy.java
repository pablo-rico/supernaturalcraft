package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.HenryDialogue;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.network.LegacyFxPayload;
import org.papiricoh.supernaturalcraft.network.LegacySyncPayload;
import org.papiricoh.supernaturalcraft.network.ResearchBoardPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * The client's copy of its own hunter's {@link Legacy} and {@link Archive} (v0.17), from the last {@link LegacySyncPayload}.
 * The journal's {@code research} unlock and the hidden Archive chapter read it. Also where every Men of Letters moment lands
 * ({@link #handleFx}: toasts, the rank's title card, Henry's dialogue) and the open desk's board ({@link #handleBoard}).
 */
public final class ClientLegacy {

    private static Legacy legacy = Legacy.NONE;
    private static Archive archive = Archive.EMPTY;
    private static ResearchBoardPayload board;
    private static final List<Runnable> LISTENERS = new ArrayList<>();

    static {
        // Tooltips and the book find the reader's generated formulas through SigilLookup (agent B).
        org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.setLocal(ClientLegacy::archive);
    }

    private ClientLegacy() {
    }

    public static void handle(LegacySyncPayload payload) {
        legacy = payload.legacy();
        archive = payload.archive();
        for (Runnable r : List.copyOf(LISTENERS)) r.run();
    }

    /** A Men of Letters moment: see {@link LegacyFxPayload}'s kinds. */
    public static void handleFx(LegacyFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.kind()) {
            case LegacyFxPayload.RESEARCH_DONE -> {
                LegacyToasts.research(p.text());
                play(AllSounds.LEGACY_RESEARCH_DONE.get(), 1.0f);
            }
            case LegacyFxPayload.RANK_UP -> {
                LegacyOverlay.rankUp(p.value());
                play(AllSounds.LEGACY_RANK_UP.get(), 1.0f);
            }
            case LegacyFxPayload.HENRY -> {
                if (p.value() == HenryDialogue.CLOSED) {
                    if (mc.screen instanceof HenryDialogueScreen) mc.setScreen(null);
                    return;
                }
                HenryDialogue.Stage stage = HenryDialogue.Stage.of(p.value());
                if (stage == null) return;
                if (mc.screen instanceof HenryDialogueScreen open && open.henry() == p.entity()) open.stage(stage);
                else mc.setScreen(new HenryDialogueScreen(p.entity(), stage));
            }
            case LegacyFxPayload.CASE_NEW -> {
                LegacyToasts.caseNew(p.value());
                play(AllSounds.LEGACY_PAPER.get(), 1.0f);
            }
            case LegacyFxPayload.CASE_CLOSED -> {
                LegacyToasts.caseClosed(p.value(), "solved".equals(p.text()));
                play(AllSounds.LEGACY_TYPEWRITER.get(), 1.0f);
            }
            default -> {
            }
        }
    }

    private static void play(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    /** The research board of the open desk: kept (it may come before the screen opens) and shown if its menu is the open one. */
    public static void handleBoard(ResearchBoardPayload payload) {
        board = payload;
        if (Minecraft.getInstance().screen instanceof ResearchScreen s) s.boardChanged();
    }

    /** The last board, if it belongs to the menu {@code container}. */
    @Nullable
    public static ResearchBoardPayload board(int container) {
        return board != null && board.container() == container ? board : null;
    }

    public static Legacy legacy() {
        return legacy;
    }

    public static Archive archive() {
        return archive;
    }

    public static boolean member() {
        return legacy.member();
    }

    public static boolean researched(String topic) {
        return archive.knows(topic);
    }

    /** Runs {@code listener} after every update (toasts, open screens). */
    public static void listen(Runnable listener) {
        LISTENERS.add(listener);
    }

    /** The hunter's case {@code index}, or null. */
    @Nullable
    public static CaseFile caseFile(int index) {
        for (CaseFile c : legacy.cases()) if (c.index() == index) return c;
        return null;
    }

    /** Opens the brief of the hunter's case {@code index} (the case file item, client side only). */
    public static void openCaseBrief(int index) {
        CaseFile c = caseFile(index);
        Minecraft mc = Minecraft.getInstance();
        if (c == null) {
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("screen.supernaturalcraft.case.missing"), true);
            return;
        }
        mc.setScreen(new CaseBriefScreen(c));
        play(AllSounds.LEGACY_PAPER.get(), 1.0f);
    }

    /** For previews: set the client's copy directly. */
    public static void set(Legacy l, Archive a) {
        legacy = l;
        archive = a;
        for (Runnable r : List.copyOf(LISTENERS)) r.run();
    }

    /** Back to nothing (on leaving a world). */
    public static void reset() {
        legacy = Legacy.NONE;
        archive = Archive.EMPTY;
        board = null;
    }
}
