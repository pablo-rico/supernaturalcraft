package org.papiricoh.supernaturalcraft.haunted;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The haunted house in the world: where its points fall round an origin, and building one whole (commands, tests). The plan is
 * {@link HauntedHouseLayout}; worldgen goes through {@link HauntedHouseStructure} and {@link HauntedHousePiece}. The family's
 * bones are {@code supernaturalcraft:grave_bones} (they raise their ghosts at night like a graveyard's), each remembering the
 * house it belongs to — the absolute position of the study's chest spot ({@link GraveBonesBlockEntity#house()}).
 * Once every ghost of the family rests, the study's chest appears ({@link HauntedRest}).
 */
public final class HauntedHouse {

    private HauntedHouse() {
    }

    /** A point of the layout (as drawn) placed round {@code origin}. */
    public static BlockPos at(BlockPos origin, LayoutPoint p, long seed, int dir) {
        LayoutPoint q = HauntedHouseLayout.at(p, seed, dir);
        return origin.offset(q.x(), q.y(), q.z());
    }

    /** Where the study's chest appears (free floor in the world). */
    public static BlockPos studyChest(BlockPos origin, long seed, int dir) {
        return at(origin, HauntedHouseLayout.STUDY_CHEST, seed, dir);
    }

    /** Where the family's bones lie (2–4). */
    public static List<BlockPos> bones(BlockPos origin, long seed, int dir) {
        List<BlockPos> out = new ArrayList<>();
        for (LayoutPoint p : HauntedHouseLayout.bones(seed, dir)) out.add(origin.offset(p.x(), p.y(), p.z()));
        return out;
    }

    /** Builds a whole house round {@code origin} now (ground surface at {@code origin.y - 1}), bones and all. */
    public static void place(ServerLevel level, BlockPos origin, long seed, int dir) {
        LayoutPlan plan = HauntedHouseLayout.plan(seed, dir);
        BoundingBox box = PlanWriter.fullBox(origin, plan, HauntedHouseLayout.HEIGHT);
        PlanWriter.load(level, box);
        PlanWriter.build(level, box, origin, plan);
        layBones(level, box, origin, seed, dir);
    }

    /** Puts the family's bones (those inside {@code box}), each linked to the house's study chest spot. */
    public static void layBones(WorldGenLevel level, BoundingBox box, BlockPos origin, long seed, int dir) {
        BlockPos chest = studyChest(origin, seed, dir);
        for (BlockPos p : bones(origin, seed, dir)) {
            if (!box.isInside(p)) continue;
            level.setBlock(p, AllBlocks.GRAVE_BONES.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(p) instanceof GraveBonesBlockEntity be) be.setHouse(chest);
        }
    }
}
