package org.papiricoh.supernaturalcraft.apocalypse;

import java.util.List;

/**
 * The contract between the Apocalypse's shared art and its code (v0.40): the Cage Shackle and Swan Song. Pure (paths relative
 * to {@code assets/supernaturalcraft/}); {@code ApocalypseAssetsTest} checks the files once the art lands.
 */
public final class ApocalypseAssets {

    private ApocalypseAssets() {
    }

    /** The Cage Shackle (item): a heavy black-iron cuff with a broken link, Enochian marks glowing faint red. */
    public static final String SHACKLE = "textures/item/cage_shackle.png";
    /** Swan Song's cinematic ({@code cinematics/swan_song.json}, anchored on Lucifer as he falls). */
    public static final String SWAN_SONG = "swan_song";
    /** The four rings' glow as they open the Cage (a particle, {@code supernaturalcraft:ring_light}). */
    public static final List<String> PARTICLES = List.of("ring_light");
    /** Sounds ({@code supernaturalcraft:apocalypse.<id>}): the Cage opening, the fall. */
    public static final List<String> SOUNDS = List.of("cage_open", "cage_fall");
}
