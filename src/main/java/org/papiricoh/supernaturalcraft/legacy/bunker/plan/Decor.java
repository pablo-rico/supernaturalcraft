package org.papiricoh.supernaturalcraft.legacy.bunker.plan;

import java.util.List;
import java.util.Map;

/**
 * Something in the plan that is more than a block state (pure): an entity (item frame, painting, armour stand) or block-entity
 * data (a banner's pattern, a container's loot table, a sign's text, a vault's key). Coordinates are local; {@code facing} is a
 * plan direction ({@code north}…, or {@code up}/{@code down} for frames on floors and ceilings).
 *
 * <ul>
 *   <li>{@link Kind#ITEM_FRAME}: in an air cell, hung on the block behind it (opposite {@code facing}); {@code data} is an item id,
 *       optionally {@code item|rotation} (0–7). {@code glow} frames when {@code extra} is "glow".</li>
 *   <li>{@link Kind#PAINTING}: its anchor cell, facing out of the wall; {@code data} is a vanilla variant ({@link #PAINTINGS}).</li>
 *   <li>{@link Kind#ARMOR_STAND}: standing in the cell, looking {@code facing}; {@code data} is
 *       {@code head,chest,legs,feet,mainhand} item ids ({@code -} for none); {@code extra} a leather dye colour (hex) or "".</li>
 *   <li>{@link Kind#BANNER}: a banner block already in the plan; {@code data} is {@code base colour}, {@code extra}
 *       {@code pattern:colour,pattern:colour…} (patterns as banner-pattern ids, {@code supernaturalcraft:men_of_letters} too).</li>
 *   <li>{@link Kind#LOOT}: a container block already in the plan; {@code data} is a loot table id.</li>
 *   <li>{@link Kind#SIGN}: a sign block already in the plan; {@code data} the lines joined by {@code |} (front), {@code extra}
 *       a dye colour or "".</li>
 *   <li>{@link Kind#VAULT}: a vault block already in the plan; {@code data} the loot table, {@code extra} the key item.</li>
 * </ul>
 */
public record Decor(Kind kind, int x, int y, int z, String facing, String data, String extra) {

    public enum Kind {ITEM_FRAME, PAINTING, ARMOR_STAND, BANNER, LOOT, SIGN, VAULT}

    /** Width × height (blocks) of the vanilla painting variants the plan may use. */
    public static final Map<String, int[]> PAINTINGS = Map.ofEntries(
            Map.entry("kebab", new int[]{1, 1}), Map.entry("aztec", new int[]{1, 1}), Map.entry("alban", new int[]{1, 1}),
            Map.entry("aztec2", new int[]{1, 1}), Map.entry("bomb", new int[]{1, 1}), Map.entry("plant", new int[]{1, 1}),
            Map.entry("wasteland", new int[]{1, 1}), Map.entry("meditative", new int[]{1, 1}),
            Map.entry("pool", new int[]{2, 1}), Map.entry("courbet", new int[]{2, 1}), Map.entry("sea", new int[]{2, 1}),
            Map.entry("sunset", new int[]{2, 1}), Map.entry("creebet", new int[]{2, 1}),
            Map.entry("wanderer", new int[]{1, 2}), Map.entry("graham", new int[]{1, 2}), Map.entry("prairie_ride", new int[]{1, 2}),
            Map.entry("match", new int[]{2, 2}), Map.entry("bust", new int[]{2, 2}), Map.entry("stage", new int[]{2, 2}),
            Map.entry("void", new int[]{2, 2}), Map.entry("skull_and_roses", new int[]{2, 2}), Map.entry("wither", new int[]{2, 2}),
            Map.entry("baroque", new int[]{2, 2}), Map.entry("humble", new int[]{2, 2}),
            Map.entry("fighters", new int[]{4, 2}), Map.entry("changing", new int[]{4, 2}), Map.entry("finding", new int[]{4, 2}),
            Map.entry("lowmist", new int[]{4, 2}), Map.entry("passage", new int[]{4, 2}),
            Map.entry("bouquet", new int[]{3, 3}), Map.entry("cavebird", new int[]{3, 3}), Map.entry("cotan", new int[]{3, 3}),
            Map.entry("endboss", new int[]{3, 3}), Map.entry("fern", new int[]{3, 3}), Map.entry("owlemons", new int[]{3, 3}),
            Map.entry("sunflowers", new int[]{3, 3}), Map.entry("tides", new int[]{3, 3}),
            Map.entry("backyard", new int[]{3, 4}), Map.entry("pond", new int[]{3, 4}),
            Map.entry("skeleton", new int[]{4, 3}), Map.entry("donkey_kong", new int[]{4, 3}),
            Map.entry("pointer", new int[]{4, 4}), Map.entry("pigscene", new int[]{4, 4}), Map.entry("burning_skull", new int[]{4, 4}),
            Map.entry("orb", new int[]{4, 4}), Map.entry("unpacked", new int[]{4, 4}));

    public static Decor frame(int x, int y, int z, String facing, String item) {
        return new Decor(Kind.ITEM_FRAME, x, y, z, facing, item, "");
    }

    public static Decor glowFrame(int x, int y, int z, String facing, String item) {
        return new Decor(Kind.ITEM_FRAME, x, y, z, facing, item, "glow");
    }

    public static Decor painting(int x, int y, int z, String facing, String variant) {
        if (!PAINTINGS.containsKey(variant)) throw new IllegalArgumentException("unknown painting " + variant);
        return new Decor(Kind.PAINTING, x, y, z, facing, variant, "");
    }

    public static Decor armorStand(int x, int y, int z, String facing, String head, String chest, String legs, String feet, String hand, String dye) {
        return new Decor(Kind.ARMOR_STAND, x, y, z, facing, String.join(",", head, chest, legs, feet, hand), dye == null ? "" : dye);
    }

    public static Decor banner(int x, int y, int z, String base, String layers) {
        return new Decor(Kind.BANNER, x, y, z, "", base, layers);
    }

    public static Decor loot(int x, int y, int z, String table) {
        return new Decor(Kind.LOOT, x, y, z, "", table, "");
    }

    public static Decor sign(int x, int y, int z, String color, String... lines) {
        return new Decor(Kind.SIGN, x, y, z, "", String.join("|", lines), color == null ? "" : color);
    }

    public static Decor vault(int x, int y, int z, String table, String key) {
        return new Decor(Kind.VAULT, x, y, z, "", table, key);
    }

    /** The cells a painting covers (vanilla: centred, even sizes offset toward the wall's counter-clockwise side, then up). */
    public List<int[]> paintingCells() {
        int[] size = PAINTINGS.get(data);
        String along = St.ccw(facing);
        int ax = St.dx(along), az = St.dz(along);
        java.util.ArrayList<int[]> out = new java.util.ArrayList<>();
        for (int i = -((size[0] - 1) / 2); i <= size[0] / 2; i++) {
            for (int j = -((size[1] - 1) / 2); j <= size[1] / 2; j++) out.add(new int[]{x + ax * i, y + j, z + az * i});
        }
        return out;
    }
}
