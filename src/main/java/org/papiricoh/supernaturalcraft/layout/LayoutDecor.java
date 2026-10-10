package org.papiricoh.supernaturalcraft.layout;

/**
 * Something a layout places that is not a plain block state (v0.18, pure contract): a block entity's contents or a decorative
 * entity. Written after the blocks by whoever builds the layout (a permanent writer only: arenas cannot restore these).
 *
 * @param kind   {@code loot} (the container block at x,y,z gets loot table {@code data}), {@code item_frame} / {@code glow_item_frame}
 *               (item id {@code data}, hanging on the face {@code facing}), {@code painting} (variant id {@code data}),
 *               {@code armor_stand} ({@code data} = comma-separated item ids head,chest,legs,feet,mainhand), {@code banner_pattern}
 *               (the banner at x,y,z, {@code data} = pattern spec), {@code block_display} (block state {@code data}, scaled by
 *               {@code scale}), {@code item_display} (item id {@code data}), {@code sign} ({@code data} = up to 4 lines joined by '|'),
 *               {@code lectern_book} ({@code data} = a translation key), {@code pot} ({@code data} = sherd ids)
 * @param x      position relative to the layout's origin (same convention as {@link LayoutPoint})
 * @param y      see x
 * @param z      see x
 * @param facing a direction name ({@code north}...) or empty
 * @param data   kind-specific
 * @param scale  for displays (1 = a block); 1 otherwise
 */
public record LayoutDecor(String kind, double x, double y, double z, String facing, String data, float scale) {

    public static LayoutDecor of(String kind, int x, int y, int z, String facing, String data) {
        return new LayoutDecor(kind, x, y, z, facing, data, 1f);
    }
}
