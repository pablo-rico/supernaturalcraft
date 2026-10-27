package org.papiricoh.supernaturalcraft.client.book.chain;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.Front;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Challenge;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The front between the chains (v0.27; SucroCorp's third colour since v0.28): a dial of its twelve sectors coloured by who holds them (the push on each as a
 * band at the rim), the two seats and the reader; beside it the war's state, scores and prestige, the reader's task and the
 * challenges waiting on them, and the latest news from across the sides.
 */
final class WarPanel extends ChainPanel {

    private static final int DCX = X0 + 12 + 74, DCY = Y0 + 40 + 66, R = 57, HUB = 6;
    private static final int PX0 = X0 + 12 + 158, PX1 = X1 - 12, PY0 = Y0 + 12, PY1 = Y1 - 12;
    private static final Set<String> FRONT_KEYS = Set.of("skirmish", "skirmish_seen", "vow", "champion_won", "sector_taken",
            "war_declared", "war_won");
    /** Sector fills: the Pit's ember, the Host's gold and SucroCorp's petrol; their pushes a little brighter. */
    private static final int PIT = 0x8A1C12, HOST = 0xD9A92B, BOARD = 0x24504A, PIT_PUSH = 0xE8473A, HOST_PUSH = 0xFFF4C8,
            BOARD_PUSH = 0x7FE3CF;

    private static int fill(String faction) {
        return switch (ChainFaction.byId(faction)) {
            case DEMON -> PIT;
            case ANGEL -> HOST;
            case LEVIATHAN -> BOARD;
        };
    }

    private static int pushColour(String faction) {
        return switch (ChainFaction.byId(faction)) {
            case DEMON -> PIT_PUSH;
            case ANGEL -> HOST_PUSH;
            case LEVIATHAN -> BOARD_PUSH;
        };
    }

    /** The faction pressing hardest on a sector it does not hold, or null. */
    private static @org.jetbrains.annotations.Nullable String pusher(int sector) {
        String owner = ClientChain.sectorOwner(sector), best = null;
        int most = 0;
        for (ChainFaction f : ChainFaction.values()) {
            int p = ClientChain.war.pressure(sector, f.id());
            if (!f.id().equals(owner) && p > most) { most = p; best = f.id(); }
        }
        return best;
    }

    private final int side;
    private final Column page = new Column();

    WarPanel(ChainSection section, HunterBookScreen book, ChainFaction viewing) {
        super(section, book);
        this.side = switch (viewing) {
            case DEMON -> 0;
            case ANGEL -> 1;
            case LEVIATHAN -> ChainAtlas.LEVIATHAN;
        };
    }

    @Override
    int side() {
        return side;
    }

    @Override
    void draw(GuiGraphics g, int mx, int my, float partial) {
        Component title = Component.translatable(K + "front");
        g.drawString(font, title, DCX - font.width(title) / 2, Y0 + 14, ACCENT[side], false);
        boolean war = ClientChain.atWar();
        Component state = war ? Component.translatable(K + "war.until", ClientChain.war.until()) : Component.translatable(K + "war.peace");
        g.drawString(font, state, DCX - font.width(state) / 2, Y0 + 25, war ? (side == 1 ? BookStyle.BLOOD : 0xFFF06A50) : FADED[side], false);
        drawDial(g, mx, my, partial);
        drawLegend(g);
        ChainAtlas.page(g, side, PX0, PY0, PX1 - PX0, PY1 - PY0);
        page.place(PX0 + 8, PY0 + 7, PX1 - PX0 - 12, PY1 - PY0 - 12);
        page.draw(g, rows(), mx, my, side, true);
    }

    private static int sectorAt(double mx, double my) {
        double dx = mx - DCX, dy = my - DCY, d = Math.hypot(dx, dy);
        if (d < HUB || d > R) return -1;
        return Front.sector(dx, dy);
    }

    private void drawDial(GuiGraphics g, int mx, int my, float partial) {
        var w = ClientChain.war;
        int hovered = sectorAt(mx, my);
        RenderSystem.enableBlend();
        // The parchment under the dial, darkened.
        ChainDraw.sector(g, DCX, DCY, 0, R + 1, 0, 2 * Math.PI, side == 1 ? 0xFF2C3554 : side == ChainAtlas.LEVIATHAN ? 0xFF0A1A18 : 0xFF1A0E0E);
        for (int i = 0; i < Front.SECTORS; i++) {
            String owner = ClientChain.sectorOwner(i);
            double a0 = i * Front.WIDTH, a1 = (i + 1) * Front.WIDTH;
            int rgb = owner == null ? 0x6E6458 : fill(owner);
            float alpha = i == hovered ? 0.95f : 0.72f;
            ChainDraw.sector(g, DCX, DCY, HUB, R, a0, a1, ChainDraw.argb(alpha, rgb));
            // Rings every 650 blocks, faintly.
            for (int r = 1; r < 4; r++) {
                float rr = HUB + (R - HUB) * r / 4f;
                ChainDraw.sector(g, DCX, DCY, rr - 0.3f, rr + 0.3f, a0, a1, 0x30000000);
            }
            String by = pusher(i);
            int push = by == null ? 0 : w.pressure(i, by);
            if (push != 0) {
                float k = Mth.clamp(push / (float) Front.FLIP, 0.15f, 1);
                int col = pushColour(by);
                float pulse = 0.65f + 0.35f * Mth.sin((Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + partial) * 0.15f);
                ChainDraw.sector(g, DCX, DCY, R - 6, R - 1, a0 + 0.04, a0 + 0.04 + (a1 - a0 - 0.08) * k, ChainDraw.argb(0.9f * pulse, col));
            }
        }
        // Sector edges.
        for (int i = 0; i < Front.SECTORS; i++) {
            double a = i * Front.WIDTH;
            ChainDraw.sector(g, DCX, DCY, HUB, R, a - 0.006, a + 0.006, 0x70000000);
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 10);
        RenderSystem.enableBlend();
        g.blit(ChainDraw.DIAL, DCX - 64, DCY - 64, 0, 0, 128, 128, 128, 128);
        // North, east, south, west round the ring.
        String[] dirs = {"n", "e", "s", "w"};
        for (int q = 0; q < 4; q++) {
            Component c = Component.translatable(K + "war." + dirs[q]);
            double a = -Math.PI / 2 + q * Math.PI / 2;
            int x = (int) Math.round(DCX + Math.cos(a) * (R + 12)), y = (int) Math.round(DCY + Math.sin(a) * (R + 12));
            g.drawString(font, c, x - font.width(c) / 2, y - 4, INK[side], false);
        }
        // The seats and the reader.
        for (ChainSyncPayload.Court c : ClientChain.courts) {
            float[] p = place(c.x(), c.z());
            var marker = ChainAtlas.marker("angel".equals(c.faction()) ? 1 : "leviathan".equals(c.faction()) ? ChainAtlas.LEVIATHAN : 0);
            marker.draw(g, Math.round(p[0]) - 3, Math.round(p[1]) - 3);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(K + "war.seat." + c.faction()));
            if (!c.built()) tip.add(Component.translatable(K + "war.seat.unbuilt").withStyle(ChatFormatting.GRAY));
            hit(Math.round(p[0]) - 4, Math.round(p[1]) - 4, 9, 9, null, tip);
        }
        var player = Minecraft.getInstance().player;
        if (player != null && player.level().dimension().location().toString().equals(ChainSection.OVERWORLD)) {
            float[] p = place(player.getX(), player.getZ());
            float pulse = 0.5f + 0.5f * Mth.sin((player.tickCount + partial) * 0.2f);
            g.fill(Math.round(p[0]) - 5, Math.round(p[1]) - 5, Math.round(p[0]) + 5, Math.round(p[1]) + 5, ChainDraw.argb(0.25f * pulse, 0x86B6E3));
            ChainAtlas.marker(2).draw(g, Math.round(p[0]) - 3, Math.round(p[1]) - 3);
            hit(Math.round(p[0]) - 4, Math.round(p[1]) - 4, 9, 9, null, List.of(Component.translatable(K + "war.you")));
        }
        g.pose().popPose();
        if (hovered >= 0 && !overMarker(mx, my)) {
            String owner = ClientChain.sectorOwner(hovered);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("chain.supernaturalcraft.sector", hovered + 1).withStyle(ChatFormatting.BOLD));
            if (owner != null) tip.add(Component.translatable(K + "war.held", Component.translatable(K + owner)));
            String by = pusher(hovered);
            if (by != null) tip.add(Component.translatable(K + "war.push", Component.translatable(K + by),
                    w.pressure(hovered, by), Front.FLIP).withStyle(ChatFormatting.GRAY));
            book.tooltip(tip);
        }
    }

    private boolean overMarker(int mx, int my) {
        for (var c : ClientChain.courts) {
            float[] p = place(c.x(), c.z());
            if (Math.abs(mx - p[0]) < 5 && Math.abs(my - p[1]) < 5) return true;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        float[] p = place(player.getX(), player.getZ());
        return Math.abs(mx - p[0]) < 5 && Math.abs(my - p[1]) < 5;
    }

    /** A world position on the dial (north up, the ring's outer edge at {@link Front#OUTER}), held inside the rim. */
    private static float[] place(double x, double z) {
        double d = Math.hypot(x, z), r = Math.min(R + 3, d / Front.OUTER * R);
        double a = Math.atan2(z, x);
        return new float[]{(float) (DCX + Math.cos(a) * r), (float) (DCY + Math.sin(a) * r)};
    }

    private void drawLegend(GuiGraphics g) {
        List<Component> labels = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        for (ChainFaction f : ChainFaction.values()) {
            int held = 0;
            for (int i = 0; i < Front.SECTORS; i++) if (f.id().equals(ClientChain.sectorOwner(i))) held++;
            if (held == 0 && f.side() == ChainFaction.Side.NONE) continue;
            labels.add(Component.translatable(K + "war.sectors", Component.translatable(K + f.id()), held));
            colours.add(fill(f.id()));
        }
        // As many to a line as fit under the dial; the rest on the next.
        int y = DCY + R + 16, max = 2 * R + 36, from = 0;
        while (from < labels.size()) {
            int to = from, total = -8;
            while (to < labels.size() && (to == from || total + font.width(labels.get(to)) + 17 <= max)) total += font.width(labels.get(to++)) + 17;
            int x = DCX - total / 2;
            for (int i = from; i < to; i++) {
                g.fill(x, y + 1, x + 6, y + 7, 0xFF000000 | colours.get(i));
                g.drawString(font, labels.get(i), x + 9, y, INK[side], false);
                x += font.width(labels.get(i)) + 17;
            }
            from = to;
            y += 10;
        }
    }

    private static Component named(String faction) {
        int col = switch (ChainFaction.byId(faction)) {
            case DEMON -> BookStyle.BLOOD;
            case ANGEL -> BookStyle.MANA;
            case LEVIATHAN -> ChainSection.LEVIATHAN_INK;
        };
        return Component.translatable(K + faction).withColor(col);
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        int w = page.w - 4;
        var war = ClientChain.war;
        text(rows, Component.translatable(K + "war.score.title"), BookStyle.GOLD, w, 0);
        rule(rows);
        if (ClientChain.atWar()) text(rows, Component.translatable(K + "war.score", named(war.sideA()), war.score(war.sideA()),
                war.score(war.sideB()), named(war.sideB())), BookStyle.INK, w, 0);
        for (ChainFaction f : ChainFaction.values())
            if (f.side() != ChainFaction.Side.NONE || war.prestige(f.id()) > 0)
                text(rows, Component.translatable(K + "war.prestige.one", named(f.id()), war.prestige(f.id())), BookStyle.INK, w, 0);
        if (!ClientChain.atWar()) text(rows, Component.translatable(K + "war.peace.desc"), BookStyle.FADED, w, 0);

        var task = ClientChain.task;
        if (task != null) {
            gap(rows, 5);
            text(rows, Component.translatable(K + "war.task"), BookStyle.GOLD, w, 0);
            rule(rows);
            Entry target = ClientChain.entry(task.targetFaction(), task.targetId());
            Component name = target == null ? Component.literal("?") : ClientChain.name(target);
            text(rows, Component.translatable("chain.supernaturalcraft.task." + task.kind().toLowerCase(Locale.ROOT), name, task.count(), task.until()),
                    BookStyle.INK, w, 0);
            text(rows, Component.translatable(K + "war.task.from", Component.translatable(K + task.lord())), BookStyle.FADED, w, 0);
        }

        List<Challenge> mine = ClientChain.challengesToMe();
        if (!mine.isEmpty()) {
            gap(rows, 5);
            text(rows, Component.translatable(K + "war.challenges"), BookStyle.GOLD, w, 0);
            rule(rows);
            for (Challenge c : mine) {
                Entry who = ClientChain.entry(c.challenger());
                link(rows, ChainAtlas.icon(2), Component.translatable(K + "war.challenge", ClientChain.name(c.challenger()),
                        Component.translatable(K + (c.lethal() ? "duels.lethal" : "duels.yield"))), BookStyle.BLOOD, w, 0,
                        who == null ? null : () -> section.openOfficer(who), who == null ? null : List.of(Component.translatable(K + "open", ClientChain.name(who))));
                text(rows, Component.translatable(K + "duels.answer", OfficerModal.playTime(c.remaining())), BookStyle.FADED, w, 13);
                gap(rows, 2);
            }
        }

        gap(rows, 5);
        text(rows, Component.translatable(K + "war.news"), BookStyle.GOLD, w, 0);
        rule(rows);
        int n = 0;
        for (int i = ClientChain.history.size() - 1; i >= 0 && n < 12; i--) {
            History h = ClientChain.history.get(i);
            if (!FRONT_KEYS.contains(h.key())) continue;
            text(rows, OfficerModal.dated(h), BookStyle.INK, w, 0);
            gap(rows, 3);
            n++;
        }
        if (n == 0) text(rows, Component.translatable(K + "war.news.none"), BookStyle.FADED, w, 0);
        return rows;
    }

    @Override
    boolean mouseScrolled(double mx, double my, double sy) {
        if (page.over(mx, my)) page.scroll(sy);
        return true;
    }
}
