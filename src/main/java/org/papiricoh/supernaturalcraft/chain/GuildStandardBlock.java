package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A guild's standard (v0.29): its seat. Placing a {@link GuildStandardItem} founds the guild or raises its standard again;
 * taking it down (by hand, fire or a raid) leaves the guild homeless ({@link GuildLife#uprooted}) and drops the standard with
 * its slot, to be raised again. {@link #SLOT} says whose it is (the colour is the guild's, tinted on the client), {@link #EMBLEM}
 * which emblem its cloth bears. No block entity: a standard is only these states.
 */
public class GuildStandardBlock extends HorizontalDirectionalBlock {
    public static final com.mojang.serialization.MapCodec<GuildStandardBlock> CODEC = simpleCodec(GuildStandardBlock::new);
    public static final IntegerProperty SLOT = IntegerProperty.create("slot", 0, ChainFaction.GUILDS.size() - 1);
    public static final IntegerProperty EMBLEM = IntegerProperty.create("emblem", 0, GuildRules.EMBLEMS - 1);
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);
    /** Set while the server takes a standard down itself (a guild dissolved): nothing drops. */
    private static boolean takingDown;

    public GuildStandardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOT, 0).setValue(EMBLEM, 0));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOT, EMBLEM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static ChainFaction slot(BlockState state) {
        return ChainFaction.GUILDS.get(state.getValue(SLOT));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && !takingDown) {
            ChainFaction slot = slot(state);
            if (GuildLife.uprooted(server.getServer(), slot, pos))
                popResource(level, pos, GuildStandardItem.of(ChainSavedData.get(server.getServer()).guild(slot)));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Puts the guild's emblem on its standing standard (if its chunk is loaded). */
    static void refresh(MinecraftServer server, Guild g) {
        ServerLevel level = GuildLife.standardLevel(server, g);
        BlockPos pos = new BlockPos(g.standardX, g.standardY, g.standardZ);
        if (level == null || !g.hasStandard || !level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GuildStandardBlock)
            level.setBlock(pos, state.setValue(SLOT, ChainFaction.GUILDS.indexOf(g.slot)).setValue(EMBLEM, g.emblem), Block.UPDATE_ALL);
    }

    /** A dissolved guild's standard comes down without a trace (if its chunk is loaded; a stale one elsewhere is inert). */
    static void takeDown(MinecraftServer server, Guild g) {
        ServerLevel level = GuildLife.standardLevel(server, g);
        BlockPos pos = new BlockPos(g.standardX, g.standardY, g.standardZ);
        if (level == null || !level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof GuildStandardBlock)) return;
        takingDown = true;
        try {
            level.removeBlock(pos, false);
        } finally {
            takingDown = false;
        }
    }
}
