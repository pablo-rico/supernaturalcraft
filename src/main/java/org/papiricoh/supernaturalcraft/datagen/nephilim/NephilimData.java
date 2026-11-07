package org.papiricoh.supernaturalcraft.datagen.nephilim;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.datagen.SNBlockStateProvider;
import org.papiricoh.supernaturalcraft.datagen.SNItemModelProvider;
import org.papiricoh.supernaturalcraft.datagen.slice.SliceData;
import org.papiricoh.supernaturalcraft.empty.EmptyBlocks;
import org.papiricoh.supernaturalcraft.empty.EmptyDimension;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimSounds;
import org.papiricoh.supernaturalcraft.nephilim.lance.LanceBane;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * v0.41 datagen hooks: Jack, the rift key, Asmodeus, season 14 (Michael's offer and vessel, soulless Jack), the Empty and the
 * Shadow. The English is in {@link NephilimLang}; the Spanish in {@code es_es.json}.
 */
public final class NephilimData implements SliceData {

    private static final PackType ASSETS = PackType.CLIENT_RESOURCES;
    private static final ExistingFileHelper.ResourceType TEXTURE = new ExistingFileHelper.ResourceType(ASSETS, ".png", "textures");

    /** Vanilla stand-ins for the core's sounds until they are encoded: {event, pitch}. */
    static final Map<String, Object[]> FALLBACK = Map.ofEntries(
            Map.entry("asmodeus.cane", new Object[]{"entity.player.attack.strong", 0.8f}),
            Map.entry("asmodeus.slam", new Object[]{"entity.generic.explode", 0.7f}),
            Map.entry("asmodeus.grace", new Object[]{"block.beacon.power_select", 1.2f}),
            Map.entry("asmodeus.summon", new Object[]{"entity.evoker.prepare_summon", 0.8f}),
            Map.entry("asmodeus.hurt", new Object[]{"entity.evoker.hurt", 0.7f}),
            Map.entry("asmodeus.death", new Object[]{"entity.evoker.death", 0.6f}),
            Map.entry("asmodeus.roar", new Object[]{"entity.evoker.celebrate", 0.6f}),
            Map.entry("michael_vessel.snap", new Object[]{"block.note_block.hat", 0.6f}),
            Map.entry("michael_vessel.choke", new Object[]{"entity.warden.heartbeat", 1.4f}),
            Map.entry("michael_vessel.smite", new Object[]{"block.beacon.power_select", 0.7f}),
            Map.entry("michael_vessel.flare", new Object[]{"entity.phantom.flap", 0.5f}),
            Map.entry("michael_vessel.hurt", new Object[]{"entity.iron_golem.hurt", 1.3f}),
            Map.entry("michael_vessel.expelled", new Object[]{"block.beacon.deactivate", 0.6f}),
            Map.entry("michael_vessel.death", new Object[]{"block.beacon.deactivate", 0.5f}),
            Map.entry("jack.smite", new Object[]{"block.beacon.power_select", 1.5f}),
            Map.entry("jack.shield", new Object[]{"item.shield.block", 0.8f}),
            Map.entry("jack.burn", new Object[]{"item.totem.use", 0.8f}),
            Map.entry("jack.snap", new Object[]{"block.note_block.hat", 0.9f}),
            Map.entry("jack.nova", new Object[]{"entity.warden.sonic_boom", 1.2f}),
            Map.entry("jack.hurt", new Object[]{"entity.player.hurt", 1.1f}),
            Map.entry("jack.death", new Object[]{"block.beacon.deactivate", 1.2f}),
            Map.entry("shadow.lash", new Object[]{"entity.warden.attack_impact", 0.7f}),
            Map.entry("shadow.engulf", new Object[]{"block.sculk.spread", 0.5f}),
            Map.entry("shadow.lull", new Object[]{"block.sculk_shrieker.shriek", 0.4f}),
            Map.entry("shadow.roar", new Object[]{"entity.warden.roar", 0.6f}),
            Map.entry("shadow.hurt", new Object[]{"entity.warden.hurt", 0.8f}),
            Map.entry("shadow.death", new Object[]{"entity.warden.death", 0.6f}),
            Map.entry("shadow.emerge", new Object[]{"entity.warden.emerge", 0.8f}),
            Map.entry("shadow.ambient", new Object[]{"ambient.soul_sand_valley.mood", 0.6f}));

    private static ResourceLocation pending(ExistingFileHelper existing, ResourceLocation tex) {
        if (!existing.exists(tex, TEXTURE)) existing.trackGenerated(tex, TEXTURE);
        return tex;
    }

    private static final List<DeferredBlock<?>> TROPHIES = List.of(NephilimRegistry.ASMODEUS_TROPHY, NephilimRegistry.MICHAEL_VESSEL_TROPHY,
            NephilimRegistry.SOULLESS_JACK_TROPHY, NephilimRegistry.SHADOW_TROPHY);

    // --- text --------------------------------------------------------------------------------------------------------

    @Override
    public Map<String, String> names() {
        return NephilimLang.NAMES;
    }

    @Override
    public void lang(BiConsumer<String, String> add) {
        NephilimLang.add(add);
    }

    @Override
    public Map<String, String> subtitles() {
        return NephilimLang.SUBTITLES;
    }

    @Override
    public void sounds(ExistingFileHelper existing, BiConsumer<DeferredHolder<SoundEvent, SoundEvent>, SoundDefinition> add) {
        NephilimSounds.all().forEach((event, holder) -> {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalCraft.MODID, event.replace('.', '/'));
            SoundDefinition def = SoundDefinition.definition().subtitle("subtitles.supernaturalcraft." + event);
            if (existing.exists(file, ASSETS, ".ogg", "sounds")) {
                def.with(SoundDefinition.Sound.sound(file, SoundDefinition.SoundType.SOUND).attenuationDistance(24));
            } else {
                Object[] f = FALLBACK.getOrDefault(event, new Object[]{"entity.vex.ambient", 0.8f});
                def.with(SoundDefinition.Sound.sound(ResourceLocation.withDefaultNamespace((String) f[0]), SoundDefinition.SoundType.EVENT)
                        .pitch((Float) f[1]));
            }
            add.accept(holder, def);
        });
    }

    // --- models --------------------------------------------------------------------------------------------------------

    @Override
    public void blockStates(SNBlockStateProvider p, ExistingFileHelper existing) {
        for (DeferredBlock<?> trophy : TROPHIES) {
            p.horizontalBlock(trophy.get(), new ModelFile.UncheckedModelFile(p.modLoc("block/" + trophy.getId().getPath())));
        }
        p.simpleBlock(NephilimRegistry.EMPTY_FLOOR.get(), p.models().cubeAll("empty_floor", pending(existing, p.modLoc("block/empty_floor"))));
        p.simpleBlock(NephilimRegistry.EMPTY_TAR.get(), p.models().withExistingParent("empty_tar", p.mcLoc("block/carpet"))
                .texture("wool", pending(existing, p.modLoc("block/empty_tar"))));
        ModelFile head = new ModelFile.UncheckedModelFile(p.modLoc("block/empty_sleeper_head"));
        ModelFile foot = new ModelFile.UncheckedModelFile(p.modLoc("block/empty_sleeper_foot"));
        p.getVariantBuilder(NephilimRegistry.EMPTY_SLEEPER.get()).forAllStates(state -> {
            int y = ((int) state.getValue(EmptyBlocks.Sleeper.FACING).toYRot() + 180) % 360;
            return ConfiguredModel.builder().modelFile(state.getValue(EmptyBlocks.Sleeper.PART) == BedPart.HEAD ? head : foot).rotationY(y).build();
        });
    }

    @Override
    public void itemModels(SNItemModelProvider p, ExistingFileHelper existing) {
        for (DeferredItem<? extends Item> flat : List.of(NephilimRegistry.NEPHILIM_GRACE, NephilimRegistry.NEPHILIM_BLOOD)) {
            pending(existing, p.modLoc("item/" + flat.getId().getPath()));
            p.basicItem(flat.get());
        }
        for (DeferredItem<? extends Item> block : List.of(NephilimRegistry.ASMODEUS_TROPHY_ITEM, NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM,
                NephilimRegistry.SOULLESS_JACK_TROPHY_ITEM, NephilimRegistry.SHADOW_TROPHY_ITEM, NephilimRegistry.EMPTY_FLOOR_ITEM,
                NephilimRegistry.EMPTY_TAR_ITEM)) {
            p.getBuilder(block.getId().getPath()).parent(new ModelFile.UncheckedModelFile(p.modLoc("block/" + block.getId().getPath())));
        }
        for (DeferredItem<? extends Item> egg : List.of(NephilimRegistry.ASMODEUS_SPAWN_EGG, NephilimRegistry.MICHAEL_VESSEL_SPAWN_EGG,
                NephilimRegistry.SOULLESS_JACK_SPAWN_EGG, NephilimRegistry.SHADOW_SPAWN_EGG)) {
            p.withExistingParent(egg.getId().getPath(), p.mcLoc("item/template_spawn_egg"));
        }
    }

    // --- loot and tags -------------------------------------------------------------------------------------------------

    @Override
    public void entityLoot(BiConsumer<EntityType<?>, LootTable.Builder> add) {
        // Their spoils are given per fighter by code (SeasonBossEntity.spoils); Jack and Michael's envoy leave nothing.
        for (EntityType<?> t : List.of(NephilimRegistry.ASMODEUS.get(), NephilimRegistry.MICHAEL_VESSEL.get(), NephilimRegistry.SOULLESS_JACK.get(),
                NephilimRegistry.SHADOW.get(), NephilimRegistry.JACK.get(), NephilimRegistry.MICHAEL_ENVOY.get())) {
            add.accept(t, LootTable.lootTable());
        }
    }

    @Override
    public void blockLoot(Consumer<Block> dropSelf, BiConsumer<Block, LootTable.Builder> add) {
        for (DeferredBlock<?> trophy : TROPHIES) dropSelf.accept(trophy.get());
        dropSelf.accept(NephilimRegistry.EMPTY_FLOOR.get());
        dropSelf.accept(NephilimRegistry.EMPTY_TAR.get());
    }

    @Override
    public void blockTags(BiConsumer<TagKey<Block>, Block> add) {
        for (DeferredBlock<?> trophy : TROPHIES) add.accept(BlockTags.MINEABLE_WITH_PICKAXE, trophy.get());
        add.accept(BlockTags.MINEABLE_WITH_PICKAXE, NephilimRegistry.EMPTY_FLOOR.get());
    }

    @Override
    public void entityTags(BiConsumer<TagKey<EntityType<?>>, EntityType<?>> add) {
        for (EntityType<?> boss : List.of(NephilimRegistry.ASMODEUS.get(), NephilimRegistry.MICHAEL_VESSEL.get(), NephilimRegistry.SOULLESS_JACK.get(),
                NephilimRegistry.SHADOW.get())) {
            add.accept(AllTags.Entities.BOSSES, boss);
            add.accept(AllTags.Entities.SUPERNATURAL, boss);
        }
        add.accept(AllTags.Entities.DEMONS, NephilimRegistry.ASMODEUS.get());
        // The archangels, for the Lance made whole (AU Michael is added by the Apocalypse World slice).
        for (EntityType<?> a : List.of(AllEntities.MICHAEL.get(), AllEntities.LUCIFER.get(), AllEntities.LUCIFER_UNCAGED.get(),
                AllEntities.RAPHAEL.get(), AllEntities.GABRIEL.get(), NephilimRegistry.MICHAEL_VESSEL.get())) {
            add.accept(LanceBane.ARCHANGELS, a);
        }
        add.accept(AllTags.Entities.SUPERNATURAL, NephilimRegistry.JACK.get());
    }

    // --- the Empty (datapack entries) ----------------------------------------------------------------------------------

    @Override
    public void dimensionTypes(BootstrapContext<DimensionType> ctx) {
        ctx.register(EmptyDimension.TYPE, new DimensionType(
                OptionalLong.of(EmptyDimension.FIXED_TIME), // always midnight
                false,                      // no sky light
                false,                      // no roof
                false,                      // not ultrawarm
                false,                      // not natural: compasses spin, beds explode
                1.0,
                false,                      // no beds
                false,                      // no anchors
                EmptyDimension.MIN_Y, EmptyDimension.HEIGHT, EmptyDimension.HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD,
                EmptyDimension.EFFECTS,
                0.3f,                       // enough to see the floor and the sleepers in the dark
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)));
    }

    @Override
    public void biomes(BootstrapContext<Biome> ctx) {
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(0x0A0610).skyColor(0x000000).waterColor(0x10081A).waterFogColor(0x05030A)
                .ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.004f))
                .build();
        ctx.register(EmptyDimension.DARK, new Biome.BiomeBuilder().hasPrecipitation(false).temperature(0.5f).downfall(0.0f)
                .specialEffects(effects).mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.PlainBuilder().build()).build());
    }

    @Override
    public void levelStems(BootstrapContext<LevelStem> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = ctx.lookup(Registries.DIMENSION_TYPE);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
                biomes.getOrThrow(EmptyDimension.DARK), List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        settings.updateLayers();
        ctx.register(EmptyDimension.STEM, new LevelStem(types.getOrThrow(EmptyDimension.TYPE),
                new net.minecraft.world.level.levelgen.FlatLevelSource(settings)));
    }
}
