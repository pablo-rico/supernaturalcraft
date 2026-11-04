package org.papiricoh.supernaturalcraft.crypt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.hunter.wall.AlarmSigilBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.BanishingSigilBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.EnochianWardBlock;
import org.papiricoh.supernaturalcraft.hunter.wall.WallSigilBlock;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A sigil plate in the Enochian crypt (v0.36): a wall stone with the scratched outline of one wall sigil. Paint that sigil
 * on its face and the plate drinks it ({@link #SOLVED}, it glows) and the sealed door beside it ({@link SealedCryptDoorBlock}
 * within {@link #DOOR_REACH}) crumbles. The wrong sigil is just paint.
 */
public class SigilPlateBlock extends HorizontalDirectionalBlock {

    public static final com.mojang.serialization.MapCodec<SigilPlateBlock> CODEC = simpleCodec(SigilPlateBlock::new);
    public static final EnumProperty<Sigil> SIGIL = EnumProperty.create("sigil", Sigil.class);
    public static final BooleanProperty SOLVED = BooleanProperty.create("solved");
    public static final int DOOR_REACH = 4, MAX_DOOR = 32;

    public enum Sigil implements StringRepresentable {
        BANISHING, WARD, ALARM;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean matches(Block block) {
            return switch (this) {
                case BANISHING -> block instanceof BanishingSigilBlock;
                case WARD -> block instanceof EnochianWardBlock;
                case ALARM -> block instanceof AlarmSigilBlock;
            };
        }
    }

    public SigilPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIGIL, Sigil.BANISHING).setValue(SOLVED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIGIL, SOLVED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, from, moving);
        if (!(level instanceof ServerLevel server) || state.getValue(SOLVED)) return;
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing);
        BlockState painted = level.getBlockState(front);
        if (!(painted.getBlock() instanceof WallSigilBlock) || painted.getValue(WallSigilBlock.FACING) != facing
                || !state.getValue(SIGIL).matches(painted.getBlock())) return;
        solve(server, pos, state);
        // The plate drinks the paint (a banishing sigil never gets to blast).
        server.setBlock(front, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Marks the plate solved and opens the sealed door beside it. @return door blocks opened */
    public static int solve(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(SOLVED, true), Block.UPDATE_ALL);
        Vec3 c = Vec3.atCenterOf(pos);
        MagicFx.ward(level, School.ENOCHIAN, null, c, 3f, MagicFxPayload.WARD_RAISED, 30);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.4f);
        return openDoor(level, pos);
    }

    /** Crumbles the sealed door joined to any door block within {@link #DOOR_REACH} of {@code pos}. */
    public static int openDoor(ServerLevel level, BlockPos pos) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-DOOR_REACH, -DOOR_REACH, -DOOR_REACH), pos.offset(DOOR_REACH, DOOR_REACH, DOOR_REACH))) {
            if (level.getBlockState(p).getBlock() instanceof SealedCryptDoorBlock && seen.add(p.immutable())) queue.add(p.immutable());
        }
        int opened = 0;
        while (!queue.isEmpty() && opened < MAX_DOOR) {
            BlockPos p = queue.poll();
            level.destroyBlock(p, false);
            opened++;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (level.getBlockState(n).getBlock() instanceof SealedCryptDoorBlock && seen.add(n)) queue.add(n);
            }
        }
        return opened;
    }
}
