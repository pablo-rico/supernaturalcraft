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

    public static final DeferredBlock<TrophyBlock> AZAZEL_TROPHY = BLOCKS.register("azazel_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2f, 1200f)
                    .noOcclusion().lightLevel(s -> 5).sound(SoundType.AMETHYST)));
    /** Samuel Colt's iron, risen through the arena floor to hold Azazel. Only ever placed by the fight. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.azazel.ColtRailBlock> COLT_RAIL = BLOCKS.register("colt_rail",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.azazel.ColtRailBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(-1f, 3_600_000f).noLootTable().noCollission().noOcclusion().sound(SoundType.METAL)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                    .lightLevel(s -> s.getValue(org.papiricoh.supernaturalcraft.entity.boss.azazel.ColtRailBlock.CHARGED) ? 7 : 0)));

    public static final DeferredBlock<TrophyBlock> LILITH_TROPHY = BLOCKS.register("lilith_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2f, 1200f)
                    .noOcclusion().lightLevel(s -> 7).sound(SoundType.AMETHYST)));
    /** A headstone Lilith's arena raises: cover from her white light, cracking each time it takes the burst. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.lilith.HeadstoneBlock> CRACKED_HEADSTONE = BLOCKS.register("cracked_headstone",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.lilith.HeadstoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(-1f, 3_600_000f).noLootTable().noOcclusion().sound(SoundType.STONE)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    public static final DeferredBlock<TrophyBlock> METATRON_TROPHY = BLOCKS.register("metatron_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 1200f)
                    .noOcclusion().lightLevel(s -> 6).sound(SoundType.AMETHYST)));
    /** Stone written over in gold: Metatron's dais and what he rewrites the ground into. Only ever placed by the fight. */
    public static final DeferredBlock<Block> SCRIPTURE_STONE = BLOCKS.register("scripture_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1f, 3_600_000f).noLootTable()
                    .lightLevel(s -> 5).sound(SoundType.STONE).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

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

    // --- Hell: the caverns ----------------------------------------------------------------------
    public static final DeferredBlock<Block> HELLSTONE = BLOCKS.register("hellstone",
            () -> new Block(hellProps(MapColor.NETHER, 1.2f).sound(SoundType.NETHERRACK)));
    public static final DeferredBlock<Block> HELLSTONE_BRICKS = BLOCKS.register("hellstone_bricks",
            () -> new Block(hellProps(MapColor.NETHER, 2.0f).sound(SoundType.NETHER_BRICKS)));
    public static final DeferredBlock<Block> RACK_STONE = BLOCKS.register("rack_stone",
            () -> new Block(hellProps(MapColor.CRIMSON_NYLIUM, 1.2f).sound(SoundType.NETHERRACK)));
    public static final DeferredBlock<Block> CONGEALED_BLOOD = BLOCKS.register("congealed_blood",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f)
                    .speedFactor(0.45f).jumpFactor(0.6f).sound(SoundType.HONEY_BLOCK)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.hell.block.MeatHookBlock> MEAT_HOOK = BLOCKS.register("meat_hook",
            () -> new org.papiricoh.supernaturalcraft.hell.block.MeatHookBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.5f, 6f).noOcclusion().sound(SoundType.CHAIN).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> ASH_BLOCK = BLOCKS.register("ash_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f).sound(SoundType.SAND)));
    public static final DeferredBlock<DropExperienceBlock> BRIMSTONE_ORE = BLOCKS.register("brimstone_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), hellProps(MapColor.NETHER, 3.0f).sound(SoundType.NETHER_ORE)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.hell.block.HellfireVentBlock> HELLFIRE_VENT = BLOCKS.register("hellfire_vent",
            () -> new org.papiricoh.supernaturalcraft.hell.block.HellfireVentBlock(hellProps(MapColor.FIRE, 1.5f)
                    .sound(SoundType.BASALT).lightLevel(s -> 7).emissiveRendering((s, l, p) -> true)));
    public static final DeferredBlock<Block> CORRIDOR_STONE = BLOCKS.register("corridor_stone",
            () -> new Block(hellProps(MapColor.STONE, 1.5f).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> CORRIDOR_BRICKS = BLOCKS.register("corridor_bricks",
            () -> new Block(hellProps(MapColor.STONE, 2.0f).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> ABYSSAL_STONE = BLOCKS.register("abyssal_stone",
            () -> new Block(hellProps(MapColor.COLOR_BLACK, 2.5f).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<DropExperienceBlock> ABYSSAL_SHARD_ORE = BLOCKS.register("abyssal_shard_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), hellProps(MapColor.COLOR_BLACK, 4.0f)
                    .sound(SoundType.DEEPSLATE).lightLevel(s -> 3)));

    // --- Hell: the Cage and its Pit (unbreakable) ------------------------------------------------
    public static final DeferredBlock<net.minecraft.world.level.block.IronBarsBlock> CAGE_BARS = BLOCKS.register("cage_bars",
            () -> new net.minecraft.world.level.block.IronBarsBlock(cageProps(MapColor.COLOR_BLACK).noOcclusion().sound(SoundType.CHAIN)));
    public static final DeferredBlock<Block> CAGE_FRAME = BLOCKS.register("cage_frame",
            () -> new Block(cageProps(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<Block> CAGE_SEAL = BLOCKS.register("cage_seal",
            () -> new Block(cageProps(MapColor.COLOR_RED).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7)
                    .emissiveRendering((s, l, p) -> true)));
    public static final DeferredBlock<net.minecraft.world.level.block.ChainBlock> CAGE_CHAIN = BLOCKS.register("cage_chain",
            () -> new net.minecraft.world.level.block.ChainBlock(cageProps(MapColor.COLOR_BLACK).noOcclusion().sound(SoundType.CHAIN)));
    public static final DeferredBlock<Block> ABYSSAL_BEDROCK = BLOCKS.register("abyssal_bedrock",
            () -> new Block(cageProps(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE)));
    /** The island floor: unbreakable by hand, but the fight may crack, freeze and collapse it (and restores it). */
    public static final DeferredBlock<Block> ABYSSAL_FLAGSTONE = BLOCKS.register("abyssal_flagstone",
            () -> new Block(cageProps(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE_TILES)));
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> ENOCHIAN_PILLAR = BLOCKS.register("enochian_pillar",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(cageProps(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> 4)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.hell.block.BrazierBlock> HELLFIRE_BRAZIER = BLOCKS.register("hellfire_brazier",
            () -> new org.papiricoh.supernaturalcraft.hell.block.BrazierBlock(cageProps(MapColor.FIRE).noOcclusion()
                    .sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 15)));
    /** Paves the dais under the Cage: the summoning circle exists only there. */
    public static final DeferredBlock<Block> CAGE_RITUAL_STONE = BLOCKS.register("cage_ritual_stone",
            () -> new Block(cageProps(MapColor.COLOR_RED).sound(SoundType.DEEPSLATE_TILES).lightLevel(s -> 5)
                    .emissiveRendering((s, l, p) -> true)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.hell.rift.HellRiftBlock> HELL_RIFT = BLOCKS.register("hell_rift",
            () -> new org.papiricoh.supernaturalcraft.hell.rift.HellRiftBlock(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE)
                    .noCollission().strength(-1f, 3_600_000f).noLootTable().lightLevel(s -> 11).sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK).noOcclusion()));

    // --- The spell bowl, hex bags and graves (v0.8) ---------------------------------------------
    /** A runic bronze bowl: liquids and ingredients go in, a lit match and the right Latin set it off. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock> SPELL_BOWL = BLOCKS.register("spell_bowl",
            () -> new org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5f, 6f).noOcclusion().sound(SoundType.COPPER).pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> s.getValue(org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock.LIT) ? 9 : 0)));
    /** A hex bag tucked out of sight: it curses whoever lingers near it until someone finds and burns it. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.hex.CurseBagBlock> CURSE_BAG = BLOCKS.register("curse_bag",
            () -> new org.papiricoh.supernaturalcraft.hex.CurseBagBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .strength(0.3f).noOcclusion().noCollission().sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    /** The bones a ghost is bound to, buried under its grave: salt and burn them to lay it to rest. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.grave.GraveBonesBlock> GRAVE_BONES = BLOCKS.register("grave_bones",
            () -> new org.papiricoh.supernaturalcraft.grave.GraveBonesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .strength(1.0f, 6f).noOcclusion().noLootTable().sound(SoundType.BONE_BLOCK).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.grave.GraveHeadstoneBlock> GRAVE_HEADSTONE = BLOCKS.register("grave_headstone",
            () -> new org.papiricoh.supernaturalcraft.grave.GraveHeadstoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops().strength(2.0f, 6f).noOcclusion().sound(SoundType.STONE)));
    /** Turned earth over a grave; digging it gives grave dirt. */
    public static final DeferredBlock<Block> GRAVE_SOIL = BLOCKS.register("grave_soil",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6f).sound(SoundType.ROOTED_DIRT)));

    // --- The Author (v0.10) ------------------------------------------------------------------------
    /** The Author's typewriter, on his desk. Creative only. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.author.TypewriterBlock> TYPEWRITER = BLOCKS.register("typewriter",
            () -> new org.papiricoh.supernaturalcraft.author.TypewriterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1f, 3_600_000f).noLootTable().noOcclusion().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)));
    /** Blank paper: the Author's last arena, and the ceiling gravity drops hunters onto. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.PageBlock> PAGE_BLOCK = BLOCKS.register("page_block",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.PageBlock(arenaProps(MapColor.SNOW).sound(SoundType.WOOL)));
    /** Solid ink: the lines his arenas are drawn with. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.InkBlock> INK_BLOCK = BLOCKS.register("ink_block",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.InkBlock(arenaProps(MapColor.COLOR_BLACK).sound(SoundType.MUD)));
    /** Ink that burns: "the floor is lava". */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BurningInkBlock> BURNING_INK = BLOCKS.register("burning_ink",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.BurningInkBlock(arenaProps(MapColor.COLOR_ORANGE).sound(SoundType.MUD).lightLevel(s -> 10)
                    .emissiveRendering((s, l, p) -> true)));

    private static BlockBehaviour.Properties hellProps(MapColor color, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(strength, strength * 3);
    }

    private static BlockBehaviour.Properties cageProps(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(-1f, 3_600_000f).noLootTable().pushReaction(PushReaction.BLOCK)
                .isValidSpawn((s, l, p, t) -> false);
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

    // --- The Four Horsemen (v0.11) ---------------------------------------------------------------
    public static final DeferredBlock<TrophyBlock> WAR_TROPHY = horsemanTrophy("war_trophy", MapColor.COLOR_RED);
    public static final DeferredBlock<TrophyBlock> FAMINE_TROPHY = horsemanTrophy("famine_trophy", MapColor.COLOR_BLACK);
    public static final DeferredBlock<TrophyBlock> PESTILENCE_TROPHY = horsemanTrophy("pestilence_trophy", MapColor.COLOR_LIGHT_GREEN);
    public static final DeferredBlock<TrophyBlock> DEATH_TROPHY = horsemanTrophy("death_trophy", MapColor.COLOR_LIGHT_GRAY);

    private static DeferredBlock<TrophyBlock> horsemanTrophy(String id, MapColor color) {
        return BLOCKS.register(id, () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(color).strength(2f, 1200f)
                .noOcclusion().sound(SoundType.STONE)));
    }

    // --- The Archangel Michael (v0.12) -----------------------------------------------------------
    public static final DeferredBlock<TrophyBlock> MICHAEL_TROPHY = horsemanTrophy("michael_trophy", MapColor.GOLD);

    // --- Gabriel, the Trickster (v0.14) ---------------------------------------------------------------------------------
    /** An old television with his grin on the screen. */
    public static final DeferredBlock<TrophyBlock> GABRIEL_TROPHY = horsemanTrophy("gabriel_trophy", MapColor.COLOR_BROWN);

    /** Raphael's bust (v0.16). */
    public static final DeferredBlock<TrophyBlock> RAPHAEL_TROPHY = horsemanTrophy("raphael_trophy", MapColor.COLOR_GRAY);
    /** Holy oil poured on the floor of Raphael's house (v0.16), not yet lit. No item. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.raphael.HolyOilSlickBlock> HOLY_OIL_SLICK = BLOCKS.register("holy_oil_slick",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.raphael.HolyOilSlickBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(-1f, 3_600_000f).noLootTable().noCollission().noOcclusion().sound(SoundType.HONEY_BLOCK)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    /** Holy oil's fire (v0.13): holds and burns angels, harmless to the rest. No item. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.allegiance.HolyOilFireBlock> HOLY_OIL_FIRE = BLOCKS.register("holy_oil_fire",
            () -> new org.papiricoh.supernaturalcraft.allegiance.HolyOilFireBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD).noCollission().instabreak().lightLevel(s -> 13).sound(SoundType.WOOL).noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).replaceable()));

    // --- The Men of Letters (v0.17) --------------------------------------------------------------------------------------
    /** The bunker's armoured door: only the Bunker Key opens it. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.legacy.bunker.BunkerDoorBlock> BUNKER_DOOR = BLOCKS.register("bunker_door",
            () -> new org.papiricoh.supernaturalcraft.legacy.bunker.BunkerDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(50f, 1200f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
    /** A research desk: green-shaded lamp, papers, typewriter. Opens the research menu. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.legacy.research.ResearchDeskBlock> RESEARCH_DESK = BLOCKS.register("research_desk",
            () -> new org.papiricoh.supernaturalcraft.legacy.research.ResearchDeskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5f, 1200f).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 10)));
    /** The war room's lit map table, where Henry hands out cases. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.legacy.cases.MapTableBlock> MAP_TABLE = BLOCKS.register("map_table",
            () -> new org.papiricoh.supernaturalcraft.legacy.cases.MapTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5f, 1200f).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 12)));
    /** The archive's stacks: old files and books. */
    public static final DeferredBlock<Block> ARCHIVE_SHELF = BLOCKS.register("archive_shelf",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.CHISELED_BOOKSHELF)));
    /** The order's emblem (the eye in the Aquarian star), inlaid in the bunker's floor. */
    public static final DeferredBlock<Block> MEN_OF_LETTERS_EMBLEM = BLOCKS.register("men_of_letters_emblem",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).requiresCorrectToolForDrops().strength(3f, 6f)));

    // --- Heaven (v0.18): a hunter's own Heaven, Naomi, Zachariah and the wild crossroads --------------------------------
    /** A gate of light into a hunter's Heaven (or out of it). No item. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.heaven.gate.HeavenGateBlock> HEAVEN_GATE = BLOCKS.register("heaven_gate",
            () -> new org.papiricoh.supernaturalcraft.heaven.gate.HeavenGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .noCollission().strength(-1f, 3_600_000f).noLootTable().lightLevel(s -> 15).sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK).noOcclusion()));
    /** The veil in a memory shrine's doorway: step through to relive that memory. No item. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.memory.MemoryVeilBlock> MEMORY_VEIL = BLOCKS.register("memory_veil",
            () -> new org.papiricoh.supernaturalcraft.memory.MemoryVeilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
                    .noCollission().strength(-1f, 3_600_000f).noLootTable().lightLevel(s -> 12).sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK).noOcclusion()));
    /** The hearth of a hunter's home in Heaven: rest by it. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.heaven.home.HearthBlock> HEARTH = BLOCKS.register("hearth",
            () -> new org.papiricoh.supernaturalcraft.heaven.home.HearthBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(-1f, 3_600_000f).noOcclusion().lightLevel(s -> 14).sound(SoundType.STONE).pushReaction(PushReaction.BLOCK)));
    /** A seal of light across a doorway in Heaven: opens when the plot says so. No item. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.heaven.home.CelestialSealBlock> CELESTIAL_SEAL = BLOCKS.register("celestial_seal",
            () -> new org.papiricoh.supernaturalcraft.heaven.home.CelestialSealBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(-1f, 3_600_000f).noLootTable().lightLevel(s -> 10).sound(SoundType.AMETHYST).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, p) -> false)));
    /** Heaven's stone: a soft, faintly luminous white the plots are built on. */
    public static final DeferredBlock<Block> CLOUD_STONE = BLOCKS.register("cloud_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 6f).sound(SoundType.CALCITE)));
    public static final DeferredBlock<Block> CLOUD_BRICKS = BLOCKS.register("cloud_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 6f).sound(SoundType.CALCITE)));
    /** Naomi's recalibration console. No item. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.naomi.ReprogrammingConsoleBlock> REPROGRAMMING_CONSOLE = BLOCKS.register("reprogramming_console",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.naomi.ReprogrammingConsoleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(-1f, 3_600_000f).noLootTable().noOcclusion().lightLevel(s -> 9)
                    .sound(SoundType.COPPER).pushReaction(PushReaction.BLOCK)));
    /** A numbered filing cabinet of Zachariah's office. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.entity.boss.zachariah.FilingCabinetBlock> FILING_CABINET = BLOCKS.register("filing_cabinet",
            () -> new org.papiricoh.supernaturalcraft.entity.boss.zachariah.FilingCabinetBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3f, 1200f).noOcclusion().sound(SoundType.METAL)));
    /** The trodden earth at the centre of a natural crossroads, where a box can be buried. */
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsSoilBlock> CROSSROADS_SOIL = BLOCKS.register("crossroads_soil",
            () -> new org.papiricoh.supernaturalcraft.crossroads.wild.CrossroadsSoilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT).strength(0.6f, 6f).sound(SoundType.ROOTED_DIRT)));
    /** Naomi's bust. */
    public static final DeferredBlock<TrophyBlock> NAOMI_TROPHY = horsemanTrophy("naomi_trophy", MapColor.QUARTZ);
    /** Zachariah's bust. */
    public static final DeferredBlock<TrophyBlock> ZACHARIAH_TROPHY = horsemanTrophy("zachariah_trophy", MapColor.TERRACOTTA_WHITE);

    public static void init() {
    }
}
