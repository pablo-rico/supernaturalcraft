package org.papiricoh.supernaturalcraft.entity.boss.amara;

import java.util.ArrayList;
import java.util.List;

/**
 * The layout of {@link AmaraAssets#HUD_SHEET} (256x256), "the closing eclipse": shared by {@code amara_gui_art.py} (its
 * docstring repeats it) and {@code client.amara.AmaraHud}. Pure; {@code AmaraHudSheetTest} keeps every region inside the sheet and
 * apart from the others.
 *
 * <p>How the HUD is drawn, top centre: the phase's {@link #SKIN} (the sun's face, 64x64, drawn at 2x), over it the
 * {@link #CORONA} (white-alpha, tinted per phase; the code wipes it radially: the lit arc is her remaining health), then the
 * {@link #DISC} (Amara's black disc) sliding over the sun phase by phase: the eclipse closes as the fight goes on (a sliver in
 * phase I, all but a thin ring in VI) and slides off at the reconciliation. Around it the six {@link #PIPS} (lunar phases, one per fight phase), under it the "Light of the World"
 * bar ({@link #WORLD_FRAME}/{@link #WORLD_FILL}: the global dying sun) and, in phase IV, Chuck's light ({@link #CHUCK_FRAME}/
 * {@link #CHUCK_FILL}). {@link #DRIPS} are 8 frames of ink falling from the disc's edge; {@link #SEAL} is the title card's seal.
 */
public final class AmaraHudSheet {

    private AmaraHudSheet() {
    }

    public static final int WIDTH = 256, HEIGHT = 256;

    /** A rectangle on the sheet. */
    public record Region(String name, int u, int v, int w, int h) {
        boolean overlaps(Region o) {
            return u < o.u + o.w && o.u < u + w && v < o.v + o.h && o.v < v + h;
        }
    }

    /** One sun face per phase, i = phase - 1: smoke (I), crayon drawing (II), filigree (III), two suns (IV), stars (V), dying (VI). */
    public static Region skin(int i) {
        return new Region("skin_" + i, (i % 4) * 64, (i / 4) * 64, 64, 64);
    }

    /** The corona ring (white-alpha; the code tints it and wipes it radially). */
    public static final Region CORONA = new Region("corona", 128, 64, 64, 64);
    /** Amara's disc: black, an ink edge, faint violet rim. */
    public static final Region DISC = new Region("disc", 192, 64, 64, 64);

    /** Lunar-phase pips, 12x12, i = phase - 1, in three states (row 0 dark, row 1 lit, row 2 the current one, glowing). */
    public static Region pip(int i, int state) {
        return new Region("pip_" + i + "_" + state, i * 12, 128 + state * 12, 12, 12);
    }

    /** The title card's seal: an eclipse with a closed eye. */
    public static final Region SEAL = new Region("seal", 192, 176, 48, 48);
    /** The "Light of the World" bar's frame (gold, worn) and its fill (white, tinted from dawn gold to ember red). */
    public static final Region WORLD_FRAME = new Region("world_frame", 0, 168, 182, 7);
    public static final Region WORLD_FILL = new Region("world_fill", 0, 176, 178, 3);
    /** Chuck's light (phase IV): a smaller frame of ink and paper, its fill white. */
    public static final Region CHUCK_FRAME = new Region("chuck_frame", 0, 184, 120, 7);
    public static final Region CHUCK_FILL = new Region("chuck_fill", 0, 192, 116, 3);

    /** Ink drips off the disc, 8 frames of 16x16. */
    public static Region drip(int frame) {
        return new Region("drip_" + frame, frame * 16, 208, 16, 16);
    }

    public static final int PHASES = 6, PIP_STATES = 3, DRIP_FRAMES = 8;

    /** Every region of the sheet. */
    public static List<Region> all() {
        List<Region> out = new ArrayList<>();
        for (int i = 0; i < PHASES; i++) out.add(skin(i));
        out.add(CORONA);
        out.add(DISC);
        for (int i = 0; i < PHASES; i++) for (int s = 0; s < PIP_STATES; s++) out.add(pip(i, s));
        out.add(SEAL);
        out.add(WORLD_FRAME);
        out.add(WORLD_FILL);
        out.add(CHUCK_FRAME);
        out.add(CHUCK_FILL);
        for (int f = 0; f < DRIP_FRAMES; f++) out.add(drip(f));
        return out;
    }

    /** @return the first pair of regions that overlap or leave the sheet, or null if the layout is sound */
    public static String problem() {
        List<Region> all = all();
        for (Region r : all) {
            if (r.u < 0 || r.v < 0 || r.u + r.w > WIDTH || r.v + r.h > HEIGHT) return r.name + " leaves the sheet";
        }
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                if (all.get(i).overlaps(all.get(j))) return all.get(i).name + " overlaps " + all.get(j).name;
            }
        }
        return null;
    }
}
