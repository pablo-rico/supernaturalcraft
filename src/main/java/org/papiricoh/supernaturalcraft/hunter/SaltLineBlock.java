package org.papiricoh.supernaturalcraft.hunter;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.ritual.block.FlatLineBlock;

/**
 * A line of salt: harmless to walk over, an invisible wall to anything tagged as a demon. The
 * wall is a collision shape (so it holds against knockback and jumping) and a blocked path type
 * (so demon AI routes around instead of pressing against it).
 */
public class SaltLineBlock extends FlatLineBlock {

    public static final MapCodec<SaltLineBlock> CODEC = simpleCodec(SaltLineBlock::new);
    private static final VoxelShape WARD = Block.box(0, 0, 0, 16, 32, 16);

    public SaltLineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SaltLineBlock> codec() {
        return CODEC;
    }

    public static boolean isWarded(@Nullable Entity entity) {
        return entity != null && entity.getType().is(AllTags.Entities.DEMONS);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ec && isWarded(ec.getEntity())) {
            return WARD;
        }
        return Shapes.empty();
    }

    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return isWarded(mob) ? PathType.BLOCKED : null;
    }
}
