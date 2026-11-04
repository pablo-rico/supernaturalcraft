package org.papiricoh.supernaturalcraft.hunter.wall;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.List;

/**
 * The alarm sigil (v0.36): it watches {@link #RADIUS} blocks round it every {@link #CHECK_TICKS} and, while a demon, an
 * angel or one of this world's monsters is there, it is {@link #LIT}: it glows and chimes, gives a redstone signal (15, also
 * through the block it is painted on), marks the intruder with Glowing and tells the one who painted it in their action
 * bar, wherever they are in the same dimension. Chime and message wait {@link #COOLDOWN} between two alarms. Its painter
 * never sets it off.
 */
public class AlarmSigilBlock extends WallSigilBlock implements EntityBlock {

    public static final MapCodec<AlarmSigilBlock> CODEC = simpleCodec(AlarmSigilBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int RADIUS = 8, CHECK_TICKS = 10, COOLDOWN = 60, MARK_TICKS = 100;

    public AlarmSigilBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends AlarmSigilBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlarmSigilBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof AlarmSigilBlockEntity be) be.setOwner(p.getUUID());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !old.is(this)) level.scheduleTick(pos, this, CHECK_TICKS);
    }

    /** Whether {@code e} sets the alarm off. */
    public static boolean intruder(LivingEntity e, @Nullable java.util.UUID owner) {
        if (!e.isAlive() || e.getUUID().equals(owner) || e.isSpectator()) return false;
        if (Kin.isDemon(e) || Kin.isAngel(e)) return true;
        return e instanceof Enemy && e.getType().builtInRegistryHolder().key().location().getNamespace().equals(SupernaturalCraft.MODID);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof AlarmSigilBlockEntity be)) return;
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS),
                e -> intruder(e, be.owner()) && e.distanceToSqr(pos.getCenter()) <= RADIUS * RADIUS);
        boolean lit = !found.isEmpty();
        if (lit != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.updateNeighborsAt(support(pos, state), this);
        }
        if (lit) {
            for (LivingEntity e : found) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_TICKS, 0, false, false, true));
            long now = level.getGameTime();
            if (now >= be.quietUntil()) {
                be.setQuietUntil(now + COOLDOWN);
                level.playSound(null, pos, AllSounds.ALARM_CHIME.get(), SoundSource.BLOCKS, 1.5f, 1f);
                Player owner = Players.byUuid(level, be.owner());
                if (owner instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("message.supernaturalcraft.alarm_sigil.tripped",
                            found.get(0).getDisplayName(), pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD), true);
                }
            }
        }
        level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(LIT) && direction == state.getValue(FACING) ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        boolean wasLit = state.getValue(LIT);
        super.onRemove(state, level, pos, newState, moved);
        if (wasLit && !newState.is(this)) level.updateNeighborsAt(support(pos, state), this);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(2) != 0) return;
        var c = pos.getCenter().relative(state.getValue(FACING), -0.4);
        level.addParticle(AllParticles.GRACE.get(), c.x + (random.nextDouble() - 0.5) * 0.7, c.y + (random.nextDouble() - 0.5) * 0.7,
                c.z + (random.nextDouble() - 0.5) * 0.7, 0, 0.02, 0);
    }
}
