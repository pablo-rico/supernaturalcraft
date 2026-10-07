package org.papiricoh.supernaturalcraft.hunter;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The devil's trap: a 3×3 painted sigil. Each block is one ninth of the drawing ({@link #PART}
 * 0–8, row-major, 4 is the centre). Any demon standing on any part is held in place.
 */
public class DevilsTrapBlock extends Block {

    public static final MapCodec<DevilsTrapBlock> CODEC = simpleCodec(DevilsTrapBlock::new);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final int CENTER = 4;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);

    public DevilsTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, CENTER));
    }

    @Override
    protected MapCodec<? extends DevilsTrapBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    /** Offset of {@code part} from the centre block, as (dx, dz). */
    public static int dx(int part) {
        return part % 3 - 1;
    }

    public static int dz(int part) {
        return part / 3 - 1;
    }

    public static BlockPos center(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        return pos.offset(-dx(part), 0, -dz(part));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        // A trap with a missing ninth is a broken seal: the whole drawing fades.
        if (dir.getAxis().isHorizontal() && !neighbor.is(this) && expectsPartAt(state, dir)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    private static boolean expectsPartAt(BlockState state, Direction dir) {
        int part = state.getValue(PART);
        int nx = dx(part) + dir.getStepX();
        int nz = dz(part) + dir.getStepZ();
        return nx >= -1 && nx <= 1 && nz >= -1 && nz <= 1;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Only the centre drops the item, so breaking any part breaks the centre too.
        BlockPos center = center(pos, state);
        if (!center.equals(pos) && level.getBlockState(center).is(this)) {
            level.destroyBlock(center, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.getType().is(AllTags.Entities.DEMONS)) {
            living.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, 40, 0, false, true, true));
        }
    }
}
