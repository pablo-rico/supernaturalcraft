package org.papiricoh.supernaturalcraft.apocworld;

import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.army.AuPrisonerEntity;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierAssets;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampLockBlock;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampPiece;
import org.papiricoh.supernaturalcraft.apocworld.camp.CampStructure;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRiftBlock;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUBiomeSource;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AUTerrain;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AshLayerBlock;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.AuRuinFeature;
import org.papiricoh.supernaturalcraft.apocworld.worldgen.BurnedTreeFeature;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelAssets;
import org.papiricoh.supernaturalcraft.entity.boss.aumichael.AuMichaelEntity;
import org.papiricoh.supernaturalcraft.registry.AllArmorMaterials;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllStructures;
import org.papiricoh.supernaturalcraft.registry.AllWorldgen;
import org.papiricoh.supernaturalcraft.reward.TrophyBlock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * v0.41: season 13, the Apocalypse World (its land, Michael's army, the prisoner camps, his fortress) and AU Michael.
 *
 * <p>This slice's own registry holder: it registers through the shared {@code AllBlocks.BLOCKS}, {@code AllItems.ITEMS},
 * {@code AllEntities.ENTITY_TYPES}, {@code AllBlockEntities.BLOCK_ENTITIES}, {@code AllAttachments.ATTACHMENT_TYPES} and
 * {@code AllSounds.registerGroup} (so the creative tab lists everything by itself), and {@link #init()} is called from the
 * mod's constructor. Blocks are declared before the items that place them. The block list (and their look) is in
 * {@code AuBlocksArt.md}, for the art.
 */
public final class ApocWorldRegistry {

    private ApocWorldRegistry() {
    }

    // --- Trophies (BossTrophies: the crossroads demon can fetch them) ------------------------------------------------
    public static final DeferredBlock<TrophyBlock> AU_MICHAEL_TROPHY = AllBlocks.BLOCKS.register("au_michael_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> AU_MICHAEL_TROPHY_ITEM = AllItems.ITEMS.register("au_michael_trophy",
            () -> new BlockItem(AU_MICHAEL_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // --- The burned land ----------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties stone(float hardness) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, 6.0f).sound(SoundType.STONE);
    }

    public static final DeferredBlock<Block> SCORCHED_STONE = AllBlocks.BLOCKS.register("scorched_stone", () -> new Block(stone(1.5f)));
    public static final DeferredBlock<Block> SCORCHED_SOIL = AllBlocks.BLOCKS.register("scorched_soil",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6f).sound(SoundType.ROOTED_DIRT)));
    public static final DeferredBlock<Block> ASH_BLOCK = AllBlocks.BLOCKS.register("au_ash_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5f).sound(SoundType.SAND)));
    public static final DeferredBlock<AshLayerBlock> ASH_LAYER = AllBlocks.BLOCKS.register("ash_layer",
            () -> new AshLayerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1f).sound(SoundType.SAND)
                    .noOcclusion().replaceable().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> SMOULDERING_ASH = AllBlocks.BLOCKS.register("smouldering_ash",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5f).sound(SoundType.SAND)
                    .lightLevel(s -> 6).emissiveRendering((s, l, p) -> true)));
    public static final DeferredBlock<RotatedPillarBlock> BURNED_LOG = AllBlocks.BLOCKS.register("burned_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> BURNED_PLANKS = AllBlocks.BLOCKS.register("burned_planks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f, 3.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> RUBBLE = AllBlocks.BLOCKS.register("rubble", () -> new Block(stone(1.2f).mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> RUINED_BRICKS = AllBlocks.BLOCKS.register("ruined_bricks", () -> new Block(stone(1.8f).mapColor(MapColor.COLOR_RED)));
    public static final DeferredBlock<Block> WRECKED_METAL = AllBlocks.BLOCKS.register("wrecked_metal",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).requiresCorrectToolForDrops().strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)));
    /** Michael's fortress: black basalt bricks with gold seams, tough as obsidian (the arena restores it anyway). */
    public static final DeferredBlock<Block> CITADEL_STONE = AllBlocks.BLOCKS.register("citadel_stone",
            () -> new Block(stone(25f).mapColor(MapColor.COLOR_BLACK).strength(25f, 1200f)));
    /** The lock on a prisoner cage: it holds while the camp's guards stand. */
    public static final DeferredBlock<CampLockBlock> CAMP_LOCK = AllBlocks.BLOCKS.register("camp_lock",
            () -> new CampLockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 1200f)
                    .sound(SoundType.CHAIN).noLootTable()));
    /** A tear between the worlds. */
    public static final DeferredBlock<AuRiftBlock> AU_RIFT = AllBlocks.BLOCKS.register("au_rift",
            () -> new AuRiftBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).noCollission().strength(-1f, 3_600_000f)
                    .noLootTable().lightLevel(s -> 12).sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK).noOcclusion()));

    /** Every plain block (each drops itself), in art order. */
    public static final List<DeferredBlock<? extends Block>> LAND = List.of(SCORCHED_STONE, SCORCHED_SOIL, ASH_BLOCK, ASH_LAYER, SMOULDERING_ASH,
            BURNED_LOG, BURNED_PLANKS, RUBBLE, RUINED_BRICKS, WRECKED_METAL, CITADEL_STONE);

    public static final DeferredItem<BlockItem> SCORCHED_STONE_ITEM = AllItems.ITEMS.registerSimpleBlockItem(SCORCHED_STONE);
    public static final DeferredItem<BlockItem> SCORCHED_SOIL_ITEM = AllItems.ITEMS.registerSimpleBlockItem(SCORCHED_SOIL);
    public static final DeferredItem<BlockItem> ASH_BLOCK_ITEM = AllItems.ITEMS.registerSimpleBlockItem(ASH_BLOCK);
    public static final DeferredItem<BlockItem> ASH_LAYER_ITEM = AllItems.ITEMS.registerSimpleBlockItem(ASH_LAYER);
    public static final DeferredItem<BlockItem> SMOULDERING_ASH_ITEM = AllItems.ITEMS.registerSimpleBlockItem(SMOULDERING_ASH);
    public static final DeferredItem<BlockItem> BURNED_LOG_ITEM = AllItems.ITEMS.registerSimpleBlockItem(BURNED_LOG);
    public static final DeferredItem<BlockItem> BURNED_PLANKS_ITEM = AllItems.ITEMS.registerSimpleBlockItem(BURNED_PLANKS);
    public static final DeferredItem<BlockItem> RUBBLE_ITEM = AllItems.ITEMS.registerSimpleBlockItem(RUBBLE);
    public static final DeferredItem<BlockItem> RUINED_BRICKS_ITEM = AllItems.ITEMS.registerSimpleBlockItem(RUINED_BRICKS);
    public static final DeferredItem<BlockItem> WRECKED_METAL_ITEM = AllItems.ITEMS.registerSimpleBlockItem(WRECKED_METAL);
    public static final DeferredItem<BlockItem> CITADEL_STONE_ITEM = AllItems.ITEMS.registerSimpleBlockItem(CITADEL_STONE);
    public static final DeferredItem<BlockItem> CAMP_LOCK_ITEM = AllItems.ITEMS.registerSimpleBlockItem(CAMP_LOCK);

    // --- AU Michael's greatcoat (his signature spoil) -----------------------------------------------------------------
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GREATCOAT_MATERIAL = AllArmorMaterials.ARMOR_MATERIALS.register("au_michael_greatcoat",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 8);
            }), 20, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(net.minecraft.world.item.Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(SupernaturalCraft.asResource("au_michael_greatcoat"))), 3.0f, 0.1f));
    /** The general's greatcoat: about netherite, and his soldiers will not raise a blade at whoever wears it unprovoked. */
    public static final DeferredItem<ArmorItem> AU_MICHAEL_GREATCOAT = AllItems.ITEMS.register("au_michael_greatcoat",
            () -> new ArmorItem(GREATCOAT_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant().durability(ArmorItem.Type.CHESTPLATE.getDurability(37))));

    // --- Michael's army, the prisoners, AU Michael ----------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<AuSoldierEntity>> AU_ANGEL_SOLDIER = AllEntities.ENTITY_TYPES.register(AuSoldierAssets.RIG,
            () -> EntityType.Builder.<AuSoldierEntity>of(AuSoldierEntity::new, MobCategory.MONSTER)
                    .sized(AuSoldierAssets.WIDTH, AuSoldierAssets.HEIGHT).eyeHeight(1.7f).fireImmune().clientTrackingRange(10).build(AuSoldierAssets.RIG));
    public static final DeferredHolder<EntityType<?>, EntityType<AuPrisonerEntity>> AU_PRISONER = AllEntities.ENTITY_TYPES.register(AuSoldierAssets.PRISONER,
            () -> EntityType.Builder.<AuPrisonerEntity>of(AuPrisonerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).build(AuSoldierAssets.PRISONER));
    public static final DeferredHolder<EntityType<?>, EntityType<AuMichaelEntity>> AU_MICHAEL = AllEntities.ENTITY_TYPES.register(AuMichaelAssets.RIG,
            () -> EntityType.Builder.<AuMichaelEntity>of(AuMichaelEntity::new, MobCategory.MONSTER)
                    .sized(AuMichaelAssets.WIDTH, AuMichaelAssets.HEIGHT).eyeHeight(1.75f).fireImmune().clientTrackingRange(24).updateInterval(1)
                    .build(AuMichaelAssets.RIG));

    public static final DeferredItem<DeferredSpawnEggItem> AU_ANGEL_SOLDIER_SPAWN_EGG = AllItems.ITEMS.register("au_angel_soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(AU_ANGEL_SOLDIER, 0x2e2f33, 0xc9a24a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> AU_PRISONER_SPAWN_EGG = AllItems.ITEMS.register("au_prisoner_spawn_egg",
            () -> new DeferredSpawnEggItem(AU_PRISONER, 0x6b5a48, 0x3a3430, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> AU_MICHAEL_SPAWN_EGG = AllItems.ITEMS.register("au_michael_spawn_egg",
            () -> new DeferredSpawnEggItem(AU_MICHAEL, 0x1c1d22, 0xd8b45a, new Item.Properties().rarity(Rarity.EPIC)));

    /** {@code #supernaturalcraft:archangels}: the archangels (the lance's bane reads it); this slice's datagen adds AU Michael. */
    public static final net.minecraft.tags.TagKey<EntityType<?>> ARCHANGELS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE, SupernaturalCraft.asResource("archangels"));

    // --- A hunter's standing with the Apocalypse World ----------------------------------------------------------------
    public static final Supplier<AttachmentType<AUStanding>> AU_STANDING = AllAttachments.ATTACHMENT_TYPES.register("au_standing",
            () -> AttachmentType.builder(() -> AUStanding.EMPTY).serialize(AUStanding.CODEC).copyOnDeath().build());

    // --- Sounds (vanilla re-pitched until the art encodes them) -------------------------------------------------------
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SOLDIER_SOUNDS = AllSounds.registerGroup(AuSoldierAssets.RIG, AuSoldierAssets.SOUNDS);
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MICHAEL_SOUNDS = AllSounds.registerGroup(AuMichaelAssets.RIG, AuMichaelAssets.SOUNDS);

    public static SoundEvent soldier(String id) {
        return sound(SOLDIER_SOUNDS, AuSoldierAssets.RIG, id);
    }

    public static SoundEvent michael(String id) {
        return sound(MICHAEL_SOUNDS, AuMichaelAssets.RIG, id);
    }

    private static SoundEvent sound(Map<String, DeferredHolder<SoundEvent, SoundEvent>> group, String rig, String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = group.get(rig + "." + id);
        if (h == null) throw new IllegalArgumentException("No sound " + rig + "." + id);
        return h.get();
    }

    // --- Worldgen: the land's density, its biome layout, its features, the camps ---------------------------------------
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<AUTerrain>> AU_TERRAIN =
            AllWorldgen.DENSITY_FUNCTIONS.register("au_terrain", () -> AUTerrain.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<AUBiomeSource>> AU_BIOMES =
            AllWorldgen.BIOME_SOURCES.register("apocalypse_world", () -> AUBiomeSource.CODEC);
    public static final DeferredHolder<Feature<?>, BurnedTreeFeature> BURNED_TREE =
            AllWorldgen.FEATURES.register("au_burned_tree", () -> new BurnedTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, AuRuinFeature> RUIN =
            AllWorldgen.FEATURES.register("au_ruin", () -> new AuRuinFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<StructureType<?>, StructureType<CampStructure>> CAMP_TYPE =
            AllStructures.TYPES.register("au_outpost", () -> () -> CampStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CAMP_PIECE =
            AllStructures.PIECES.register("au_outpost", () -> (StructurePieceType.ContextlessType) CampPiece::new);

    public static void init() {
    }
}
