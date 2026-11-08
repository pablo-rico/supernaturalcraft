package org.papiricoh.supernaturalcraft.datagen.hands;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.datagen.slice.SliceData;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * v0.42 datagen hooks: the Hands of God, their reliquaries, the Thule Society, the banshee and the Chitters. The English text is
 * in {@code HandsLang}; the Spanish in {@code tools/lang_fragments/hands.json} (merged into {@code es_es.json} at the end); the
 * reliquaries' worldgen and their biome and structure tags in {@link HandsGen}.
 *
 * <p>Textures the art draws ({@code textures/block/}): {@code <id>.png} for every reliquary block, {@code chitter_nest} and
 * {@code chitter_egg}; the altar is a column: {@code hand_altar_side.png} and {@code hand_altar_top.png}. Missing ones are only
 * tracked here (the models point at them), so datagen never fails before the art lands.
 */
public final class HandsData implements SliceData {

    private static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");

    /** Vanilla stand-ins (event, pitch) until the art encodes the slice's sounds. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("thule.rise", new Object[]{"entity.zombie.ambient", 0.6f}),
            Map.entry("thule.shoot", new Object[]{"entity.generic.explode", 1.8f}),
            Map.entry("thule.chant", new Object[]{"entity.evoker.prepare_summon", 0.7f}),
            Map.entry("thule.hurt", new Object[]{"entity.zombie.hurt", 0.8f}),
            Map.entry("thule.death", new Object[]{"entity.zombie.death", 0.7f}),
            Map.entry("banshee.wail", new Object[]{"entity.ghast.ambient", 1.6f}),
            Map.entry("banshee.scream", new Object[]{"entity.ghast.scream", 1.3f}),
            Map.entry("banshee.hurt", new Object[]{"entity.vex.hurt", 0.7f}),
            Map.entry("banshee.death", new Object[]{"entity.ghast.death", 1.4f}),
            Map.entry("chitter.chitter", new Object[]{"entity.silverfish.ambient", 0.6f}),
            Map.entry("chitter.screech", new Object[]{"entity.ravager.roar", 1.8f}),
            Map.entry("chitter.hurt", new Object[]{"entity.spider.hurt", 0.8f}),
            Map.entry("chitter.death", new Object[]{"entity.spider.death", 0.7f}));

    @Override
    public void lang(BiConsumer<String, String> add) {
        HandsLang.lang(add);
    }

    @Override
    public Map<String, String> names() {
        return HandsLang.NAMES;
    }

    @Override
    public Map<String, String> subtitles() {
        return HandsLang.SUBTITLES;
    }

    @Override
    public void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (var group : HandsRegistry.soundGroups()) {
            group.forEach((event, holder) -> {
                ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
                SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
                if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                    def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND)
                            .attenuationDistance(event.endsWith("scream") || event.endsWith("shoot") ? 48 : 20));
                } else {
                    Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.zombie.ambient", 0.8f});
                    def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                            .pitch((Float) f[1]));
                }
                add.accept(holder, def);
            });
        }
    }

    // --- models ---------------------------------------------------------------------------------------------------------------

    private static ResourceLocation pending(ExistingFileHelper existing, ResourceLocation tex) {
        if (!existing.exists(tex, TEXTURE)) existing.trackGenerated(tex, TEXTURE);
        return tex;
    }

    @Override
    public void blockStates(SNBlockStateProvider p, ExistingFileHelper existing) {
        for (var b : HandsRegistry.plainBlocks()) {
            String id = b.getId().getPath();
            p.simpleBlockWithItem(b.get(), p.models().cubeAll(id, pending(existing, p.modLoc("block/" + id))));
        }
        p.simpleBlockWithItem(HandsRegistry.HAND_ALTAR.get(), p.models().cubeColumn(S11Assets.HAND_ALTAR,
                pending(existing, p.modLoc("block/hand_altar_side")), pending(existing, p.modLoc("block/hand_altar_top"))));
        ResourceLocation egg = pending(existing, p.modLoc("block/chitter_egg"));
        ModelFile sac = p.models().withExistingParent("chitter_egg", p.mcLoc("block/block"))
                .texture("particle", egg).texture("egg", egg)
                .element().from(3, 0, 3).to(13, 10, 13).allFaces((dir, face) -> face.texture("#egg")).end();
        p.simpleBlockWithItem(HandsRegistry.CHITTER_EGG.get(), sac);
    }

    @Override
    public void itemModels(SNItemModelProvider p, ExistingFileHelper existing) {
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            pending(existing, p.modLoc("item/" + h.item));
            p.basicItem(HandsRegistry.hand(h));
        }
        for (var egg : HandsRegistry.EGGS) p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
    }

    // --- loot -----------------------------------------------------------------------------------------------------------------

    private static LootPool.Builder pool(Item item, float min, float max) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))));
    }

    private static LootPool.Builder rare(Item item, float chance) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(chance)).add(LootItem.lootTableItem(item));
    }

    @Override
    public void entityLoot(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        // A raised soldier: rotten flesh, the rifle's iron and powder now and then.
        add.accept(HandsRegistry.THULE_SOLDIER.get(), LootTable.lootTable().withPool(pool(Items.ROTTEN_FLESH, 0, 2))
                .withPool(pool(Items.IRON_NUGGET, 0, 2)).withPool(rare(Items.GUNPOWDER, 0.25f)));
        // An officer: gold, and sometimes his book of rites.
        add.accept(HandsRegistry.THULE_OFFICER.get(), LootTable.lootTable().withPool(pool(Items.GOLD_NUGGET, 1, 4))
                .withPool(pool(Items.ROTTEN_FLESH, 0, 1)).withPool(rare(Items.BOOK, 0.3f)));
        // The banshee leaves what a spirit leaves.
        add.accept(HandsRegistry.BANSHEE.get(), LootTable.lootTable().withPool(pool(AllItems.ECTOPLASM.get(), 1, 2))
                .withPool(rare(Items.PHANTOM_MEMBRANE, 0.3f)));
        // A Chitter: hair, teeth, what the thing in it did not eat.
        add.accept(HandsRegistry.CHITTER.get(), LootTable.lootTable().withPool(pool(Items.STRING, 0, 2))
                .withPool(pool(Items.BONE, 0, 1)).withPool(rare(Items.SPIDER_EYE, 0.4f)));
    }

    @Override
    public void blockLoot(Consumer<Block> dropSelf, BiConsumer<Block, LootTable.Builder> add) {
        for (var b : HandsRegistry.plainBlocks()) dropSelf.accept(b.get());
        // An egg sac bursts with nothing inside.
        add.accept(HandsRegistry.CHITTER_EGG.get(), LootTable.lootTable());
    }

    // --- tags -----------------------------------------------------------------------------------------------------------------

    @Override
    public void blockTags(BiConsumer<TagKey<Block>, Block> add) {
        for (var b : HandsRegistry.RELIQUARY_BLOCKS.values()) add.accept(BlockTags.MINEABLE_WITH_PICKAXE, b.get());
        for (String id : List.of("rusted_hull", "rusted_hull_plate", "thule_sigil_plate", "ark_gold_trim")) {
            add.accept(BlockTags.NEEDS_STONE_TOOL, HandsRegistry.RELIQUARY_BLOCKS.get(id).get());
        }
        add.accept(BlockTags.MINEABLE_WITH_HOE, HandsRegistry.CHITTER_NEST.get());
        add.accept(BlockTags.MINEABLE_WITH_HOE, HandsRegistry.CHITTER_EGG.get());
    }

    @Override
    public void itemTags(BiConsumer<TagKey<Item>, Item> add) {
        for (S11Assets.Hand h : S11Assets.Hand.values()) {
            add.accept(HandsRegistry.HANDS_OF_GOD, HandsRegistry.hand(h));
            // Amara's rite spends them as its arena's stake: given back if the fight is lost.
            add.accept(AllTags.Items.PROGRESSION_KEYS, HandsRegistry.hand(h));
        }
    }

    @Override
    public void entityTags(BiConsumer<TagKey<EntityType<?>>, EntityType<?>> add) {
        for (EntityType<?> t : List.<EntityType<?>>of(HandsRegistry.THULE_SOLDIER.get(), HandsRegistry.THULE_OFFICER.get())) {
            add.accept(EntityTypeTags.UNDEAD, t);
            add.accept(EntityTypeTags.SENSITIVE_TO_SMITE, t);
            add.accept(AllTags.Entities.SUPERNATURAL, t);
        }
        add.accept(AllTags.Entities.SUPERNATURAL, HandsRegistry.BANSHEE.get());
        add.accept(AllTags.Entities.SUPERNATURAL, HandsRegistry.CHITTER.get());
        add.accept(EntityTypeTags.CAN_BREATHE_UNDER_WATER, HandsRegistry.THULE_SOLDIER.get());
        add.accept(EntityTypeTags.CAN_BREATHE_UNDER_WATER, HandsRegistry.THULE_OFFICER.get());
    }

    // --- worldgen -------------------------------------------------------------------------------------------------------------

    @Override
    public void structures(BootstrapContext<Structure> ctx) {
        HandsGen.structures(ctx);
    }

    @Override
    public void structureSets(BootstrapContext<StructureSet> ctx) {
        HandsGen.structureSets(ctx);
    }

    @Override
    public void gather(GatherDataEvent event, DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                       ExistingFileHelper existing) {
        HandsGen.gather(event, generator, output, lookup, existing);
    }
}
