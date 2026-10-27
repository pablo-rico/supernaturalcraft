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
import org.papiricoh.supernaturalcraft.network.ChainRealmPayload.Dominion;
import org.papiricoh.supernaturalcraft.network.ChainRealmPayload.RealmWar;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Challenge;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.History;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The front between the chains (v0.27; SucroCorp's third colour since v0.28): a dial of its sectors ({@link Front#SECTORS},
 * 24 since v0.29) coloured by who holds them, a guild's in its dye (the push on each as a band at the rim), the seats, the
 * guilds' standards and the reader; beside it the war's state, scores and prestige, the reader's task and the
 * challenges waiting on them, and the latest news from across the sides. v0.32: with Nations &amp; War there is no ring;
 * its dominions take the dial's place ({@link #drawDominions}: each power's seat and enclaves on a map, the contested ones
 * ringed) and the page lists the nations' wars a chain fights and every town by power.
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
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> ClientChain.dyeColor(faction);
        };
    }

    private static int pushColour(String faction) {
        return switch (ChainFaction.byId(faction)) {
            case DEMON -> PIT_PUSH;
            case ANGEL -> HOST_PUSH;
            case LEVIATHAN -> BOARD_PUSH;
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> ClientChain.mix(ClientChain.dyeColor(faction), 0xFFFFFF, 0.45f);
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
        this.side = ChainAtlas.side(viewing);
    }

    @Override
    int side() {
        return side;
    }

    @Override
    void draw(GuiGraphics g, int mx, int my, float partial) {
        boolean ring = ClientChain.sectors();
        Component title = Component.translatable(K + (ring ? "front" : "dominions"));
        g.drawString(font, title, DCX - font.width(title) / 2, Y0 + 14, accent(side), false);
        boolean war = ring ? ClientChain.atWar() : !ClientChain.realmWars().isEmpty();
        Component state = !war ? Component.translatable(K + "war.peace") : ring ? Component.translatable(K + "war.until", ClientChain.war.until())
                : Component.translatable(K + "realm.state", ClientChain.realmWars().size());
        g.drawString(font, state, DCX - font.width(state) / 2, Y0 + 25, war ? (side == 1 ? BookStyle.BLOOD : 0xFFF06A50) : faded(side), false);
        if (ring) {
            drawDial(g, mx, my, partial);
            drawLegend(g);
        } else {
            drawDominions(g, partial);
            drawRealmLegend(g);
        }
        ChainAtlas.page(g, side, PX0, PY0, PX1 - PX0, PY1 - PY0);
        page.place(PX0 + 8, PY0 + 7, PX1 - PX0 - 12, PY1 - PY0 - 12);
        page.draw(g, rows(), mx, my, side, true);
    }

    /** The parchment under the dial or the map, darkened. */
    private int plate() {
        return ChainAtlas.guild(side) ? 0xFF000000 | ClientChain.mix(ChainAtlas.tintOf(side), 0x000000, 0.82f)
                : side == 1 ? 0xFF2C3554 : side == ChainAtlas.LEVIATHAN ? 0xFF0A1A18 : 0xFF1A0E0E;
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
        ChainDraw.sector(g, DCX, DCY, 0, R + 1, 0, 2 * Math.PI, plate());
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
            g.drawString(font, c, x - font.width(c) / 2, y - 4, ink(side), false);
        }
        // The seats and the reader.
        for (ChainSyncPayload.Court c : ClientChain.courts) {
            // A guild's seat is its standard, marked below in its colour.
            if (ClientChain.isGuild(c.faction())) continue;
            float[] p = place(c.x(), c.z());
            var marker = ChainAtlas.marker("angel".equals(c.faction()) ? 1 : "leviathan".equals(c.faction()) ? ChainAtlas.LEVIATHAN : 0);
            marker.draw(g, Math.round(p[0]) - 3, Math.round(p[1]) - 3);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(K + "war.seat." + c.faction()));
            if (!c.built()) tip.add(Component.translatable(K + "war.seat.unbuilt").withStyle(ChatFormatting.GRAY));
            hit(Math.round(p[0]) - 4, Math.round(p[1]) - 4, 9, 9, null, tip);
        }
        for (ChainSyncPayload.Guild guild : ClientChain.guilds) {
            if (!guild.standard()) continue;
            float[] p = place(guild.x(), guild.z());
            int gs = ChainAtlas.side(ChainFaction.byId(guild.slot()));
            ChainAtlas.colour(g, ClientChain.guildGlow(guild.slot()));
            ChainAtlas.marker(gs).draw(g, Math.round(p[0]) - 3, Math.round(p[1]) - 3);
            g.setColor(1, 1, 1, 1);
            hit(Math.round(p[0]) - 4, Math.round(p[1]) - 4, 9, 9, null,
                    List.of(Component.translatable(K + "guild.standard.marker", ClientChain.factionName(guild.slot()))));
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
            if (owner != null) tip.add(Component.translatable(K + "war.held", ClientChain.factionName(owner)));
            String by = pusher(hovered);
            if (by != null) tip.add(Component.translatable(K + "war.push", ClientChain.factionName(by),
                    w.pressure(hovered, by), Front.FLIP).withStyle(ChatFormatting.GRAY));
            book.tooltip(tip);
        }
    }

    private boolean overMarker(int mx, int my) {
        for (var c : ClientChain.courts) {
            if (ClientChain.isGuild(c.faction())) continue;
            float[] p = place(c.x(), c.z());
            if (Math.abs(mx - p[0]) < 5 && Math.abs(my - p[1]) < 5) return true;
        }
        for (var guild : ClientChain.guilds) {
            float[] p = place(guild.x(), guild.z());
            if (guild.standard() && Math.abs(mx - p[0]) < 5 && Math.abs(my - p[1]) < 5) return true;
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
            boolean founded = f.guild() && ClientChain.guild(f.id()) != null;
            if (held == 0 && f.side() == ChainFaction.Side.NONE && !founded) continue;
            Component name = founded ? Component.literal(ClientChain.guild(f.id()).name()) : Component.translatable(K + f.id());
            labels.add(Component.translatable(K + "war.sectors", name, held));
            colours.add(fill(f.id()));
        }
        drawLegend(g, labels, colours, DCY + R + 16);
    }

    /** Swatches and labels centred under the dial or the map, as many to a line as fit; the rest on the next. */
    private void drawLegend(GuiGraphics g, List<Component> labels, List<Integer> colours, int top) {
        int y = top, max = 2 * R + 36, from = 0;
        while (from < labels.size()) {
            int to = from, total = -8;
            while (to < labels.size() && (to == from || total + font.width(labels.get(to)) + 17 <= max)) total += font.width(labels.get(to++)) + 17;
            int x = DCX - total / 2;
            for (int i = from; i < to; i++) {
                g.fill(x, y + 1, x + 6, y + 7, 0xFF000000 | colours.get(i));
                g.drawString(font, labels.get(i), x + 9, y, ink(side), false);
                x += font.width(labels.get(i)) + 17;
            }
            from = to;
            y += 10;
        }
    }

    // --- Nations & War's dominions (v0.32) ---------------------------------------------------------

    private static final int MAP_X = DCX - 68, MAP_Y = Y0 + 38, MAP_W = 136, MAP_H = 146, MAP_PAD = 9;

    /** World to map: every dominion and raised standard fitted in the box, north up, one scale for both axes. */
    private record Fit(double cx, double cz, double scale) {
        float[] at(double x, double z) {
            return new float[]{(float) (MAP_X + MAP_W / 2.0 + (x - cx) * scale), (float) (MAP_Y + MAP_H / 2.0 + (z - cz) * scale)};
        }
    }

    private static Fit fit() {
        double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        List<double[]> points = new ArrayList<>();
        for (Dominion d : ClientChain.dominions()) points.add(new double[]{d.x(), d.z()});
        for (ChainSyncPayload.Guild guild : ClientChain.guilds) if (guild.standard()) points.add(new double[]{guild.x(), guild.z()});
        if (points.isEmpty()) return new Fit(0, 0, (MAP_W - 2 * MAP_PAD) / 2000.0);
        for (double[] p : points) {
            x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]);
            z0 = Math.min(z0, p[1]); z1 = Math.max(z1, p[1]);
        }
        double spanX = Math.max(400, x1 - x0), spanZ = Math.max(400, z1 - z0);
        return new Fit((x0 + x1) / 2, (z0 + z1) / 2, Math.min((MAP_W - 2 * MAP_PAD) / spanX, (MAP_H - 2 * MAP_PAD) / spanZ));
    }

    /** The binding a power's marker comes from: 0 the Pit, 1 the Host, SucroCorp's. */
    private static int markerSide(String faction) {
        return "angel".equals(faction) ? 1 : "leviathan".equals(faction) ? ChainAtlas.LEVIATHAN : 0;
    }

    /**
     * The map of Nations &amp; War's dominions where the ring was: each power's seat (its marker) tied to its enclaves (squares
     * in its colour), a pulsing ring round a contested one, the guilds' standards and the reader (held inside the frame).
     */
    private void drawDominions(GuiGraphics g, float partial) {
        RenderSystem.enableBlend();
        g.fill(MAP_X - 2, MAP_Y - 2, MAP_X + MAP_W + 2, MAP_Y + MAP_H + 2, 0xFF000000 | ClientChain.mix(accent(side) & 0xFFFFFF, 0x000000, 0.45f));
        g.fill(MAP_X - 1, MAP_Y - 1, MAP_X + MAP_W + 1, MAP_Y + MAP_H + 1, 0xFF000000);
        g.fill(MAP_X, MAP_Y, MAP_X + MAP_W, MAP_Y + MAP_H, plate());
        for (int i = 1; i < 4; i++) {
            int x = MAP_X + MAP_W * i / 4, y = MAP_Y + MAP_H * i / 4;
            g.fill(x, MAP_Y, x + 1, MAP_Y + MAP_H, 0x18FFFFFF);
            g.fill(MAP_X, y, MAP_X + MAP_W, y + 1, 0x18FFFFFF);
        }
        Component north = Component.translatable(K + "war.n");
        g.drawString(font, north, MAP_X + MAP_W / 2 - font.width(north) / 2, MAP_Y + 3, 0x90FFFFFF, false);
        Fit fit = fit();
        var level = Minecraft.getInstance().level;
        float pulse = 0.55f + 0.45f * Mth.sin(((level == null ? 0 : level.getGameTime()) + partial) * 0.15f);
        // Each enclave tied to its power's seat, faintly.
        for (ChainFaction f : ChainFaction.FIXED) {
            List<Dominion> towns = ClientChain.dominions(f.id());
            Dominion seat = towns.stream().filter(Dominion::seat).findFirst().orElse(null);
            if (seat == null) continue;
            float[] s = fit.at(seat.x(), seat.z());
            for (Dominion d : towns) {
                if (d.seat()) continue;
                float[] e = fit.at(d.x(), d.z());
                ChainDraw.curve(g, s[0] + 0.5f, s[1] + 0.5f, e[0] + 0.5f, e[1] + 0.5f, 0.06f, 0.8f, ChainDraw.argb(0.32f, pushColour(f.id())));
            }
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 10);
        RenderSystem.enableBlend();
        for (Dominion d : ClientChain.dominions()) {
            float[] p = fit.at(d.x(), d.z());
            int px = Math.round(p[0]), py = Math.round(p[1]);
            if (d.contested()) ChainDraw.sector(g, px + 0.5f, py + 0.5f, 5, 6.5f, 0, 2 * Math.PI, ChainDraw.argb(pulse, 0xE8473A));
            if (d.seat()) {
                ChainAtlas.marker(markerSide(d.faction())).draw(g, px - 3, py - 3);
            } else {
                g.fill(px - 2, py - 2, px + 3, py + 3, 0xFF000000);
                // The Host's push is near white: its enclaves keep its gold, so the map reads by colour.
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFF000000 | ("angel".equals(d.faction()) ? HOST : pushColour(d.faction())));
            }
            hit(px - 4, py - 4, 9, 9, null, dominionTip(d));
        }
        for (ChainSyncPayload.Guild guild : ClientChain.guilds) {
            if (!guild.standard()) continue;
            float[] p = fit.at(guild.x(), guild.z());
            int px = Math.round(p[0]), py = Math.round(p[1]);
            ChainAtlas.colour(g, ClientChain.guildGlow(guild.slot()));
            ChainAtlas.marker(ChainAtlas.side(ChainFaction.byId(guild.slot()))).draw(g, px - 3, py - 3);
            g.setColor(1, 1, 1, 1);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(K + "guild.standard.marker", ClientChain.factionName(guild.slot())));
            var nation = ClientChain.guildNation(guild.slot());
            if (nation != null) tip.add(Component.translatable(K + "guild.nation", nation.nation()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable(K + "dominion.at", guild.x(), guild.z()).withStyle(ChatFormatting.DARK_GRAY));
            hit(px - 4, py - 4, 9, 9, null, tip);
        }
        var player = Minecraft.getInstance().player;
        if (player != null && player.level().dimension().location().toString().equals(ChainSection.OVERWORLD)) {
            float[] p = fit.at(player.getX(), player.getZ());
            int px = Mth.clamp(Math.round(p[0]), MAP_X + 4, MAP_X + MAP_W - 5), py = Mth.clamp(Math.round(p[1]), MAP_Y + 4, MAP_Y + MAP_H - 5);
            float glow = 0.5f + 0.5f * Mth.sin((player.tickCount + partial) * 0.2f);
            g.fill(px - 5, py - 5, px + 5, py + 5, ChainDraw.argb(0.25f * glow, 0x86B6E3));
            ChainAtlas.marker(2).draw(g, px - 3, py - 3);
            hit(px - 4, py - 4, 9, 9, null, List.of(Component.translatable(K + "war.you")));
        }
        g.pose().popPose();
    }

    private static List<Component> dominionTip(Dominion d) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal(d.name()).withStyle(ChatFormatting.BOLD));
        tip.add(Component.translatable(K + (d.seat() ? "dominion.seat" : "dominion.enclave"), ClientChain.factionName(d.faction()))
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(K + "dominion.at", d.x(), d.z()).withStyle(ChatFormatting.DARK_GRAY));
        if (d.contested()) tip.add(Component.translatable(K + "dominion.contested.tip").withStyle(ChatFormatting.RED));
        return tip;
    }

    /** Under the map: each power's towns, and the founded guilds in their colours. */
    private void drawRealmLegend(GuiGraphics g) {
        List<Component> labels = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        for (ChainFaction f : ChainFaction.FIXED) {
            int towns = ClientChain.dominions(f.id()).size();
            if (towns == 0) continue;
            labels.add(Component.translatable(K + "war.sectors", Component.translatable(K + f.id()), towns));
            colours.add(fill(f.id()));
        }
        for (ChainSyncPayload.Guild guild : ClientChain.guilds) {
            labels.add(Component.literal(guild.name()));
            colours.add(fill(guild.slot()));
        }
        drawLegend(g, labels, colours, MAP_Y + MAP_H + 8);
    }

    /** A side of a nations' war: a power by its chain id, else a nation (a guild's own in the guild's colour). */
    private static Component warSide(String s) {
        for (ChainFaction f : ChainFaction.FIXED) if (f.id().equals(s)) return named(s);
        for (ChainSyncPayload.Guild guild : ClientChain.guilds) {
            var n = ClientChain.guildNation(guild.slot());
            if (n != null && n.nation().equals(s)) return Component.literal(s).withColor(ClientChain.guildInk(guild.slot()));
        }
        return Component.literal(s).withColor(BookStyle.INK);
    }

    /** The nations' wars, then each power's towns: the page's head with Nations &amp; War. */
    private void realmRows(List<Row> rows, int w) {
        text(rows, Component.translatable(K + "realm.wars"), BookStyle.GOLD, w, 0);
        rule(rows);
        for (RealmWar war : ClientChain.realmWars()) {
            String kind = war.kind().toLowerCase(Locale.ROOT), phase = war.phase().toLowerCase(Locale.ROOT);
            List<Component> tip = List.of(Component.translatableWithFallback(K + "realm.kind." + kind, kind).copy().withStyle(ChatFormatting.BOLD),
                    Component.translatableWithFallback(K + "realm.kind." + kind + ".tip", "").copy().withStyle(ChatFormatting.GRAY));
            link(rows, ChainAtlas.icon(4), Component.translatable(K + "realm.war", warSide(war.a()), warSide(war.b())), BookStyle.INK, w, 0, null, tip);
            boolean active = "active".equals(phase);
            Component state = Component.translatable(K + "realm.war.state", Component.translatableWithFallback(K + "realm.kind." + kind, kind),
                    Component.translatableWithFallback(K + "realm.phase." + phase, phase).copy().withColor(active ? BookStyle.BLOOD : BookStyle.FADED),
                    war.scoreA(), war.scoreB());
            text(rows, state, BookStyle.FADED, w, 13);
            gap(rows, 3);
        }
        if (ClientChain.realmWars().isEmpty()) text(rows, Component.translatable(K + "realm.wars.none"), BookStyle.FADED, w, 0);

        gap(rows, 5);
        text(rows, Component.translatable(K + "dominions"), BookStyle.GOLD, w, 0);
        rule(rows);
        boolean any = false;
        for (ChainFaction f : ChainFaction.FIXED) {
            List<Dominion> towns = new ArrayList<>(ClientChain.dominions(f.id()));
            if (towns.isEmpty()) continue;
            any = true;
            towns.sort(java.util.Comparator.comparing((Dominion d) -> !d.seat()).thenComparing(Dominion::name));
            link(rows, ChainAtlas.marker(markerSide(f.id())), named(f.id()), BookStyle.INK, w, 0, null, null);
            for (Dominion d : towns) {
                Component line = Component.literal((d.seat() ? "★ " : "· ") + d.name()).withColor(d.seat() ? BookStyle.GOLD : BookStyle.INK)
                        .append(Component.literal("  ").append(Component.translatable(K + "dominion.at", d.x(), d.z())).withColor(BookStyle.FADED));
                if (d.contested()) line = line.copy().append(Component.literal("  ").append(Component.translatable(K + "dominion.contested"))
                        .withColor(BookStyle.BLOOD));
                link(rows, null, line, BookStyle.INK, w, 8, null, dominionTip(d));
            }
            gap(rows, 3);
        }
        if (!any) text(rows, Component.translatable(K + "dominions.none"), BookStyle.FADED, w, 0);
        gap(rows, 5);
    }

    /** A faction's name as ink on the page: a guild's own, darkened from its dye; the world's in their colour. */
    private static Component named(String faction) {
        return GuildPanel.inkName(faction);
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        int w = page.w - 4;
        var war = ClientChain.war;
        boolean ring = ClientChain.sectors();
        if (!ring) realmRows(rows, w);
        // With Nations & War the war is theirs (above): only the prestige stays here.
        text(rows, Component.translatable(K + (ring ? "war.score.title" : "realm.prestige")), BookStyle.GOLD, w, 0);
        rule(rows);
        if (ring && ClientChain.atWar()) text(rows, Component.translatable(K + "war.score", named(war.sideA()), war.score(war.sideA()),
                war.score(war.sideB()), named(war.sideB())), BookStyle.INK, w, 0);
        for (ChainFaction f : ChainFaction.values())
            if (f.side() != ChainFaction.Side.NONE || war.prestige(f.id()) > 0 || f.guild() && ClientChain.guild(f.id()) != null)
                text(rows, Component.translatable(K + "war.prestige.one", named(f.id()), war.prestige(f.id())), BookStyle.INK, w, 0);
        if (ring && !ClientChain.atWar()) text(rows, Component.translatable(K + "war.peace.desc"), BookStyle.FADED, w, 0);

        var task = ClientChain.task;
        if (task != null) {
            gap(rows, 5);
            text(rows, Component.translatable(K + "war.task"), BookStyle.GOLD, w, 0);
            rule(rows);
            Entry target = ClientChain.entry(task.targetFaction(), task.targetId());
            Component name = target == null ? Component.literal("?") : ClientChain.name(target);
            text(rows, Component.translatable("chain.supernaturalcraft.task." + task.kind().toLowerCase(Locale.ROOT), name, task.count(), task.until()),
                    BookStyle.INK, w, 0);
            text(rows, Component.translatable(K + "war.task.from", named(task.lord())), BookStyle.FADED, w, 0);
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
