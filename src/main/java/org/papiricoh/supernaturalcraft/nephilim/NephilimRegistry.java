package org.papiricoh.supernaturalcraft.nephilim;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.TrophyBlock;

/**
 * v0.41: the core of seasons 12-14: the road's state, Jack, the rift key, Asmodeus, Michael's offer and vessel, soulless Jack, the Empty and the Shadow.
 *
 * <p>This slice's own registry holder: it registers through the shared {@code AllBlocks.BLOCKS}, {@code AllItems.ITEMS},
 * {@code AllEntities.ENTITY_TYPES}, {@code AllBlockEntities.BLOCK_ENTITIES}, {@code AllAttachments.ATTACHMENT_TYPES} and
 * {@code AllSounds.registerGroup} (so the creative tab lists everything by itself), and {@link #init()} is called from the
 * mod's constructor. Blocks are declared before the items that place them.
 */
public final class NephilimRegistry {

    private NephilimRegistry() {
    }

    // --- Trophies (BossTrophies: the crossroads demon can fetch them) ------------------------------------------------
    public static final DeferredBlock<TrophyBlock> ASMODEUS_TROPHY = AllBlocks.BLOCKS.register("asmodeus_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> ASMODEUS_TROPHY_ITEM = AllItems.ITEMS.register("asmodeus_trophy",
            () -> new BlockItem(ASMODEUS_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredBlock<TrophyBlock> MICHAEL_VESSEL_TROPHY = AllBlocks.BLOCKS.register("michael_vessel_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> MICHAEL_VESSEL_TROPHY_ITEM = AllItems.ITEMS.register("michael_vessel_trophy",
            () -> new BlockItem(MICHAEL_VESSEL_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredBlock<TrophyBlock> SOULLESS_JACK_TROPHY = AllBlocks.BLOCKS.register("soulless_jack_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> SOULLESS_JACK_TROPHY_ITEM = AllItems.ITEMS.register("soulless_jack_trophy",
            () -> new BlockItem(SOULLESS_JACK_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredBlock<TrophyBlock> SHADOW_TROPHY = AllBlocks.BLOCKS.register("shadow_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> SHADOW_TROPHY_ITEM = AllItems.ITEMS.register("shadow_trophy",
            () -> new BlockItem(SHADOW_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // --- The road's state ---------------------------------------------------------------------------------------------
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Nephilim>> NEPHILIM =
            org.papiricoh.supernaturalcraft.registry.AllAttachments.ATTACHMENT_TYPES.register("nephilim",
                    () -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> Nephilim.EMPTY)
                            .serialize(Nephilim.CODEC).copyOnDeath().build());

    // --- Jack's gifts ---------------------------------------------------------------------------------------------------
    /** What soulless Jack leaves: a nephilim's grace (+mana once taken in; opens the Empty). */
    public static final DeferredItem<org.papiricoh.supernaturalcraft.nephilim.NephilimGraceItem> NEPHILIM_GRACE = AllItems.ITEMS.register("nephilim_grace",
            () -> new org.papiricoh.supernaturalcraft.nephilim.NephilimGraceItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    /** Jack's blood (once a day, from Jack while he has his grace): a reagent of the rift's rite. */
    public static final DeferredItem<Item> NEPHILIM_BLOOD = AllItems.ITEMS.register("nephilim_blood",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    // --- Jack -----------------------------------------------------------------------------------------------------------
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.nephilim.jack.JackEntity>> JACK =
            org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register("jack", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.nephilim.jack.JackEntity>of(org.papiricoh.supernaturalcraft.nephilim.jack.JackEntity::new,
                            net.minecraft.world.entity.MobCategory.CREATURE)
                    .sized(org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets.WIDTH, org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets.HEIGHT)
                    .eyeHeight(1.6f).fireImmune().clientTrackingRange(10).build("jack"));
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<org.papiricoh.supernaturalcraft.nephilim.jack.JackBond>> JACK_BOND =
            org.papiricoh.supernaturalcraft.registry.AllAttachments.ATTACHMENT_TYPES.register("jack_bond",
                    () -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.nephilim.jack.JackBond.EMPTY)
                            .serialize(org.papiricoh.supernaturalcraft.nephilim.jack.JackBond.CODEC).copyOnDeath().build());

    // --- The Lance made whole ---------------------------------------------------------------------------------------
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.component.DataComponentType<?>,
            net.minecraft.core.component.DataComponentType<Boolean>> ARCHANGEL_BANE =
            org.papiricoh.supernaturalcraft.registry.AllDataComponents.DATA_COMPONENTS.registerComponentType("archangel_bane",
                    b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    // --- The bosses -----------------------------------------------------------------------------------------------------
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusEntity>> ASMODEUS = boss("asmodeus",
            org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusEntity::new, org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusAssets.WIDTH,
            org.papiricoh.supernaturalcraft.entity.boss.asmodeus.AsmodeusAssets.HEIGHT);

    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselEntity>> MICHAEL_VESSEL = boss("michael_vessel",
            org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselEntity::new,
            org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselAssets.WIDTH, org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselAssets.HEIGHT);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackEntity>> SOULLESS_JACK = boss("soulless_jack",
            org.papiricoh.supernaturalcraft.entity.boss.soulless.SoullessJackEntity::new,
            org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets.WIDTH, org.papiricoh.supernaturalcraft.nephilim.jack.JackAssets.HEIGHT);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowEntity>> SHADOW = boss("shadow",
            org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowEntity::new,
            org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowAssets.WIDTH, org.papiricoh.supernaturalcraft.entity.boss.shadow.ShadowAssets.HEIGHT);
    /** Michael at dawn with his offer (season 14). */
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<org.papiricoh.supernaturalcraft.nephilim.michael.MichaelEnvoyEntity>> MICHAEL_ENVOY =
            org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register("michael_envoy", () -> net.minecraft.world.entity.EntityType.Builder
                    .<org.papiricoh.supernaturalcraft.nephilim.michael.MichaelEnvoyEntity>of(org.papiricoh.supernaturalcraft.nephilim.michael.MichaelEnvoyEntity::new,
                            net.minecraft.world.entity.MobCategory.CREATURE)
                    .sized(0.6f, 1.85f).eyeHeight(1.62f).fireImmune().clientTrackingRange(10).build("michael_envoy"));

    // --- The Empty ------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<net.minecraft.world.level.block.Block> EMPTY_FLOOR = AllBlocks.BLOCKS.register("empty_floor",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(50f, 1200f)
                    .sound(SoundType.SCULK)));
    public static final DeferredBlock<org.papiricoh.supernaturalcraft.empty.EmptyBlocks.Sleeper> EMPTY_SLEEPER = AllBlocks.BLOCKS.register("empty_sleeper",
            () -> new org.papiricoh.supernaturalcraft.empty.EmptyBlocks.Sleeper(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1f, 3600000f).noOcclusion().noLootTable().sound(SoundType.HONEY_BLOCK)));
    public static final DeferredBlock<net.minecraft.world.level.block.CarpetBlock> EMPTY_TAR = AllBlocks.BLOCKS.register("empty_tar",
            () -> new net.minecraft.world.level.block.CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.3f)
                    .sound(SoundType.HONEY_BLOCK).speedFactor(0.6f)));
    public static final DeferredItem<BlockItem> EMPTY_FLOOR_ITEM = AllItems.ITEMS.register("empty_floor",
            () -> new BlockItem(EMPTY_FLOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> EMPTY_TAR_ITEM = AllItems.ITEMS.register("empty_tar",
            () -> new BlockItem(EMPTY_TAR.get(), new Item.Properties()));
    public static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<org.papiricoh.supernaturalcraft.empty.EmptyReturn>> EMPTY_RETURN =
            org.papiricoh.supernaturalcraft.registry.AllAttachments.ATTACHMENT_TYPES.register("empty_return",
                    () -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> org.papiricoh.supernaturalcraft.empty.EmptyReturn.NONE)
                            .serialize(org.papiricoh.supernaturalcraft.empty.EmptyReturn.CODEC).copyOnDeath().build());

    // --- Spawn eggs (template art) -------------------------------------------------------------------------------------
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> ASMODEUS_SPAWN_EGG = egg("asmodeus_spawn_egg", ASMODEUS, 0xF2F0E8, 0xC9A23A);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> MICHAEL_VESSEL_SPAWN_EGG = egg("michael_vessel_spawn_egg", MICHAEL_VESSEL, 0x2A2C33, 0x9FD8FF);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> SOULLESS_JACK_SPAWN_EGG = egg("soulless_jack_spawn_egg", SOULLESS_JACK, 0xB08A5A, 0xFFD24A);
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> SHADOW_SPAWN_EGG = egg("shadow_spawn_egg", SHADOW, 0x0A0A0E, 0x6A3FA0);

    private static DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> egg(String id,
            java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type, int a, int b) {
        return AllItems.ITEMS.register(id, () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(type, a, b, new Item.Properties().rarity(Rarity.RARE)));
    }

    private static <T extends net.minecraft.world.entity.Mob> net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
            net.minecraft.world.entity.EntityType<T>> boss(String id, net.minecraft.world.entity.EntityType.EntityFactory<T> f, float w, float h) {
        return org.papiricoh.supernaturalcraft.registry.AllEntities.ENTITY_TYPES.register(id, () -> net.minecraft.world.entity.EntityType.Builder
                .of(f, net.minecraft.world.entity.MobCategory.MONSTER).sized(w, h).eyeHeight(h * 0.88f).fireImmune()
                .clientTrackingRange(24).updateInterval(1).build(id));
    }

    public static void init() {
        NephilimSounds.init();
        org.papiricoh.supernaturalcraft.nephilim.jack.JackCompanions.registerDialogue();
        org.papiricoh.supernaturalcraft.nephilim.michael.MichaelEnvoyEntity.registerDialogue();
    }
}
