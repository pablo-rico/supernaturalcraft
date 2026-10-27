package org.papiricoh.supernaturalcraft.client.book.chain;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas.Sprite;

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

    static int slot(int side) {
        return side == LEVIATHAN ? 0 : side;
    }

    static ResourceLocation widgets(int side) {
        return side == LEVIATHAN ? WIDGETS_LEVIATHAN : WIDGETS;
    }

    static ResourceLocation modalSheet(int side) {
        return side == LEVIATHAN ? MODAL_LEVIATHAN : ChainDraw.MODAL;
    }

    static ResourceLocation portraitSheet(int side) {
        return side == LEVIATHAN ? PORTRAIT_LEVIATHAN : ChainDraw.PORTRAIT;
    }

    /** The tree's paper and vignette for the faction shown. */
    static ResourceLocation paper(org.papiricoh.supernaturalcraft.chain.ChainFaction f) {
        return switch (f) {
            case DEMON -> PAPER_PIT;
            case ANGEL -> PAPER_HOST;
            case LEVIATHAN -> PAPER_LEVIATHAN;
        };
    }

    static ResourceLocation frame(org.papiricoh.supernaturalcraft.chain.ChainFaction f) {
        return switch (f) {
            case DEMON -> FRAME_PIT;
            case ANGEL -> FRAME_HOST;
            case LEVIATHAN -> FRAME_LEVIATHAN;
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

    static Sprite factionButton(org.papiricoh.supernaturalcraft.chain.ChainFaction f, boolean on) {
        if (f == org.papiricoh.supernaturalcraft.chain.ChainFaction.LEVIATHAN) return new Sprite(WIDGETS_LEVIATHAN, on ? 64 : 0, 184, 64, 16, 256);
        return factionButton(f == org.papiricoh.supernaturalcraft.chain.ChainFaction.ANGEL, on);
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

    /** The record's binding (side 0 Pit, 1 Host, 2 Fallen) at any size. */
    static void folio(GuiGraphics g, int side, int x, int y, int w, int h) {
        ChainDraw.nine(g, modalSheet(side), 256, 256, slot(side) * 48, 0, 48, 48, FOLIO_CORNER, x, y, w, h);
    }

    static void page(GuiGraphics g, int side, int x, int y, int w, int h) {
        ChainDraw.nine(g, modalSheet(side), 256, 256, 144 + slot(side) * 32, 0, 32, 32, 6, x, y, w, h);
    }

    static void tab(GuiGraphics g, int side, boolean on, int x, int y, int w) {
        ChainDraw.strip(g, modalSheet(side), 256, 256, (slot(side) * 2 + (on ? 1 : 0)) * 32, 48, 32, 16, 6, x, y, w);
    }

    /** A message button, 14 high; state 0 off, 1 hover, 2 disabled. */
    static void button(GuiGraphics g, int side, int state, int x, int y, int w) {
        ChainDraw.strip(g, modalSheet(side), 256, 256, state * 32, 80 + slot(side) * 14, 32, 14, 4, x, y, w);
    }

    /** A heading's flourish: a rule {@code w} wide with a small diamond in its middle. */
    static void rule(GuiGraphics g, int side, int x, int y, int w) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        for (int ox = 0; ox < w; ox += 24) g.blit(modalSheet(side), x + ox, y, 0, 124 + slot(side) * 6, Math.min(24, w - ox), 5, 256, 256);
        g.blit(modalSheet(side), x + w / 2 - 2, y, 30, 124 + slot(side) * 6, 5, 5, 256, 256);
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

    /** 0 the Pit's seat, 1 the Host's, 2 the reader, 3 SucroCorp's seat. */
    static Sprite marker(int i) {
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
