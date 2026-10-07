package org.papiricoh.supernaturalcraft.hell.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.papiricoh.supernaturalcraft.registry.AllWorldgen;

import java.util.Optional;

/** A structure that exists exactly once, starting in one chosen chunk (Lucifer's Cage, at the origin). */
public class FixedPlacement extends StructurePlacement {

    public static final MapCodec<FixedPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("chunk_x").forGetter(p -> p.chunkX),
            Codec.INT.fieldOf("chunk_z").forGetter(p -> p.chunkZ)
    ).apply(i, FixedPlacement::new));

    private final int chunkX, chunkZ;

    public FixedPlacement(int chunkX, int chunkZ) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, 0, Optional.empty());
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return x == chunkX && z == chunkZ;
    }

    @Override
    public StructurePlacementType<?> type() {
        return AllWorldgen.FIXED_PLACEMENT.get();
    }
}
