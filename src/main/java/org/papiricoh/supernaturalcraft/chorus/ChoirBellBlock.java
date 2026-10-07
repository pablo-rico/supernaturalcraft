package org.papiricoh.supernaturalcraft.chorus;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks;

/**
 * One of the seven Choir Bells. Struck by hand or by an arrow it sounds its note, and any Choir
 * Altar within earshot listens. Creative-only: it cannot be broken or gathered in survival.
 * {@link #LIT} marks the bell the Chorus is singing in a Hymn.
 */
public class ChoirBellBlock extends Block {

    public static final MapCodec<ChoirBellBlock> CODEC = simpleCodec(p -> new ChoirBellBlock(0, p));
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** How far (blocks) an altar hears its bells. */
    public static final int EARSHOT = 16;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 2, 4, 12, 11, 12), Block.box(3, 2, 3, 13, 4, 13),
            Block.box(1, 13, 7, 15, 15, 9), Block.box(1, 0, 7, 3, 15, 9), Block.box(13, 0, 7, 15, 15, 9));

    public final int note;

    public ChoirBellBlock(int note, Properties properties) {
        super(properties);
        this.note = note;
        registerDefaultState(stateDefinition.any().setValue(RINGING, false).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RINGING, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) ring(server, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server) {
            ring(server, hit.getBlockPos(), projectile.getOwner() instanceof Player p ? p : null);
        }
    }

    /** Sounds the bell at {@code pos} and tells every altar (and any Chorus) in earshot. */
    public static boolean ring(ServerLevel level, BlockPos pos, @Nullable Player ringer) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChoirBellBlock bell)) return false;
        float pitch = Melody.pitch(bell.note);
        level.playSound(null, pos, org.papiricoh.supernaturalcraft.registry.AllSounds.CHOIR_BELL_RING.get(), SoundSource.BLOCKS, 2.0f, pitch);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2f, pitch);
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, bell.note / 24.0 * 3, 0, 0, 1);
        level.setBlock(pos, state.setValue(RINGING, true), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, bell, 16);
        for (ChoirAltarBlockEntity altar : altarsNear(level, pos)) altar.onBellRung(bell.note, pos, ringer);
        ChorusAttacks.onBellRung(level, pos, bell.note, ringer);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RINGING)) level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_CLIENTS);
    }

    static java.util.List<ChoirAltarBlockEntity> altarsNear(ServerLevel level, BlockPos pos) {
        java.util.List<ChoirAltarBlockEntity> out = new java.util.ArrayList<>();
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                chunk.getBlockEntities().values().forEach(be -> {
                    if (be instanceof ChoirAltarBlockEntity a && a.getBlockPos().distSqr(pos) <= EARSHOT * EARSHOT) out.add(a);
                });
            }
        }
        return out;
    }

    /** Every Choir Bell around {@code centre} (within earshot horizontally, a few blocks vertically). */
    public static java.util.List<BlockPos> bellsAround(Level level, BlockPos centre) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-EARSHOT, -4, -EARSHOT), centre.offset(EARSHOT, 6, EARSHOT))) {
            if (level.getBlockState(p).getBlock() instanceof ChoirBellBlock) out.add(p.immutable());
        }
        return out;
    }
}
