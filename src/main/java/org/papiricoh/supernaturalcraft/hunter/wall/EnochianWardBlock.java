package org.papiricoh.supernaturalcraft.hunter.wall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;
import org.papiricoh.supernaturalcraft.prophecy.Tablet;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * The Enochian warding sigil (v0.36): no angel but a boss walks, blinks or calls on its grace inside its sphere
 * ({@link WardGeometry}), and the angels outside lose sight of whoever shelters in it ({@link WardEvents}). Painted by one
 * who read the second chapter of the Angel Tablet it is {@link #SUPREME}: twice the reach, and it burns what it pushes out.
 */
public class EnochianWardBlock extends WallSigilBlock {

    public static final BooleanProperty SUPREME = BooleanProperty.create("supreme");

    public EnochianWardBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SUPREME, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SUPREME);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) return null;
        return state.setValue(SUPREME, ctx.getPlayer() != null && Prophecies.read(ctx.getPlayer(), Tablet.ANGEL) >= Tablet.SUPREME_WARD_CHAPTER);
    }

    public static int radius(BlockState state) {
        return WardGeometry.radius(state.getValue(SUPREME));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level instanceof ServerLevel server && !old.is(this)) {
            WardIndex.get(server).add(pos, radius(state));
            MagicFx.ward(server, School.ENOCHIAN, null, Vec3.atCenterOf(pos), radius(state), MagicFxPayload.WARD_RAISED, 30);
            server.playSound(null, pos, org.papiricoh.supernaturalcraft.registry.AllSounds.WARD_HUM.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel server && !newState.is(this)) {
            WardIndex.get(server).remove(pos);
            MagicFx.ward(server, School.ENOCHIAN, null, Vec3.atCenterOf(pos), radius(state), MagicFxPayload.WARD_BROKEN, 20);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(SUPREME) ? 3 : 6) != 0) return;
        Vec3 c = Vec3.atCenterOf(pos).relative(state.getValue(FACING), -0.4);
        level.addParticle(AllParticles.GRACE.get(), c.x + (random.nextDouble() - 0.5) * 0.8, c.y + (random.nextDouble() - 0.5) * 0.8,
                c.z + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
    }
}
