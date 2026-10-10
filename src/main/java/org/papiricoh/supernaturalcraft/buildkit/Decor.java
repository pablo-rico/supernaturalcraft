package org.papiricoh.supernaturalcraft.buildkit;

import org.papiricoh.supernaturalcraft.layout.LayoutDecor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Something in a layout that is more than a block state (pure record; {@link #toContract} turns it into the mod's
 * {@link LayoutDecor}, which a permanent writer places after the blocks — arenas cannot restore these). Coordinates are canvas
 * cells; {@code facing} is a {@link Dir} id ({@code north}…, {@code up}/{@code down} for frames on floors and ceilings) or "".
 *
 * <ul>
 *   <li>{@link Kind#ITEM_FRAME}: in an air cell, hung on the block behind it (opposite {@code facing}); {@code data} an item id;
 *       {@code extra} "glow" for a glow frame.</li>
 *   <li>{@link Kind#PAINTING}: its anchor cell, facing out of the wall; {@code data} a vanilla variant ({@link #PAINTINGS}).</li>
 *   <li>{@link Kind#ARMOR_STAND}: standing in the cell, looking {@code facing}; {@code data} {@code head,chest,legs,feet,mainhand}
 *       item ids ({@code -} for none).</li>
 *   <li>{@link Kind#BANNER}: a banner block already in the layout; {@code data} {@code base|pattern:colour,pattern:colour…}.</li>
 *   <li>{@link Kind#LOOT}: a container block already in the layout; {@code data} a loot table id.</li>
 *   <li>{@link Kind#SIGN}: a sign block already in the layout; {@code data} up to 4 lines joined by {@code |}.</li>
 *   <li>{@link Kind#LECTERN_BOOK}: a lectern already in the layout; {@code data} a translation key.</li>
 *   <li>{@link Kind#POT}: a decorated pot already in the layout; {@code data} its sherd ids.</li>
 *   <li>{@link Kind#BLOCK_DISPLAY}: a block display; {@code data} a block state; {@code extra} {@code scale,tx,ty,tz} (scale and
 *       offset in blocks from the cell's corner).</li>
 *   <li>{@link Kind#ITEM_DISPLAY}: an item display at the cell's centre; {@code data} an item id; {@code extra} its scale.</li>
 * </ul>
 */
public record Decor(Kind kind, int x, int y, int z, String facing, String data, String extra) {

    public enum Kind {ITEM_FRAME, PAINTING, ARMOR_STAND, BANNER, LOOT, SIGN, LECTERN_BOOK, POT, BLOCK_DISPLAY, ITEM_DISPLAY}

    /** Width × height (blocks) of the vanilla 1.21.1 painting variants. */
    public static final Map<String, int[]> PAINTINGS = Map.ofEntries(
            Map.entry("kebab", new int[]{1, 1}), Map.entry("aztec", new int[]{1, 1}), Map.entry("alban", new int[]{1, 1}),
            Map.entry("aztec2", new int[]{1, 1}), Map.entry("bomb", new int[]{1, 1}), Map.entry("plant", new int[]{1, 1}),
            Map.entry("wasteland", new int[]{1, 1}), Map.entry("meditative", new int[]{1, 1}),
            Map.entry("pool", new int[]{2, 1}), Map.entry("courbet", new int[]{2, 1}), Map.entry("sea", new int[]{2, 1}),
            Map.entry("sunset", new int[]{2, 1}), Map.entry("creebet", new int[]{2, 1}),
            Map.entry("wanderer", new int[]{1, 2}), Map.entry("graham", new int[]{1, 2}), Map.entry("prairie_ride", new int[]{1, 2}),
            Map.entry("match", new int[]{2, 2}), Map.entry("bust", new int[]{2, 2}), Map.entry("stage", new int[]{2, 2}),
            Map.entry("void", new int[]{2, 2}), Map.entry("skull_and_roses", new int[]{2, 2}), Map.entry("wither", new int[]{2, 2}),
            Map.entry("baroque", new int[]{2, 2}), Map.entry("humble", new int[]{2, 2}), Map.entry("earth", new int[]{2, 2}),
            Map.entry("wind", new int[]{2, 2}), Map.entry("water", new int[]{2, 2}), Map.entry("fire", new int[]{2, 2}),
            Map.entry("fighters", new int[]{4, 2}), Map.entry("changing", new int[]{4, 2}), Map.entry("finding", new int[]{4, 2}),
            Map.entry("lowmist", new int[]{4, 2}), Map.entry("passage", new int[]{4, 2}),
            Map.entry("bouquet", new int[]{3, 3}), Map.entry("cavebird", new int[]{3, 3}), Map.entry("cotan", new int[]{3, 3}),
            Map.entry("endboss", new int[]{3, 3}), Map.entry("fern", new int[]{3, 3}), Map.entry("owlemons", new int[]{3, 3}),
            Map.entry("sunflowers", new int[]{3, 3}), Map.entry("tides", new int[]{3, 3}),
            Map.entry("backyard", new int[]{3, 4}), Map.entry("pond", new int[]{3, 4}),
            Map.entry("skeleton", new int[]{4, 3}), Map.entry("donkey_kong", new int[]{4, 3}),
            Map.entry("pointer", new int[]{4, 4}), Map.entry("pigscene", new int[]{4, 4}), Map.entry("burning_skull", new int[]{4, 4}),
            Map.entry("orb", new int[]{4, 4}), Map.entry("unpacked", new int[]{4, 4}));

    public static Decor frame(int x, int y, int z, Dir facing, String item) {
        return new Decor(Kind.ITEM_FRAME, x, y, z, facing.id(), St.full(item), "");
    }

    public static Decor glowFrame(int x, int y, int z, Dir facing, String item) {
        return new Decor(Kind.ITEM_FRAME, x, y, z, facing.id(), St.full(item), "glow");
    }

    public static Decor painting(int x, int y, int z, Dir facing, String variant) {
        if (!PAINTINGS.containsKey(variant)) throw new IllegalArgumentException("unknown painting " + variant);
        return new Decor(Kind.PAINTING, x, y, z, facing.id(), variant, "");
    }

    public static Decor armorStand(int x, int y, int z, Dir facing, String head, String chest, String legs, String feet, String hand) {
        return new Decor(Kind.ARMOR_STAND, x, y, z, facing.id(), String.join(",", head, chest, legs, feet, hand), "");
    }

    /** {@code layers}: {@code pattern:colour,pattern:colour…} (banner-pattern ids). */
    public static Decor banner(int x, int y, int z, String base, String layers) {
        return new Decor(Kind.BANNER, x, y, z, "", base + "|" + (layers == null ? "" : layers), "");
    }

    public static Decor loot(int x, int y, int z, String table) {
        return new Decor(Kind.LOOT, x, y, z, "", table, "");
    }

    public static Decor sign(int x, int y, int z, String... lines) {
        if (lines.length > 4) throw new IllegalArgumentException("a sign has 4 lines");
        return new Decor(Kind.SIGN, x, y, z, "", String.join("|", lines), "");
    }

    public static Decor lecternBook(int x, int y, int z, String translationKey) {
        return new Decor(Kind.LECTERN_BOOK, x, y, z, "", translationKey, "");
    }

    public static Decor pot(int x, int y, int z, String sherds) {
        return new Decor(Kind.POT, x, y, z, "", sherds, "");
    }

    /** A block display: {@code state} scaled by {@code scale}, its corner moved by (tx, ty, tz) blocks from the cell's corner. */
    public static Decor blockDisplay(int x, int y, int z, String state, double scale, double tx, double ty, double tz) {
        return new Decor(Kind.BLOCK_DISPLAY, x, y, z, "", St.check(St.full(state)), scale + "," + tx + "," + ty + "," + tz);
    }

    public static Decor itemDisplay(int x, int y, int z, Dir facing, String item, double scale) {
        return new Decor(Kind.ITEM_DISPLAY, x, y, z, facing.id(), St.full(item), String.valueOf(scale));
    }

    /** This decor moved by (dx, dy, dz). */
    public Decor shift(int dx, int dy, int dz) {
        return new Decor(kind, x + dx, y + dy, z + dz, facing, data, extra);
    }

    /** The mod's shared form ({@link LayoutDecor}). */
    public LayoutDecor toContract() {
        return switch (kind) {
            case ITEM_FRAME -> LayoutDecor.of(extra.equals("glow") ? "glow_item_frame" : "item_frame", x, y, z, facing, data);
            case PAINTING -> LayoutDecor.of("painting", x, y, z, facing, data);
            case ARMOR_STAND -> LayoutDecor.of("armor_stand", x, y, z, facing, data);
            case BANNER -> LayoutDecor.of("banner_pattern", x, y, z, facing, data);
            case LOOT -> LayoutDecor.of("loot", x, y, z, facing, data);
            case SIGN -> LayoutDecor.of("sign", x, y, z, facing, data);
            case LECTERN_BOOK -> LayoutDecor.of("lectern_book", x, y, z, facing, data);
            case POT -> LayoutDecor.of("pot", x, y, z, facing, data);
            case BLOCK_DISPLAY -> {
                String[] p = extra.split(",");
                yield new LayoutDecor("block_display", x + Double.parseDouble(p[1]), y + Double.parseDouble(p[2]), z + Double.parseDouble(p[3]),
                        facing, data, (float) Double.parseDouble(p[0]));
            }
            case ITEM_DISPLAY -> new LayoutDecor("item_display", x + 0.5, y + 0.5, z + 0.5, facing, data, (float) Double.parseDouble(extra));
        };
    }

    /** The kit's form of a contract decor (null for a kind it does not know). Display offsets come back relative to the cell. */
    public static Decor fromContract(LayoutDecor d) {
        int x = (int) Math.floor(d.x()), y = (int) Math.floor(d.y()), z = (int) Math.floor(d.z());
        String f = d.facing() == null ? "" : d.facing();
        return switch (d.kind().toLowerCase(Locale.ROOT)) {
            case "item_frame" -> new Decor(Kind.ITEM_FRAME, x, y, z, f, d.data(), "");
            case "glow_item_frame" -> new Decor(Kind.ITEM_FRAME, x, y, z, f, d.data(), "glow");
            case "painting" -> new Decor(Kind.PAINTING, x, y, z, f, d.data(), "");
            case "armor_stand" -> new Decor(Kind.ARMOR_STAND, x, y, z, f, d.data(), "");
            case "banner_pattern" -> new Decor(Kind.BANNER, x, y, z, f, d.data(), "");
            case "loot" -> new Decor(Kind.LOOT, x, y, z, f, d.data(), "");
            case "sign" -> new Decor(Kind.SIGN, x, y, z, f, d.data(), "");
            case "lectern_book" -> new Decor(Kind.LECTERN_BOOK, x, y, z, f, d.data(), "");
            case "pot" -> new Decor(Kind.POT, x, y, z, f, d.data(), "");
            case "block_display" -> new Decor(Kind.BLOCK_DISPLAY, x, y, z, f, d.data(), d.scale() + "," + (d.x() - x) + "," + (d.y() - y) + "," + (d.z() - z));
            case "item_display" -> new Decor(Kind.ITEM_DISPLAY, x, y, z, f, d.data(), String.valueOf(d.scale()));
            default -> null;
        };
    }

    /** The cells a painting covers (vanilla: centred, even sizes offset toward the wall's counter-clockwise side, then up). */
    public List<int[]> paintingCells() {
        int[] size = PAINTINGS.get(data);
        Dir along = Dir.of(facing).ccw();
        List<int[]> out = new ArrayList<>();
        for (int i = -((size[0] - 1) / 2); i <= size[0] / 2; i++) {
            for (int j = -((size[1] - 1) / 2); j <= size[1] / 2; j++) out.add(new int[]{x + along.dx * i, y + j, z + along.dz * i});
        }
        return out;
    }
}
