package org.papiricoh.supernaturalcraft.memory.scenes;

import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;

import java.util.List;

/**
 * A memory staged on a plot's stage (v0.18, pure contract from the foundations; the scenes work fills it, {@code MemoryStage}
 * writes it through a private arena and restores the stage afterwards).
 * <p>Cells use the build kit's convention: relative to the stage centre, {@code dy = 0} is the first air layer above the stage
 * floor and {@code dy = -1} the floor itself (a scene may repaint it). No block entities (the arena cannot restore them).
 *
 * @param cells    the blocks of the scene
 * @param figures  who stands in it
 * @param entry    where the visitor appears ({dx, dy, dz})
 * @param tint     ARGB the client tints sky and fog with while inside
 * @param titleKey translation key of its title card
 * @param music    sound event id to play while inside, or empty for the plot's own
 */
public record Scene(List<ArenaCell> cells, List<Figure> figures, int[] entry, int tint, String titleKey, String music) {

    /** An empty stage (the visitor stands in the light, nothing else). */
    public static Scene empty(String titleKey) {
        return new Scene(List.of(), List.of(), new int[]{0, 0, -8}, 0xFFFFFFFF, titleKey, "");
    }
}
