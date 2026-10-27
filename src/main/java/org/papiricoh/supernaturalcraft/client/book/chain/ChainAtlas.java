package org.papiricoh.supernaturalcraft.client.book.chain;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas.Sprite;
import org.papiricoh.supernaturalcraft.client.chain.ClientChain;

/** Texture coordinates and sheet dimensions must match {@code tools/artgen/chain_book_art.py}. */
final class ChainAtlas {
    static final ResourceLocation WIDGETS = texture("chain_widgets");
    static final ResourceLocation PAPER_PIT = texture("chain_paper_pit");
    static final ResourceLocation PAPER_HOST = texture("chain_paper_host");
    static final ResourceLocation FRAME_PIT = texture("chain_frame_pit");
    static final ResourceLocation FRAME_HOST = texture("chain_frame_host");
    /**
     * v0.28: SucroCorp's side ({@link #LEVIATHAN}) has twins of every sheet ({@code *_leviathan}) with its sprites in the Pit's
     * slots: {@link #slot} gives the slot a side reads, {@link #widgets}/{@link #modalSheet}/{@link #portraitSheet} the sheet.
     */
    static final int LEVIATHAN = 3;
    static final ResourceLocation WIDGETS_LEVIATHAN = texture("chain_widgets_leviathan"), PAPER_LEVIATHAN = texture("chain_paper_leviathan"),
            FRAME_LEVIATHAN = texture("chain_frame_leviathan"), MODAL_LEVIATHAN = texture("chain_modal_leviathan"),
            PORTRAIT_LEVIATHAN = texture("chain_portrait_leviathan");

    /**
     * v0.29: a guild's side is {@link #GUILD} plus its slot's index (4..7). Its sheets ({@code *_guild}) are greyscale twins of
     * SucroCorp's, sprites in the Pit's slots, drawn multiplied by the guild's colour ({@link #tint}); its pages only lightly
     * ({@link #tintPale}), so ink still reads on them. Its medallions' windows are empty: the emblem goes there ({@link #emblem}).
     */
    static final int GUILD = 4;
    static final ResourceLocation WIDGETS_GUILD = texture("chain_widgets_guild"), PAPER_GUILD = texture("chain_paper_guild"),
            FRAME_GUILD = texture("chain_frame_guild"), MODAL_GUILD = texture("chain_modal_guild"),
            PORTRAIT_GUILD = texture("chain_portrait_guild");
    /** The guilds' emblems: 16×16, four to a row, light on transparent (index = emblem). */
    static final ResourceLocation EMBLEMS = texture("guild_emblems");
    static final int EMBLEM = 16, EMBLEMS_W = 64, EMBLEMS_H = 48;

    static boolean guild(int side) {
        return side >= GUILD;
    }

    /** The guild slot ("guild_1") a guild side stands for. */
    static String guildSlot(int side) {
        return ChainFaction.GUILDS.get(Math.min(side - GUILD, ChainFaction.GUILDS.size() - 1)).id();
    }

    /** The side a faction's book pages are bound in: 0 the Pit, 1 the Host, 3 SucroCorp, a guild's own. */
    static int side(ChainFaction f) {
        return switch (f) {
            case DEMON -> 0;
            case ANGEL -> 1;
            case LEVIATHAN -> LEVIATHAN;
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> GUILD + f.ordinal() - ChainFaction.GUILD_1.ordinal();
        };
    }

    static int slot(int side) {
        return side == LEVIATHAN || guild(side) ? 0 : side;
    }

    static ResourceLocation widgets(int side) {
        return guild(side) ? WIDGETS_GUILD : side == LEVIATHAN ? WIDGETS_LEVIATHAN : WIDGETS;
    }

    static ResourceLocation modalSheet(int side) {
        return guild(side) ? MODAL_GUILD : side == LEVIATHAN ? MODAL_LEVIATHAN : ChainDraw.MODAL;
    }

    static ResourceLocation portraitSheet(int side) {
        return guild(side) ? PORTRAIT_GUILD : side == LEVIATHAN ? PORTRAIT_LEVIATHAN : ChainDraw.PORTRAIT;
    }

    /** What a side's sheets are multiplied by (0xRRGGBB; white for the world's own factions). */
    static int tintOf(int side) {
        return guild(side) ? ClientChain.guildTint(guildSlot(side)) : 0xFFFFFF;
    }

    /** Draws what follows in a guild side's colour (nothing for the others); pair with {@link #untint}. */
    static void tint(GuiGraphics g, int side) {
        if (guild(side)) colour(g, tintOf(side));
    }

    /** As {@link #tint}, washed towards white: for the pages ink is written on. */
    static void tintPale(GuiGraphics g, int side) {
        if (guild(side)) colour(g, ClientChain.mix(tintOf(side), 0xFFFFFF, 0.65f));
    }

    /** Whether a guild side's binding comes out light (a pale dye): its ink is then dark, as on the Host's ivory. */
    static boolean light(int side) {
        if (!guild(side)) return side == 1;
        int c = tintOf(side);
        return 0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255) > 0.6f * 255;
    }

    static void untint(GuiGraphics g, int side) {
        if (guild(side)) g.setColor(1, 1, 1, 1);
    }

    static void colour(GuiGraphics g, int rgb) {
        g.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1);
    }

    /** A sprite of a side's sheets, in its colour. */
    static void draw(GuiGraphics g, int side, Sprite s, int x, int y) {
        tint(g, side);
        s.draw(g, x, y);
        untint(g, side);
    }

    /** A guild's emblem at {@code size} pixels, as drawn (never tinted: a black guild's would vanish), faded by {@code alpha}. */
    static void emblem(GuiGraphics g, int emblem, int x, int y, int size, float alpha) {
        int e = Math.floorMod(emblem, 12);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        if (alpha < 1) g.setColor(1, 1, 1, alpha);
        g.blit(EMBLEMS, x, y, size, size, (e % 4) * EMBLEM, (e / 4) * EMBLEM, EMBLEM, EMBLEM, EMBLEMS_W, EMBLEMS_H);
        if (alpha < 1) g.setColor(1, 1, 1, 1);
    }

    /** The tree's paper and vignette for the faction shown. */
    static ResourceLocation paper(ChainFaction f) {
        return switch (f) {
            case DEMON -> PAPER_PIT;
            case ANGEL -> PAPER_HOST;
            case LEVIATHAN -> PAPER_LEVIATHAN;
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> PAPER_GUILD;
        };
    }

    static ResourceLocation frame(ChainFaction f) {
        return switch (f) {
            case DEMON -> FRAME_PIT;
            case ANGEL -> FRAME_HOST;
            case LEVIATHAN -> FRAME_LEVIATHAN;
            case GUILD_1, GUILD_2, GUILD_3, GUILD_4 -> FRAME_GUILD;
        };
    }
    static final int MEDALLION = 36;
    static final int BACKDROP = 26;
    static final int FRAME_W = 384, FRAME_H = 248;
    static final int FRAME_SHEET_W = 384, FRAME_SHEET_H = 248;
    static final int PAPER = 64;
    static final int CARD_CORNER = 8;

    static final Sprite HOVER = sprite(144, 0, 36, 36);
    static final Sprite SELECTED = sprite(180, 0, 36, 36);
    static final Sprite VACANT = sprite(216, 0, 36, 36);
    static final Sprite CHAIN_H = sprite(0, 108, 8, 6);
    static final Sprite CHAIN_V = sprite(8, 108, 6, 8);
    static final Sprite CORD_H = sprite(16, 108, 8, 4);
    static final Sprite CORD_V = sprite(24, 108, 4, 8);
    static final Sprite BROKEN = sprite(32, 108, 12, 12);
    static final Sprite CROWN = sprite(0, 120, 10, 10);
    static final Sprite RETURNING = sprite(10, 120, 10, 10);
    static final Sprite DEAD = sprite(20, 120, 10, 10);
    static final Sprite KILL = sprite(30, 120, 10, 10);
    static final Sprite PIP = sprite(40, 120, 5, 5);
    static final Sprite EMBER = sprite(50, 120, 10, 10);
    static final Sprite CARD = sprite(0, 136, 48, 48);

    /** Faction: 0 Pit, 1 Host, 2 Fallen, 3 SucroCorp. Rank: Soldier, Captain, Lieutenant, Commander, Lord (the Lord's row is under the rims). */
    static Sprite medallion(int faction, int rank) {
        int f = slot(faction);
        return rank < 4 ? new Sprite(widgets(faction), rank * MEDALLION, f * MEDALLION, MEDALLION, MEDALLION, 256)
                : new Sprite(widgets(faction), 144 + f * MEDALLION, 62, MEDALLION, MEDALLION, 256);
    }

    /** Opaque disc drawn behind the mob; faction: 0 Pit, 1 Host, 2 Fallen, 3 SucroCorp. */
    static Sprite backdrop(int faction) {
        return new Sprite(widgets(faction), 144 + slot(faction) * BACKDROP, 36, BACKDROP, BACKDROP, 256);
    }

    static Sprite factionButton(boolean host, boolean on) {
        return sprite((host ? 128 : 0) + (on ? 64 : 0), 184, 64, 16);
    }

    static Sprite factionButton(ChainFaction f, boolean on) {
        if (f.guild()) return new Sprite(WIDGETS_GUILD, on ? 64 : 0, 184, 64, 16, 256);
        if (f == ChainFaction.LEVIATHAN) return new Sprite(WIDGETS_LEVIATHAN, on ? 64 : 0, 184, 64, 16, 256);
        return factionButton(f == ChainFaction.ANGEL, on);
    }

    static Sprite eventsButton(boolean on) {
        return sprite(on ? 20 : 0, 200, 20, 16);
    }

    static Sprite recenterButton(boolean on) {
        return sprite(on ? 60 : 40, 200, 20, 16);
    }

    // --- v0.27: the furniture's new buttons, the hunter's rims, and chain_modal.png / chain_portrait.png ------------

    static Sprite bondsButton(boolean on) {
        return sprite(on ? 100 : 80, 200, 20, 16);
    }

    static Sprite frontButton(boolean on) {
        return sprite(on ? 140 : 120, 200, 20, 16);
    }

    /** A post a hunter holds; the reader's own glows. */
    static final Sprite HUNTER_RIM = sprite(0, 216, 36, 36);
    static final Sprite MY_RIM = sprite(36, 216, 36, 36);
    static final Sprite PACT_LINK = sprite(72, 216, 9, 7);

    static final int FOLIO_CORNER = 12;

    /** The record's binding (side 0 Pit, 1 Host, 2 Fallen, 3 SucroCorp, a guild's) at any size. */
    static void folio(GuiGraphics g, int side, int x, int y, int w, int h) {
        tint(g, side);
        ChainDraw.nine(g, modalSheet(side), 256, 256, slot(side) * 48, 0, 48, 48, FOLIO_CORNER, x, y, w, h);
        untint(g, side);
    }

    static void page(GuiGraphics g, int side, int x, int y, int w, int h) {
        tintPale(g, side);
        ChainDraw.nine(g, modalSheet(side), 256, 256, 144 + slot(side) * 32, 0, 32, 32, 6, x, y, w, h);
        untint(g, side);
    }

    static void tab(GuiGraphics g, int side, boolean on, int x, int y, int w) {
        if (on) tintPale(g, side);
        else tint(g, side);
        ChainDraw.strip(g, modalSheet(side), 256, 256, (slot(side) * 2 + (on ? 1 : 0)) * 32, 48, 32, 16, 6, x, y, w);
        untint(g, side);
    }

    /** A message button, 14 high; state 0 off, 1 hover, 2 disabled. */
    static void button(GuiGraphics g, int side, int state, int x, int y, int w) {
        tint(g, side);
        ChainDraw.strip(g, modalSheet(side), 256, 256, state * 32, 80 + slot(side) * 14, 32, 14, 4, x, y, w);
        untint(g, side);
    }

    /** A heading's flourish: a rule {@code w} wide with a small diamond in its middle. */
    static void rule(GuiGraphics g, int side, int x, int y, int w) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        tint(g, side);
        for (int ox = 0; ox < w; ox += 24) g.blit(modalSheet(side), x + ox, y, 0, 124 + slot(side) * 6, Math.min(24, w - ox), 5, 256, 256);
        g.blit(modalSheet(side), x + w / 2 - 2, y, 30, 124 + slot(side) * 6, 5, 5, 256, 256);
        untint(g, side);
    }

    static Sprite close(boolean on) {
        return modal(on ? 204 : 192, 48, 12, 12);
    }

    /** 0 chronicle, 1 bonds, 2 duels, 3 actions, 4 the front. */
    static Sprite icon(int i) {
        return modal(i * 10, 64, 10, 10);
    }

    /** A bond's icon, or null for a mild one. */
    static Sprite bond(org.papiricoh.supernaturalcraft.chain.Bonds.Kind kind) {
        return switch (kind) {
            case LOYAL -> modal(60, 64, 9, 9);
            case RIVAL -> modal(70, 64, 9, 9);
            case GRUDGE -> modal(80, 64, 9, 9);
            case PACT -> modal(90, 64, 9, 9);
            case NONE -> null;
        };
    }

    /** 0 the Pit's seat, 1 the Host's, 2 the reader, 3 SucroCorp's seat, a guild side its standard (drawn tinted). */
    static Sprite marker(int i) {
        if (guild(i)) return new Sprite(MODAL_GUILD, 104, 64, 7, 7, 256);
        if (i == LEVIATHAN) return new Sprite(MODAL_LEVIATHAN, 104, 64, 7, 7, 256);
        return modal(104 + i * 8, 64, 7, 7);
    }

    static Sprite portraitFrame(int side) {
        return new Sprite(portraitSheet(side), slot(side) * 64, 0, 64, 80, 256);
    }

    static Sprite portraitBack(int side) {
        return new Sprite(portraitSheet(side), slot(side) * 64, 80, 64, 80, 256);
    }

    private static Sprite modal(int u, int v, int w, int h) {
        return new Sprite(ChainDraw.MODAL, u, v, w, h, 256);
    }

    private static ResourceLocation texture(String name) {
        return SupernaturalCraft.asResource("textures/gui/book/" + name + ".png");
    }

    private static Sprite sprite(int u, int v, int w, int h) {
        return new Sprite(WIDGETS, u, v, w, h, 256);
    }

    private ChainAtlas() {
    }
}
