package org.papiricoh.supernaturalcraft.leviathan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickBalance;

/**
 * The Leviathans' black goo on an arena floor (v0.25): a flat, unbreakable decal laid with {@code ArenaController.mutate}
 * (restored with the arena). Wading through it is slow and it burns, a little, every second — except for the Leviathans.
 * Borax dries it ({@link #DRIED}): a crust of white that is safe to stand on, until the goo surges back.
 */
public class LeviathanGooBlock extends Block {

    public static final BooleanProperty DRIED = BooleanProperty.create("dried");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public LeviathanGooBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DRIED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRIED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** Whether {@code e} is one of the goo's own (it neither slows nor burns them). */
    public static boolean ownGoo(Entity e) {
        return e instanceof BoraxSensitive;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(DRIED) || !(entity instanceof LivingEntity living) || ownGoo(entity)) return;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        entity.makeStuckInBlock(state, new Vec3(0.55, 1.0, 0.55));
        if (!level.isClientSide && living.tickCount % 20 == 0) living.hurt(level.damageSources().magic(), DickBalance.FLOOD_DAMAGE);
    }
}
