package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryLog;
import org.papiricoh.supernaturalcraft.network.AshMenuPayload;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.network.HeavenSyncPayload;
import org.papiricoh.supernaturalcraft.network.MemorySyncPayload;

/**
 * The client's side of Heaven (v0.18): plays each {@link HeavenFxPayload} (washes, title cards, the plot's progress, the
 * memory tint and toasts, the chair's struggle, the training test, the forms, the docket, the office's wrap, a Termination
 * Notice), keeps a mirror of the hunter's {@link MemoryLog} and {@link HeavenStanding} (plus the tag about the plot they stand
 * in) for the HUD and the book, and opens Ash's menu.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientHeaven {

    private static MemoryLog log = MemoryLog.EMPTY;
    private static HeavenStanding standing = HeavenStanding.NONE;
    private static CompoundTag plot = new CompoundTag();
    /** Bumped whenever the log changes (the book rebuilds its cards). */
    private static int logVersion;

    private ClientHeaven() {
    }

    // --- the mirrors -------------------------------------------------------------------------------------------------

    public static MemoryLog log() {
        return log;
    }

    public static int logVersion() {
        return logVersion;
    }

    public static HeavenStanding standing() {
        return standing;
    }

    /** What the server said about the plot the hunter stands in (owner name, wing/office open, home...): may be empty. */
    public static CompoundTag plot() {
        return plot;
    }

    public static @Nullable Memory memory(String id) {
        for (Memory m : log.entries()) if (m.id().equals(id)) return m;
        return null;
    }

    /** For previews and tests: as if the server had sent this log. */
    public static void setLog(MemoryLog value) {
        log = value;
        logVersion++;
    }

    // --- handlers ----------------------------------------------------------------------------------------------------

    public static void handleMemorySync(MemorySyncPayload payload) {
        setLog(MemoryLog.CODEC.parse(NbtOps.INSTANCE, payload.log()).result().orElse(MemoryLog.EMPTY));
    }

    public static void handleHeavenSync(HeavenSyncPayload payload) {
        standing = HeavenStanding.CODEC.parse(NbtOps.INSTANCE, payload.standing()).result().orElse(HeavenStanding.NONE);
        plot = payload.plot() == null ? new CompoundTag() : payload.plot().copy();
    }

    public static void handleAshMenu(AshMenuPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new AshMenuScreen(payload.ash(), AshMenu.read(payload.data())));
    }

    public static void handleFx(HeavenFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        switch (p.kind()) {
            case HeavenFxPayload.ARRIVE -> arrive(p.text(), p.duration());
            case HeavenFxPayload.PLOT_PROGRESS -> PlotProgress.update(p.arg());
            case HeavenFxPayload.MEMORY_ENTER -> {
                MemoryTint.enter(p.arg(), Math.max(10, p.duration()));
                if (!p.text().isEmpty()) {
                    HeavenOverlay.add(new HeavenOverlay.TitleCard(Component.empty(), Component.translatable(p.text()),
                            Component.translatable("hud.supernaturalcraft.heaven.memory_enter"), HeavenGui.Style.HEAVEN,
                            p.arg() & 0xFFFFFF, 120));
                }
            }
            case HeavenFxPayload.MEMORY_LEAVE -> MemoryTint.leave(Math.max(10, p.duration()));
            case HeavenFxPayload.MEMORY_COLLECTED -> collected(p.text());
            case HeavenFxPayload.TITLE -> {
                if (!p.text().isEmpty()) {
                    HeavenOverlay.add(new HeavenOverlay.TitleCard(Component.empty(), Component.translatable(p.text()),
                            Component.translatableWithFallback(p.text() + ".subtitle", ""), HeavenGui.Style.HEAVEN, HeavenGui.GOLD,
                            p.duration() > 0 ? p.duration() : 110));
                }
            }
            case HeavenFxPayload.QTE_START -> QteOverlay.start(p.entity(), p.arg(), p.duration());
            case HeavenFxPayload.QTE_PROGRESS -> QteOverlay.progress(p.arg(), p.arg2());
            case HeavenFxPayload.QTE_END -> QteOverlay.end(p.arg() == 1);
            case HeavenFxPayload.WHITEOUT -> {
                int d = Math.max(10, p.duration());
                HeavenOverlay.add(new HeavenOverlay.Wash(0xFFFFFF, 1f, 3, Math.max(0, d - 20), d));
            }
            case HeavenFxPayload.TRAINING_TEST -> {
                if (p.duration() > 0) TrainingTest.start(p.duration());
                else TrainingTest.end(p.arg() == 1);
            }
            case HeavenFxPayload.NAOMI_TITLE -> bossTitle("naomi", p.arg(), p.arg2() == 1, HeavenGui.Style.CLINIC, HeavenGui.CYAN, p.duration());
            case HeavenFxPayload.FORM -> FormHud.form(p.arg(), p.duration());
            case HeavenFxPayload.APPROVED -> FormHud.approved(p.duration());
            case HeavenFxPayload.FORETOLD -> DocketHud.foretell(p.text());
            case HeavenFxPayload.REVISION -> DocketHud.revise(p.arg(), p.text(), p.duration());
            case HeavenFxPayload.WRAP -> WrapCompensation.wrap(p.entity(), p.point());
            case HeavenFxPayload.TERMINATION -> Termination.serve(p.entity(), p.duration());
            case HeavenFxPayload.ZACHARIAH_TITLE -> {
                zachariahTitle(p.arg(), p.arg2(), p.duration());
                if (p.arg2() == 1) {
                    DocketHud.clear();
                    FormHud.clear();
                    Termination.clear();
                }
            }
            default -> {
            }
        }
    }

    /** Through a gate: the white of Heaven fading out, and whose Heaven this is. */
    public static void arrive(String owner, int duration) {
        int d = Math.max(20, duration);
        HeavenOverlay.add(new HeavenOverlay.Wash(0xFFFFFF, 1f, 1, 6, d));
        Minecraft mc = Minecraft.getInstance();
        boolean own = owner.isEmpty() || mc.player != null && owner.equals(mc.player.getGameProfile().getName());
        Component title = own ? Component.translatable("hud.supernaturalcraft.heaven.arrive_own")
                : Component.translatable("hud.supernaturalcraft.heaven.arrive_other", owner);
        HeavenOverlay.add(new HeavenOverlay.TitleCard(Component.empty(), title,
                Component.translatable("hud.supernaturalcraft.heaven.arrive_sub"), HeavenGui.Style.HEAVEN, HeavenGui.GOLD, d + 60));
    }

    private static void collected(String id) {
        Memory m = memory(id);
        Component title = m != null ? HeavenText.title(m) : Component.literal(HeavenText.pretty(id));
        MemoryToast.show(title);
        // The next time the book opens, it opens at the Memories tab.
        org.papiricoh.supernaturalcraft.client.book.HunterBookScreen.openNextAt(
                org.papiricoh.supernaturalcraft.client.book.HunterBookScreen.Tab.MEMORIES, null);
        org.papiricoh.supernaturalcraft.client.book.memories.MemoriesSection.focusNext(id);
        if (m != null && !log.collected().contains(id)) setLog(log.withCollected(id));
        HeavenOverlay.add(new HeavenOverlay.Wash(0xFFF4D0, 0.35f, 2, 2, 24));
    }

    /** A boss's phase card ({@code hud.supernaturalcraft.heaven.<boss>.phase<n>} and its {@code .sub}), or its fall. */
    private static void bossTitle(String boss, int phase, boolean fall, HeavenGui.Style style, int accent, int duration) {
        String key = "hud.supernaturalcraft.heaven." + boss + (fall ? ".fall" : ".phase" + Mth.clamp(phase, 1, 4));
        Component numeral = fall ? Component.empty() : Component.literal(HeavenGui.roman(phase));
        HeavenOverlay.add(new HeavenOverlay.TitleCard(numeral, Component.translatable(key), Component.translatable(key + ".sub"),
                style, accent, duration > 0 ? duration : 110));
    }

    /** Zachariah's cards in his own lang ({@code title.supernaturalcraft.zachariah.*}): a phase, his fall (1), the office dissolving (2). */
    private static void zachariahTitle(int phase, int kind, int duration) {
        String key = "title.supernaturalcraft.zachariah." + (kind == 1 ? "death" : kind == 2 ? "dissolve" : "phase" + Mth.clamp(phase, 1, 4));
        Component numeral = kind == 0 ? Component.literal(HeavenGui.roman(phase)) : Component.empty();
        HeavenOverlay.add(new HeavenOverlay.TitleCard(numeral, Component.translatable(key), Component.translatable(key + ".sub"),
                HeavenGui.Style.OFFICE, HeavenGui.GOLD_DEEP, duration > 0 ? duration : 110));
    }

    // --- ticking -----------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().level == null) return;
        WrapCompensation.preTick();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) return;
        HeavenOverlay.tick();
        MemoryTint.tick();
        PlotProgress.tick();
        TrainingTest.tick();
        FormHud.tick();
        DocketHud.tick();
        Termination.tick();
        QteOverlay.tick();
        WrapCompensation.postTick();
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        log = MemoryLog.EMPTY;
        standing = HeavenStanding.NONE;
        plot = new CompoundTag();
        logVersion++;
    }

    /** Forget every passing effect (left the world, or a preview between scenes); the mirrors stay. */
    public static void clear() {
        HeavenOverlay.clear();
        MemoryTint.clear();
        PlotProgress.clear();
        TrainingTest.clear();
        FormHud.clear();
        DocketHud.clear();
        Termination.clear();
        QteOverlay.clear();
        WrapCompensation.clear();
    }
}
