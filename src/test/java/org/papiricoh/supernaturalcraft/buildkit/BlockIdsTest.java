package org.papiricoh.supernaturalcraft.buildkit;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockIdsTest {

    static final BlockIds IDS = BlockIds.vanilla();

    @Test
    void theListIsMinecraft1211() {
        assertTrue(IDS.size() > 1000, "about 1060 blocks: " + IDS.size());
        for (String id : List.of("minecraft:cherry_leaves", "minecraft:chiseled_bookshelf", "minecraft:decorated_pot", "minecraft:light",
                "minecraft:tuff_bricks", "minecraft:waxed_copper_bulb", "minecraft:copper_grate", "minecraft:pink_petals",
                "minecraft:mangrove_roots", "minecraft:bamboo_mosaic")) {
            assertTrue(IDS.known(id), id);
        }
        // Later versions' blocks must not sneak in.
        for (String id : List.of("minecraft:pale_oak_log", "minecraft:pale_oak_leaves", "minecraft:leaf_litter", "minecraft:firefly_bush",
                "minecraft:bush", "minecraft:creaking_heart", "minecraft:resin_block")) {
            assertFalse(IDS.known(id), id);
        }
    }

    @Test
    void checkCatchesWhatTheParserWouldRefuse() {
        assertNull(IDS.check("minecraft:oak_stairs[facing=north,half=top,shape=straight,waterlogged=false]"));
        assertNull(IDS.check("minecraft:oak_stairs"));
        assertNotNull(IDS.check("minecraft:oak_stair"), "unknown id");
        assertNotNull(IDS.check("minecraft:oak_stairs[facing=up]"), "bad value");
        assertNotNull(IDS.check("minecraft:oak_stairs[axis=y]"), "unknown property");
        assertNotNull(IDS.check("minecraft:pink_petals[amount=2]"), "1.21.1 calls it flower_amount");
        assertNotNull(IDS.check("Minecraft:Stone"), "malformed");
        assertNotNull(IDS.check("supernaturalcraft:angel_statue"));
        assertNull(IDS.allow("supernaturalcraft:angel_statue").check("supernaturalcraft:angel_statue[facing=north]"));
    }

    @Test
    void theCanvasRefusesMalformedStatesAtOnce() {
        Canvas c = new Canvas("t");
        assertThrows(IllegalArgumentException.class, () -> c.set(0, 0, 0, "minecraft:oak_stairs[facing=north"));
        assertThrows(IllegalArgumentException.class, () -> c.set(0, 0, 0, "oak stairs"));
        assertThrows(IllegalArgumentException.class, () -> c.set(0, 0, 0, "minecraft:Stone"));
        c.set(0, 0, 0, "stone");
        assertTrue(c.get(0, 0, 0).equals("minecraft:stone"), "the namespace is added");
    }

    @Test
    void everyBuilderWritesReadableStates() {
        List<String> states = new java.util.ArrayList<>();
        for (Dir d : Dir.HORIZONTAL) {
            states.add(St.stairs("minecraft:oak_stairs", d, true));
            states.add(St.trapdoor("minecraft:spruce_trapdoor", d, true, true));
            states.add(St.trapdoorAgainst("minecraft:iron_trapdoor", d));
            states.addAll(List.of(St.door("minecraft:oak_door", d, true, false)));
            states.addAll(List.of(St.bed("red", d)));
            states.add(St.gate("minecraft:oak_fence_gate", d, true, false));
            states.add(St.petals(3, d));
            states.add(St.wallTorch(d));
            states.add(St.soulWallTorch(d));
            states.add(St.button("minecraft:oak_button", "wall", d));
            states.add(St.lever("floor", d));
            states.add(St.ladder(d));
            states.add(St.wallSign("oak", d));
            states.add(St.wallBanner("white", d));
            states.add(St.campfire(true, false, d));
            states.add(St.barrel(d, false));
            states.add(St.chiseledShelf(d, 21));
            states.add(St.decoratedPot(d));
            states.add(St.lectern(d));
            states.add(St.facing("minecraft:furnace", d));
            states.add(St.wallHead("minecraft:skeleton_skull", d));
            states.add(St.vine(d));
        }
        for (String a : List.of("x", "y", "z")) {
            states.add(St.axis("minecraft:oak_log", a));
            states.add(St.chain(a));
        }
        states.addAll(List.of(St.slabTop("minecraft:oak_slab"), St.slab("minecraft:oak_slab", "double"), St.candle("red", 4, true),
                St.candle(null, 1, false), St.lantern(true), St.soulLantern(false), St.light(7), St.bulb("minecraft:waxed_copper_bulb"),
                St.leaves("minecraft:cherry_leaves"), St.pane("minecraft:glass_pane"), St.bars(), St.fence("minecraft:oak_fence"),
                St.wall("minecraft:cobblestone_wall"), St.carpet("red"), St.mossCarpet(), St.hangingSign("oak", 4),
                St.dripstone(Dir.DOWN, "tip"), St.dripstone(Dir.UP, "base"), St.potted("fern"), St.water(), St.waterFalling(),
                St.waterlogged(St.slabBottom("minecraft:stone_slab")), St.snow(3), St.crop("minecraft:wheat", 7), St.seaPickle(2, true),
                St.head("minecraft:skeleton_skull", 3), St.snowy("minecraft:grass_block", false), St.endRod(Dir.UP),
                St.rod("minecraft:lightning_rod", Dir.UP), St.torch()));
        states.addAll(List.of(St.tall("minecraft:tall_grass")));
        states.addAll(List.of(St.tall("minecraft:peony")));
        for (String s : states) assertNull(IDS.check(s), s);
    }

    @Test
    void everyFamilyAndWoodIsReal() throws IllegalAccessException {
        for (Field f : Family.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Family.class) continue;
            Family fam = (Family) f.get(null);
            assertNull(IDS.check(fam.block()), f.getName());
            if (fam.stairs() != null) assertNull(IDS.check(fam.stairs(Dir.EAST, false)), f.getName());
            if (fam.slab() != null) assertNull(IDS.check(fam.slabTop()), f.getName());
            if (fam.wall() != null) assertNull(IDS.check(fam.wallBlock()), f.getName());
        }
        for (Field f : Wood.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Wood.class) continue;
            Wood w = (Wood) f.get(null);
            for (String s : List.of(w.planks(), w.stairs(Dir.NORTH, false), w.slabTop(), w.fence(), w.trapdoor(Dir.NORTH, true, false),
                    w.log("x"), w.strippedLog("z"), w.wood("y"), w.strippedWood("y"), St.gate(w.gateId(), Dir.NORTH, false, false),
                    St.door(w.doorId(), Dir.NORTH, false, false)[0], St.button(w.buttonId(), "floor", Dir.NORTH), w.plateId())) {
                assertNull(IDS.check(s), f.getName() + ": " + s);
            }
            if (w.leaves() != null) assertNull(IDS.check(w.leavesState()), f.getName());
        }
    }
}
