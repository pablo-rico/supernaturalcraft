package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaBlock;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.reward.TrophyBlock;
import org.papiricoh.supernaturalcraft.hunter.SaltLineBlock;
import org.papiricoh.supernaturalcraft.ritual.block.FlatLineBlock;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlock;

public class AllBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SupernaturalCraft.MODID);

    // --- Ores --------------------------------------------------------------------------------
    public static final DeferredBlock<DropExperienceBlock> ROCK_SALT_ORE = BLOCKS.register("rock_salt_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0f, 3.0f)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_ROCK_SALT_ORE = BLOCKS.register("deepslate_rock_salt_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<DropExperienceBlock> NETHER_SULFUR_ORE = BLOCKS.register("nether_sulfur_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER).requiresCorrectToolForDrops().strength(3.0f, 3.0f).sound(SoundType.NETHER_ORE)));

    // --- Floor drawings ----------------------------------------------------------------------
    public static final DeferredBlock<SaltLineBlock> SALT_LINE = BLOCKS.register("salt_line",
            () -> new SaltLineBlock(lineProps(MapColor.SNOW).sound(SoundType.SAND)));
    public static final DeferredBlock<FlatLineBlock> CHALK_LINE = BLOCKS.register("chalk_line",
            () -> new FlatLineBlock(lineProps(MapColor.QUARTZ)));
    public static final DeferredBlock<FlatLineBlock> BLOOD_CHALK_LINE = BLOCKS.register("blood_chalk_line",
            () -> new FlatLineBlock(lineProps(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<DevilsTrapBlock> DEVILS_TRAP = BLOCKS.register("devils_trap",
            () -> new DevilsTrapBlock(lineProps(MapColor.COLOR_RED).lightLevel(s -> 0)));

    // --- Rituals -----------------------------------------------------------------------------
    public static final DeferredBlock<RitualAltarBlock> RITUAL_ALTAR = BLOCKS.register("ritual_altar",
            () -> new RitualAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops().strength(3.5f, 9f).noOcclusion().sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> 3)));

    // --- The Cage: arena floor placed and restored by the boss fight --------------------------
    public static final DeferredBlock<ArenaBlock> HELLFIRE_CRACK = BLOCKS.register("hellfire_crack",
            () -> new ArenaBlock(arenaProps(MapColor.FIRE).lightLevel(s -> 10).sound(SoundType.BASALT), ArenaBlock.Kind.HELLFIRE));
    public static final DeferredBlock<ArenaBlock> CAGE_FROST = BLOCKS.register("cage_frost",
            () -> new ArenaBlock(arenaProps(MapColor.ICE).friction(0.98f).sound(SoundType.GLASS), ArenaBlock.Kind.FROST));
    public static final DeferredBlock<ArenaBlock> CAGE_ICE = BLOCKS.register("cage_ice",
            () -> new ArenaBlock(arenaProps(MapColor.ICE).strength(0.6f).noOcclusion().sound(SoundType.GLASS)
                    .isViewBlocking((s, l, p) -> false), ArenaBlock.Kind.PLAIN));
    public static final DeferredBlock<ArenaBlock> SERAPHIC_PILLAR = BLOCKS.register("seraphic_pillar",
            () -> new ArenaBlock(arenaProps(MapColor.GOLD).lightLevel(s -> 15).strength(1.5f).sound(SoundType.AMETHYST),
                    ArenaBlock.Kind.PLAIN));

    public static final DeferredBlock<TrophyBlock> MORNINGSTAR_TROPHY = BLOCKS.register("morningstar_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 1200f)
                    .noOcclusion().lightLevel(s -> 8).sound(SoundType.AMETHYST)));

    public static final DeferredBlock<TrophyBlock> ECLIPSE_TROPHY = BLOCKS.register("eclipse_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2f, 1200f)
                    .noOcclusion().lightLevel(s -> 6).sound(SoundType.AMETHYST)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.light.LightWellBlock> LIGHT_WELL = BLOCKS.register("light_well",
            () -> new org.papiricoh.supernaturalcraft.light.LightWellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(-1f, 3_600_000f).noLootTable().sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(org.papiricoh.supernaturalcraft.light.LightWellBlock.LIT) ? 15 : 0)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.weapon.forge.HellforgeBlock> HELLFORGE = BLOCKS.register("hellforge",
            () -> new org.papiricoh.supernaturalcraft.weapon.forge.HellforgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NETHER)
                    .requiresCorrectToolForDrops().strength(5f, 1200f).sound(SoundType.ANVIL).lightLevel(s -> 9)));

    public static final DeferredBlock<TrophyBlock> CHOIR_TROPHY = BLOCKS.register("choir_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 1200f)
                    .noOcclusion().lightLevel(s -> 8).sound(SoundType.AMETHYST)));

    // --- The Hymnal Spire: creative-only, unbreakable in survival -----------------------------
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlock> CHOIR_ALTAR = BLOCKS.register("choir_altar",
            () -> new org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlock(spireProps(MapColor.QUARTZ).lightLevel(s -> 9)));
    /** The seven Choir Bells, one per note: choir_bell_&lt;colour&gt;. */
    public static final java.util.List<DeferredBlock<org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock>> CHOIR_BELLS = registerBells();

    private static java.util.List<DeferredBlock<org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock>> registerBells() {
        java.util.List<DeferredBlock<org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock>> out = new java.util.ArrayList<>();
        for (int i = 0; i < org.papiricoh.supernaturalcraft.chorus.Melody.NOTES; i++) {
            int note = i;
            out.add(BLOCKS.register("choir_bell_" + org.papiricoh.supernaturalcraft.chorus.Melody.NAMES[i],
                    () -> new org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock(note, spireProps(MapColor.GOLD).noOcclusion()
                            .lightLevel(s -> s.getValue(org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock.LIT) ? 15
                                    : s.getValue(org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock.RINGING) ? 10 : 4))));
        }
        return java.util.List.copyOf(out);
    }

    private static BlockBehaviour.Properties spireProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1f, 3_600_000f).noLootTable().sound(SoundType.AMETHYST)
                .pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties arenaProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0f, 6.0f).noLootTable()
                .pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties lineProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollission().instabreak().noOcclusion()
                .pushReaction(PushReaction.DESTROY).sound(SoundType.WOOL);
    }

    public static void init() {
    }
}
