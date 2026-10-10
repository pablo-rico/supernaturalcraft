package org.papiricoh.supernaturalcraft.memory.scenes;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseLayout;

import java.util.List;
import java.util.Locale;

import static org.papiricoh.supernaturalcraft.memory.scenes.MemoryScenes.floor;
import static org.papiricoh.supernaturalcraft.memory.scenes.MemoryScenes.litter;

/**
 * The boss victories' scenes (v0.18, pure): a small rendering of each fight's ground — its palette, one or two telling pieces
 * (the Colt's rails, Lilith's headstones, the Cage's chains, a TV set, a library, a storm-wrecked house, an office, a blank page…)
 * — with the fallen enemy (the focus) before the hunter, Dean and Sam.
 */
final class BossScenes {

    private BossScenes() {
    }

    /** Draws the scene for boss {@code id} (a {@code BossProgression.Boss} id); returns the tint. */
    static int draw(Canvas c, List<Figure> figures, String id, int s) {
        String boss = id.toLowerCase(Locale.ROOT);
        String entity = "supernaturalcraft:" + boss;
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (b.name().equalsIgnoreCase(boss)) entity = "supernaturalcraft:" + b.entity;
        int tint = switch (boss) {
            case "azazel" -> azazel(c, s);
            case "lilith" -> lilith(c, s);
            case "lucifer", "lucifer_uncaged" -> cage(c, s);
            case "gabriel" -> tvSet(c, s);
            case "war", "famine", "pestilence", "death" -> horseman(c, boss, s);
            case "raphael" -> storm(c, s);
            case "broken_chorus", "metatron" -> library(c, boss.equals("broken_chorus"), s);
            case "amara" -> eclipse(c, s);
            case "michael" -> heaven(c, s);
            case "naomi", "zachariah" -> office(c, boss.equals("naomi"), s);
            case "chuck" -> blankPage(c, s);
            default -> field(c, s);
        };
        figures.add(MemoryScenes.fig(0, -4, 0f, entity, "fallen", true));
        figures.add(MemoryScenes.fig(0, 3, 180f, "@owner", "strike", false));
        figures.add(MemoryScenes.fig(-3, 5, 200f, "@ally:dean", "stand", false));
        figures.add(MemoryScenes.fig(3, 5, 160f, "@ally:sam", "stand", false));
        return tint;
    }

    /** A ring of {@code n} posts at radius {@code r} (skipping the entry's lane), {@code h} tall. */
    static void ring(Canvas c, double r, int n, int h, String post, String cap) {
        for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2 / n;
            int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
            if (z > 6 && Math.abs(x) <= 3) continue;
            for (int y = 0; y < h; y++) c.set(x, y, z, post);
            if (cap != null) c.set(x, h, z, cap);
        }
    }

    private static int azazel(Canvas c, int s) {
        floor(c, Palette.patches2(s, 3, "minecraft:coarse_dirt", 4, "minecraft:dirt", 2, "minecraft:yellow_terracotta", 1, "minecraft:sand", 1));
        // The Colt's trap: a circle and a five-pointed star of rails round the fallen demon.
        for (int k = 0; k < 360; k += 4) {
            double a = Math.toRadians(k);
            c.set((int) Math.round(Math.cos(a) * 6), 0, (int) Math.round(Math.sin(a) * 6) - 4, "minecraft:rail[shape=north_south,waterlogged=false]");
        }
        for (int k = 0; k < 5; k++) {
            double a0 = Math.toRadians(-90 + k * 144), a1 = Math.toRadians(-90 + (k + 1) * 144);
            for (double t = 0; t <= 1; t += 0.05) {
                int x = (int) Math.round(Math.cos(a0) * 6 * (1 - t) + Math.cos(a1) * 6 * t);
                int z = (int) Math.round(Math.sin(a0) * 6 * (1 - t) + Math.sin(a1) * 6 * t) - 4;
                c.set(x, 0, z, "minecraft:rail[shape=east_west,waterlogged=false]");
            }
        }
        for (CaseLayout.Cell cell : CaseLayout.piece("barn")) c.set(cell.x() - 11, cell.y(), cell.z() - 6, cell.state());
        litter(c, s + 1, 0.25, "minecraft:dead_bush", 2, "minecraft:short_grass", 3);
        return 0xFFE8D070;
    }

    private static int lilith(Canvas c, int s) {
        floor(c, Palette.patches2(s, 3, "minecraft:grass_block", 5, "minecraft:podzol", 2, "minecraft:coarse_dirt", 1, "minecraft:moss_block", 1));
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5;
            int x = (int) Math.round(Math.cos(a) * 11), z = (int) Math.round(Math.sin(a) * 11) - 2;
            if (z > 6 && Math.abs(x) <= 3) continue;
            Dir face = Dir.toward(-x, -z);
            c.set(x, 0, z, St.wall("minecraft:mossy_stone_brick_wall"));
            c.set(x, 1, z, Noise.chance(x, 0, z, s, 0.4) ? "minecraft:cracked_stone_bricks" : "minecraft:chiseled_stone_bricks");
            c.set(x, 2, z, Family.STONE_BRICKS.slabBottom());
            c.set(x + face.dx, 0, z + face.dz, St.candle("white", 1 + k % 3, true));
        }
        Trees.deadOak(c, -13, 0, -10, 1, false, s + 2);
        litter(c, s + 3, 0.35, "minecraft:short_grass", 5, "minecraft:fern", 2, "minecraft:lily_of_the_valley", 1);
        return 0xFFF0F0FF;
    }

    private static int cage(Canvas c, int s) {
        floor(c, Palette.patches2(s, 2, "minecraft:polished_blackstone", 3, "minecraft:deepslate_tiles", 2, "minecraft:cracked_deepslate_tiles", 1,
                "minecraft:basalt[axis=y]", 1));
        for (int x = -5; x <= 5; x++) {
            for (int z = -9; z <= 1; z++) {
                double d = Math.hypot(x, z + 4);
                if (d > 5.4) continue;
                c.set(x, -1, z, d < 1.5 ? "minecraft:magma_block" : "minecraft:polished_blackstone_bricks");
                if (d > 4.4) c.set(x, 0, z, d > 4.9 ? St.bars() : Family.POLISHED_BLACKSTONE_BRICKS.slabBottom());
            }
        }
        ring(c, 13, 8, 9, St.axis("minecraft:polished_basalt", "y"), "minecraft:chiseled_polished_blackstone");
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int x = (int) Math.round(Math.cos(a) * 7), z = (int) Math.round(Math.sin(a) * 7) - 4;
            for (int y = 3; y <= 12; y++) c.set(x, y, z, St.chain("y"));
            c.set(x, 13, z, "minecraft:polished_blackstone_bricks");
            c.set(x, 2, z, St.soulLantern(true));
        }
        return 0xFFA03020;
    }

    private static int tvSet(Canvas c, int s) {
        floor(c, Palette.checker(2, Brush.of("minecraft:black_concrete"), Brush.of("minecraft:white_concrete")));
        for (int x = -8; x <= 8; x++) {
            for (int y = 0; y <= 6; y++) c.set(x, y, -12, y == 6 ? "minecraft:yellow_concrete" : (x + y) % 2 == 0 ? "minecraft:light_blue_concrete" : "minecraft:cyan_concrete");
        }
        for (int x = -3; x <= 3; x++) c.set(x, 0, -9, St.stairs("minecraft:crimson_stairs", Dir.NORTH, false));
        for (int x : new int[]{-9, 9}) {
            for (int y = 0; y <= 5; y++) c.set(x, y, -4, St.fence("minecraft:dark_oak_fence"));
            c.set(x, 6, -4, St.endRod(Dir.DOWN));
        }
        for (int z = 0; z <= 6; z += 2) {
            for (int x = -12; x <= -8; x++) c.set(x, z / 2, z, St.stairs("minecraft:dark_oak_stairs", Dir.WEST, false));
            for (int x = 8; x <= 12; x++) c.set(x, z / 2, z, St.stairs("minecraft:dark_oak_stairs", Dir.EAST, false));
        }
        return 0xFFFFFFFF;
    }

    private static int horseman(Canvas c, String which, int s) {
        switch (which) {
            case "war" -> {
                floor(c, Palette.patches2(s, 2, "minecraft:coarse_dirt", 4, "minecraft:mud", 2, "minecraft:gravel", 1, "minecraft:red_terracotta", 1));
                for (int x = -12; x <= 12; x++) {
                    if (Noise.chance(x, 0, -10, s, 0.25)) continue;
                    c.set(x, 0, -10, "minecraft:packed_mud");
                    c.set(x, 1, -10, Noise.chance(x, 1, -10, s, 0.5) ? "minecraft:packed_mud" : "minecraft:mud_bricks");
                    if (x % 4 == 0) c.set(x, 2, -10, St.fence("minecraft:spruce_fence"));
                }
                litter(c, s + 1, 0.1, "minecraft:dead_bush", 2, "minecraft:short_grass", 1);
                return 0xFFC04030;
            }
            case "famine" -> {
                floor(c, Palette.patches2(s, 2, "minecraft:coarse_dirt", 3, "minecraft:farmland[moisture=0]", 3, "minecraft:dirt", 1));
                for (int x = -12; x <= 12; x++) for (int z = -14; z <= -4; z++) if (Noise.chance(x, 0, z, s + 2, 0.5) && "minecraft:farmland[moisture=0]".equals(c.get(x, -1, z))) c.set(x, 0, z, St.crop("minecraft:wheat", 1));
                c.set(-6, 0, 0, St.axis("minecraft:hay_block", "x"));
                c.set(-5, 0, 0, St.axis("minecraft:hay_block", "x"));
                litter(c, s + 1, 0.15, "minecraft:dead_bush", 3);
                return 0xFFC8B070;
            }
            case "pestilence" -> {
                floor(c, Palette.patches2(s, 2, "minecraft:mud", 3, "minecraft:moss_block", 2, "minecraft:muddy_mangrove_roots", 1));
                for (int x = -8; x <= 8; x++) {
                    for (int z = -14; z <= -6; z++) {
                        if (Math.hypot(x / 1.6, z + 10) < 3.2) c.set(x, -1, z, St.water());
                    }
                }
                for (int x = -8; x <= 8; x++) for (int z = -14; z <= -6; z++) if ("minecraft:water[level=0]".equals(c.get(x, -1, z)) && Noise.chance(x, 0, z, s, 0.2)) c.set(x, 0, z, "minecraft:lily_pad");
                litter(c, s + 1, 0.2, "minecraft:fern", 2, "minecraft:brown_mushroom", 1, "minecraft:red_mushroom", 1);
                return 0xFF90A050;
            }
            default -> {
                floor(c, Palette.patches2(s, 2, "minecraft:gravel", 3, "minecraft:light_gray_concrete_powder", 2, "minecraft:tuff", 1));
                Trees.deadOak(c, -10, 0, -8, 2, false, s + 3);
                Trees.deadOak(c, 11, 0, -6, 1, false, s + 4);
                for (int k = 0; k < 6; k++) {
                    int x = -8 + k * 3, z = -12;
                    c.set(x, 0, z, St.wall("minecraft:cobblestone_wall"));
                    c.set(x, 1, z, Family.STONE.slabBottom());
                }
                return 0xFFA0A0A0;
            }
        }
    }

    private static int storm(Canvas c, int s) {
        floor(c, Palette.patches2(s, 2, "minecraft:spruce_planks", 3, "minecraft:dark_oak_planks", 2, "minecraft:coarse_dirt", 1));
        for (int x = -10; x <= 10; x++) {
            int h = 1 + Noise.pick(x, 0, -12, s, 4);
            for (int y = 0; y < h; y++) c.set(x, y, -12, Noise.chance(x, y, -12, s + 1, 0.3) ? "minecraft:cracked_stone_bricks" : "minecraft:stone_bricks");
        }
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            c.set((int) Math.round(Math.cos(a) * 5), -1, (int) Math.round(Math.sin(a) * 5) - 4, "minecraft:magma_block");
        }
        c.set(4, 0, -9, St.rod("minecraft:lightning_rod", Dir.UP));
        return 0xFF5060A0;
    }

    private static int library(Canvas c, boolean chorus, int s) {
        floor(c, Palette.checker(2, Brush.of(chorus ? "minecraft:smooth_quartz" : "minecraft:dark_oak_planks"), Brush.of(chorus ? "minecraft:calcite" : "minecraft:spruce_planks")));
        for (int row = 0; row < 3; row++) {
            int z = -14 + row * 4;
            for (int x = -12; x <= 12; x++) {
                if (Math.abs(x) <= 2) continue;
                int h = 4 - row;
                for (int y = 0; y < h; y++) c.set(x, y, z, chorus ? (y == h - 1 ? "minecraft:amethyst_block" : St.axis("minecraft:quartz_pillar", "y")) : x % 5 == 0 ? St.log("minecraft:stripped_dark_oak_log") : "minecraft:bookshelf");
            }
        }
        litter(c, s + 1, 0.12, St.carpet("white"), 3, St.candle("white", 2, true), 1);
        return chorus ? 0xFFE0D8FF : 0xFFE8D0A0;
    }

    private static int eclipse(Canvas c, int s) {
        floor(c, (x, y, z) -> Math.floorMod((int) Math.round(Math.atan2(z + 4, x) * 8 / Math.PI), 2) == 0 ? "minecraft:black_concrete" : "minecraft:blackstone");
        ring(c, 11, 6, 6, "minecraft:obsidian", "minecraft:crying_obsidian");
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI * 2 / 3 + 0.4;
            int x = (int) Math.round(Math.cos(a) * 6), z = (int) Math.round(Math.sin(a) * 6) - 4;
            c.set(x, -1, z, "minecraft:sea_lantern");
            c.set(x, 0, z, "minecraft:white_stained_glass");
        }
        return 0xFF202030;
    }

    private static int heaven(Canvas c, int s) {
        floor(c, Palette.patches2(s, 3, "minecraft:grass_block", 6, "minecraft:moss_block", 1));
        for (int x = -3; x <= 3; x++) for (int z = -10; z <= 10; z++) c.set(x, -1, z, Math.abs(x) == 3 ? "minecraft:smooth_quartz" : "minecraft:calcite");
        ring(c, 12, 10, 6, St.axis("minecraft:quartz_pillar", "y"), "minecraft:gold_block");
        Trees.cherry(c, -10, 0, -9, 1, s + 1);
        Trees.cherry(c, 10, 0, -10, 1, s + 2);
        litter(c, s + 3, 0.4, "minecraft:white_tulip", 2, "minecraft:lily_of_the_valley", 2, "minecraft:short_grass", 4, "minecraft:pink_petals", 2);
        return 0xFFFFF0C0;
    }

    private static int office(Canvas c, boolean clinic, int s) {
        floor(c, clinic ? Palette.checker(2, Brush.of("minecraft:white_concrete"), Brush.of("minecraft:polished_diorite"))
                : Palette.checker(2, Brush.of("minecraft:light_gray_wool"), Brush.of("minecraft:gray_wool")));
        for (int dx = -9; dx <= 9; dx += 6) {
            for (int dz = -12; dz <= -6; dz += 6) {
                if (clinic) {
                    c.set(dx, 0, dz, Family.SMOOTH_QUARTZ.stairs(Dir.NORTH, false));
                    c.set(dx, 0, dz + 1, Family.SMOOTH_QUARTZ.slabBottom());
                    c.set(dx + 1, 0, dz, St.rod("minecraft:lightning_rod", Dir.UP));
                } else {
                    c.set(dx, 0, dz, St.slabTop("minecraft:birch_slab"));
                    c.set(dx + 1, 0, dz, St.slabTop("minecraft:birch_slab"));
                    c.set(dx, 1, dz, St.trapdoor("minecraft:iron_trapdoor", Dir.SOUTH, false, true));
                    c.set(dx, 0, dz + 1, St.stairs("minecraft:birch_stairs", Dir.SOUTH, false));
                    c.set(dx + 2, 0, dz, St.of("supernaturalcraft:filing_cabinet", "facing", "south", "number", String.valueOf(1 + Math.floorMod(dx + dz, 4))));
                }
            }
        }
        litter(c, s + 1, 0.08, St.carpet("white"), 1);
        return clinic ? 0xFFE8F4FF : 0xFFF0E8D0;
    }

    private static int blankPage(Canvas c, int s) {
        floor(c, Palette.patches2(s, 3, "minecraft:white_concrete", 5, "minecraft:white_concrete_powder", 2, "minecraft:calcite", 1));
        for (int x = -2; x <= 2; x++) c.set(x, 0, -9, x == 0 ? "minecraft:note_block" : "minecraft:black_concrete");
        for (int x = -2; x <= 2; x++) c.set(x, 1, -10, St.stairs("minecraft:blackstone_stairs", Dir.NORTH, false));
        for (int k = 0; k < 40; k++) {
            int x = Noise.pick(k, 0, 0, s, 30) - 15, z = Noise.pick(k, 1, 0, s, 24) - 16;
            if (x * x + z * z > 300) continue;
            c.set(x, -1, z, "minecraft:black_concrete");
        }
        litter(c, s + 1, 0.15, St.carpet("white"), 4, St.carpet("light_gray"), 1);
        return 0xFFFFFFFF;
    }

    private static int field(Canvas c, int s) {
        floor(c, Palette.patches2(s, 3, "minecraft:grass_block", 6, "minecraft:coarse_dirt", 2, "minecraft:podzol", 1));
        litter(c, s + 1, 0.35, "minecraft:short_grass", 6, "minecraft:tall_grass", 2, "minecraft:fern", 2);
        return 0xFFE0E0E0;
    }
}
