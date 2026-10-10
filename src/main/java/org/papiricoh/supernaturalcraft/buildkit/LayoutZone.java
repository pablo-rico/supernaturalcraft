package org.papiricoh.supernaturalcraft.buildkit;

import java.util.Map;

/**
 * A named part of a layout (pure): the box its builder may write in and the anchors set while it was active (spots code looks
 * up by name: where an NPC stands, where the camera of a preview looks from, a door…). Anchors are {@code {x, y, z}} feet cells.
 */
public record LayoutZone(String name, Box bounds, Map<String, int[]> anchors) {

    public int[] anchor(String key) {
        int[] a = anchors.get(key);
        if (a == null) throw new IllegalArgumentException("zone " + name + " has no anchor " + key + " (has " + anchors.keySet() + ")");
        return a.clone();
    }
}
