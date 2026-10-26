package org.papiricoh.supernaturalcraft.client.book.chain;

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

    /** Faction: 0 Pit, 1 Host, 2 Fallen. Rank: Soldier, Captain, Lieutenant, Commander, Lord (the Lord's row is under the rims). */
    static Sprite medallion(int faction, int rank) {
        return rank < 4 ? sprite(rank * MEDALLION, faction * MEDALLION, MEDALLION, MEDALLION)
                : sprite(144 + faction * MEDALLION, 62, MEDALLION, MEDALLION);
    }

    /** Opaque disc drawn behind the mob; faction: 0 Pit, 1 Host, 2 Fallen. */
    static Sprite backdrop(int faction) {
        return sprite(144 + faction * BACKDROP, 36, BACKDROP, BACKDROP);
    }

    static Sprite factionButton(boolean host, boolean on) {
        return sprite((host ? 128 : 0) + (on ? 64 : 0), 184, 64, 16);
    }

    static Sprite eventsButton(boolean on) {
        return sprite(on ? 20 : 0, 200, 20, 16);
    }

    static Sprite recenterButton(boolean on) {
        return sprite(on ? 60 : 40, 200, 20, 16);
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
