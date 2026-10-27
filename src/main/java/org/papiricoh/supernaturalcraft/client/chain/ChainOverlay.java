package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainNames;
import org.papiricoh.supernaturalcraft.chain.ChainRank;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;

import java.util.Locale;

/**
 * What the Chain of Command's HUD pieces share ({@link OfficerCard}, {@link OfficerBar}, {@link DungeonTitle}): the textures
 * of {@code textures/gui/chain/} (laid out by {@code tools/artgen/chain_gui_art.py}), the three sides' colours, the rank
 * medallions and level pips, and the officers' names. Every texture may be missing: callers draw a plain stand-in then.
 */
final class ChainOverlay {

    /**
     * Sides, the art's rows: 0 the Pit, 1 the Host, 2 the Fallen; 3 SucroCorp (v0.28), whose sprites sit in the Pit's slots of
     * its own {@code *_leviathan} sheets ({@link #sheet}, {@link #row}).
     */
    static final int PIT = 0, HOST = 1, FALLEN = 2, LEVIATHAN = 3;
    private static final String[] SIDES = {"pit", "host", "fallen", "leviathan"};

    static final ResourceLocation MEDALLIONS = tex("medallions"), PIPS = tex("pips"),
            MEDALLIONS_LEVIATHAN = tex("medallions_leviathan"), PIPS_LEVIATHAN = tex("pips_leviathan");
    static final ResourceLocation[] CARD = new ResourceLocation[4], BAR = new ResourceLocation[4], LORD = new ResourceLocation[4],
            COURT = {tex("court_pit"), tex("court_host"), tex("court_leviathan")};

    /**
     * v0.29: a guild's side is {@link #GUILD} plus its slot's index (4..7). Its sheets are greyscale twins ({@code *_guild},
     * sprites in the Pit's slots) drawn multiplied by the guild's colour ({@link #tint}); where no twin exists, the Fallen's ash
     * sheets stand in, tinted alike.
     */
    static final int GUILD = 4;
    static final ResourceLocation MEDALLIONS_GUILD = tex("medallions_guild"), PIPS_GUILD = tex("pips_guild"), COURT_GUILD = tex("court_guild"),
            EMBLEMS = SupernaturalCraft.asResource("textures/gui/book/guild_emblems.png");

    static boolean guild(int side) {
        return side >= GUILD;
    }

    static String guildSlot(int side) {
        return ChainFaction.GUILDS.get(Math.min(side - GUILD, ChainFaction.GUILDS.size() - 1)).id();
    }

    /** What a side's art is multiplied by (0xRRGGBB): a guild's colour, else white. */
    static int tint(int side) {
        return guild(side) ? ClientChain.guildTint(guildSlot(side)) : 0xFFFFFF;
    }

    /** The side whose art and colours a side borrows where it has none of its own: a guild the Fallen's ash. */
    static int base(int side) {
        return guild(side) ? FALLEN : side;
    }

    /** A row-per-side sheet for {@code side}: SucroCorp's own twin, a guild's (if drawn), else the shared one. */
    static ResourceLocation sheet(ResourceLocation shared, ResourceLocation leviathan, int side) {
        if (guild(side)) {
            ResourceLocation twin = leviathan.equals(MEDALLIONS_LEVIATHAN) ? MEDALLIONS_GUILD : leviathan.equals(PIPS_LEVIATHAN) ? PIPS_GUILD : null;
            return twin != null && has(twin) ? twin : shared;
        }
        return side == LEVIATHAN ? leviathan : shared;
    }

    /** The row {@code side} reads in its sheet ({@link #sheet}; a guild without a twin reads the Fallen's). */
    static int row(ResourceLocation sheet, int side) {
        if (guild(side)) return sheet.getPath().endsWith("_guild.png") ? PIT : FALLEN;
        return side == LEVIATHAN ? PIT : side;
    }

    static int row(int side) {
        return guild(side) ? PIT : side == LEVIATHAN ? PIT : side;
    }

    /** The officer card, bar and Lord's bar of a side (a guild's are the Fallen's, tinted). */
    static ResourceLocation card(int side) {
        return CARD[base(side)];
    }

    static ResourceLocation bar(int side) {
        return BAR[base(side)];
    }

    static ResourceLocation lord(int side) {
        return LORD[base(side)];
    }

    /** Per side: the accent (rules, rank), the name's ink, the stand-in plate, the health fill's ends; a guild's from its colour. */
    static int accent(int side) {
        return guild(side) ? ClientChain.guildGlow(guildSlot(side)) : ACCENT[side];
    }

    static int ink(int side) {
        return guild(side) ? ClientChain.mix(ClientChain.guildGlow(guildSlot(side)), 0xFFFFFF, 0.7f) : INK[side];
    }

    static int plate(int side) {
        return guild(side) ? ClientChain.mix(ClientChain.dyeColor(guildSlot(side)), 0x000000, 0.82f) : PLATE[side];
    }

    static int fillHi(int side) {
        return guild(side) ? ClientChain.mix(ClientChain.guildGlow(guildSlot(side)), 0xFFFFFF, 0.25f) : FILL_HI[side];
    }

    static int fillLo(int side) {
        return guild(side) ? ClientChain.mix(ClientChain.dyeColor(guildSlot(side)), 0x000000, 0.4f) : FILL_LO[side];
    }

    static {
        for (int i = 0; i < SIDES.length; i++) {
            CARD[i] = tex("card_" + SIDES[i]);
            BAR[i] = tex("bar_" + SIDES[i]);
            LORD[i] = tex("lord_" + SIDES[i]);
        }
    }

    /** Sheet sizes (see the art script). */
    static final int MEDALLION = 36, CARD_W = 256, CARD_H = 88, BAR_W = 176, BAR_H = 24, LORD_W = 248, LORD_H = 44,
            COURT_W = 256, COURT_H = 72, PIP = 6;

    /** Per side: the accent (rules, rank), the name's ink, the stand-in plate, and the health fill's light and dark ends. */
    static final int[] ACCENT = {0xD8352A, 0xF2D257, 0xB8B0A8, 0xD6A33A};
    static final int[] INK = {0xFFDCD2, 0xFFF6DE, 0xE4DED8, 0xDCEBE6};
    static final int[] PLATE = {0x1C0C0E, 0x1A2036, 0x1C1A1E, 0x0A1A18};
    static final int[] FILL_HI = {0xE8473A, 0xFFF4C8, 0xD0C8C0, 0x7FE3CF};
    static final int[] FILL_LO = {0x7A1012, 0xD9A92B, 0x5E5658, 0x24504A};

    private ChainOverlay() {
    }

    private static ResourceLocation tex(String name) {
        return SupernaturalCraft.asResource("textures/gui/chain/" + name + ".png");
    }

    static boolean has(ResourceLocation texture) {
        return MichaelGui.has(texture);
    }

    static int argb(float alpha, int rgb) {
        return MichaelGui.argb(alpha, rgb);
    }

    static void blit(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                     int tw, int th, float a) {
        MichaelGui.blit(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, 0xFFFFFF);
    }

    /** As {@link #blit}, multiplied by {@code side}'s colour ({@link #tint}). */
    static void blit(GuiGraphics g, int side, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                     int tw, int th, float a) {
        MichaelGui.blit(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, tint(side));
    }

    static int side(String faction, String branch) {
        if ("leviathan".equals(faction)) return LEVIATHAN;
        if (ClientChain.isGuild(faction)) return GUILD + ChainFaction.byId(faction).ordinal() - ChainFaction.GUILD_1.ordinal();
        return !"angel".equals(faction) ? PIT : "FALLEN".equals(branch) ? FALLEN : HOST;
    }

    static int side(Entry e) {
        return side(e.faction(), e.branch());
    }

    static int rank(String rank) {
        try {
            return ChainRank.valueOf(rank.toUpperCase(Locale.ROOT)).ordinal();
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

    static boolean lord(Entry e) {
        return rank(e.rank()) == ChainRank.LORD.ordinal();
    }

    static Component name(Entry e) {
        return Component.literal(ChainNames.name(ChainFaction.byId(e.faction()), e.nameSeed(), e.entityType()));
    }

    static Component rankName(String faction, String rank) {
        return Component.translatable(org.papiricoh.supernaturalcraft.chain.ChainRank.titleKey(faction, rank));
    }

    static Component epithet(Entry e) {
        return ClientChain.epithet(e);
    }

    /** The living officer of {@code faction} with this name and rank (the one a title card announces), if synced. */
    static @Nullable Entry find(String faction, String rank, String name) {
        Entry best = null;
        for (Entry e : ClientChain.officers) {
            if (!e.faction().equals(faction) || !e.rank().equalsIgnoreCase(rank)) continue;
            if (!ChainNames.name(ChainFaction.byId(faction), e.nameSeed()).equals(name)) continue;
            if (best == null || "ALIVE".equals(e.status())) best = e;
        }
        return best;
    }

    /** {@code faction}'s living Lord, a loyal one first (the Fallen serve the other side), or null while the throne is empty. */
    static @Nullable Entry lordOf(String faction) {
        Entry best = null;
        for (Entry e : ClientChain.officers) {
            if (!e.faction().equals(faction) || !lord(e) || !"ALIVE".equals(e.status())) continue;
            if (best == null || "FALLEN".equals(best.branch()) && !"FALLEN".equals(e.branch())) best = e;
        }
        return best;
    }

    /** The rank medallion at {@code size} GUI pixels, its top-left at (x, y); a plain disc and numeral without the art. */
    static void medallion(GuiGraphics g, int side, int rank, float x, float y, int size, float a) {
        if (a <= 0.01f) return;
        ResourceLocation sheet = sheet(MEDALLIONS, MEDALLIONS_LEVIATHAN, side);
        if (has(sheet)) {
            blit(g, side, sheet, x, y, size, size, rank * MEDALLION, row(sheet, side) * MEDALLION, MEDALLION, MEDALLION, 180, 108, a);
            if (guild(side) && sheet.equals(MEDALLIONS_GUILD)) {
                // The guild's emblem in the window its sheet leaves empty (untinted: a black guild's would vanish).
                int e = ClientChain.emblem(guildSlot(side));
                float k = size / (float) MEDALLION;
                blit(g, EMBLEMS, x + 10 * k, y + 10 * k, Math.round(16 * k), Math.round(16 * k), (e % 4) * 16, (e / 4) * 16, 16, 16, 64, 48, a);
            }
            return;
        }
        int x0 = Math.round(x), y0 = Math.round(y), r = size / 2;
        for (int dy = 0; dy < size; dy++) {
            float h = Mth.sqrt(Math.max(0, r * r - (dy + 0.5f - r) * (dy + 0.5f - r)));
            g.fill(x0 + Math.round(r - h), y0 + dy, x0 + Math.round(r + h), y0 + dy + 1, argb(a, dy < 2 || dy >= size - 2 ? accent(side) : plate(side)));
        }
        Component numeral = Component.literal(MichaelGui.roman(rank + 1));
        GuiDraw.centred(g, numeral, x0 + r, y0 + r - 4 * size / 36f, size / 36f, argb(a, accent(side)), true);
    }

    /**
     * Five level pips ending at {@code right} (lit up to the level), and (if {@code number}) the level as a number before them
     * past five. Returns
     * the left edge of what it drew.
     */
    static float pips(GuiGraphics g, int side, int level, float right, float y, float a, boolean number) {
        if (level < 0 || a <= 0.01f) return right;
        int lit = Math.min(5, level);
        float x = right - 5 * PIP;
        for (int i = 0; i < 5; i++) {
            boolean on = i < lit;
            ResourceLocation sheet = sheet(PIPS, PIPS_LEVIATHAN, side);
            if (has(sheet)) {
                blit(g, side, sheet, x + i * PIP, y, PIP, PIP, (row(sheet, side) * 2 + (on ? 1 : 0)) * PIP, 0, PIP, PIP, 36, 6, a);
            } else {
                int px = Math.round(x + i * PIP), py = Math.round(y);
                g.fill(px + 1, py + 1, px + 4, py + 4, argb(a, on ? accent(side) : 0x302A2C));
            }
        }
        if (number && level > 5) {
            Component n = Component.literal(String.valueOf(level));
            float w = GuiDraw.width(n, 1f);
            text(g, n, x - w - 2, y - 1, 1f, a, accent(side));
            return x - w - 2;
        }
        return x;
    }

    /** Text at alpha {@code a} (vanilla draws an all-but-transparent colour opaque, so it is skipped instead). */
    static void text(GuiGraphics g, Component text, float x, float y, float scale, float a, int rgb) {
        if (a > 0.02f) GuiDraw.text(g, text, x, y, scale, argb(a, rgb), true);
    }

    static void centred(GuiGraphics g, Component text, float cx, float y, float scale, float a, int rgb) {
        if (a > 0.02f) GuiDraw.centred(g, text, cx, y, scale, argb(a, rgb), true);
    }

    /** A stand-in plate: a dark fill inside a 1px accent frame. */
    static void plate(GuiGraphics g, int x0, int y0, int x1, int y1, int side, float a) {
        g.fill(x0, y0, x1, y1, argb(a * 0.85f, plate(side)));
        g.fill(x0, y0, x1, y0 + 1, argb(a, accent(side)));
        g.fill(x0, y1 - 1, x1, y1, argb(a, accent(side)));
        g.fill(x0, y0, x0 + 1, y1, argb(a, accent(side)));
        g.fill(x1 - 1, y0, x1, y1, argb(a, accent(side)));
    }

    static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
