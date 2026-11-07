package org.papiricoh.supernaturalcraft.apocworld.camp;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;

import java.util.Optional;

/**
 * Michael's outposts in the Apocalypse World (v0.41): prisoner camps and road checkpoints, one every so often across the burned
 * land. Finding the spot costs five height samples (the pattern of the graves and the natural crossroads: this runs for
 * {@code /locate} too), judged by the pure {@link CampSites#suitable}; the plan is pure ({@link CampPlan}) and decided by the
 * structure's seed, rebuilt by every chunk it touches ({@link CampPiece}).
 */
public class CampStructure extends Structure {

    public static final MapCodec<CampStructure> CODEC = simpleCodec(CampStructure::new);
    public static final ResourceKey<Structure> KEY = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("au_outpost"));
    public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, SupernaturalCraft.asResource("au_outposts"));
    /** Placement: about one per {@code SPACING} x {@code SPACING} chunks, never closer than {@code SEPARATION}. */
    public static final int SPACING = 22, SEPARATION = 9, SALT = 0x0A0C_A3F1;
    private static final int REACH = 14;

    public CampStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int[][] at = {{0, 0}, {-REACH, -REACH}, {REACH, -REACH}, {-REACH, REACH}, {REACH, REACH}};
        int[] tops = new int[at.length];
        for (int i = 0; i < at.length; i++) {
            tops[i] = ctx.chunkGenerator().getBaseHeight(x + at[i][0], z + at[i][1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(),
                    ctx.randomState()) - 1;
        }
        if (!CampSites.suitable(tops)) return Optional.empty();
        BlockPos origin = new BlockPos(x, tops[0] + 1, z);
        long seed = ctx.seed() ^ cp.toLong() * 0x9E3779B97F4A7C15L ^ SALT;
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CampPiece(origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ApocWorldRegistry.CAMP_TYPE.get();
    }
}
