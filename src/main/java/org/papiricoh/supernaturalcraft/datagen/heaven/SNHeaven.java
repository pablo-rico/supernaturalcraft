package org.papiricoh.supernaturalcraft.datagen.heaven;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.sounds.Music;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Heaven as datapack entries (v0.18): a void at a fixed noon, one garden biome, no structures and no features. Every plot is
 * written by code ({@code heaven.plot.PlotWriter}); nothing here generates a block.
 */
public final class SNHeaven {

    private SNHeaven() {
    }

    public static void bootstrapType(BootstrapContext<DimensionType> ctx) {
        ctx.register(HeavenDimension.TYPE, new DimensionType(
                OptionalLong.of(6000L),     // a fixed noon: Heaven never darkens
                true,                       // sky light
                false,                      // no roof
                false,                      // not ultrawarm
                true,                       // natural: compasses point, clocks turn
                1.0,                        // one block is one block
                true,                       // beds do not explode (you cannot sleep at noon either: the hearth is for resting)
                false,                      // no respawn anchors
                HeavenDimension.MIN_Y, HeavenDimension.HEIGHT, HeavenDimension.HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD,
                HeavenDimension.EFFECTS,
                0.15f,                      // a little light everywhere, even in shade
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)));
    }

    public static void bootstrapBiomes(BootstrapContext<Biome> ctx) {
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .fogColor(0xF4EEDC).skyColor(0xBFD9FF).waterColor(0x7FC4F0).waterFogColor(0xBFE3FF)
                .grassColorOverride(0x8FD06A).foliageColorOverride(0x7CC25A)
                .ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0008f))
                .backgroundMusic(new Music(AllSounds.HEAVEN_SOUNDS.get("music.heaven"), 1200, 6000, true))
                .build();
        ctx.register(HeavenDimension.GARDEN, new Biome.BiomeBuilder().hasPrecipitation(false).temperature(0.7f).downfall(0.0f)
                .specialEffects(effects).mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.PlainBuilder().build()).build());
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<DimensionType> types = ctx.lookup(Registries.DIMENSION_TYPE);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
                biomes.getOrThrow(HeavenDimension.GARDEN), List.of());
        settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        settings.updateLayers();
        ctx.register(HeavenDimension.STEM, new LevelStem(types.getOrThrow(HeavenDimension.TYPE), new FlatLevelSource(settings)));
    }
}
