package org.papiricoh.supernaturalcraft.hands.reliquary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;

import java.util.Optional;

/**
 * A reliquary of one of the Hands of God (v0.42): one structure type with a {@code kind} (the Hand it keeps, by its reliquary's
 * name: {@code uboat_wreck}, {@code thule_bunker}, {@code church_crypt}, {@code ark_temple}, {@code snow_monastery}), and one
 * configured structure and structure set per kind. Finding the spot costs five height samples (it runs for {@code /locate}
 * and the tracking rite too), judged by the pure {@link ReliquarySites}; the plan is pure ({@link ReliquaryPlans}), decided by
 * the kind and the turn the site gives, and rebuilt by every chunk it touches ({@link ReliquaryPiece}).
 */
public class ReliquaryStructure extends Structure {

    public static final Codec<S11Assets.Hand> KIND_CODEC = Codec.STRING.comapFlatMap(s -> {
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (h.reliquary.equals(s)) return DataResult.success(h);
        return DataResult.error(() -> "Unknown reliquary kind " + s);
    }, h -> h.reliquary);

    public static final MapCodec<ReliquaryStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            KIND_CODEC.fieldOf("kind").forGetter(ReliquaryStructure::kind)
    ).apply(i, ReliquaryStructure::new));

    private final S11Assets.Hand kind;

    public ReliquaryStructure(StructureSettings settings, S11Assets.Hand kind) {
        super(settings);
        this.kind = kind;
    }

    public S11Assets.Hand kind() {
        return kind;
    }

    public static ResourceKey<Structure> key(S11Assets.Hand kind) {
        return ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource(kind.reliquary));
    }

    public static ResourceKey<StructureSet> set(S11Assets.Hand kind) {
        return ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource(kind.reliquary + "s"));
    }

    public static TagKey<Biome> biomes(S11Assets.Hand kind) {
        return TagKey.create(Registries.BIOME, SupernaturalCraft.asResource("has_structure/" + kind.reliquary));
    }

    /** Placement: about one per {@code spacing} x {@code spacing} chunks (rare: a few in ten thousand blocks), never closer than {@code separation}. */
    public record Spread(int spacing, int separation, int salt) {
    }

    public static Spread spread(S11Assets.Hand kind) {
        return switch (kind) {
            case DELPHINES_HAND -> new Spread(72, 28, 0x0B0A_7111);
            case NAIL_OF_THE_CROSS -> new Spread(64, 24, 0x0B0A_7222);
            case APOSTLES_CHALICE -> new Spread(72, 28, 0x0B0A_7333);
            case ROD_OF_AARON -> new Spread(64, 24, 0x0B0A_7444);
            case TRUMPET_OF_JERICHO -> new Spread(64, 24, 0x0B0A_7555);
        };
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ(), d = ReliquarySites.SAMPLE;
        int[][] at = {{0, 0}, {0, -d}, {d, 0}, {0, d}, {-d, 0}};
        Heightmap.Types map = kind == S11Assets.Hand.DELPHINES_HAND ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
        int[] tops = new int[at.length];
        for (int i = 0; i < at.length; i++) {
            tops[i] = ctx.chunkGenerator().getBaseHeight(x + at[i][0], z + at[i][1], map, ctx.heightAccessor(), ctx.randomState()) - 1;
        }
        long seed = ctx.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ spread(kind).salt();
        ReliquarySites.Site site = ReliquarySites.site(kind, tops, ctx.chunkGenerator().getSeaLevel(), seed);
        if (site == null) return Optional.empty();
        BlockPos origin = new BlockPos(x, site.y(), z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ReliquaryPiece(origin, kind, site.q(), seed))));
    }

    @Override
    public StructureType<?> type() {
        return HandsRegistry.RELIQUARY_TYPE.get();
    }
}
