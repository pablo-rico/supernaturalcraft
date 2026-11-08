package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.structure.Structure;
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
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.altar.HandAltarBlock;
import org.papiricoh.supernaturalcraft.hands.altar.HandAltarBlockEntity;
import org.papiricoh.supernaturalcraft.hands.mob.BansheeEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterEggBlock;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ChitterNestBlock;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleOfficerEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleSoldierEntity;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryPiece;
import org.papiricoh.supernaturalcraft.hands.reliquary.ReliquaryStructure;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllStructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * v0.42 (season 11): the Hands of God, their five reliquaries, the Thule Society, the banshee and the Chitters.
 *
 * <p>This slice's own registry holder: it registers through the shared {@code AllBlocks.BLOCKS}, {@code AllItems.ITEMS},
 * {@code AllEntities.ENTITY_TYPES}, {@code AllBlockEntities.BLOCK_ENTITIES}, {@code AllStructures} and
 * {@code AllSounds.registerGroup} (the creative tab lists everything by itself); {@link #init()} is called from the mod's
 * constructor. Blocks before the items that place them. Names and looks: {@code S11Assets}.
 */
public final class HandsRegistry {

    private HandsRegistry() {
    }

    // --- The reliquaries' blocks ----------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties stone(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(hardness, 6.0f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(hardness, 6.0f).sound(SoundType.METAL);
    }

    /** The decorative blocks of each reliquary ({@link S11Assets#RELIQUARY_BLOCKS}), by id. */
    public static final Map<String, DeferredBlock<Block>> RELIQUARY_BLOCKS;
    /** Their items, by id. */
    public static final Map<String, DeferredItem<BlockItem>> RELIQUARY_ITEMS;

    static {
        Map<String, BlockBehaviour.Properties> props = new LinkedHashMap<>();
        props.put("rusted_hull", metal(MapColor.TERRACOTTA_BROWN, 3.0f));
        props.put("rusted_hull_plate", metal(MapColor.TERRACOTTA_ORANGE, 3.0f));
        props.put("thule_concrete", stone(MapColor.STONE, 2.5f));
        props.put("thule_sigil_plate", metal(MapColor.COLOR_BLACK, 3.5f));
        props.put("crypt_bricks", stone(MapColor.DEEPSLATE, 2.0f));
        props.put("sun_baked_bricks", stone(MapColor.SAND, 1.5f));
        props.put("ark_gold_trim", metal(MapColor.GOLD, 3.0f).sound(SoundType.METAL));
        props.put("monastery_stone", stone(MapColor.QUARTZ, 2.0f));
        Map<String, DeferredBlock<Block>> blocks = new LinkedHashMap<>();
        Map<String, DeferredItem<BlockItem>> items = new LinkedHashMap<>();
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            for (String id : S11Assets.RELIQUARY_BLOCKS.get(h.reliquary)) {
                BlockBehaviour.Properties p = props.get(id);
                if (p == null) throw new IllegalStateException("No properties for reliquary block " + id);
                DeferredBlock<Block> b = AllBlocks.BLOCKS.register(id, () -> new Block(p));
                blocks.put(id, b);
            }
        }
        for (Map.Entry<String, DeferredBlock<Block>> e : blocks.entrySet()) items.put(e.getKey(), AllItems.ITEMS.registerSimpleBlockItem(e.getValue()));
        RELIQUARY_BLOCKS = Collections.unmodifiableMap(blocks);
        RELIQUARY_ITEMS = Collections.unmodifiableMap(items);
    }

    /** The altar at each reliquary's heart: it keeps its Hand for every hunter, once each. */
    public static final DeferredBlock<HandAltarBlock> HAND_ALTAR = AllBlocks.BLOCKS.register(S11Assets.HAND_ALTAR,
            () -> new HandAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1f, 3_600_000f).noLootTable()
                    .lightLevel(s -> 9).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> HAND_ALTAR_ITEM = AllItems.ITEMS.register(S11Assets.HAND_ALTAR,
            () -> new BlockItem(HAND_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandAltarBlockEntity>> HAND_ALTAR_BE =
            AllBlockEntities.BLOCK_ENTITIES.register(S11Assets.HAND_ALTAR, () -> BlockEntityType.Builder
                    .of(HandAltarBlockEntity::new, HAND_ALTAR.get()).build(null));

    // --- The Chitters' nest ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<ChitterNestBlock> CHITTER_NEST = AllBlocks.BLOCKS.register(S11Assets.NEST_BLOCKS.get(0),
            () -> new ChitterNestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8f).speedFactor(0.5f)
                    .jumpFactor(0.6f).sound(SoundType.WART_BLOCK)));
    public static final DeferredBlock<ChitterEggBlock> CHITTER_EGG = AllBlocks.BLOCKS.register(S11Assets.NEST_BLOCKS.get(1),
            () -> new ChitterEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).strength(0.5f).noOcclusion()
                    .randomTicks().sound(SoundType.FROGSPAWN).pushReaction(PushReaction.DESTROY).lightLevel(s -> 2)));
    public static final DeferredItem<BlockItem> CHITTER_NEST_ITEM = AllItems.ITEMS.registerSimpleBlockItem(CHITTER_NEST);
    public static final DeferredItem<BlockItem> CHITTER_EGG_ITEM = AllItems.ITEMS.registerSimpleBlockItem(CHITTER_EGG);

    // --- The five Hands of God ------------------------------------------------------------------------------------------------
    /** Each Hand's item, in {@link S11Assets.Hand} order. */
    public static final Map<S11Assets.Hand, DeferredItem<HandItem>> HANDS;

    static {
        Map<S11Assets.Hand, DeferredItem<HandItem>> m = new EnumMap<>(S11Assets.Hand.class);
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            m.put(h, AllItems.ITEMS.register(h.item, () -> new HandItem(h, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())));
        }
        HANDS = Collections.unmodifiableMap(m);
    }

    public static Item hand(S11Assets.Hand h) {
        return HANDS.get(h).get();
    }

    // --- The Thule Society, the banshee, the Chitters --------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<ThuleSoldierEntity>> THULE_SOLDIER = AllEntities.ENTITY_TYPES.register(
            S11Assets.THULE_SOLDIER.rig(), () -> EntityType.Builder.<ThuleSoldierEntity>of(ThuleSoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).clientTrackingRange(10).build(S11Assets.THULE_SOLDIER.rig()));
    public static final DeferredHolder<EntityType<?>, EntityType<ThuleOfficerEntity>> THULE_OFFICER = AllEntities.ENTITY_TYPES.register(
            S11Assets.THULE_OFFICER.rig(), () -> EntityType.Builder.<ThuleOfficerEntity>of(ThuleOfficerEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.7f).clientTrackingRange(10).build(S11Assets.THULE_OFFICER.rig()));
    public static final DeferredHolder<EntityType<?>, EntityType<BansheeEntity>> BANSHEE = AllEntities.ENTITY_TYPES.register(
            S11Assets.BANSHEE.rig(), () -> EntityType.Builder.<BansheeEntity>of(BansheeEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f).eyeHeight(1.65f).clientTrackingRange(10).build(S11Assets.BANSHEE.rig()));
    public static final DeferredHolder<EntityType<?>, EntityType<ChitterEntity>> CHITTER = AllEntities.ENTITY_TYPES.register(
            S11Assets.CHITTER.rig(), () -> EntityType.Builder.<ChitterEntity>of(ChitterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).eyeHeight(1.55f).clientTrackingRange(8).build(S11Assets.CHITTER.rig()));

    public static final DeferredItem<DeferredSpawnEggItem> THULE_SOLDIER_SPAWN_EGG = AllItems.ITEMS.register("thule_soldier_spawn_egg",
            () -> new DeferredSpawnEggItem(THULE_SOLDIER, 0x5a5f58, 0x2a2a2a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> THULE_OFFICER_SPAWN_EGG = AllItems.ITEMS.register("thule_officer_spawn_egg",
            () -> new DeferredSpawnEggItem(THULE_OFFICER, 0x1a1a1c, 0x8a1c1c, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BANSHEE_SPAWN_EGG = AllItems.ITEMS.register("banshee_spawn_egg",
            () -> new DeferredSpawnEggItem(BANSHEE, 0xd8d8d0, 0x6e7a86, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CHITTER_SPAWN_EGG = AllItems.ITEMS.register("chitter_spawn_egg",
            () -> new DeferredSpawnEggItem(CHITTER, 0x8a6e5c, 0x2e1c14, new Item.Properties()));

    public static final List<DeferredItem<DeferredSpawnEggItem>> EGGS = List.of(THULE_SOLDIER_SPAWN_EGG, THULE_OFFICER_SPAWN_EGG,
            BANSHEE_SPAWN_EGG, CHITTER_SPAWN_EGG);

    // --- A hunter's claimed Hands ---------------------------------------------------------------------------------------------
    public static final Supplier<AttachmentType<HandClaims>> HAND_CLAIMS = AllAttachments.ATTACHMENT_TYPES.register("hand_claims",
            () -> AttachmentType.builder(() -> HandClaims.EMPTY).serialize(HandClaims.CODEC).copyOnDeath().build());

    // --- Sounds (vanilla re-pitched until the art encodes them) ---------------------------------------------------------------
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> THULE_SOUNDS = AllSounds.registerGroup("thule", S11Assets.SOUNDS.get("thule"));
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BANSHEE_SOUNDS = AllSounds.registerGroup("banshee", S11Assets.SOUNDS.get("banshee"));
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> CHITTER_SOUNDS = AllSounds.registerGroup("chitter", S11Assets.SOUNDS.get("chitter"));

    public static List<Map<String, DeferredHolder<SoundEvent, SoundEvent>>> soundGroups() {
        return List.of(THULE_SOUNDS, BANSHEE_SOUNDS, CHITTER_SOUNDS);
    }

    public static SoundEvent thule(String id) {
        return sound(THULE_SOUNDS, "thule." + id);
    }

    public static SoundEvent banshee(String id) {
        return sound(BANSHEE_SOUNDS, "banshee." + id);
    }

    public static SoundEvent chitter(String id) {
        return sound(CHITTER_SOUNDS, "chitter." + id);
    }

    private static SoundEvent sound(Map<String, DeferredHolder<SoundEvent, SoundEvent>> group, String name) {
        DeferredHolder<SoundEvent, SoundEvent> h = group.get(name);
        if (h == null) throw new IllegalArgumentException("No sound " + name);
        return h.get();
    }

    // --- Worldgen: the reliquaries --------------------------------------------------------------------------------------------
    public static final DeferredHolder<StructureType<?>, StructureType<ReliquaryStructure>> RELIQUARY_TYPE =
            AllStructures.TYPES.register("reliquary", () -> () -> ReliquaryStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RELIQUARY_PIECE =
            AllStructures.PIECES.register("reliquary", () -> (StructurePieceType.ContextlessType) ReliquaryPiece::new);

    // --- Tags -----------------------------------------------------------------------------------------------------------------
    /** {@code #supernaturalcraft:hands_of_god}: the five Hands (Amara's rite spends 3-5 of them). */
    public static final TagKey<Item> HANDS_OF_GOD = TagKey.create(Registries.ITEM, SupernaturalCraft.asResource("hands_of_god"));
    /** {@code #supernaturalcraft:hand_reliquaries}: the five reliquaries (the tracking rite and Casifer search it). */
    public static final TagKey<Structure> HAND_RELIQUARIES = TagKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("hand_reliquaries"));
    /** Where a banshee haunts (swamps, mangroves, dark forests). */
    public static final TagKey<Biome> BANSHEE_HAUNTS = TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("banshee_haunts"));

    /** Every plain block of the slice (each drops itself), in art order. */
    public static List<DeferredBlock<? extends Block>> plainBlocks() {
        List<DeferredBlock<? extends Block>> out = new ArrayList<>(RELIQUARY_BLOCKS.values());
        out.add(CHITTER_NEST);
        return out;
    }

    public static void init() {
    }
}
