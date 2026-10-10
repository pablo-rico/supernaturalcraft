package org.papiricoh.supernaturalcraft.buildkit;

/** One planned block: canvas coordinates and a full block-state string (pure). */
public record Cell(int x, int y, int z, String state) {

    /** Maps a planned cell to whatever a consumer stores (e.g. {@code ArenaCell::new}, whose constructor has the same shape). */
    @FunctionalInterface
    public interface Mapper<T> {
        T map(int x, int y, int z, String state);
    }
}
