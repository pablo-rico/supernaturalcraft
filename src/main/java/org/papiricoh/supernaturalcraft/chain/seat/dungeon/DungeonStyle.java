package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Wood;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;

/**
 * The look of one stronghold (pure): the Pit Castle in blackstone, basalt and nether brick with crimson wood and soul light, or
 * the Monastery in calcite, quartz and diorite under verdigris copper roofs. Every broad surface brush mixes several close blocks
 * (and a natural one the eye reads as the same stone) so no wall, floor or ceiling reads as a fill.
 */
final class DungeonStyle {

    final boolean pit;
    final Brush wall, plinth, floor, ceiling, court, path, rubble, vault;
    final Family trim, accent, roof, roofAlt, roofTrim;
    final Wood wood;
    /** A masonry wall id (railings, posts, merlons). */
    final String post;
    /** A full light block set into floors and ceilings. */
    final String light;
    final String[] panes;
    final String carpet;

    private DungeonStyle(boolean pit, Brush wall, Brush plinth, Brush floor, Brush ceiling, Brush court, Brush path, Brush rubble,
                         Brush vault, Family trim, Family accent, Family roof, Family roofAlt, Family roofTrim, Wood wood, String post,
                         String light, String[] panes, String carpet) {
        this.pit = pit;
        this.wall = wall;
        this.plinth = plinth;
        this.floor = floor;
        this.ceiling = ceiling;
        this.court = court;
        this.path = path;
        this.rubble = rubble;
        this.vault = vault;
        this.trim = trim;
        this.accent = accent;
        this.roof = roof;
        this.roofAlt = roofAlt;
        this.roofTrim = roofTrim;
        this.wood = wood;
        this.post = post;
        this.light = light;
        this.panes = panes;
        this.carpet = carpet;
    }

    static DungeonStyle of(ChainFaction faction, int s) {
        return faction == ChainFaction.DEMON ? castle(s) : monastery(s);
    }

    private static DungeonStyle castle(int s) {
        Brush wall = Palette.mix(s + 1, "minecraft:polished_blackstone_bricks", 30, "minecraft:cracked_polished_blackstone_bricks", 24,
                "minecraft:blackstone", 18, "minecraft:polished_blackstone", 9, "minecraft:deepslate", 14, "minecraft:nether_bricks", 4,
                "minecraft:gilded_blackstone", 1);
        Brush plinth = Palette.mix(s + 2, "minecraft:polished_basalt", 3, "minecraft:basalt", 3, "minecraft:blackstone", 2,
                "minecraft:deepslate", 2);
        Brush floor = Palette.checker(2,
                Palette.mix(s + 3, "minecraft:polished_blackstone", 5, "minecraft:polished_blackstone_bricks", 3, "minecraft:deepslate", 2),
                Palette.mix(s + 4, "minecraft:deepslate_tiles", 4, "minecraft:cracked_deepslate_tiles", 3, "minecraft:polished_deepslate", 2,
                        "minecraft:tuff", 1));
        Brush ceiling = Palette.mix(s + 5, "minecraft:blackstone", 3, "minecraft:polished_blackstone_bricks", 3, "minecraft:deepslate", 2,
                "minecraft:nether_bricks", 2);
        Brush court = Palette.mix(s + 6, "minecraft:blackstone", 28, "minecraft:deepslate", 18, "minecraft:soul_soil", 16,
                "minecraft:cobbled_deepslate", 12, "minecraft:basalt", 10, "minecraft:gravel", 8, "minecraft:netherrack", 6);
        Brush path = Palette.mix(s + 7, "minecraft:polished_blackstone_bricks", 3, "minecraft:cracked_polished_blackstone_bricks", 2,
                "minecraft:polished_blackstone", 2, "minecraft:deepslate", 2);
        Brush rubble = Palette.mix(s + 8, "minecraft:blackstone", 3, "minecraft:cracked_polished_blackstone_bricks", 2, "minecraft:basalt", 1,
                "minecraft:deepslate", 1);
        Brush vault = Palette.mix(s + 9, "minecraft:deepslate_bricks", 3, "minecraft:cracked_deepslate_bricks", 3, "minecraft:blackstone", 2,
                "minecraft:deepslate", 2, "minecraft:gilded_blackstone", 1);
        return new DungeonStyle(true, wall, plinth, floor, ceiling, court, path, rubble, vault, Family.POLISHED_BLACKSTONE_BRICKS,
                Family.RED_NETHER_BRICKS, Family.DEEPSLATE_TILES, Family.NETHER_BRICKS, Family.POLISHED_BLACKSTONE, Wood.CRIMSON,
                "minecraft:polished_blackstone_brick_wall", "minecraft:shroomlight",
                new String[]{"minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane", "minecraft:purple_stained_glass_pane"},
                "red");
    }

    private static DungeonStyle monastery(int s) {
        Brush wall = Palette.mix(s + 1, "minecraft:calcite", 38, "minecraft:quartz_bricks", 16, "minecraft:polished_diorite", 18,
                "minecraft:smooth_quartz", 12, "minecraft:diorite", 10);
        Brush plinth = Palette.mix(s + 2, "minecraft:stone_bricks", 3, "minecraft:polished_andesite", 3, "minecraft:andesite", 2,
                "minecraft:tuff_bricks", 2);
        Brush floor = Palette.checker(2,
                Palette.mix(s + 3, "minecraft:smooth_stone", 4, "minecraft:polished_diorite", 3, "minecraft:calcite", 2),
                Palette.mix(s + 4, "minecraft:quartz_bricks", 3, "minecraft:polished_andesite", 3, "minecraft:stone", 2));
        Brush ceiling = Palette.mix(s + 5, "minecraft:calcite", 3, "minecraft:quartz_bricks", 2, "minecraft:birch_planks", 2,
                "minecraft:polished_diorite", 2);
        Brush court = Palette.patches2(s + 6, 4, "minecraft:grass_block", 8, "minecraft:moss_block", 1);
        Brush path = Palette.mix(s + 7, "minecraft:gravel", 2, "minecraft:polished_andesite", 2, "minecraft:stone_bricks", 2,
                "minecraft:mossy_stone_bricks", 1, "minecraft:smooth_stone", 1);
        Brush rubble = Palette.mix(s + 8, "minecraft:calcite", 3, "minecraft:diorite", 2, "minecraft:stone_bricks", 1);
        Brush vault = Palette.mix(s + 9, "minecraft:quartz_bricks", 3, "minecraft:calcite", 3, "minecraft:chiseled_quartz_block", 1,
                "minecraft:polished_diorite", 2, "minecraft:diorite", 1);
        return new DungeonStyle(false, wall, plinth, floor, ceiling, court, path, rubble, vault, Family.SMOOTH_QUARTZ, Family.QUARTZ,
                Family.OXIDIZED_CUT_COPPER, Family.WEATHERED_CUT_COPPER, Family.SMOOTH_QUARTZ, Wood.SPRUCE, "minecraft:diorite_wall",
                "minecraft:sea_lantern",
                new String[]{"minecraft:white_stained_glass_pane", "minecraft:yellow_stained_glass_pane", "minecraft:light_blue_stained_glass_pane"},
                "white");
    }

    String lantern(boolean hanging) {
        return pit ? St.soulLantern(hanging) : St.lantern(hanging);
    }

    /** A pane of the faction's glass, picked by {@code i}. */
    String pane(int i) {
        return panes[Math.floorMod(i, panes.length)];
    }

    /** Steep roofs over the wings and towers. */
    Roofs.Style roofStyle(boolean ceiling) {
        return new Roofs.Style(roof, roofTrim, wall, 1, Roofs.Pitch.STEEP, 0, true, null, ceiling);
    }
}
