package org.papiricoh.supernaturalcraft.datagen.apocworld;

import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.rift.AuRiftBlock;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.datagen.slice.SliceData;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * v0.41 datagen hooks: season 13, the Apocalypse World (its land, blocks and worldgen in {@link ApocWorldGen}), Michael's army, the
 * camps, his fortress and AU Michael. The English text is in {@link ApocWorldLang}; the Spanish in
 * {@code tools/lang_fragments/apocworld.json}.
 */
public final class ApocWorldData implements SliceData {

    private static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".png", "textures");

    /** Vanilla stand-ins (event, pitch) until the art encodes the slice's sounds. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("au_angel_soldier.smite", new Object[]{"block.beacon.power_select", 1.4f}),
            Map.entry("au_angel_soldier.hurt", new Object[]{"entity.iron_golem.hurt", 1.3f}),
            Map.entry("au_angel_soldier.death", new Object[]{"block.beacon.deactivate", 1.2f}),
            Map.entry("au_michael.slash", new Object[]{"entity.player.attack.sweep", 0.7f}),
            Map.entry("au_michael.smite", new Object[]{"block.beacon.activate", 0.7f}),
            Map.entry("au_michael.gale", new Object[]{"entity.breeze.wind_burst", 0.6f}),
            Map.entry("au_michael.thunder", new Object[]{"entity.lightning_bolt.thunder", 0.8f}),
            Map.entry("au_michael.hurt", new Object[]{"entity.iron_golem.hurt", 0.9f}),
            Map.entry("au_michael.flee", new Object[]{"block.end_portal.spawn", 0.7f}));

    @Override
    public void lang(BiConsumer<String, String> add) {
        ApocWorldLang.lang(add);
    }

    @Override
    public Map<String, String> names() {
        return ApocWorldLang.NAMES;
    }

    @Override
    public void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        for (var group : List.of(ApocWorldRegistry.SOLDIER_SOUNDS, ApocWorldRegistry.MICHAEL_SOUNDS)) {
            group.forEach((event, holder) -> {
                ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
                SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
                if (existing.exists(file, PackType.CLIENT_RESOURCES, ".ogg", "sounds")) {
                    def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(event.endsWith("thunder") ? 64 : 24));
                } else {
                    Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.allay.ambient_without_item", 0.8f});
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
        for (var b : List.of(ApocWorldRegistry.SCORCHED_STONE, ApocWorldRegistry.ASH_BLOCK, ApocWorldRegistry.SMOULDERING_ASH,
                ApocWorldRegistry.BURNED_PLANKS, ApocWorldRegistry.RUBBLE, ApocWorldRegistry.RUINED_BRICKS, ApocWorldRegistry.WRECKED_METAL,
                ApocWorldRegistry.CITADEL_STONE, ApocWorldRegistry.CAMP_LOCK)) {
            String id = b.getId().getPath();
            p.simpleBlockWithItem(b.get(), p.models().cubeAll(id, pending(existing, p.modLoc("block/" + id))));
        }
        p.simpleBlockWithItem(ApocWorldRegistry.SCORCHED_SOIL.get(), p.models().cubeBottomTop("scorched_soil",
                pending(existing, p.modLoc("block/scorched_soil")), pending(existing, p.modLoc("block/scorched_soil")),
                pending(existing, p.modLoc("block/scorched_soil_top"))));
        p.axisBlock(ApocWorldRegistry.BURNED_LOG.get(), pending(existing, p.modLoc("block/burned_log")), pending(existing, p.modLoc("block/burned_log_top")));
        p.simpleBlockItem(ApocWorldRegistry.BURNED_LOG.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/burned_log")));
        ResourceLocation ash = pending(existing, p.modLoc("block/au_ash_block"));
        ModelFile layer = p.models().withExistingParent("ash_layer", p.mcLoc("block/thin_block"))
                .texture("particle", ash)
                .element().from(0, 0, 0).to(16, 2, 16)
                .allFaces((dir, face) -> face.texture("#ash").cullface(dir == Direction.UP ? null : dir)).end()
                .texture("ash", ash);
        p.simpleBlockWithItem(ApocWorldRegistry.ASH_LAYER.get(), layer);
        p.horizontalBlock(ApocWorldRegistry.AU_MICHAEL_TROPHY.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/au_michael_trophy")));
        ResourceLocation rift = pending(existing, p.modLoc("block/au_rift"));
        ModelFile ns = p.models().withExistingParent("au_rift_ns", p.mcLoc("block/nether_portal_ns"))
                .texture("portal", rift).texture("particle", rift).renderType("translucent");
        ModelFile ew = p.models().withExistingParent("au_rift_ew", p.mcLoc("block/nether_portal_ew"))
                .texture("portal", rift).texture("particle", rift).renderType("translucent");
        p.getVariantBuilder(ApocWorldRegistry.AU_RIFT.get()).forAllStates(st -> ConfiguredModel.builder()
                .modelFile(st.getValue(AuRiftBlock.AXIS) == Direction.Axis.X ? ns : ew).build());
    }

    @Override
    public void itemModels(SNItemModelProvider p, ExistingFileHelper existing) {
        p.getBuilder(ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile(p.modLoc("block/au_michael_trophy")));
        pending(existing, p.modLoc("item/au_michael_greatcoat"));
        p.basicItem(ApocWorldRegistry.AU_MICHAEL_GREATCOAT.get());
        for (var egg : List.of(ApocWorldRegistry.AU_ANGEL_SOLDIER_SPAWN_EGG, ApocWorldRegistry.AU_PRISONER_SPAWN_EGG, ApocWorldRegistry.AU_MICHAEL_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    // --- loot -----------------------------------------------------------------------------------------------------------------

    @Override
    public void entityLoot(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        // A soldier carries a little iron and, now and then, his angel blade.
        add.accept(ApocWorldRegistry.AU_ANGEL_SOLDIER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(net.minecraft.world.item.Items.IRON_NUGGET)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 3)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.04f))
                        .add(LootItem.lootTableItem(AllItems.ANGEL_BLADE.get()))));
        add.accept(ApocWorldRegistry.AU_PRISONER.get(), LootTable.lootTable());
        // His spoils are given per hunter when he flees (AuMichaelSpoils); the shards by ShardSpoils.
        add.accept(ApocWorldRegistry.AU_MICHAEL.get(), LootTable.lootTable());
    }

    @Override
    public void blockLoot(Consumer<Block> dropSelf, BiConsumer<Block, LootTable.Builder> add) {
        for (var b : ApocWorldRegistry.LAND) dropSelf.accept(b.get());
        dropSelf.accept(ApocWorldRegistry.AU_MICHAEL_TROPHY.get());
    }

    // --- tags -----------------------------------------------------------------------------------------------------------------

    @Override
    public void blockTags(BiConsumer<TagKey<Block>, Block> add) {
        for (var b : List.of(ApocWorldRegistry.SCORCHED_STONE, ApocWorldRegistry.RUBBLE, ApocWorldRegistry.RUINED_BRICKS, ApocWorldRegistry.WRECKED_METAL,
                ApocWorldRegistry.CITADEL_STONE, ApocWorldRegistry.CAMP_LOCK, ApocWorldRegistry.AU_MICHAEL_TROPHY)) {
            add.accept(BlockTags.MINEABLE_WITH_PICKAXE, b.get());
        }
        for (var b : List.of(ApocWorldRegistry.SCORCHED_SOIL, ApocWorldRegistry.ASH_BLOCK, ApocWorldRegistry.ASH_LAYER, ApocWorldRegistry.SMOULDERING_ASH)) {
            add.accept(BlockTags.MINEABLE_WITH_SHOVEL, b.get());
        }
        add.accept(BlockTags.MINEABLE_WITH_AXE, ApocWorldRegistry.BURNED_LOG.get());
        add.accept(BlockTags.MINEABLE_WITH_AXE, ApocWorldRegistry.BURNED_PLANKS.get());
        add.accept(BlockTags.NEEDS_DIAMOND_TOOL, ApocWorldRegistry.CITADEL_STONE.get());
        add.accept(BlockTags.NEEDS_IRON_TOOL, ApocWorldRegistry.CAMP_LOCK.get());
        add.accept(BlockTags.NEEDS_STONE_TOOL, ApocWorldRegistry.WRECKED_METAL.get());
        // Michael's soldiers and their prisoners stand on the burned world's own ground, and the dead bushes grow there.
        for (var b : List.of(ApocWorldRegistry.SCORCHED_SOIL, ApocWorldRegistry.ASH_BLOCK)) add.accept(BlockTags.DEAD_BUSH_MAY_PLACE_ON, b.get());
    }

    @Override
    public void itemTags(BiConsumer<TagKey<Item>, Item> add) {
    }

    @Override
    public void entityTags(BiConsumer<TagKey<EntityType<?>>, EntityType<?>> add) {
        add.accept(AllTags.Entities.ANGELS, ApocWorldRegistry.AU_ANGEL_SOLDIER.get());
        add.accept(AllTags.Entities.SUPERNATURAL, ApocWorldRegistry.AU_ANGEL_SOLDIER.get());
        add.accept(AllTags.Entities.SUPERNATURAL, ApocWorldRegistry.AU_MICHAEL.get());
        add.accept(AllTags.Entities.BOSSES, ApocWorldRegistry.AU_MICHAEL.get());
        add.accept(ApocWorldRegistry.ARCHANGELS, ApocWorldRegistry.AU_MICHAEL.get());
    }

    // --- worldgen -------------------------------------------------------------------------------------------------------------

    @Override
    public void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ApocWorldGen.configuredFeatures(ctx);
    }

    @Override
    public void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        ApocWorldGen.placedFeatures(ctx);
    }

    @Override
    public void dimensionTypes(BootstrapContext<DimensionType> ctx) {
        ApocWorldGen.dimensionTypes(ctx);
    }

    @Override
    public void noises(BootstrapContext<NormalNoise.NoiseParameters> ctx) {
        ApocWorldGen.noises(ctx);
    }

    @Override
    public void noiseSettings(BootstrapContext<NoiseGeneratorSettings> ctx) {
        ApocWorldGen.noiseSettings(ctx);
    }

    @Override
    public void biomes(BootstrapContext<Biome> ctx) {
        ApocWorldGen.biomes(ctx);
    }

    @Override
    public void levelStems(BootstrapContext<LevelStem> ctx) {
        ApocWorldGen.levelStems(ctx);
    }

    @Override
    public void structures(BootstrapContext<Structure> ctx) {
        ApocWorldGen.structures(ctx);
    }

    @Override
    public void structureSets(BootstrapContext<StructureSet> ctx) {
        ApocWorldGen.structureSets(ctx);
    }
}
