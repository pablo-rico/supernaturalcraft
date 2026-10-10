package org.papiricoh.supernaturalcraft.memory.scenes;

import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Cell;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Kinds;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.buildkit.Palette;
import org.papiricoh.supernaturalcraft.buildkit.St;
import org.papiricoh.supernaturalcraft.buildkit.Trees;
import org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsLayout;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseLayout;
import org.papiricoh.supernaturalcraft.memory.Memory;
import org.papiricoh.supernaturalcraft.memory.MemoryKind;
import org.papiricoh.supernaturalcraft.memory.MemoryText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a memory into the scene staged for it (v0.18, pure and deterministic): a crafted vignette per {@link MemoryKind} built
 * with the build kit, on a floor of radius {@link #FLOOR} that fades into the stage's own marble and a low ring of cloud at its
 * edge, figures posed in it (one of them the focus to touch), a tint and a title.
 * <ul>
 *   <li>{@code BOSS_VICTORY}: a miniature of the fight's ground in the boss's theme ({@link BossScenes}), the fallen enemy (focus)
 *       before the hunter and two allies;</li>
 *   <li>{@code CROSSROADS_DEAL}: the wild crossroads ({@code CrossroadsLayout}, its signs left out) at night, the demon offering
 *       (focus), the hunter; a bowl deal adds a ring of candles round the crossing;</li>
 *   <li>{@code CASE_SOLVED}/{@code CASE_LOST}: the case's set piece ({@code CaseLayout}), the monster fallen or standing (focus);</li>
 *   <li>{@code ASCENSION}/{@code HEEDED_CALL}: a grace circle of candles and light (angel), a dark dais in chains (demon), an armoury
 *       circle (hunter); the hunter kneeling (focus), the messenger;</li>
 *   <li>{@code LEGACY_RANK}: a corner of the bunker's war room, Henry and the hunter at the map table (Henry the focus);</li>
 *   <li>{@code PET_LOST}: a small grave under a blossoming tree, the pet sitting by it (focus), the hunter kneeling;</li>
 *   <li>{@code FIRST_SIGHTING}/{@code FAVOURITE_PREY}: a misty woodland clearing, the creature (focus); for prey, more of them.</li>
 * </ul>
 * No block entities; at most {@link #MAX_CELLS} cells; nothing past {@link #RADIUS} or above {@link #HEIGHT}.
 */
public final class MemoryScenes {

    /** The stage's radius: no scene reaches further from its centre. */
    public static final int RADIUS = 26;
    /** The stage's height above its floor. */
    public static final int HEIGHT = 24;
    /** A scene's cells, air included (the stage's arena keeps a snapshot of every one). */
    public static final int MAX_CELLS = 18_000;
    /** The scene's own floor; from here to {@link #RADIUS} it fades into the stage. */
    static final int FLOOR = 20;
    /** Where a visitor appears (on the south, looking north into the scene). */
    static final int[] ENTRY = {0, 0, 17};

    private MemoryScenes() {
    }

    /** @param seed the world's seed mixed with the hunter's UUID, so the same memory looks the same every visit */
    public static Scene scene(Memory memory, long seed) {
        int s = (int) (seed ^ (seed >>> 32)) ^ memory.id().hashCode();
        String title = MemoryText.titleKey(memory);
        Canvas c = new Canvas("memory_" + memory.kind().name().toLowerCase(Locale.ROOT));
        List<Figure> figures = new ArrayList<>();
        int tint;
        switch (memory.kind()) {
            case BOSS_VICTORY -> tint = BossScenes.draw(c, figures, memory.subject(), s);
            case CROSSROADS_DEAL -> tint = crossroads(c, figures, memory, s);
            case CASE_SOLVED, CASE_LOST -> tint = caseScene(c, figures, memory, s);
            case ASCENSION, HEEDED_CALL -> tint = ascension(c, figures, memory, s);
            case LEGACY_RANK -> tint = legacy(c, figures, s);
            case PET_LOST -> tint = petGrave(c, figures, memory, s);
            case FIRST_SIGHTING, FAVOURITE_PREY -> tint = sighting(c, figures, memory, s);
            default -> tint = 0xFFFFFFFF;
        }
        vignette(c, s);
        return new Scene(cells(c), List.copyOf(figures), ENTRY.clone(), tint, title, "");
    }

    // --- shared ---------------------------------------------------------------------------------------------------------------

    /** The scene's cells: inside the stage, without block entities. */
    static List<ArenaCell> cells(Canvas c) {
        c.finish();
        List<ArenaCell> out = new ArrayList<>();
        for (Cell cell : c.cells()) {
            if (cell.x() * cell.x() + cell.z() * cell.z() > RADIUS * RADIUS || cell.y() >= HEIGHT || cell.y() < -1) continue;
            if (Kinds.blockEntity(cell.state())) continue;
            out.add(new ArenaCell(cell.x(), cell.y(), cell.z(), cell.state()));
        }
        return List.copyOf(out);
    }

    /** A disc of floor of radius {@link #FLOOR}. */
    static void floor(Canvas c, Brush b) {
        for (int x = -FLOOR; x <= FLOOR; x++) {
            for (int z = -FLOOR; z <= FLOOR; z++) if (x * x + z * z <= FLOOR * FLOOR) c.set(x, -1, z, b.at(x, -1, z));
        }
    }

    /**
     * The vignette: past {@code FLOOR - 3} the scene's floor gives way, cell by cell, to the stage's pale marble, and a broken ring of
     * cloud mounds closes the edge — the memory seems to dissolve into light. The entry's lane stays open.
     */
    static void vignette(Canvas c, int s) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS - 0.5 || d < FLOOR - 3) continue;
                double fade = (d - (FLOOR - 3)) / (RADIUS - FLOOR + 3);
                if (Noise.hash01(x, 0, z, s + 900) < fade * 1.3) {
                    c.set(x, -1, z, Noise.chance(x, 1, z, s + 901, 0.5) ? "minecraft:calcite" : "minecraft:white_concrete_powder");
                    for (int y = 0; y <= 2; y++) {
                        String a = c.get(x, y, z);
                        if (a != null && !Kinds.air(a)) c.remove(x, y, z);
                    }
                }
                boolean lane = Math.abs(x) <= 2 && z > 0;
                if (!lane && d > RADIUS - 3.5) {
                    double mound = Noise.value2(x, z, 3, s + 902);
                    if (mound > 0.55) c.set(x, 0, z, "supernaturalcraft:cloud_stone");
                    if (mound > 0.72) c.set(x, 1, z, "supernaturalcraft:cloud_stone");
                }
            }
        }
        for (int z = 14; z <= RADIUS; z++) for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) if (c.has(x, y, z)) c.remove(x, y, z);
        for (int z = 14; z <= 19; z++) for (int x = -1; x <= 1; x++) if (!c.has(x, -1, z)) c.set(x, -1, z, "minecraft:calcite");
    }

    /** Scatters {@code states} (weighted) on the floor's air cells at {@code density}. */
    static void litter(Canvas c, int s, double density, Object... weighted) {
        for (int x = -FLOOR; x <= FLOOR; x++) {
            for (int z = -FLOOR; z <= FLOOR; z++) {
                if (x * x + z * z > (FLOOR - 2) * (FLOOR - 2) || !Noise.chance(x, 0, z, s, density)) continue;
                if (!c.isAir(x, 0, z) || !Kinds.holdsAbove(c.world(x, -1, z))) continue;
                String id = org.papiricoh.supernaturalcraft.buildkit.Scatter.pick(x, z, s + 1, weighted);
                if (Kinds.soilPlant(id) && !Kinds.soil(c.world(x, -1, z))) continue;
                org.papiricoh.supernaturalcraft.buildkit.Scatter.plant(c, x, 0, z, id, s);
            }
        }
    }

    static Figure fig(double x, double z, float yaw, String who, String pose, boolean focus) {
        return new Figure(x + 0.5, 0, z + 0.5, yaw, who, pose, 1f, focus);
    }

    // --- the crossroads -------------------------------------------------------------------------------------------------------

    private static int crossroads(Canvas c, List<Figure> figures, Memory m, int s) {
        Canvas road = CrossroadsLayout.canvas(s, Math.floorMod(s, 2));
        for (Cell cell : road.cells()) {
            if (cell.y() < -1 || Kinds.blockEntity(cell.state())) continue;
            // The soil that summons is the world's, not the memory's: a memory's crossing is plain trodden earth.
            c.set(cell.x(), cell.y(), cell.z(), cell.state().startsWith("supernaturalcraft:") ? "minecraft:coarse_dirt" : cell.state());
        }
        boolean bowl = !"wild".equals(m.detail());
        if (bowl) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int x = (int) Math.round(Math.cos(a) * 3), z = (int) Math.round(Math.sin(a) * 3);
                c.set(x, -1, z, "minecraft:packed_mud");
                c.set(x, 0, z, St.candle(k % 2 == 0 ? "black" : "red", 1 + k % 3, true));
            }
        }
        figures.add(fig(0, -2, 0f, "supernaturalcraft:crossroads_demon", "offer", true));
        figures.add(fig(0, 2, 180f, "@owner", bowl ? "stand" : "kneel", false));
        return 0xFF6A5A8A;
    }

    // --- cases ---------------------------------------------------------------------------------------------------------------

    private static int caseScene(Canvas c, List<Figure> figures, Memory m, int s) {
        String scenario = m.detail().isEmpty() ? "barn" : m.detail();
        boolean night = scenario.equals("graveyard") || scenario.equals("night_woods") || scenario.equals("haunted_house");
        floor(c, Palette.patches2(s + 10, 3, "minecraft:grass_block", 8, "minecraft:coarse_dirt", 2, "minecraft:podzol", 2, "minecraft:dirt", 1));
        for (CaseLayout.Cell cell : CaseLayout.piece(scenario)) {
            if (Kinds.blockEntity(cell.state())) continue;
            c.set(cell.x(), cell.y(), cell.z() - 3, cell.state());
        }
        for (CaseLayout.Cell cell : CaseLayout.piece(scenario)) {
            if (St.path(cell.state()).endsWith("_carpet") && Kinds.air(c.world(cell.x(), cell.y() - 1, cell.z() - 3))) c.remove(cell.x(), cell.y(), cell.z() - 3);
        }
        Trees.deadOak(c, -12, 0, -6, 1, false, s + 11);
        Trees.oak(c, 13, 0, -8, 1, s + 12);
        Trees.spruce(c, -14, 0, 6, 0, s + 13);
        litter(c, s + 14, 0.3, "minecraft:short_grass", 6, "minecraft:fern", 3, "minecraft:tall_grass", 2, "minecraft:dead_bush", 1);
        boolean solved = m.kind() == MemoryKind.CASE_SOLVED;
        String monster = m.subject().isEmpty() ? "supernaturalcraft:vampire" : m.subject();
        figures.add(fig(0, -3, 0f, monster, solved ? "fallen" : "stand", true));
        figures.add(fig(-1, 3, 180f, "@owner", solved ? "strike" : "kneel", false));
        figures.add(fig(2, 4, 200f, "@ally:dean", "stand", false));
        return night ? 0xFF404A70 : 0xFFE0D8C0;
    }

    // --- ascension, the call ------------------------------------------------------------------------------------------------

    private static int ascension(Canvas c, List<Figure> figures, Memory m, int s) {
        String faction = m.kind() == MemoryKind.HEEDED_CALL ? "angel" : m.subject();
        switch (faction) {
            case "demon" -> {
                floor(c, Palette.patches2(s + 20, 3, "minecraft:polished_blackstone", 4, "minecraft:blackstone", 2, "minecraft:basalt[axis=y]", 1,
                        "minecraft:crimson_nylium", 1));
                dais(c, Family.POLISHED_BLACKSTONE_BRICKS, "minecraft:crying_obsidian");
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3;
                    int x = (int) Math.round(Math.cos(a) * 9), z = (int) Math.round(Math.sin(a) * 9);
                    for (int y = 0; y <= 5; y++) c.set(x, y, z, y == 5 ? "minecraft:chiseled_polished_blackstone" : St.axis("minecraft:polished_basalt", "y"));
                    for (int y = 6; y <= 9; y++) c.set(x, y, z, St.chain("y"));
                    c.set(x, 10, z, "minecraft:polished_blackstone_bricks");
                    c.set(x, 6, z, St.soulLantern(false));
                }
                figures.add(fig(0, 0, 180f, "@owner", "kneel", true));
                figures.add(fig(0, -6, 0f, "supernaturalcraft:black_eyed_demon", "stand", false));
                return 0xFF8A3030;
            }
            case "hunter" -> {
                floor(c, Palette.boards(true, Brush.of("minecraft:spruce_planks"), Brush.of("minecraft:dark_oak_planks"), Brush.of("minecraft:oak_planks"), 4));
                dais(c, Family.STONE_BRICKS, "minecraft:ochre_froglight");
                for (int x = -6; x <= 6; x++) {
                    c.set(x, 0, -10, "minecraft:spruce_planks");
                    c.set(x, 1, -10, x % 3 == 0 ? St.log("minecraft:stripped_spruce_log") : "minecraft:bookshelf");
                    c.set(x, 2, -10, St.trapdoor("minecraft:spruce_trapdoor", Dir.SOUTH, true, false));
                    if (x % 2 == 0) c.set(x, 3, -10, St.lantern(false));
                }
                figures.add(fig(0, 0, 180f, "@owner", "kneel", true));
                figures.add(fig(-3, -4, 20f, "@ally:bobby", "stand", false));
                return 0xFFE8C890;
            }
            default -> {
                floor(c, Palette.patches2(s + 21, 3, "minecraft:calcite", 4, "minecraft:polished_diorite", 3, "minecraft:smooth_quartz", 1));
                dais(c, Family.SMOOTH_QUARTZ, "minecraft:pearlescent_froglight");
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + Math.PI / 4;
                    int x = (int) Math.round(Math.cos(a) * 5), z = (int) Math.round(Math.sin(a) * 5);
                    c.set(x, 0, z, "minecraft:quartz_bricks");
                    c.set(x, 1, z, St.candle("white", 4, true));
                }
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    int x = (int) Math.round(Math.cos(a) * 11), z = (int) Math.round(Math.sin(a) * 11);
                    if (z > 8) continue;
                    for (int y = 0; y <= 7; y++) c.set(x, y, z, y == 7 ? "minecraft:chiseled_quartz_block" : St.axis("minecraft:quartz_pillar", "y"));
                    c.set(x, 8, z, St.endRod(Dir.UP));
                }
                figures.add(fig(0, 0, 180f, "@owner", "kneel", true));
                figures.add(fig(0, -5, 0f, "supernaturalcraft:messenger", "offer", false));
                return 0xFFFFF4D8;
            }
        }
    }

    /** A round stepped dais of radius 3 with a light at its heart, a stair skirt. */
    private static void dais(Canvas c, Family f, String light) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                double d = Math.hypot(x, z);
                if (d > 4.4) continue;
                if (d > 3.4) {
                    c.set(x, -1, z, f.block());
                    Dir out = Dir.toward(x, z);
                    if (f.hasStairs()) c.set(x, 0, z, f.stairs(out.opposite(), false));
                } else {
                    c.set(x, -1, z, x == 0 && z == 0 ? light : f.block());
                    c.set(x, 0, z, f.slabBottom());
                }
            }
        }
    }

    // --- the Men of Letters ---------------------------------------------------------------------------------------------------

    private static int legacy(Canvas c, List<Figure> figures, int s) {
        floor(c, Palette.checker(3, Palette.patches2(s + 30, 2, "minecraft:polished_andesite", 4, "minecraft:polished_tuff", 1),
                Brush.of("minecraft:polished_tuff")));
        for (int x = -2; x <= 2; x++) for (int z = -3; z <= -1; z++) c.set(x, 0, z, "supernaturalcraft:map_table");
        for (int x = -10; x <= 10; x++) {
            for (int y = 0; y <= 4; y++) {
                String st = y == 4 ? "minecraft:polished_deepslate" : x % 4 == 0 ? St.log("minecraft:stripped_dark_oak_log") : y == 0 ? "minecraft:dark_oak_planks" : "minecraft:bookshelf";
                c.set(x, y, -12, st);
            }
            c.set(x, 5, -12, Family.POLISHED_DEEPSLATE.stairs(Dir.NORTH, true));
        }
        for (int x : new int[]{-6, 6}) {
            for (int y = 0; y <= 5; y++) c.set(x, y, -6, St.axis("minecraft:polished_basalt", "y"));
            for (int y = 6; y <= 7; y++) c.set(x, y, -6, St.chain("y"));
            c.set(x, 8, -6, "minecraft:polished_deepslate");
            c.set(x, 6, -5, "minecraft:polished_deepslate");
            c.set(x, 5, -5, St.lantern(true));
        }
        c.set(0, 4, -2, St.lantern(true));
        for (int y = 5; y <= 8; y++) c.set(0, y, -2, St.chain("y"));
        c.set(0, 9, -2, "minecraft:polished_deepslate");
        figures.add(fig(0, -5, 0f, "supernaturalcraft:henry_winchester", "offer", true));
        figures.add(fig(0, 1, 180f, "@owner", "stand", false));
        return 0xFFC8D8B0;
    }

    // --- a pet's grave --------------------------------------------------------------------------------------------------------

    private static int petGrave(Canvas c, List<Figure> figures, Memory m, int s) {
        floor(c, Palette.patches2(s + 40, 4, "minecraft:grass_block", 10, "minecraft:moss_block", 2, "minecraft:podzol", 1));
        Trees.cherry(c, -3, 0, -7, 2, s + 41);
        c.set(0, 0, -3, St.slabBottom("minecraft:mossy_cobblestone_slab"));
        c.set(0, -1, -3, "minecraft:rooted_dirt");
        c.set(0, 0, -4, St.wall("minecraft:mossy_stone_brick_wall"));
        c.set(0, 1, -4, St.fence("minecraft:spruce_fence"));
        c.set(-1, 1, -4, St.trapdoorAgainst("minecraft:spruce_trapdoor", Dir.EAST));
        c.set(1, 1, -4, St.trapdoorAgainst("minecraft:spruce_trapdoor", Dir.WEST));
        c.set(-1, 0, -3, St.candle("white", 2, true));
        c.set(1, 0, -3, "minecraft:potted_white_tulip");
        litter(c, s + 42, 0.45, "minecraft:short_grass", 6, "minecraft:pink_petals", 3, "minecraft:lily_of_the_valley", 2, "minecraft:azure_bluet", 2,
                "minecraft:oxeye_daisy", 2, "minecraft:fern", 1);
        String pet = m.subject().isEmpty() ? "minecraft:wolf" : m.subject();
        figures.add(fig(1, -1, 180f, pet, "sit", true));
        figures.add(fig(-1, 0, 160f, "@owner", "kneel", false));
        return 0xFFFFE0E8;
    }

    // --- sightings and prey ---------------------------------------------------------------------------------------------------

    private static int sighting(Canvas c, List<Figure> figures, Memory m, int s) {
        floor(c, Palette.patches2(s + 50, 3, "minecraft:podzol", 4, "minecraft:grass_block", 4, "minecraft:coarse_dirt", 2, "minecraft:moss_block", 2));
        int[][] trees = {{-12, -8}, {11, -10}, {-15, 4}, {14, 6}, {-6, -14}, {5, -15}};
        for (int i = 0; i < trees.length; i++) {
            if (i % 3 == 2) Trees.deadOak(c, trees[i][0], 0, trees[i][1], 1, false, s + 51 + i);
            else Trees.spruce(c, trees[i][0], 0, trees[i][1], i % 2, s + 51 + i);
        }
        litter(c, s + 58, 0.4, "minecraft:fern", 5, "minecraft:large_fern", 2, "minecraft:short_grass", 4, "minecraft:moss_carpet", 3,
                "minecraft:brown_mushroom", 1, "minecraft:sweet_berry_bush", 1);
        String who = m.subject().isEmpty() ? "supernaturalcraft:ghost" : m.subject();
        figures.add(fig(0, -6, 0f, who, "stand", true));
        if (m.kind() == MemoryKind.FAVOURITE_PREY) {
            figures.add(fig(-5, -3, 30f, who, "fallen", false));
            figures.add(fig(4, -2, 330f, who, "fallen", false));
            figures.add(fig(0, 4, 180f, "@owner", "strike", false));
        } else {
            figures.add(fig(3, 5, 200f, "@owner", "stand", false));
        }
        return 0xFF607860;
    }
}
