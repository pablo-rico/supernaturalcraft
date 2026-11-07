package org.papiricoh.supernaturalcraft.british;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentAssets;
import org.papiricoh.supernaturalcraft.british.agent.BritishAgentEntity;
import org.papiricoh.supernaturalcraft.british.arsenal.BritishDossierItem;
import org.papiricoh.supernaturalcraft.british.arsenal.BritishPistolItem;
import org.papiricoh.supernaturalcraft.british.arsenal.DagonsBrandItem;
import org.papiricoh.supernaturalcraft.british.arsenal.HessFolioItem;
import org.papiricoh.supernaturalcraft.british.arsenal.HexGrenadeEntity;
import org.papiricoh.supernaturalcraft.british.arsenal.HexGrenadeItem;
import org.papiricoh.supernaturalcraft.british.birth.BirthAssets;
import org.papiricoh.supernaturalcraft.british.birth.KellyKlineEntity;
import org.papiricoh.supernaturalcraft.british.birth.RourkeLuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonAssets;
import org.papiricoh.supernaturalcraft.entity.boss.dagon.DagonEntity;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessAssets;
import org.papiricoh.supernaturalcraft.entity.boss.hess.HessEntity;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchAssets;
import org.papiricoh.supernaturalcraft.entity.boss.ketch.KetchEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.TrophyBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * v0.41: season 12, the British Men of Letters (Hess, Ketch, their agents and arsenal), Dagon, Kelly Kline and Jack's birth.
 *
 * <p>This slice's own registry holder: it registers through the shared {@code AllBlocks.BLOCKS}, {@code AllItems.ITEMS},
 * {@code AllEntities.ENTITY_TYPES}, {@code AllBlockEntities.BLOCK_ENTITIES}, {@code AllAttachments.ATTACHMENT_TYPES} and
 * {@code AllSounds.registerGroup} (so the creative tab lists everything by itself), and {@link #init()} is called from the
 * mod's constructor. Blocks are declared before the items that place them.
 */
public final class BritishRegistry {

    private BritishRegistry() {
    }

    // --- Trophies (BossTrophies: the crossroads demon can fetch them) ------------------------------------------------
    public static final DeferredBlock<TrophyBlock> KETCH_TROPHY = AllBlocks.BLOCKS.register("ketch_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> KETCH_TROPHY_ITEM = AllItems.ITEMS.register("ketch_trophy",
            () -> new BlockItem(KETCH_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredBlock<TrophyBlock> HESS_TROPHY = AllBlocks.BLOCKS.register("hess_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> HESS_TROPHY_ITEM = AllItems.ITEMS.register("hess_trophy",
            () -> new BlockItem(HESS_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredBlock<TrophyBlock> DAGON_TROPHY = AllBlocks.BLOCKS.register("dagon_trophy",
            () -> new TrophyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2f, 1200f).noOcclusion().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> DAGON_TROPHY_ITEM = AllItems.ITEMS.register("dagon_trophy",
            () -> new BlockItem(DAGON_TROPHY.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // --- Entities -------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KetchEntity>> KETCH = AllEntities.ENTITY_TYPES.register("ketch",
            () -> EntityType.Builder.<KetchEntity>of(KetchEntity::new, MobCategory.MONSTER)
                    .sized(KetchAssets.WIDTH, KetchAssets.HEIGHT).eyeHeight(1.65f).fireImmune().clientTrackingRange(24).updateInterval(1).build("ketch"));
    public static final DeferredHolder<EntityType<?>, EntityType<HessEntity>> HESS = AllEntities.ENTITY_TYPES.register("hess",
            () -> EntityType.Builder.<HessEntity>of(HessEntity::new, MobCategory.MONSTER)
                    .sized(HessAssets.WIDTH, HessAssets.HEIGHT).eyeHeight(1.6f).fireImmune().clientTrackingRange(24).updateInterval(1).build("hess"));
    public static final DeferredHolder<EntityType<?>, EntityType<HessEnvoyEntity>> HESS_ENVOY = AllEntities.ENTITY_TYPES.register("hess_envoy",
            () -> EntityType.Builder.<HessEnvoyEntity>of(HessEnvoyEntity::new, MobCategory.MISC)
                    .sized(HessAssets.WIDTH, HessAssets.HEIGHT).eyeHeight(1.6f).fireImmune().clientTrackingRange(10).build("hess_envoy"));
    public static final DeferredHolder<EntityType<?>, EntityType<DagonEntity>> DAGON = AllEntities.ENTITY_TYPES.register("dagon",
            () -> EntityType.Builder.<DagonEntity>of(DagonEntity::new, MobCategory.MONSTER)
                    .sized(DagonAssets.WIDTH, DagonAssets.HEIGHT).eyeHeight(1.75f).fireImmune().clientTrackingRange(24).updateInterval(1).build("dagon"));
    public static final DeferredHolder<EntityType<?>, EntityType<BritishAgentEntity>> BRITISH_AGENT = AllEntities.ENTITY_TYPES.register("british_agent",
            () -> EntityType.Builder.<BritishAgentEntity>of(BritishAgentEntity::new, MobCategory.MONSTER)
                    .sized(BritishAgentAssets.WIDTH, BritishAgentAssets.HEIGHT).eyeHeight(1.62f).clientTrackingRange(10).build("british_agent"));
    public static final DeferredHolder<EntityType<?>, EntityType<KellyKlineEntity>> KELLY_KLINE = AllEntities.ENTITY_TYPES.register("kelly_kline",
            () -> EntityType.Builder.<KellyKlineEntity>of(KellyKlineEntity::new, MobCategory.MISC)
                    .sized(BirthAssets.WIDTH, BirthAssets.HEIGHT).eyeHeight(1.55f).fireImmune().clientTrackingRange(10).build("kelly_kline"));
    public static final DeferredHolder<EntityType<?>, EntityType<RourkeLuciferEntity>> ROURKE_LUCIFER = AllEntities.ENTITY_TYPES.register("rourke_lucifer",
            () -> EntityType.Builder.<RourkeLuciferEntity>of(RourkeLuciferEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.9f).eyeHeight(1.7f).fireImmune().clientTrackingRange(10).build("rourke_lucifer"));
    public static final DeferredHolder<EntityType<?>, EntityType<HexGrenadeEntity>> HEX_GRENADE_ENTITY = AllEntities.ENTITY_TYPES.register("hex_grenade",
            () -> EntityType.Builder.<HexGrenadeEntity>of(HexGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("hex_grenade"));

    // --- The British arsenal ------------------------------------------------------------------------------------------
    /** A suppressed pistol that fires {@link #ENCHANTED_ROUND}s: each shot carries a hex. */
    public static final DeferredItem<BritishPistolItem> BRITISH_PISTOL = AllItems.ITEMS.register("british_pistol",
            () -> new BritishPistolItem(new Item.Properties().stacksTo(1).durability(600).rarity(Rarity.RARE)));
    /** A brass round with a sigil carved into it (the pistol's ammunition). */
    public static final DeferredItem<Item> ENCHANTED_ROUND = AllItems.ITEMS.register("enchanted_round",
            () -> new Item(new Item.Properties().stacksTo(64)));
    /** A canister that bursts into a hex: slows, weakens and blinds what stands in it. */
    public static final DeferredItem<HexGrenadeItem> HEX_GRENADE = AllItems.ITEMS.register("hex_grenade",
            () -> new HexGrenadeItem(new Item.Properties().stacksTo(16)));
    /** Ketch's dossier on the British compound: reading it raises the compound and points a compass at it. */
    public static final DeferredItem<BritishDossierItem> BRITISH_DOSSIER = AllItems.ITEMS.register("british_dossier",
            () -> new BritishDossierItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** Hess's folio: one ward read aloud, once. */
    public static final DeferredItem<HessFolioItem> HESS_FOLIO = AllItems.ITEMS.register("hess_folio",
            () -> new HessFolioItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    /** Dagon's brand: a charm of hellfire, a ring of it round its bearer. */
    public static final DeferredItem<DagonsBrandItem> DAGONS_BRAND = AllItems.ITEMS.register("dagons_brand",
            () -> new DagonsBrandItem(new Item.Properties().stacksTo(1).durability(24).rarity(Rarity.EPIC).fireResistant()));

    // --- Spawn eggs -----------------------------------------------------------------------------------------------------
    public static final DeferredItem<DeferredSpawnEggItem> KETCH_SPAWN_EGG = AllItems.ITEMS.register("ketch_spawn_egg",
            () -> new DeferredSpawnEggItem(KETCH, 0x3a3d42, 0x111214, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> HESS_SPAWN_EGG = AllItems.ITEMS.register("hess_spawn_egg",
            () -> new DeferredSpawnEggItem(HESS, 0x7a5c44, 0xc9c2b5, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> DAGON_SPAWN_EGG = AllItems.ITEMS.register("dagon_spawn_egg",
            () -> new DeferredSpawnEggItem(DAGON, 0x141014, 0x8a1418, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> BRITISH_AGENT_SPAWN_EGG = AllItems.ITEMS.register("british_agent_spawn_egg",
            () -> new DeferredSpawnEggItem(BRITISH_AGENT, 0x18191c, 0xe8e8e8, new Item.Properties()));

    // --- State ----------------------------------------------------------------------------------------------------------
    public static final Supplier<AttachmentType<British>> BRITISH = AllAttachments.ATTACHMENT_TYPES.register("british",
            () -> AttachmentType.builder(() -> British.EMPTY).serialize(British.CODEC).copyOnDeath().build());

    // --- Sounds ---------------------------------------------------------------------------------------------------------
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SOUNDS = new HashMap<>();

    static {
        SOUNDS.putAll(AllSounds.registerGroup(KetchAssets.RIG, KetchAssets.SOUNDS));
        SOUNDS.putAll(AllSounds.registerGroup(HessAssets.RIG, HessAssets.SOUNDS));
        SOUNDS.putAll(AllSounds.registerGroup(DagonAssets.RIG, DagonAssets.SOUNDS));
        SOUNDS.putAll(AllSounds.registerGroup(BritishAgentAssets.RIG, BritishAgentAssets.SOUNDS));
        SOUNDS.putAll(AllSounds.registerGroup(BirthAssets.RIG, BirthAssets.SOUNDS));
    }

    /** One of this slice's sounds, {@code <rig>.<id>}. */
    public static SoundEvent sound(String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = SOUNDS.get(id);
        if (h == null) throw new IllegalArgumentException("No British sound " + id);
        return h.get();
    }

    public static void init() {
        BritishServer.registerDialogues();
    }
}
