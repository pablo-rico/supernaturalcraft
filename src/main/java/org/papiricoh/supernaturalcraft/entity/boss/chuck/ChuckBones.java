package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import java.util.ArrayList;
import java.util.List;

/**
 * Bone names the code relies on (pure). The art must build them; the renderer hides, shows or turns them.
 */
public final class ChuckBones {

    // --- the man (chuck.geo.json) ------------------------------------------------------------------------------
    /** One group per outfit: the renderer shows only the one {@link ChuckLook#outfit()} names. */
    public static final String OUTFIT_ROBE = "outfit_robe", OUTFIT_FLANNEL = "outfit_flannel", OUTFIT_SUIT = "outfit_suit";
    /** The whiskey glass in his right hand (hidden while it is in the air, and in the suit). */
    public static final String GLASS = "glass";
    public static final String HEAD = "head";

    // --- the light (chuck_divine.geo.json) ---------------------------------------------------------------------
    /** The heart of the light, at {@link ChuckGeometry#CORE_Y}: what the rings orbit, and what is exposed in chapter 4. */
    public static final String CORE = "core";
    /** The halo of typewriter keys behind his head, spun by the renderer. */
    public static final String HALO = "halo";
    /** The 4 tilted frames of the rings ({@code tilt_i}, pivot at the core) and the rings spinning in them ({@code ring_i}). */
    public static final int RINGS = 4, NODES = 3, KEYS = 24;

    public static String tilt(int ring) {
        return "tilt_" + ring;
    }

    public static String ring(int ring) {
        return "ring_" + ring;
    }

    /** The weak point {@code node} of ring {@code ring}: a child of {@code ring_i}, on the ring at angle node·120°. */
    public static String node(int ring, int node) {
        return "ring_" + ring + "_node_" + node;
    }

    /** {@code key_00} … {@code key_23}: children of {@link #HALO}. */
    public static String key(int i) {
        return String.format("key_%02d", i);
    }

    /** Every bone the renderer turns each frame: no clip may keyframe these. */
    public static final List<String> PROCEDURAL;

    static {
        List<String> out = new ArrayList<>(List.of(HALO, CORE));
        for (int r = 0; r < RINGS; r++) {
            out.add(tilt(r));
            out.add(ring(r));
            for (int n = 0; n < NODES; n++) out.add(node(r, n));
        }
        for (int k = 0; k < KEYS; k++) out.add(key(k));
        PROCEDURAL = List.copyOf(out);
    }

    private ChuckBones() {
    }
}
